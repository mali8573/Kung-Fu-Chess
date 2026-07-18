import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * A styled Time/Move table for one side's move history, matching the course spec's
 * "moves log" mockup. Purely a view - it never touches the board or game logic, only
 * displays whatever MoveLogEntry list it is given via sync().
 */
public class MoveLogPanel extends JPanel {
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel scoreLabel;
    private int renderedCount = 0;
    private int lastScore = -1;

    private static final Color PANEL_BACKGROUND = Color.BLACK;
    private static final Color MOVE_TEXT_COLOR = BoardRenderer.SELECTION_COLOR;

    public MoveLogPanel(String title, Color accentColor) {
        setLayout(new BorderLayout());
        setBackground(PANEL_BACKGROUND);

        JLabel header = new JLabel(title, SwingConstants.CENTER);
        header.setOpaque(true);
        header.setBackground(accentColor);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        header.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        scoreLabel.setOpaque(true);
        scoreLabel.setBackground(PANEL_BACKGROUND);
        scoreLabel.setForeground(MOVE_TEXT_COLOR);
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 16f));
        scoreLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JPanel headerBlock = new JPanel(new BorderLayout());
        headerBlock.add(header, BorderLayout.NORTH);
        headerBlock.add(scoreLabel, BorderLayout.SOUTH);
        add(headerBlock, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"Time", "Move"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        table.setRowHeight(32);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setShowGrid(true);
        table.setGridColor(new Color(50, 50, 50));
        table.setFillsViewportHeight(true);
        table.setBackground(PANEL_BACKGROUND);
        table.setForeground(MOVE_TEXT_COLOR);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 15f));
        table.getTableHeader().setBackground(accentColor);
        table.getTableHeader().setForeground(Color.WHITE);

        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        centered.setBackground(PANEL_BACKGROUND);
        centered.setForeground(MOVE_TEXT_COLOR);
        table.setDefaultRenderer(Object.class, centered);

        JScrollPane scroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PANEL_BACKGROUND);
        scroll.setPreferredSize(new Dimension(190, 720));
        add(scroll, BorderLayout.CENTER);
    }

    /** Empties the table and resets the score line - used when starting a new game. */
    public void clear() {
        model.setRowCount(0);
        renderedCount = 0;
        lastScore = -1;
        scoreLabel.setText("Score: 0");
    }

    /** Updates the score line (total point-value of enemy pieces this side has captured). */
    public void setScore(int score) {
        if (score == lastScore) return;
        lastScore = score;
        scoreLabel.setText("Score: " + score);
    }

    /** Appends any entries not yet shown (the log only ever grows) and scrolls to the newest one. */
    public void sync(List<MoveLogEntry> entries) {
        if (entries.size() <= renderedCount) return;

        for (int i = renderedCount; i < entries.size(); i++) {
            MoveLogEntry e = entries.get(i);
            model.addRow(new Object[]{formatTime(e.gameTimeMillis), e.notation});
        }
        renderedCount = entries.size();

        int lastRow = table.getRowCount() - 1;
        if (lastRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
        }
    }

    private static String formatTime(long millis) {
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        long millisPart = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millisPart);
    }
}
