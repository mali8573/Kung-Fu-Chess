import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameEngineRequestMoveTest {

    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void rejectsWhenGameIsAlreadyOver() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        engine.gameOver = true;

        GameEngine.RequestResult result = engine.requestMove(0, 0, 0, 3);

        assertFalse(result.accepted);
        assertEquals("game_over", result.reason);
        assertTrue(engine.activeMoves.isEmpty());
    }

    @Test
    public void rejectsWhenSourcePieceIsAlreadyInFlight() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        engine.addMove(new MovingPiece(GameConstants.W_ROOK, 0, 0, 0, 2, 5000L));

        GameEngine.RequestResult result = engine.requestMove(0, 0, 0, 3);

        assertFalse(result.accepted);
        assertEquals("motion_in_progress", result.reason);
    }

    @Test
    public void rejectsMoveOntoFriendlyPiece() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        engine.board[0][3] = GameConstants.W_PAWN;

        GameEngine.RequestResult result = engine.requestMove(0, 0, 0, 3);

        assertFalse(result.accepted);
        assertEquals("friendly_destination", result.reason);
    }

    @Test
    public void rejectsGeometricallyIllegalMove() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_KNIGHT;

        GameEngine.RequestResult result = engine.requestMove(0, 0, 0, 1); // not an L-shape

        assertFalse(result.accepted);
        assertEquals("illegal_piece_move", result.reason);
    }

    @Test
    public void acceptsLegalMoveAndSchedulesArrivalFromConfigSpeed() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        engine.currentTime = 1000;

        GameEngine.RequestResult result = engine.requestMove(0, 0, 0, 3); // 3 squares

        assertTrue(result.accepted);
        assertEquals("ok", result.reason);
        assertEquals(1, engine.activeMoves.size());

        MovingPiece scheduled = engine.activeMoves.get(0);
        // move config speed is 1.5 m/s -> 3 squares / 1.5 = 2000ms, starting from currentTime=1000
        assertEquals(3000L, scheduled.arrivalTime);
    }
}
