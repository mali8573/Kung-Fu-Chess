import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import view.BoardRowLayout;

import javax.swing.JPanel;
import java.awt.Panel;

public class BoardRowLayoutTest {

    @Test
    public void squareContainerSplitsLeftoverWidthEquallyBetweenTheTwoLogs() {
        Panel blackLog = new Panel();
        Panel board = new Panel();
        Panel whiteLog = new Panel();
        JPanel container = new JPanel(new BoardRowLayout(blackLog, board, whiteLog, 220));
        container.add(blackLog);
        container.add(board);
        container.add(whiteLog);

        container.setBounds(0, 0, 1000, 700);
        container.doLayout();

        // squareSize = min(1000, 700) = 700; leftover width = 300, split 150/150.
        assertEquals(700, board.getWidth());
        assertEquals(700, board.getHeight());
        assertEquals(150, blackLog.getWidth());
        assertEquals(150, whiteLog.getWidth());
        assertEquals(150, board.getX());
        assertEquals(0, blackLog.getX());
        assertEquals(850, whiteLog.getX());
    }

    @Test
    public void boardStaysSquareEvenWhenHeightIsTheTighterConstraint() {
        Panel blackLog = new Panel();
        Panel board = new Panel();
        Panel whiteLog = new Panel();
        JPanel container = new JPanel(new BoardRowLayout(blackLog, board, whiteLog, 220));
        container.add(blackLog);
        container.add(board);
        container.add(whiteLog);

        container.setBounds(0, 0, 400, 300);
        container.doLayout();

        assertEquals(300, board.getWidth());
        assertEquals(300, board.getHeight());
        assertEquals(50, blackLog.getWidth()); // (400-300)/2
        assertEquals(50, whiteLog.getWidth());
    }

    @Test
    public void logPanelsAreNeverHiddenNoMatterHowSmallTheWindowGets() {
        Panel blackLog = new Panel();
        Panel board = new Panel();
        Panel whiteLog = new Panel();
        JPanel container = new JPanel(new BoardRowLayout(blackLog, board, whiteLog, 220));
        container.add(blackLog);
        container.add(board);
        container.add(whiteLog);

        container.setBounds(0, 0, 120, 100);
        container.doLayout();

        assertEquals(100, board.getWidth());
        assertEquals(10, blackLog.getWidth()); // (120-100)/2, shrunk but still present
        assertEquals(10, whiteLog.getWidth());
    }

    @Test
    public void aMomentarilyBogusZeroSizeDuringResizeIsSkippedWithoutThrowing() {
        Panel blackLog = new Panel();
        Panel board = new Panel();
        Panel whiteLog = new Panel();
        JPanel container = new JPanel(new BoardRowLayout(blackLog, board, whiteLog, 220));
        container.add(blackLog);
        container.add(board);
        container.add(whiteLog);

        // First lay out normally so the components have known bounds...
        container.setBounds(0, 0, 800, 800);
        container.doLayout();
        // ...then simulate the bogus intermediate 0x0 a live drag-resize can report.
        container.setBounds(0, 0, 0, 0);
        assertDoesNotThrow(container::doLayout);

        // Bounds from the last valid layout are left untouched, not collapsed to zero.
        assertEquals(800, board.getWidth());
    }

    @Test
    public void preferredAndMinimumSizesAreReportedForSwingsLayoutMachinery() {
        Panel blackLog = new Panel();
        Panel board = new Panel();
        Panel whiteLog = new Panel();
        BoardRowLayout layout = new BoardRowLayout(blackLog, board, whiteLog, 220);
        JPanel container = new JPanel(layout);

        assertEquals(720 + 2 * 220, layout.preferredLayoutSize(container).width);
        assertEquals(720, layout.preferredLayoutSize(container).height);
        assertEquals(100, layout.minimumLayoutSize(container).width);
    }
}
