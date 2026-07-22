import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import controller.GameController;
import engine.GameEngine;
import engine.GameSnapshot;
import engine.MoveLogEntry;
import view.BoardRenderer;
import view.FireworksEffect;
import view.GameBoardView;
import view.MoveLogPanel;
import view.SoundPlayer;

/** Graphical entry point. Main.java stays the text-command harness; this is the GUI one. */
public class App {

    private static final String BOARD_IMAGE_PATH = "assets/board.png";
    private static final int ROWS = 8;
    private static final int COLS = 8;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShow);
    }

    /** Turns a GameEngine.RequestResult's machine-readable reason into something readable
     *  for the status line under the title - mirrors NetworkApp's friendlyRejection, minus
     *  the two rejection reasons that only the network server can ever produce. */
    private static String friendlyRejection(String reason) {
        switch (reason) {
            case "game_over": return "The game is already over";
            case "empty_source": return "There's no piece there";
            case "motion_in_progress": return "That piece is already moving";
            case "resting": return "That piece is resting - not yet";
            case "friendly_destination": return "You already have a piece there";
            case "illegal_piece_move": return "That piece can't move there";
            default: return "Move rejected";
        }
    }

    private static String[][] startingPosition() {
        return new String[][] {
                {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"},
                {"bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {"wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"},
                {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"},
        };
    }

    private static void createAndShow() {
        GameEngine engine = new GameEngine();
        engine.board = startingPosition();

        BoardRenderer renderer = new BoardRenderer(BOARD_IMAGE_PATH, ROWS, COLS);
        GameController controller = new GameController(engine, renderer.getGeometry());
        FireworksEffect fireworks = new FireworksEffect();

        GameBoardView boardView = new GameBoardView(renderer, fireworks, controller::snapshot, 220);
        JPanel panel = boardView.getBoardPanel();
        panel.setLayout(null); // only child is the absolutely-positioned "Play Again" button

        JButton playAgainButton = new JButton("Play Again");
        playAgainButton.setFont(new Font(Font.SERIF, Font.BOLD, 22));
        playAgainButton.setBackground(new Color(30, 30, 30));
        playAgainButton.setForeground(BoardRenderer.SELECTION_COLOR);
        playAgainButton.setFocusPainted(false);
        playAgainButton.setBorder(BorderFactory.createLineBorder(BoardRenderer.SELECTION_COLOR, 3));
        playAgainButton.setVisible(false);
        panel.add(playAgainButton);

        Runnable positionPlayAgainButton = () -> {
            int w = 220, h = 56;
            playAgainButton.setBounds((panel.getWidth() - w) / 2, panel.getHeight() / 2 + 50, w, h);
        };

        MoveLogPanel blackLog = boardView.getBlackLog();
        MoveLogPanel whiteLog = boardView.getWhiteLog();

        JLabel title = new JLabel("Kung Fu Chess", SwingConstants.CENTER);
        title.setOpaque(true);
        title.setBackground(Color.BLACK);
        title.setForeground(BoardRenderer.SELECTION_COLOR);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 48));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 8, 0));

        JLabel status = new JLabel(" ", SwingConstants.CENTER);
        status.setOpaque(true);
        status.setBackground(Color.BLACK);
        status.setForeground(Color.LIGHT_GRAY);
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        status.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.add(title, BorderLayout.NORTH);
        header.add(status, BorderLayout.SOUTH);

        // GameBoardView already wraps [blackLog | panel | whiteLog] in a BoardRowLayout - see
        // that class for why a real LayoutManager (invoked by Swing's own validate machinery)
        // is used instead of a componentResized listener reactively mutating preferredSize.
        JPanel boardRow = boardView.getRow();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.BLACK);
        root.add(header, BorderLayout.NORTH);
        root.add(boardRow, BorderLayout.CENTER);

        JFrame frame = new JFrame("Kung Fu Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Color.BLACK);
        frame.add(root);
        frame.setMinimumSize(new Dimension(400, 400));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                renderer.resize(panel.getWidth(), panel.getHeight());
                positionPlayAgainButton.run();
                panel.repaint();
            }
        });
        renderer.resize(panel.getWidth(), panel.getHeight());
        positionPlayAgainButton.run();

        panel.addMouseListener(new MouseAdapter() {
            // mouseReleased (not mouseClicked) - mouseClicked silently drops the event if the
            // mouse moves even one pixel between press and release, which reads as "unresponsive"
            // on a trackpad.
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    GameEngine.RequestResult result = controller.jump(e.getX(), e.getY());
                    if (result == null) {
                        System.out.println("jump ignored (outside board)");
                    } else {
                        System.out.println(result.accepted
                                ? "jump accepted"
                                : "jump REJECTED - reason: " + result.reason);
                        if (result.accepted) {
                            SoundPlayer.playJump();
                            status.setText(" ");
                        } else {
                            status.setText(friendlyRejection(result.reason));
                        }
                    }
                    return;
                }

                boolean hadSelectionBefore = controller.hasSelection();
                GameEngine.RequestResult result = controller.click(e.getX(), e.getY());
                boolean hasSelectionNow = controller.hasSelection();

                if (!hadSelectionBefore && hasSelectionNow) {
                    System.out.println("selected " + engine.board[controller.getSelectedRow()][controller.getSelectedCol()]
                            + " at row=" + controller.getSelectedRow() + " col=" + controller.getSelectedCol());
                    status.setText(" ");
                } else if (result != null) {
                    System.out.println(result.accepted
                            ? "move accepted"
                            : "move REJECTED - reason: " + result.reason);
                    if (result.accepted) {
                        SoundPlayer.playMove();
                        status.setText(" ");
                    } else {
                        status.setText(friendlyRejection(result.reason));
                    }
                } else if (hadSelectionBefore && !hasSelectionNow) {
                    System.out.println("selection cancelled (clicked outside the board)");
                    status.setText(" ");
                } else {
                    System.out.println("click ignored (empty cell or outside board)");
                }
            }
        });

        Random random = new Random();
        final int[] loggedMoveCount = {0};
        final long[] gameOverAt = {0};
        final long[] nextBurstAt = {0};
        final long fireworksSpawnWindowMs = 4000;
        final boolean[] gameOverAnnounced = {false};

        Runnable startNewGame = () -> {
            engine.reset(startingPosition());
            controller.clearSelection();
            blackLog.clear();
            whiteLog.clear();
            loggedMoveCount[0] = 0;
            fireworks.clear();
            gameOverAnnounced[0] = false;
            playAgainButton.setVisible(false);
            status.setText(" ");
            panel.repaint();
        };
        playAgainButton.addActionListener(e -> startNewGame.run());

        // Drives simulated game time from real wall-clock time, resolves arrived moves,
        // and repaints. This is the "frame" per the course design doc's render loop.
        final long[] lastTick = {System.currentTimeMillis()};
        Timer gameLoop = new Timer(16, e -> {
            long now = System.currentTimeMillis();
            long dt = now - lastTick[0];
            lastTick[0] = now;

            engine.advanceTime(dt);
            fireworks.update(dt / 1000.0);

            if (engine.gameOver && !gameOverAnnounced[0]) {
                gameOverAnnounced[0] = true;
                System.out.println("GAME OVER - winner: " + engine.winner);
                SoundPlayer.playVictoryFanfare();
                gameOverAt[0] = now;
                nextBurstAt[0] = now;
                positionPlayAgainButton.run();
                playAgainButton.setVisible(true);
            }

            if (engine.gameOver && now - gameOverAt[0] < fireworksSpawnWindowMs && now >= nextBurstAt[0]) {
                double x = random.nextDouble() * panel.getWidth();
                double y = random.nextDouble() * panel.getHeight() * 0.6;
                fireworks.spawnBurst(x, y);
                nextBurstAt[0] = now + 300 + random.nextInt(400);
            }

            GameSnapshot snap = controller.snapshot();
            List<MoveLogEntry> whiteMoves = new ArrayList<>();
            List<MoveLogEntry> blackMoves = new ArrayList<>();
            for (MoveLogEntry entry : snap.moveLog) {
                (entry.white ? whiteMoves : blackMoves).add(entry);
            }
            whiteLog.sync(whiteMoves);
            blackLog.sync(blackMoves);
            whiteLog.setScore(snap.whiteScore);
            blackLog.setScore(snap.blackScore);

            for (int i = loggedMoveCount[0]; i < snap.moveLog.size(); i++) {
                if (snap.moveLog.get(i).notation.contains("x")) {
                    SoundPlayer.playCapture();
                }
            }
            loggedMoveCount[0] = snap.moveLog.size();

            panel.repaint();
        });
        gameLoop.start();
    }
}
