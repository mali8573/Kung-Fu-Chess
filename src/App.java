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
import view.BoardRowLayout;
import view.FireworksEffect;
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

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                renderer.paint((Graphics2D) g, controller.snapshot(), getWidth(), getHeight());
                fireworks.render((Graphics2D) g);
            }
        };
        panel.setPreferredSize(new Dimension(720, 720));
        // Black to match the moves-log panels flanking it, so there is no visible seam
        // between them regardless of how the window is resized.
        panel.setBackground(Color.BLACK);
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

        MoveLogPanel blackLog = new MoveLogPanel("Black", new Color(40, 40, 40));
        MoveLogPanel whiteLog = new MoveLogPanel("White", new Color(90, 90, 90));

        JLabel title = new JLabel("Kung Fu Chess", SwingConstants.CENTER);
        title.setOpaque(true);
        title.setBackground(Color.BLACK);
        title.setForeground(BoardRenderer.SELECTION_COLOR);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 48));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 32, 0));

        // A dedicated LayoutManager (BoardRowLayout) instead of a componentResized listener
        // that reactively mutates preferredSize - the reactive approach could compute against
        // a stale intermediate size mid-resize (e.g. during a maximize animation) and never
        // get corrected, leaving the board collapsed. A real LayoutManager is invoked by
        // Swing's own validate machinery, always against the final settled size.
        final int preferredLogWidth = 220;
        JPanel boardRow = new JPanel(new BoardRowLayout(blackLog, panel, whiteLog, preferredLogWidth));
        boardRow.setBackground(Color.BLACK);
        boardRow.add(blackLog);
        boardRow.add(panel);
        boardRow.add(whiteLog);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.BLACK);
        root.add(title, BorderLayout.NORTH);
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
                        if (result.accepted) SoundPlayer.playJump();
                    }
                    return;
                }

                boolean hadSelectionBefore = controller.hasSelection();
                GameEngine.RequestResult result = controller.click(e.getX(), e.getY());
                boolean hasSelectionNow = controller.hasSelection();

                if (!hadSelectionBefore && hasSelectionNow) {
                    System.out.println("selected " + engine.board[controller.getSelectedRow()][controller.getSelectedCol()]
                            + " at row=" + controller.getSelectedRow() + " col=" + controller.getSelectedCol());
                } else if (result != null) {
                    System.out.println(result.accepted
                            ? "move accepted"
                            : "move REJECTED - reason: " + result.reason);
                    if (result.accepted) SoundPlayer.playMove();
                } else if (hadSelectionBefore && !hasSelectionNow) {
                    System.out.println("selection cancelled (clicked outside the board)");
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
