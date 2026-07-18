package view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * Lays out [blackLog | board | whiteLog] so the board stays square (sized from whichever of
 * width/height is the tighter constraint) and the two log panels split *all* remaining width
 * between them - never leaving empty margin between a log panel and the board. The logs are
 * always shown, however small the window gets - they shrink along with everything else
 * rather than disappearing.
 *
 * A real LayoutManager (invoked by Swing's own validate/revalidate machinery) instead of a
 * componentResized listener that reactively mutates preferredSize - that reactive approach
 * could compute against a stale intermediate size mid-resize (e.g. during a maximize
 * animation) and never get corrected by a follow-up event, leaving the board collapsed.
 */
public class BoardRowLayout implements LayoutManager {
    private final Component blackLog;
    private final Component board;
    private final Component whiteLog;
    private final int preferredLogWidth;

    public BoardRowLayout(Component blackLog, Component board, Component whiteLog, int preferredLogWidth) {
        this.blackLog = blackLog;
        this.board = board;
        this.whiteLog = whiteLog;
        this.preferredLogWidth = preferredLogWidth;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(720 + 2 * preferredLogWidth, 720);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(100, 100);
    }

    @Override
    public void layoutContainer(Container parent) {
        int totalWidth = parent.getWidth();
        int totalHeight = parent.getHeight();
        // A live mouse-drag resize can momentarily report a bogus intermediate size (even 0x0)
        // before Windows settles on the final one. Skip that pass entirely rather than collapse
        // everything to it - the very next (valid) resize event lays out correctly again.
        if (totalWidth <= 0 || totalHeight <= 0) return;

        int squareSize = Math.max(0, Math.min(totalWidth, totalHeight));
        int logWidth = (totalWidth - squareSize) / 2;

        blackLog.setBounds(0, 0, logWidth, squareSize);
        board.setBounds(logWidth, 0, squareSize, squareSize);
        whiteLog.setBounds(logWidth + squareSize, 0, totalWidth - logWidth - squareSize, squareSize);
    }
}
