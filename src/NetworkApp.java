import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import bus.EventBus;
import bus.GameEndedEvent;
import bus.GameStartedEvent;
import bus.MoveLogUpdatedEvent;
import bus.MoveLoggedEvent;
import bus.ScoreChangedEvent;
import client.GameClient;
import engine.GameSnapshot;
import engine.MoveLogEntry;
import engine.PieceSnapshot;
import model.GameConstants;
import model.Position;
import net.SnapshotCodec;
import rules.RuleEngine;
import view.BoardRenderer;
import view.FireworksEffect;
import view.GameBoardView;
import view.MoveLogPanel;
import view.SoundPlayer;

/**
 * Networked GUI entry point: connects to a GameServer instead of running a GameEngine
 * locally. App.java (local hot-seat play) is untouched - this is a second, independent
 * orchestration entry point over the same view classes, the same relationship Main.java
 * already has to App.java.
 *
 * Run a server first (server.GameServer, default port 8765), then run this twice - once
 * per player - pointing at that server: "java NetworkApp localhost 8765".
 *
 * Simplification (documented, not a bug): legal-move-square highlighting is not shown here,
 * since that requires rule access the client doesn't have; the server still enforces and
 * rejects illegal/wrong-color moves, so correctness isn't affected, only that one highlight.
 * "Play Again" also isn't wired up yet - resetting a networked game is a later step.
 */
public class NetworkApp {

    private static final String BOARD_IMAGE_PATH = "assets/board.png";
    private static final int ROWS = 8;
    private static final int COLS = 8;

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8765;
        SwingUtilities.invokeLater(() -> createAndShow(host, port));
    }

    /** Tiny mutable holder - the client's own click-selection state, since there is no
     *  local GameController (that class is wired to a local GameEngine). */
    private static final class Selection {
        volatile int row = -1;
        volatile int col = -1;
    }

    private static void createAndShow(String host, int port) {
        BoardRenderer renderer = new BoardRenderer(BOARD_IMAGE_PATH, ROWS, COLS);
        FireworksEffect fireworks = new FireworksEffect();
        Selection selection = new Selection();

        AtomicReference<GameSnapshot> latestSnapshot = new AtomicReference<>();
        AtomicReference<String> statusMessage = new AtomicReference<>("Connecting...");
        AtomicReference<String> assignedColorRef = new AtomicReference<>();
        AtomicReference<String> whiteNameRef = new AtomicReference<>("-");
        AtomicReference<String> blackNameRef = new AtomicReference<>("-");
        AtomicReference<String> whiteEloRef = new AtomicReference<>("-");
        AtomicReference<String> blackEloRef = new AtomicReference<>("-");
        // A spectator never gets a COLOR, so its clicks must never be read as move attempts.
        java.util.concurrent.atomic.AtomicBoolean isSpectator = new java.util.concurrent.atomic.AtomicBoolean(false);
        // Set the moment a room code is known (created, or about to be joined) - null for a
        // plain matchmade (Play) game, which has no room code to show anywhere.
        AtomicReference<String> roomCodeRef = new AtomicReference<>();

        Supplier<GameSnapshot> snapshotSupplier = () -> {
            GameSnapshot server = latestSnapshot.get();
            if (server == null) return null; // haven't received a STATE broadcast yet
            List<Position> legalMoves = (selection.row == -1)
                    ? Collections.emptyList()
                    : legalDestinationsFrom(server, selection.row, selection.col);
            String winnerName = "white".equals(server.winner) ? nameOrColor(whiteNameRef.get(), "white")
                    : "black".equals(server.winner) ? nameOrColor(blackNameRef.get(), "black")
                    : server.winner;
            // Safe to show the yellow border again - selection is only ever set on your
            // own piece (see isOwnPiece), so it can never land on an opponent's piece.
            return new GameSnapshot(server.boardRows, server.boardCols, server.pieces,
                    selection.row, selection.col, server.gameOver, winnerName,
                    legalMoves, server.moveLog, server.whiteScore, server.blackScore);
        };
        GameBoardView boardView = new GameBoardView(renderer, fireworks, snapshotSupplier, 220);
        JPanel panel = boardView.getBoardPanel();

        MoveLogPanel blackLog = boardView.getBlackLog();
        MoveLogPanel whiteLog = boardView.getWhiteLog();

        JLabel title = new JLabel("Kung Fu Chess", SwingConstants.CENTER);
        title.setOpaque(true);
        title.setBackground(Color.BLACK);
        title.setForeground(BoardRenderer.SELECTION_COLOR);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 40));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 8, 0));

        JLabel status = new JLabel("Connecting...", SwingConstants.CENTER);
        status.setOpaque(true);
        status.setBackground(Color.BLACK);
        status.setForeground(Color.LIGHT_GRAY);
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        status.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // Both network windows otherwise look identical - this is the one loud, unmissable
        // signal for which color this particular window is playing.
        JLabel colorBanner = new JLabel("", SwingConstants.CENTER);
        colorBanner.setOpaque(true);
        colorBanner.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        colorBanner.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // Empty for a matchmade (Play) game; shows the room's code for a Room-based game -
        // "the room ID is again written on the top of the screen", same as on the home screen.
        JLabel roomTag = new JLabel("", SwingConstants.CENTER);
        roomTag.setOpaque(true);
        roomTag.setBackground(Color.BLACK);
        roomTag.setForeground(Color.LIGHT_GRAY);
        roomTag.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        roomTag.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JPanel boardRow = boardView.getRow();

        JPanel titleAndBanner = new JPanel(new BorderLayout());
        titleAndBanner.setBackground(Color.BLACK);
        titleAndBanner.add(roomTag, BorderLayout.NORTH);
        titleAndBanner.add(title, BorderLayout.CENTER);
        titleAndBanner.add(colorBanner, BorderLayout.SOUTH);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.add(titleAndBanner, BorderLayout.NORTH);
        header.add(status, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.BLACK);
        root.add(header, BorderLayout.NORTH);
        root.add(boardRow, BorderLayout.CENTER);

        // The lobby screen shown between "logged in" and "matched" - a player has to click
        // Play and wait to be paired before the board ever appears.
        JLabel lobbyTitle = new JLabel("Kung Fu Chess", SwingConstants.CENTER);
        lobbyTitle.setOpaque(true);
        lobbyTitle.setBackground(Color.BLACK);
        lobbyTitle.setForeground(BoardRenderer.SELECTION_COLOR);
        lobbyTitle.setFont(new Font(Font.SERIF, Font.BOLD, 40));
        lobbyTitle.setBorder(BorderFactory.createEmptyBorder(40, 0, 24, 0));

        // Written at the top of the home screen the moment a room code is known (created or
        // joined) - empty otherwise, per the spec's "writes it on top of the screen".
        JLabel roomCodeLabel = new JLabel(" ", SwingConstants.CENTER);
        roomCodeLabel.setForeground(BoardRenderer.SELECTION_COLOR);
        roomCodeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        roomCodeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomCodeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel lobbyStatus = new JLabel("Logging in...", SwingConstants.CENTER);
        lobbyStatus.setForeground(Color.LIGHT_GRAY);
        lobbyStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        lobbyStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton playButton = new JButton("Play");
        playButton.setEnabled(false);
        playButton.setFont(new Font(Font.SERIF, Font.BOLD, 22));
        playButton.setBackground(new Color(30, 30, 30));
        playButton.setForeground(BoardRenderer.SELECTION_COLOR);
        playButton.setFocusPainted(false);
        playButton.setBorder(BorderFactory.createLineBorder(BoardRenderer.SELECTION_COLOR, 3));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton roomButton = smallLobbyButton("Room");
        roomButton.setEnabled(false);
        roomButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel lobbyCenter = new JPanel();
        lobbyCenter.setBackground(Color.BLACK);
        lobbyCenter.setLayout(new BoxLayout(lobbyCenter, BoxLayout.Y_AXIS));
        lobbyCenter.add(roomCodeLabel);
        lobbyCenter.add(lobbyStatus);
        lobbyCenter.add(Box.createVerticalStrut(20));
        lobbyCenter.add(playButton);
        lobbyCenter.add(Box.createVerticalStrut(16));
        lobbyCenter.add(roomButton);

        JPanel lobbyPanel = new JPanel(new BorderLayout());
        lobbyPanel.setBackground(Color.BLACK);
        lobbyPanel.add(lobbyTitle, BorderLayout.NORTH);
        lobbyPanel.add(lobbyCenter, BorderLayout.CENTER);

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);
        cards.setBackground(Color.BLACK);
        cards.add(lobbyPanel, "lobby");
        cards.add(root, "game");

        JFrame frame = new JFrame("Kung Fu Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Color.BLACK);
        frame.add(cards);
        frame.setMinimumSize(new Dimension(400, 440));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                renderer.resize(panel.getWidth(), panel.getHeight());
                panel.repaint();
            }
        });
        renderer.resize(panel.getWidth(), panel.getHeight());

        // The pub/sub bus: whatever notices a game change (the polling loop below, or a
        // server message) just publishes an event; whatever reacts to it (score, move log,
        // sound, animation) subscribes once here and never talks to the others directly.
        EventBus bus = new EventBus();

        bus.subscribe(ScoreChangedEvent.class, ev -> {
            whiteLog.setScore(ev.whiteScore);
            blackLog.setScore(ev.blackScore);
        });
        bus.subscribe(MoveLogUpdatedEvent.class, ev -> {
            whiteLog.sync(ev.whiteMoves);
            blackLog.sync(ev.blackMoves);
        });
        bus.subscribe(MoveLoggedEvent.class, ev -> {
            if (ev.entry.notation.contains("x")) SoundPlayer.playCapture();
        });
        long[] gameOverAt = {0};
        long[] nextBurstAt = {0};
        // Set once this window has heard (or, on hydration, already knows) that the game
        // ended, so a server-forwarded "EVENT ENDED" is never re-published a second time.
        boolean[] gameOverAnnounced = {false};
        bus.subscribe(GameEndedEvent.class, ev -> {
            SoundPlayer.playVictoryFanfare();
            gameOverAt[0] = System.currentTimeMillis();
            nextBurstAt[0] = gameOverAt[0];
        });
        bus.subscribe(GameStartedEvent.class, ev -> SwingUtilities.invokeLater(() -> {
            whiteLog.clear();
            blackLog.clear();
            fireworks.spawnBurst(panel.getWidth() / 2.0, panel.getHeight() * 0.15);
        }));

        GameClient[] clientHolder = new GameClient[1];
        try {
            clientHolder[0] = GameClient.connect(host, port, message -> {
                if (message.startsWith("STATE\n")) {
                    latestSnapshot.set(SnapshotCodec.decode(message.substring("STATE\n".length())));
                } else if (message.startsWith("COLOR ")) {
                    String color = message.substring("COLOR ".length()).trim();
                    assignedColorRef.set(color);
                    statusMessage.set("Connected"); // color already shown loud and clear in the banner below
                    // Also fires on reconnect, which skips MATCH_FOUND entirely and jumps
                    // straight back into the match - COLOR is the one signal both paths share.
                    String roomCode = roomCodeRef.get();
                    SwingUtilities.invokeLater(() -> {
                        roomTag.setText(roomCode != null ? "Room: " + roomCode : "");
                        cardLayout.show(cards, "game");
                    });
                    bus.publish(new GameStartedEvent());
                } else if (message.startsWith("OPPONENT_DISCONNECTED")) {
                    String secondsLeft = message.substring("OPPONENT_DISCONNECTED".length()).trim();
                    statusMessage.set("Opponent disconnected - auto-win in " + secondsLeft + "s...");
                } else if (message.equals("OPPONENT_RECONNECTED")) {
                    statusMessage.set("Opponent reconnected");
                } else if (message.startsWith("LOGIN_FAILED")) {
                    String reason = message.length() > "LOGIN_FAILED".length()
                            ? message.substring("LOGIN_FAILED".length()).trim() : "";
                    String friendly = "already_logged_in".equals(reason)
                            ? "That username is already playing in another window. Use a different name."
                            : "Wrong password - or that username already belongs to someone else. Try a different name or password.";
                    SwingUtilities.invokeLater(() -> attemptLogin(clientHolder[0], frame, friendly));
                } else if (message.startsWith("LOGIN_OK ")) {
                    String elo = message.substring("LOGIN_OK ".length()).trim();
                    SwingUtilities.invokeLater(() -> {
                        lobbyStatus.setText("Your rating: " + elo);
                        playButton.setEnabled(true);
                        roomButton.setEnabled(true);
                    });
                } else if (message.equals("SEARCHING")) {
                    SwingUtilities.invokeLater(() -> lobbyStatus.setText("Searching for an opponent..."));
                } else if (message.equals("NO_MATCH")) {
                    SwingUtilities.invokeLater(() -> {
                        lobbyStatus.setText("Couldn't find an opponent - try again");
                        playButton.setEnabled(true);
                        roomButton.setEnabled(true);
                    });
                } else if (message.equals("MATCH_FOUND")) {
                    SwingUtilities.invokeLater(() -> cardLayout.show(cards, "game"));
                } else if (message.startsWith("NAMES ")) {
                    String[] sides = message.substring("NAMES ".length()).split("\\|", -1);
                    if (sides.length == 2) {
                        String[] whiteParts = sides[0].split(":", -1);
                        String[] blackParts = sides[1].split(":", -1);
                        whiteNameRef.set(whiteParts[0]);
                        whiteEloRef.set(whiteParts.length > 1 ? whiteParts[1] : "-");
                        blackNameRef.set(blackParts[0]);
                        blackEloRef.set(blackParts.length > 1 ? blackParts[1] : "-");
                    }
                } else if (message.startsWith("REJECTED")) {
                    statusMessage.set(friendlyRejection(message));
                    System.out.println(message);
                } else if (message.startsWith("EVENT MOVE ")) {
                    String[] eventParts = message.substring("EVENT MOVE ".length()).trim().split("\\s+", 2);
                    if (eventParts.length == 2) {
                        boolean white = "white".equals(eventParts[0]);
                        bus.publish(new MoveLoggedEvent(new MoveLogEntry(white, eventParts[1], 0)));
                    }
                } else if (message.startsWith("EVENT SCORE ")) {
                    String[] eventParts = message.substring("EVENT SCORE ".length()).trim().split("\\s+");
                    if (eventParts.length == 2) {
                        bus.publish(new ScoreChangedEvent(Integer.parseInt(eventParts[0]), Integer.parseInt(eventParts[1])));
                    }
                } else if (message.startsWith("EVENT ENDED ")) {
                    if (!gameOverAnnounced[0]) {
                        gameOverAnnounced[0] = true;
                        bus.publish(new GameEndedEvent(message.substring("EVENT ENDED ".length()).trim()));
                    }
                } else if (message.startsWith("ROOM_CREATED ")) {
                    String code = message.substring("ROOM_CREATED ".length()).trim();
                    roomCodeRef.set(code);
                    SwingUtilities.invokeLater(() -> {
                        roomCodeLabel.setText("Room: " + code);
                        lobbyStatus.setText("Waiting for a friend to join...");
                    });
                } else if (message.equals("ROOM_NOT_FOUND")) {
                    roomCodeRef.set(null);
                    SwingUtilities.invokeLater(() -> {
                        roomCodeLabel.setText(" ");
                        lobbyStatus.setText("No room found with that code");
                        playButton.setEnabled(true);
                        roomButton.setEnabled(true);
                    });
                } else if (message.equals("SPECTATING")) {
                    isSpectator.set(true);
                    statusMessage.set("Watching");
                    String roomCode = roomCodeRef.get();
                    SwingUtilities.invokeLater(() -> {
                        roomTag.setText(roomCode != null ? "Room: " + roomCode : "");
                        cardLayout.show(cards, "game");
                    });
                }
            });
        } catch (IOException e) {
            lobbyStatus.setText("Could not connect to " + host + ":" + port + " - " + e.getMessage());
            return;
        }
        GameClient client = clientHolder[0];
        attemptLogin(client, frame, null);

        playButton.addActionListener(e -> {
            roomCodeRef.set(null); // a plain matchmade game never shows a room code
            playButton.setEnabled(false);
            roomButton.setEnabled(false);
            lobbyStatus.setText("Searching for an opponent...");
            try {
                client.sendPlay();
            } catch (IOException ex) {
                System.out.println("play send failed: " + ex.getMessage());
            }
        });

        roomButton.addActionListener(e -> {
            RoomDialogResult result = promptRoomDialog(frame);
            if (result.choice == RoomDialogResult.CREATE) {
                playButton.setEnabled(false);
                roomButton.setEnabled(false);
                lobbyStatus.setText("Creating room...");
                try {
                    client.sendCreateRoom();
                } catch (IOException ex) {
                    System.out.println("create room send failed: " + ex.getMessage());
                }
            } else if (result.choice == RoomDialogResult.JOIN) {
                if (result.code.isEmpty()) {
                    lobbyStatus.setText("Type a room name first");
                    return;
                }
                roomCodeRef.set(result.code); // optimistic - cleared again on ROOM_NOT_FOUND
                playButton.setEnabled(false);
                roomButton.setEnabled(false);
                lobbyStatus.setText("Joining room " + result.code + "...");
                try {
                    client.sendJoinRoom(result.code);
                } catch (IOException ex) {
                    System.out.println("join room send failed: " + ex.getMessage());
                }
            }
            // CREATE and JOIN handled above; Cancel (or closing the dialog) does nothing.
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isSpectator.get()) return; // watching only - no moves, no jumps

                int[] cell = renderer.getGeometry().pixelToCell(e.getX(), e.getY());

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (cell != null) {
                        try {
                            client.sendJump(squareName(cell[0], cell[1]));
                        } catch (IOException ex) {
                            System.out.println("jump send failed: " + ex.getMessage());
                        }
                    }
                    return;
                }

                if (cell == null) {
                    selection.row = -1;
                    selection.col = -1;
                    panel.repaint();
                    return;
                }

                if (selection.row == -1) {
                    if (isOwnPiece(latestSnapshot.get(), cell[0], cell[1], client.getAssignedColor())) {
                        selection.row = cell[0];
                        selection.col = cell[1];
                    }
                } else {
                    try {
                        client.sendMove(squareName(selection.row, selection.col), squareName(cell[0], cell[1]));
                    } catch (IOException ex) {
                        System.out.println("move send failed: " + ex.getMessage());
                    }
                    selection.row = -1;
                    selection.col = -1;
                }
                panel.repaint();
            }
        });

        Random random = new Random();
        final int[] loggedMoveCount = {0};
        final long fireworksSpawnWindowMs = 4000;
        // Set on the very first snapshot this window ever sees, so a match already under way
        // (a spectator tuning in, or a reconnect) has its score/move-log/game-over display
        // caught up silently instead of replaying every historical MoveLoggedEvent/fanfare.
        final boolean[] hydrated = {false};
        final String[] bannerShownFor = {null};

        Timer uiLoop = new Timer(16, e -> {
            long now = System.currentTimeMillis();
            fireworks.update(0.016);

            GameSnapshot snap = latestSnapshot.get();
            status.setText(statusMessage.get());

            String color = assignedColorRef.get();
            String whiteName = whiteNameRef.get();
            String blackName = blackNameRef.get();
            String whiteElo = whiteEloRef.get();
            String blackElo = blackEloRef.get();
            String signature = color + "|" + whiteName + "|" + blackName + "|" + whiteElo + "|" + blackElo;
            if (isSpectator.get() && !signature.equals(bannerShownFor[0])) {
                bannerShownFor[0] = signature;
                colorBanner.setText("● Watching " + nameOrColor(whiteName, "White") + " vs " + nameOrColor(blackName, "Black"));
                colorBanner.setBackground(new Color(25, 25, 25));
                colorBanner.setForeground(Color.LIGHT_GRAY);

                whiteLog.setTitle("White" + describeSide(whiteName, whiteElo));
                blackLog.setTitle("Black" + describeSide(blackName, blackElo));
            } else if (color != null && !signature.equals(bannerShownFor[0])) {
                bannerShownFor[0] = signature;
                boolean isWhite = color.equals("white");
                String myName = isWhite ? whiteName : blackName;
                String myElo = isWhite ? whiteElo : blackElo;
                String namePart = (myName != null && !myName.equals("-"))
                        ? " (" + myName + (myElo != null && !myElo.equals("-") ? ", " + myElo : "") + ")"
                        : "";

                colorBanner.setText("● You are playing " + (isWhite ? "WHITE" : "BLACK") + namePart);
                colorBanner.setBackground(isWhite ? Color.WHITE : new Color(25, 25, 25));
                colorBanner.setForeground(isWhite ? Color.BLACK : Color.WHITE);

                whiteLog.setTitle("White" + describeSide(whiteName, whiteElo));
                blackLog.setTitle("Black" + describeSide(blackName, blackElo));
            }

            if (snap != null) {
                // Live score/move-log/game-over updates arrive as server-forwarded "EVENT ..."
                // messages (published at the true moment they happen, on the authoritative
                // GameEngine's own bus) - this block only ever runs once, to silently catch
                // this window's display up to whatever already happened before it connected.
                if (!hydrated[0]) {
                    hydrated[0] = true;
                    gameOverAnnounced[0] = snap.gameOver;
                    loggedMoveCount[0] = snap.moveLog.size();
                    whiteLog.setScore(snap.whiteScore);
                    blackLog.setScore(snap.blackScore);
                    whiteLog.sync(splitByColor(snap.moveLog, true));
                    blackLog.sync(splitByColor(snap.moveLog, false));
                }

                if (snap.gameOver && now - gameOverAt[0] < fireworksSpawnWindowMs && now >= nextBurstAt[0]) {
                    double x = random.nextDouble() * panel.getWidth();
                    double y = random.nextDouble() * panel.getHeight() * 0.6;
                    fireworks.spawnBurst(x, y);
                    nextBurstAt[0] = now + 300 + random.nextInt(400);
                }

                if (snap.moveLog.size() > loggedMoveCount[0]) {
                    loggedMoveCount[0] = snap.moveLog.size();
                    bus.publish(new MoveLogUpdatedEvent(splitByColor(snap.moveLog, true), splitByColor(snap.moveLog, false)));
                }
            }

            panel.repaint();
        });
        uiLoop.start();
    }

    /** Every move belonging to one side, in order - the move-log panel only ever wants its own side. */
    private static List<MoveLogEntry> splitByColor(List<MoveLogEntry> moveLog, boolean white) {
        List<MoveLogEntry> result = new ArrayList<>();
        for (MoveLogEntry entry : moveLog) {
            if (entry.white == white) result.add(entry);
        }
        return result;
    }

    /** " (Alice, 1200)" for a move-log header, or "" if nobody's logged in for that side yet. */
    private static String describeSide(String name, String elo) {
        if (name == null || name.equals("-")) return "";
        return (elo != null && !elo.equals("-")) ? " (" + name + ", " + elo + ")" : " (" + name + ")";
    }

    /** The player's real name if it's known yet, otherwise just falls back to the color word. */
    private static String nameOrColor(String name, String color) {
        return (name != null && !name.equals("-")) ? name : color;
    }

    /** Prompts for username+password and sends LOGIN. A new username is registered on the spot;
     *  an existing one must match its password, or the server sends back "LOGIN_FAILED", which
     *  re-invokes this same method with an error message so the player can just try again on the
     *  same connection - no need to reconnect. */
    private static void attemptLogin(GameClient client, JFrame parent, String errorMessage) {
        String[] credentials = promptForCredentials(parent, errorMessage);
        try {
            client.sendLogin(credentials[0], credentials[1]);
        } catch (IOException ex) {
            System.out.println("login send failed: " + ex.getMessage());
        }
    }

    /** A blank username falls back to "Player"; a blank password is allowed (this is a simple
     *  account system, not a secure one). Cancelling just quits - there's nothing sensible to
     *  show without logging in. */
    private static String[] promptForCredentials(JFrame parent, String errorMessage) {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        if (errorMessage != null) {
            JLabel error = new JLabel(errorMessage);
            error.setForeground(Color.RED);
            panel.add(error);
        }
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Kung Fu Chess - Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        String username = sanitizeCredential(usernameField.getText());
        if (username.isEmpty()) username = "Player";
        String password = sanitizeCredential(new String(passwordField.getPassword()));
        return new String[] { username, password };
    }

    /** Strips whitespace/"|" (our field separators) so a typed value can't break the wire
     *  protocol, and caps the length for the on-screen labels. */
    private static String sanitizeCredential(String input) {
        if (input == null) return "";
        String cleaned = input.trim().replace("|", "").replaceAll("\\s+", "_");
        return cleaned.length() > 20 ? cleaned.substring(0, 20) : cleaned;
    }

    /** A smaller, secondary-looking lobby button - visually distinct from the big "Play" one,
     *  since these are the less-common paths (a private room instead of open matchmaking). */
    private static JButton smallLobbyButton(String label) {
        JButton button = new JButton(label);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        button.setBackground(new Color(20, 20, 20));
        button.setForeground(Color.LIGHT_GRAY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        return button;
    }

    /** Which button the player pressed in the Room dialog, and whatever they'd typed at that
     *  point - Create ignores it (the server generates a fresh code), Join looks it up. */
    private static final class RoomDialogResult {
        static final int CREATE = 0;
        static final int JOIN = 1;
        static final int CANCEL = 2;

        final int choice;
        final String code;

        RoomDialogResult(int choice, String code) {
            this.choice = choice;
            this.code = code;
        }
    }

    /** The single "Room" dialog from the spec: one text box, three buttons - Create / Join /
     *  Cancel. Create makes a brand new room (the box is ignored); Join enters whatever room
     *  name is typed in the box. */
    private static RoomDialogResult promptRoomDialog(JFrame parent) {
        JTextField codeField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Room name:"));
        panel.add(codeField);

        Object[] options = {"Create", "Join", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent, panel, "Room",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        int normalizedChoice = (choice == JOptionPane.CLOSED_OPTION) ? RoomDialogResult.CANCEL : choice;
        String code = codeField.getText().trim().toUpperCase();
        return new RoomDialogResult(normalizedChoice, code);
    }

    /** True only if the piece sitting on (row,col) belongs to "color" - used to decide whether
     *  a click starts a selection, so clicking an opponent's piece is simply ignored instead of
     *  selecting it. The server enforces this too (rejects a move on the wrong piece), this is
     *  just so the player never even sees it get selected in the first place. */
    private static boolean isOwnPiece(GameSnapshot snapshot, int row, int col, String color) {
        if (snapshot == null || color == null) return false;
        char prefix = color.equals("white") ? 'w' : 'b';
        for (PieceSnapshot piece : snapshot.pieces) {
            if (Math.round(piece.row) == row && Math.round(piece.col) == col) {
                return piece.pieceCode.length() >= 2 && piece.pieceCode.charAt(0) == prefix;
            }
        }
        return false;
    }

    /** Every square the piece at (row,col) could legally move to, purely for the highlight -
     *  reruns the same RuleEngine geometry check GameEngine.legalDestinations uses locally,
     *  fed a board reconstructed from the last server snapshot instead of a live GameEngine.
     *  Cosmetic only: the server is still the one that actually accepts or rejects a move. */
    private static List<Position> legalDestinationsFrom(GameSnapshot snapshot, int row, int col) {
        String[][] board = boardFrom(snapshot);
        List<Position> destinations = new ArrayList<>();
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (r == row && c == col) continue;
                if (RuleEngine.checkMove(row, col, r, c, board) == RuleEngine.MoveResult.OK) {
                    destinations.add(new Position(r, c));
                }
            }
        }
        return destinations;
    }

    private static String[][] boardFrom(GameSnapshot snapshot) {
        String[][] board = new String[snapshot.boardRows][snapshot.boardCols];
        for (String[] row : board) {
            java.util.Arrays.fill(row, GameConstants.EMPTY);
        }
        for (PieceSnapshot piece : snapshot.pieces) {
            int r = (int) Math.round(piece.row);
            int c = (int) Math.round(piece.col);
            if (r >= 0 && r < board.length && c >= 0 && c < board[0].length) {
                board[r][c] = piece.pieceCode;
            }
        }
        return board;
    }

    /** Turns "REJECTED wrong_color" into something readable for the status line at the top. */
    private static String friendlyRejection(String rejectedMessage) {
        String reason = rejectedMessage.length() > "REJECTED".length()
                ? rejectedMessage.substring("REJECTED".length()).trim()
                : "";
        switch (reason) {
            case "wrong_color": return "That's not your piece";
            case "table_full": return "The game already has two players";
            case "game_over": return "The game is already over";
            case "empty_source": return "There's no piece there";
            case "motion_in_progress": return "That piece is already moving";
            case "resting": return "That piece is resting - not yet";
            case "friendly_destination": return "You already have a piece there";
            case "illegal_piece_move": return "That piece can't move there";
            default: return "Move rejected";
        }
    }

    /** {row, col} -> algebraic, e.g. row=5 col=4 -> "e3". Inverse of GameServer.squareToRowCol. */
    private static String squareName(int row, int col) {
        char file = (char) ('a' + col);
        int rank = ROWS - row;
        return "" + file + rank;
    }
}
