import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameEngineResetTest {
    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void resetClearsGameOverScoresMovesAndActiveMoves() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[4][4] = GameConstants.B_KING;
        engine.board[6][4] = GameConstants.W_ROOK;

        engine.addMove(new MovingPiece(GameConstants.W_ROOK, 6, 4, 4, 4, 1L));
        engine.currentTime = 1;
        engine.processMoves();

        assertTrue(engine.gameOver);
        assertEquals("white", engine.winner);
        assertFalse(engine.snapshot(-1, -1).moveLog.isEmpty());

        String[][] freshBoard = emptyBoard(8);
        freshBoard[6][0] = GameConstants.W_PAWN;
        engine.reset(freshBoard);

        assertFalse(engine.gameOver);
        assertNull(engine.winner);
        assertEquals(0, engine.currentTime);
        assertEquals(0, engine.whiteScore);
        assertEquals(0, engine.blackScore);
        assertTrue(engine.activeMoves.isEmpty());
        assertTrue(engine.snapshot(-1, -1).moveLog.isEmpty());
        assertEquals(GameConstants.W_PAWN, engine.board[6][0]);

        // A piece that had rested under the old game should not still be "resting" after reset.
        assertFalse(engine.isPieceResting(6, 0));
    }
}
