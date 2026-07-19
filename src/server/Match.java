package server;

import engine.GameEngine;
import engine.GameSnapshot;
import model.GameConstants;
import net.SnapshotCodec;
import net.WebSocketConnection;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One live game between exactly two already-matched players: its own board, its own
 * broadcast loop, its own ELO update when it ends. GameServer is the lobby that creates
 * one of these once matchmaking pairs two waiting players - many Matches can run at once.
 */
public class Match {
    private static final int BOARD_SIZE = 8;
    private static final long TICK_MS = 100;
    private static final long DEFAULT_RECONNECT_WINDOW_MS = 20_000;

    private final GameEngine engine = new GameEngine();
    private final AccountStore accountStore;
    private final long reconnectWindowMs;
    private volatile WebSocketConnection whiteConn;
    private volatile WebSocketConnection blackConn;
    private final String whiteName;
    private final String blackName;
    private volatile int whiteElo;
    private volatile int blackElo;
    private final Object lock = new Object();
    private boolean eloApplied = false;
    private volatile boolean active = true;
    /** Read-only onlookers watching this match by its room code - they get every STATE/NAMES
     *  broadcast the players do, but never a COLOR, and any MOVE/JUMP they send is ignored
     *  (GameServer never even routes it here, since spectators aren't in matchOf). */
    private final List<WebSocketConnection> spectators = new CopyOnWriteArrayList<>();

    /** Set while one side is disconnected and the other is waiting out the reconnect window;
     *  null again once resolved (reconnect, or the window expires and it's auto-forfeited). */
    private String disconnectedColor = null;
    private long disconnectedAt = 0;

    public Match(AccountStore accountStore,
                 WebSocketConnection whiteConn, String whiteName, int whiteElo,
                 WebSocketConnection blackConn, String blackName, int blackElo) {
        this(accountStore, whiteConn, whiteName, whiteElo, blackConn, blackName, blackElo,
                DEFAULT_RECONNECT_WINDOW_MS);
    }

    /** Lets tests shrink the reconnect window instead of waiting 20 real seconds. */
    public Match(AccountStore accountStore,
                 WebSocketConnection whiteConn, String whiteName, int whiteElo,
                 WebSocketConnection blackConn, String blackName, int blackElo,
                 long reconnectWindowMs) {
        this.accountStore = accountStore;
        this.whiteConn = whiteConn;
        this.whiteName = whiteName;
        this.whiteElo = whiteElo;
        this.blackConn = blackConn;
        this.blackName = blackName;
        this.blackElo = blackElo;
        this.reconnectWindowMs = reconnectWindowMs;
        engine.reset(startingPosition());
    }

    public WebSocketConnection whiteConnection() { return whiteConn; }
    public WebSocketConnection blackConnection() { return blackConn; }

    /** Sends the initial COLOR/NAMES/STATE and starts ticking. Called once both connections
     *  are known, right after GameServer creates this Match. */
    public void start() {
        trySend(whiteConn, "COLOR white");
        trySend(blackConn, "COLOR black");
        broadcastNames();
        broadcastState();

        Thread loop = new Thread(this::runLoop);
        loop.start(); // deliberately non-daemon while active - see GameServer's matchmaking loop
    }

    private void runLoop() {
        long last = System.currentTimeMillis();
        while (active) {
            long now = System.currentTimeMillis();
            long dt = now - last;
            last = now;

            synchronized (lock) {
                if (disconnectedColor != null) {
                    handleDisconnectCountdown(now);
                } else {
                    engine.advanceTime(dt);
                }
                if (engine.gameOver && !eloApplied && engine.winner != null) {
                    eloApplied = true;
                    applyEloForGameEnd();
                }
            }
            broadcastState();

            try { Thread.sleep(TICK_MS); } catch (InterruptedException ignored) { }
        }
    }

    /** Caller must hold {@code lock}. Game time is paused while a side is gone - it wouldn't
     *  be fair for pieces to keep resolving moves nobody's there to see - and the remaining
     *  player gets a running countdown until the disconnected side auto-forfeits. */
    private void handleDisconnectCountdown(long now) {
        long elapsed = now - disconnectedAt;
        if (elapsed >= reconnectWindowMs) {
            engine.gameOver = true;
            engine.winner = disconnectedColor.equals("white") ? "black" : "white";
            return;
        }
        WebSocketConnection remaining = disconnectedColor.equals("white") ? blackConn : whiteConn;
        long secondsLeft = (reconnectWindowMs - elapsed + 999) / 1000;
        trySend(remaining, "OPPONENT_DISCONNECTED " + secondsLeft);
    }

    /** Every command is checked against the sender's assigned color - real time chess has no
     *  turn order, so without this a client could move its opponent's pieces just by sending
     *  the right square. */
    public void handleMessage(WebSocketConnection connection, String[] parts) {
        char myPrefix = (connection == whiteConn) ? 'w' : 'b';

        if (parts.length == 2 && parts[0].equalsIgnoreCase("MOVE") && parts[1].length() == 4) {
            int[] from = squareToRowCol(parts[1].substring(0, 2));
            int[] to = squareToRowCol(parts[1].substring(2, 4));
            synchronized (lock) {
                if (!ownsPieceAt(from, myPrefix)) {
                    trySend(connection, "REJECTED wrong_color");
                    return;
                }
                engine.requestMove(from[0], from[1], to[0], to[1]);
            }
        } else if (parts.length == 2 && parts[0].equalsIgnoreCase("JUMP") && parts[1].length() == 2) {
            int[] at = squareToRowCol(parts[1]);
            synchronized (lock) {
                if (!ownsPieceAt(at, myPrefix)) {
                    trySend(connection, "REJECTED wrong_color");
                    return;
                }
                engine.requestJump(at[0], at[1]);
            }
        }
    }

    /** Called by GameServer when either side's connection drops. Doesn't end the match -
     *  starts a reconnect countdown instead; the other side is notified every tick via
     *  "OPPONENT_DISCONNECTED <secondsLeft>" until either reconnect() succeeds or the window
     *  runs out and the disconnected side is auto-forfeited. */
    public void onPlayerDisconnected(WebSocketConnection connection) {
        synchronized (lock) {
            if (eloApplied) return; // already decided, nothing to do
            disconnectedColor = (connection == whiteConn) ? "white" : "black";
            disconnectedAt = System.currentTimeMillis();
        }
    }

    /** Called by GameServer when the same username logs back in while this match is still
     *  waiting out a reconnect countdown. Returns false if there's nothing to reconnect to
     *  any more (the window already expired and the match was decided) - the caller should
     *  fall back to treating this as a fresh login in that case. */
    public boolean reconnect(WebSocketConnection newConnection) {
        String reconnectedColor;
        synchronized (lock) {
            if (disconnectedColor == null || eloApplied) return false;
            reconnectedColor = disconnectedColor;
            if (reconnectedColor.equals("white")) whiteConn = newConnection;
            else blackConn = newConnection;
            disconnectedColor = null;
        }

        trySend(newConnection, "COLOR " + reconnectedColor);
        trySend(reconnectedColor.equals("white") ? blackConn : whiteConn, "OPPONENT_RECONNECTED");
        broadcastNames();
        broadcastState();
        return true;
    }

    /** True once this match has been decided (normal win, or an auto-forfeit) - lets
     *  GameServer clean up any stale reconnect bookkeeping for a match nobody came back to. */
    public boolean isResolved() {
        synchronized (lock) {
            return eloApplied;
        }
    }

    /** Called by GameServer once a WATCH_ROOM lookup finds this match. Immediately catches the
     *  new spectator up on the current names and board, instead of making it wait for the next
     *  100ms tick. */
    public void addSpectator(WebSocketConnection connection) {
        spectators.add(connection);
        trySend(connection, "SPECTATING");
        String namesMessage;
        GameSnapshot snapshot;
        synchronized (lock) {
            namesMessage = "NAMES " + whiteName + ":" + whiteElo + "|" + blackName + ":" + blackElo;
            snapshot = engine.snapshot(-1, -1);
        }
        trySend(connection, namesMessage);
        trySend(connection, "STATE\n" + SnapshotCodec.encode(snapshot));
    }

    public void removeSpectator(WebSocketConnection connection) {
        spectators.remove(connection);
    }

    /** Caller must hold {@code lock}. */
    private boolean ownsPieceAt(int[] rowCol, char colorPrefix) {
        String piece = engine.board[rowCol[0]][rowCol[1]];
        return piece.length() >= 2 && piece.charAt(0) == colorPrefix;
    }

    /** Caller must hold {@code lock}. */
    private void applyEloForGameEnd() {
        boolean whiteWon = "white".equals(engine.winner);
        String winnerName = whiteWon ? whiteName : blackName;
        String loserName = whiteWon ? blackName : whiteName;
        AccountStore.EloUpdate update = accountStore.applyGameResult(winnerName, loserName);

        if (whiteWon) {
            whiteElo = update.winnerElo;
            blackElo = update.loserElo;
        } else {
            blackElo = update.winnerElo;
            whiteElo = update.loserElo;
        }
        broadcastNames();
    }

    private void broadcastState() {
        GameSnapshot snapshot;
        synchronized (lock) {
            snapshot = engine.snapshot(-1, -1);
        }
        String state = "STATE\n" + SnapshotCodec.encode(snapshot);
        trySend(whiteConn, state);
        trySend(blackConn, state);
        for (WebSocketConnection spectator : spectators) trySend(spectator, state);
    }

    private void broadcastNames() {
        String message = "NAMES " + whiteName + ":" + whiteElo + "|" + blackName + ":" + blackElo;
        trySend(whiteConn, message);
        trySend(blackConn, message);
        for (WebSocketConnection spectator : spectators) trySend(spectator, message);
    }

    private void trySend(WebSocketConnection connection, String text) {
        try {
            connection.sendText(text);
        } catch (IOException ignored) { }
    }

    /** "e2" -> {row, col}. File a-h -> col 0-7. Rank 1-8 -> row 7-0 (rank 8 is row 0, matching
     *  the board arrays used throughout this project, where row 0 is Black's back rank). */
    private static int[] squareToRowCol(String square) {
        int col = square.charAt(0) - 'a';
        int rank = square.charAt(1) - '0';
        int row = BOARD_SIZE - rank;
        return new int[] { row, col };
    }

    private static String[][] startingPosition() {
        return new String[][] {
                {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"},
                {"bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"},
                {GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY,
                        GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY},
                {GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY,
                        GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY},
                {GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY,
                        GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY},
                {GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY,
                        GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY, GameConstants.EMPTY},
                {"wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"},
                {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"},
        };
    }
}
