import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameControllerTest {

    private static final int CELL = 100;

    private GameController newController(GameEngine engine) {
        BoardGeometry geometry = new BoardGeometry(8, 8);
        geometry.resize(8 * CELL, 8 * CELL); // fixed 100px/cell, no letterboxing
        return new GameController(engine, geometry);
    }

    private int px(int col) { return col * CELL + CELL / 2; }
    private int py(int row) { return row * CELL + CELL / 2; }

    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void firstClickOnOccupiedCellSelectsIt() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        GameController controller = newController(engine);

        GameEngine.RequestResult result = controller.click(px(0), py(0));

        assertNull(result); // a selection click, not a move
        assertTrue(controller.hasSelection());
        assertEquals(0, controller.getSelectedRow());
        assertEquals(0, controller.getSelectedCol());
    }

    @Test
    public void firstClickOnEmptyCellDoesNothing() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        GameController controller = newController(engine);

        GameEngine.RequestResult result = controller.click(px(3), py(3));

        assertNull(result);
        assertFalse(controller.hasSelection());
    }

    @Test
    public void firstClickOutsideBoardDoesNothing() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        GameController controller = newController(engine);

        GameEngine.RequestResult result = controller.click(-50, -50);

        assertNull(result);
        assertFalse(controller.hasSelection());
    }

    @Test
    public void clickOutsideBoardWhileSelectedCancelsSelectionWithoutMoving() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        GameController controller = newController(engine);

        controller.click(px(0), py(0)); // select
        GameEngine.RequestResult result = controller.click(-50, -50); // click outside

        assertNull(result); // no move was ever requested
        assertFalse(controller.hasSelection());
        assertEquals(GameConstants.W_ROOK, engine.board[0][0]); // untouched
        assertTrue(engine.activeMoves.isEmpty());
    }

    @Test
    public void secondClickOnFriendlyPieceAttemptsMoveInsteadOfSwitchingSelection() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        engine.board[0][1] = GameConstants.W_PAWN;
        GameController controller = newController(engine);

        controller.click(px(0), py(0));            // select the rook
        GameEngine.RequestResult result = controller.click(px(1), py(0)); // click own pawn

        assertNotNull(result); // per spec: any second click inside the board requests a move
        assertFalse(result.accepted);
        assertEquals("friendly_destination", result.reason);
        assertFalse(controller.hasSelection()); // selection clears either way
    }

    @Test
    public void secondClickOnLegalTargetAcceptsMoveAndClearsSelection() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        GameController controller = newController(engine);

        controller.click(px(0), py(0));
        GameEngine.RequestResult result = controller.click(px(3), py(0)); // straight line, empty

        assertTrue(result.accepted);
        assertEquals("ok", result.reason);
        assertFalse(controller.hasSelection());
        assertFalse(engine.activeMoves.isEmpty());
    }

    @Test
    public void snapshotReflectsCurrentSelection() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[2][2] = GameConstants.B_KING;
        GameController controller = newController(engine);

        controller.click(px(2), py(2));
        GameSnapshot snapshot = controller.snapshot();

        assertEquals(2, snapshot.selectedRow);
        assertEquals(2, snapshot.selectedCol);
    }
}
