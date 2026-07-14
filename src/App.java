import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                renderer.paint((Graphics2D) g, controller.snapshot(), getWidth(), getHeight());
            }
        };
        panel.setPreferredSize(new Dimension(720, 720));
        panel.setBackground(Color.DARK_GRAY);

        JFrame frame = new JFrame("Kung Fu Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
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

        panel.addMouseListener(new MouseAdapter() {
            // mouseReleased (not mouseClicked) - mouseClicked silently drops the event if the
            // mouse moves even one pixel between press and release, which reads as "unresponsive"
            // on a trackpad.
            @Override
            public void mouseReleased(MouseEvent e) {
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
                } else if (hadSelectionBefore && !hasSelectionNow) {
                    System.out.println("selection cancelled (clicked outside the board)");
                } else {
                    System.out.println("click ignored (empty cell or outside board)");
                }
            }
        });

        // Drives simulated game time from real wall-clock time, resolves arrived moves,
        // and repaints. This is the "frame" per the course design doc's render loop.
        final long[] lastTick = {System.currentTimeMillis()};
        final boolean[] gameOverAnnounced = {false};
        Timer gameLoop = new Timer(16, e -> {
            long now = System.currentTimeMillis();
            long dt = now - lastTick[0];
            lastTick[0] = now;

            engine.advanceTime(dt);

            if (engine.gameOver && !gameOverAnnounced[0]) {
                gameOverAnnounced[0] = true;
                System.out.println("GAME OVER - winner: " + engine.winner);
            }

            panel.repaint();
        });
        gameLoop.start();
    }
}
