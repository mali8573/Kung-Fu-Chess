import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import view.MoveLogPanel;
import engine.MoveLogEntry;

import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

public class MoveLogPanelTest {

    /** Depth-first search for the first descendant of the given type - MoveLogPanel keeps its
     *  JTable/JLabel children private, but they're real Swing components underneath, so their
     *  own public state (row count, cell values, label text) is fair game to inspect directly. */
    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return (T) child;
            if (child instanceof Container) {
                T found = find((Container) child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JLabel findScoreLabel(MoveLogPanel panel) {
        for (Component child : panel.getComponents()) {
            JLabel found = findLabelWithScoreText(child);
            if (found != null) return found;
        }
        return null;
    }

    private static JLabel findLabelWithScoreText(Component component) {
        if (component instanceof JLabel && ((JLabel) component).getText().startsWith("Score:")) {
            return (JLabel) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                JLabel found = findLabelWithScoreText(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    public void startsEmptyWithZeroScore() {
        MoveLogPanel panel = new MoveLogPanel("White", Color.WHITE);

        assertEquals(0, find(panel, JTable.class).getRowCount());
        assertEquals("Score: 0", findScoreLabel(panel).getText());
    }

    @Test
    public void syncAppendsOnlyTheNewEntries() {
        MoveLogPanel panel = new MoveLogPanel("White", Color.WHITE);
        JTable table = find(panel, JTable.class);

        List<MoveLogEntry> log = new ArrayList<>();
        log.add(new MoveLogEntry(true, "e4", 1000));
        panel.sync(log);
        assertEquals(1, table.getRowCount());
        assertEquals("e4", table.getValueAt(0, 1));

        log.add(new MoveLogEntry(true, "Nf3", 5000));
        panel.sync(log);
        assertEquals(2, table.getRowCount());
        assertEquals("e4", table.getValueAt(0, 1), "the first row must not be re-added or reordered");
        assertEquals("Nf3", table.getValueAt(1, 1));
    }

    @Test
    public void resyncingWithNoNewEntriesDoesNotDuplicateRows() {
        MoveLogPanel panel = new MoveLogPanel("White", Color.WHITE);
        JTable table = find(panel, JTable.class);

        List<MoveLogEntry> log = new ArrayList<>();
        log.add(new MoveLogEntry(true, "e4", 1000));
        panel.sync(log);
        panel.sync(log); // same list again - nothing new to append

        assertEquals(1, table.getRowCount());
    }

    @Test
    public void setScoreUpdatesTheLabel() {
        MoveLogPanel panel = new MoveLogPanel("Black", Color.DARK_GRAY);

        panel.setScore(7);

        assertEquals("Score: 7", findScoreLabel(panel).getText());
    }

    @Test
    public void clearEmptiesTheTableAndResetsTheScore() {
        MoveLogPanel panel = new MoveLogPanel("White", Color.WHITE);
        JTable table = find(panel, JTable.class);
        List<MoveLogEntry> log = new ArrayList<>();
        log.add(new MoveLogEntry(true, "e4", 1000));
        panel.sync(log);
        panel.setScore(3);

        panel.clear();

        assertEquals(0, table.getRowCount());
        assertEquals("Score: 0", findScoreLabel(panel).getText());

        // And the panel must be able to grow again after being cleared, from row zero.
        panel.sync(log);
        assertEquals(1, table.getRowCount());
    }

    @Test
    public void formatsGameTimeAsMinutesSecondsMillis() {
        MoveLogPanel panel = new MoveLogPanel("White", Color.WHITE);
        JTable table = find(panel, JTable.class);
        List<MoveLogEntry> log = new ArrayList<>();
        log.add(new MoveLogEntry(true, "e4", 65432)); // 1:05.432

        panel.sync(log);

        assertEquals("01:05.432", table.getValueAt(0, 0));
    }
}
