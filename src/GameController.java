/**
 * Translates clicks into game commands. Owns the selected-cell state (per the course
 * spec, this does NOT belong to GameEngine). Never decides chess legality itself -
 * that's RuleEngine's job via GameEngine.requestMove.
 */
public class GameController {

    private final GameEngine engine;
    private final BoardGeometry geometry;

    private int selectedRow = -1;
    private int selectedCol = -1;

    public GameController(GameEngine engine, BoardGeometry geometry) {
        this.engine = engine;
        this.geometry = geometry;
    }

    public int getSelectedRow() { return selectedRow; }
    public int getSelectedCol() { return selectedCol; }
    public boolean hasSelection() { return selectedRow != -1; }

    /** Drops any pending selection - used when starting a new game. */
    public synchronized void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
    }

    /**
     * Handles a click at raw pixel coordinates. Returns the move outcome when this click
     * triggered a requestMove, or null otherwise (selection, ignored click, or cancel) -
     * callers use this to show the user whether a move was actually accepted.
     *
     * - No selection yet: click on an occupied cell selects it; click on empty/outside is ignored.
     * - Selection active: click outside the board cancels the selection (no move sent);
     *   any click inside the board requests a move and clears the selection either way.
     */
    public synchronized GameEngine.RequestResult click(int pixelX, int pixelY) {
        int[] cell = geometry.pixelToCell(pixelX, pixelY);

        if (cell == null) {
            selectedRow = -1;
            selectedCol = -1;
            return null;
        }

        int row = cell[0], col = cell[1];

        if (!hasSelection()) {
            boolean occupied = !engine.board[row][col].equals(GameConstants.EMPTY);
            boolean unavailable = engine.isPieceInFlight(row, col) || engine.isPieceResting(row, col);
            if (occupied && !unavailable) {
                selectedRow = row;
                selectedCol = col;
            }
            return null;
        }

        GameEngine.RequestResult result = engine.requestMove(selectedRow, selectedCol, row, col);
        selectedRow = -1;
        selectedCol = -1;
        return result;
    }

    /**
     * Handles a right-click (jump) at raw pixel coordinates. Independent of the current
     * left-click selection - it acts directly on whatever piece is under the cursor.
     * Returns null if the click landed outside the board.
     */
    public synchronized GameEngine.RequestResult jump(int pixelX, int pixelY) {
        int[] cell = geometry.pixelToCell(pixelX, pixelY);
        if (cell == null) return null;

        return engine.requestJump(cell[0], cell[1]);
    }

    public GameSnapshot snapshot() {
        return engine.snapshot(selectedRow, selectedCol);
    }
}
