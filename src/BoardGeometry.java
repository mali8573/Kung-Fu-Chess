/**
 * Pure pixel<->cell math for a square rows x cols board rendered inside an
 * arbitrarily sized panel. No AWT/Swing/Img dependency, so it can be unit
 * tested without a display.
 */
public class BoardGeometry {

    private final int rows;
    private final int cols;

    private int originX;
    private int originY;
    private int boardSize;
    private double cellSize;

    public BoardGeometry(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /** Recomputes layout for the current panel size. Call on every resize. */
    public void resize(int panelWidth, int panelHeight) {
        boardSize = Math.max(0, Math.min(panelWidth, panelHeight));
        originX = (panelWidth - boardSize) / 2;
        originY = (panelHeight - boardSize) / 2;
        cellSize = boardSize / (double) Math.max(rows, cols);
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getBoardSize() { return boardSize; }
    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public double getCellSize() { return cellSize; }

    /** Returns {row, col} for a pixel, or null if the pixel is outside the board. */
    public int[] pixelToCell(int px, int py) {
        if (boardSize <= 0) return null;
        int localX = px - originX;
        int localY = py - originY;
        if (localX < 0 || localY < 0 || localX >= boardSize || localY >= boardSize) return null;

        int col = Math.min((int) (localX / cellSize), cols - 1);
        int row = Math.min((int) (localY / cellSize), rows - 1);
        return new int[] { row, col };
    }

    public int cellTopLeftX(int col) { return originX + (int) Math.round(col * cellSize); }
    public int cellTopLeftY(int row) { return originY + (int) Math.round(row * cellSize); }
}
