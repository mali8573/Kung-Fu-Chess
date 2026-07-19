package server;

import net.WebSocketConnection;
import net.WebSocketFrame;
import net.WebSocketServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Lobby server: players log in, then click "Play" to join the matchmaking queue. Two
 * waiting players are paired once their ELO is within MAX_ELO_DIFF of each other; a
 * player who waits past the matchmaking timeout with nobody suitable gets "NO_MATCH" and
 * has to click Play again. Once paired, a Match (its own board, its own broadcast loop) is
 * created just for that pair - many matches can run at once, unlike the single shared game
 * this server used to hold.
 *
 * Wire protocol (our own, deliberately simple):
 *   client -> server, once, right after connecting: "LOGIN <username> <password>" - a new
 *     username is registered on the spot with that password and starting ELO; an existing
 *     username must match its stored password
 *   server -> client, on a bad password: "LOGIN_FAILED wrong_password"
 *   server -> client, if that username is already connected right now: "LOGIN_FAILED already_logged_in"
 *   server -> client, once login succeeds: "LOGIN_OK <elo>"
 *   client -> server, to join matchmaking: "PLAY"
 *   server -> client, once queued: "SEARCHING"
 *   server -> client, after the matchmaking timeout with nobody suitable found: "NO_MATCH"
 *   server -> both players once paired: "MATCH_FOUND", then "COLOR white"/"COLOR black"
 *   client -> server, a move: "MOVE e2e5"                  (algebraic squares)
 *   client -> server, a jump: "JUMP e2"
 *   server -> both players in a match, after anything changes: "STATE\n<SnapshotCodec payload>"
 *   server -> both players in a match, whenever a rating changes: "NAMES <white>|<black>",
 *     each side "<name>:<elo>"
 *   server -> a client, when it tries to move a piece that isn't its color: "REJECTED wrong_color"
 *   server -> the remaining player, every tick while the other side is gone:
 *     "OPPONENT_DISCONNECTED <secondsLeft>" - game time is paused until the window runs out
 *   server -> the remaining player, if the other side comes back in time: "OPPONENT_RECONNECTED"
 *     (if the window runs out instead, the remaining player just gets a normal game-over STATE,
 *     since an auto-forfeit is scored exactly like any other win)
 *   client -> server, to start a private match instead of matchmaking: "CREATE_ROOM"
 *   server -> client, once a room is set aside: "ROOM_CREATED <code>" - share the code with a friend
 *   client -> server, to join a room by its code: "JOIN_ROOM <code>" - the second person to join a
 *     room becomes its Black player; anyone joining the same code afterwards becomes a read-only
 *     spectator instead (there's no separate "watch" command - joining a full room just spectates it)
 *   server -> client, if a room code doesn't exist or is mistyped: "ROOM_NOT_FOUND"
 *   server -> a new spectator, once accepted: "SPECTATING" (then the usual NAMES/STATE broadcasts,
 *     but a spectator's own MOVE/JUMP messages are simply ignored - it isn't seated in the match)
 */
public class GameServer {
    private static final int DEFAULT_PORT = 8765;
    private static final int MAX_ELO_DIFF = 100;
    private static final long QUEUE_SCAN_MS = 500;
    private static final long DEFAULT_MATCHMAKING_TIMEOUT_MS = 60_000;
    private static final long DEFAULT_RECONNECT_WINDOW_MS = 20_000;
    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I - easy to read aloud
    private static final int ROOM_CODE_LENGTH = 4;

    private final AccountStore accountStore;
    private final Map<WebSocketConnection, String> usernameOf = new HashMap<>();
    private final Map<WebSocketConnection, Integer> eloOf = new HashMap<>();
    private final List<WaitingPlayer> queue = new ArrayList<>();
    private final Map<WebSocketConnection, Match> matchOf = new HashMap<>();
    /** A username whose connection just dropped mid-match, and the Match it can still
     *  rejoin if it logs back in before the reconnect window expires. */
    private final Map<String, Match> pendingReconnect = new HashMap<>();
    /** A room code that's been handed out but only has its creator waiting so far. */
    private final Map<String, WaitingPlayer> pendingRooms = new HashMap<>();
    /** A room code whose match has already started - kept around purely so a spectator can
     *  still find it by code after the two players are already seated. */
    private final Map<String, Match> roomToMatch = new HashMap<>();
    private final Map<WebSocketConnection, Match> spectatorOf = new HashMap<>();
    private final Random random = new Random();
    private final Object lock = new Object();
    private long matchmakingTimeoutMs = DEFAULT_MATCHMAKING_TIMEOUT_MS;
    private long reconnectWindowMs = DEFAULT_RECONNECT_WINDOW_MS;

    public GameServer() {
        this("accounts.db");
    }

    /** Lets tests point at a throwaway accounts file instead of the real one. */
    public GameServer(String accountsDbPath) {
        this.accountStore = new AccountStore(accountsDbPath);
    }

    /** Test-only hook: shrinks the matchmaking timeout so a "nobody's out there" test doesn't
     *  have to actually wait 60 real seconds. */
    public void setMatchmakingTimeoutMsForTesting(long ms) {
        this.matchmakingTimeoutMs = ms;
    }

    /** Test-only hook: shrinks the disconnect/reconnect window so an auto-forfeit test doesn't
     *  have to actually wait 20 real seconds. */
    public void setReconnectWindowMsForTesting(long ms) {
        this.reconnectWindowMs = ms;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new GameServer().start(port);
    }

    /** Starts listening and returns the actual bound port (useful when port==0, auto-assign). */
    public int start(int port) throws IOException {
        WebSocketServer server = new WebSocketServer(port);
        server.start(this::onConnect);
        int boundPort = server.getLocalPort();
        System.out.println("GameServer listening on port " + boundPort);

        startMatchmakingLoop();
        return boundPort;
    }

    private void onConnect(WebSocketConnection connection) {
        Thread listenerThread = new Thread(() -> listenLoop(connection));
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenLoop(WebSocketConnection connection) {
        try {
            while (true) {
                String message = connection.readText();
                handleMessage(connection, message);
            }
        } catch (WebSocketFrame.ConnectionClosedException e) {
            System.out.println("Player disconnected: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } finally {
            removeConnection(connection);
        }
    }

    private void handleMessage(WebSocketConnection connection, String message) {
        String[] parts = message.trim().split("\\s+");

        if (parts.length == 3 && parts[0].equalsIgnoreCase("LOGIN")) {
            handleLogin(connection, parts[1], parts[2]);
            return;
        }
        if (parts.length == 1 && parts[0].equalsIgnoreCase("PLAY")) {
            handlePlay(connection);
            return;
        }
        if (parts.length == 1 && parts[0].equalsIgnoreCase("CREATE_ROOM")) {
            handleCreateRoom(connection);
            return;
        }
        if (parts.length == 2 && parts[0].equalsIgnoreCase("JOIN_ROOM")) {
            handleJoinRoom(connection, parts[1].toUpperCase());
            return;
        }

        Match match;
        synchronized (lock) {
            match = matchOf.get(connection);
        }
        if (match != null) {
            match.handleMessage(connection, parts);
        }
    }

    /** Sets aside a room code for this (already logged-in) player to share with a friend;
     *  the room turns into a real Match once someone else calls handleJoinRoom with the code. */
    private void handleCreateRoom(WebSocketConnection connection) {
        String code;
        synchronized (lock) {
            String username = usernameOf.get(connection);
            if (username == null) return; // must log in first
            code = generateUniqueRoomCode();
            pendingRooms.put(code, new WaitingPlayer(connection, username, eloOf.get(connection)));
        }
        trySend(connection, "ROOM_CREATED " + code);
    }

    /** Caller must hold {@code lock}. */
    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(ROOM_CODE_CHARS.charAt(random.nextInt(ROOM_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (pendingRooms.containsKey(code) || roomToMatch.containsKey(code));
        return code;
    }

    /** The second person to join a room becomes its Black player; anyone joining that same
     *  code afterwards - the room's match already under way - becomes a read-only spectator
     *  instead. Both cases are the same JOIN_ROOM command; the caller never has to say which
     *  one they mean, exactly like the spec's single "Join" button. */
    private void handleJoinRoom(WebSocketConnection connection, String code) {
        Match newMatch = null;
        WaitingPlayer host = null;
        WaitingPlayer joiner = null;
        Match existingMatch = null;
        synchronized (lock) {
            String username = usernameOf.get(connection);
            if (username == null) return; // must log in first

            WaitingPlayer pending = pendingRooms.get(code);
            if (pending != null) {
                if (pending.connection == connection) {
                    // Trying to join the room you yourself just created - there's no second
                    // player yet, so this must be rejected exactly like an unknown code.
                    trySend(connection, "ROOM_NOT_FOUND");
                    return;
                }
                pendingRooms.remove(code);
                host = pending;
                joiner = new WaitingPlayer(connection, username, eloOf.get(connection));
                newMatch = new Match(accountStore, host.connection, host.username, host.elo,
                        joiner.connection, joiner.username, joiner.elo, reconnectWindowMs);
                matchOf.put(host.connection, newMatch);
                matchOf.put(joiner.connection, newMatch);
                roomToMatch.put(code, newMatch);
            } else {
                existingMatch = roomToMatch.get(code);
                if (existingMatch == null) {
                    trySend(connection, "ROOM_NOT_FOUND");
                    return;
                }
                spectatorOf.put(connection, existingMatch);
            }
        }

        if (newMatch != null) {
            trySend(host.connection, "MATCH_FOUND");
            trySend(joiner.connection, "MATCH_FOUND");
            newMatch.start();
        } else {
            existingMatch.addSpectator(connection);
        }
    }

    private void handleLogin(WebSocketConnection connection, String username, String password) {
        AccountStore.LoginResult result = accountStore.login(username, password);
        if (!result.success) {
            trySend(connection, "LOGIN_FAILED " + result.reason);
            return;
        }

        // If this username disconnected mid-match and is still within its reconnect window,
        // rejoin that same match instead of starting fresh - no queueing, no new Play click.
        Match pendingMatch;
        synchronized (lock) {
            pendingMatch = pendingReconnect.remove(username);
        }
        if (pendingMatch != null && pendingMatch.reconnect(connection)) {
            synchronized (lock) {
                usernameOf.put(connection, username);
                eloOf.put(connection, result.elo);
                matchOf.put(connection, pendingMatch);
            }
            trySend(connection, "LOGIN_OK " + result.elo);
            return;
        }

        synchronized (lock) {
            // Matching username+password isn't enough on its own - without this check, the
            // same account could log in from a second connection while the first is still
            // playing (e.g. testing both windows with the same throwaway credentials).
            if (usernameOf.containsValue(username)) {
                trySend(connection, "LOGIN_FAILED already_logged_in");
                return;
            }
            usernameOf.put(connection, username);
            eloOf.put(connection, result.elo);
        }

        trySend(connection, "LOGIN_OK " + result.elo);
    }

    private void handlePlay(WebSocketConnection connection) {
        synchronized (lock) {
            String username = usernameOf.get(connection);
            if (username == null) return; // must log in first
            // A repeated PLAY (double click, or a resend) must not queue the same connection
            // twice - otherwise it could even get paired against itself once matchmaking scans.
            boolean alreadyQueued = queue.stream().anyMatch(p -> p.connection == connection);
            if (alreadyQueued) return;
            queue.add(new WaitingPlayer(connection, username, eloOf.get(connection)));
        }
        trySend(connection, "SEARCHING");
    }

    /** Runs for the server's entire lifetime - deliberately NOT a daemon thread (every other
     *  thread here is daemon), so the process doesn't exit the instant main() returns from
     *  start(); it also does the actual pairing work. */
    private void startMatchmakingLoop() {
        Thread loop = new Thread(() -> {
            while (true) {
                tryPairWaitingPlayers();
                try { Thread.sleep(QUEUE_SCAN_MS); } catch (InterruptedException ignored) { }
            }
        });
        loop.start();
    }

    /** One pairing (at most) per scan; a queue with several compatible players sorts itself
     *  out over a couple of scans instead of needing trickier same-pass index bookkeeping. */
    private void tryPairWaitingPlayers() {
        List<WebSocketConnection> timedOut = new ArrayList<>();
        Match newMatch = null;

        synchronized (lock) {
            long now = System.currentTimeMillis();
            for (Iterator<WaitingPlayer> it = queue.iterator(); it.hasNext(); ) {
                WaitingPlayer player = it.next();
                if (now - player.joinedAt >= matchmakingTimeoutMs) {
                    it.remove();
                    timedOut.add(player.connection);
                }
            }

            // Nobody ever came back for these - stop tracking them instead of leaking the map.
            pendingReconnect.entrySet().removeIf(entry -> entry.getValue().isResolved());
            roomToMatch.entrySet().removeIf(entry -> entry.getValue().isResolved());

            outer:
            for (int i = 0; i < queue.size(); i++) {
                for (int j = i + 1; j < queue.size(); j++) {
                    WaitingPlayer a = queue.get(i);
                    WaitingPlayer b = queue.get(j);
                    if (Math.abs(a.elo - b.elo) <= MAX_ELO_DIFF) {
                        queue.remove(j);
                        queue.remove(i);
                        newMatch = new Match(accountStore, a.connection, a.username, a.elo,
                                b.connection, b.username, b.elo, reconnectWindowMs);
                        matchOf.put(a.connection, newMatch);
                        matchOf.put(b.connection, newMatch);
                        break outer;
                    }
                }
            }
        }

        for (WebSocketConnection connection : timedOut) {
            trySend(connection, "NO_MATCH");
        }
        if (newMatch != null) {
            trySend(newMatch.whiteConnection(), "MATCH_FOUND");
            trySend(newMatch.blackConnection(), "MATCH_FOUND");
            newMatch.start();
        }
    }

    private void removeConnection(WebSocketConnection connection) {
        Match match;
        Match spectating;
        synchronized (lock) {
            String username = usernameOf.remove(connection);
            eloOf.remove(connection);
            queue.removeIf(p -> p.connection == connection);
            pendingRooms.entrySet().removeIf(entry -> entry.getValue().connection == connection);
            spectating = spectatorOf.remove(connection);
            match = matchOf.remove(connection);
            if (match != null && username != null) {
                pendingReconnect.put(username, match);
            }
        }
        connection.close();
        if (spectating != null) {
            spectating.removeSpectator(connection);
        }
        if (match != null) {
            match.onPlayerDisconnected(connection);
        }
    }

    private void trySend(WebSocketConnection connection, String text) {
        try {
            connection.sendText(text);
        } catch (IOException ignored) { }
    }

    private static final class WaitingPlayer {
        final WebSocketConnection connection;
        final String username;
        final int elo;
        final long joinedAt = System.currentTimeMillis();

        WaitingPlayer(WebSocketConnection connection, String username, int elo) {
            this.connection = connection;
            this.username = username;
            this.elo = elo;
        }
    }
}
