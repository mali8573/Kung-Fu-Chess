package view;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;

import engine.GameSnapshot;

/**
 * The board-rendering panel plus the two move-log panels and the row layout wrapping them -
 * the one chunk of Swing UI that was byte-for-byte identical between App.java (local play) and
 * NetworkApp.java (networked play). snapshotSupplier is how the two differ: App's never
 * returns null (it calls a local GameController directly); NetworkApp's returns null until the
 * first server STATE arrives, in which case nothing is painted at all this frame - same as
 * both apps' original inline paintComponent bodies did.
 */
public class GameBoardView {
    private final JPanel boardPanel;
    private final JPanel row;
    private final MoveLogPanel blackLog;
    private final MoveLogPanel whiteLog;

    public GameBoardView(BoardRenderer renderer, FireworksEffect fireworks,
                          Supplier<GameSnapshot> snapshotSupplier, int preferredLogWidth) {
        blackLog = new MoveLogPanel("Black", new Color(40, 40, 40));
        whiteLog = new MoveLogPanel("White", new Color(90, 90, 90));

        boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                GameSnapshot snapshot = snapshotSupplier.get();
                if (snapshot == null) return;
                renderer.paint((Graphics2D) g, snapshot, getWidth(), getHeight());
                fireworks.render((Graphics2D) g);
            }
        };
        boardPanel.setPreferredSize(new Dimension(720, 720));
        boardPanel.setBackground(Color.BLACK);

        row = new JPanel(new BoardRowLayout(blackLog, boardPanel, whiteLog, preferredLogWidth));
        row.setBackground(Color.BLACK);
        row.add(blackLog);
        row.add(boardPanel);
        row.add(whiteLog);
    }

    public JPanel getBoardPanel() { return boardPanel; }
    public JPanel getRow() { return row; }
    public MoveLogPanel getBlackLog() { return blackLog; }
    public MoveLogPanel getWhiteLog() { return whiteLog; }
}
