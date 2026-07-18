import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;
import engine.GameEngine;
import engine.MovingPiece;

public class ScoreTest {
    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void capturingAPieceAwardsItsPointValueToTheCapturersScore() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[4][4] = GameConstants.B_ROOK; // worth 5
        engine.board[6][4] = GameConstants.W_KNIGHT;

        assertEquals(0, engine.whiteScore);
        assertEquals(0, engine.blackScore);

        engine.addMove(new MovingPiece(GameConstants.W_KNIGHT, 6, 4, 4, 4, 1L));
        engine.currentTime = 1;
        engine.processMoves();

        assertEquals(5, engine.whiteScore);
        assertEquals(0, engine.blackScore);
    }

    @Test
    public void scoresAccumulateSeparatelyPerSideAcrossMultipleCaptures() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.B_PAWN;  // worth 1, white will capture
        engine.board[7][7] = GameConstants.W_QUEEN; // worth 9, black will capture
        engine.board[1][1] = GameConstants.W_ROOK;
        engine.board[6][6] = GameConstants.B_BISHOP;

        engine.addMove(new MovingPiece(GameConstants.W_ROOK, 1, 1, 0, 0, 1L));
        engine.addMove(new MovingPiece(GameConstants.B_BISHOP, 6, 6, 7, 7, 1L));
        engine.currentTime = 1;
        engine.processMoves();

        assertEquals(1, engine.whiteScore);
        assertEquals(9, engine.blackScore);
    }

    @Test
    public void capturingTheKingDoesNotAwardPoints() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[4][4] = GameConstants.B_KING;
        engine.board[6][4] = GameConstants.W_ROOK;

        engine.addMove(new MovingPiece(GameConstants.W_ROOK, 6, 4, 4, 4, 1L));
        engine.currentTime = 1;
        engine.processMoves();

        assertTrue(engine.gameOver);
        assertEquals(0, engine.whiteScore);
    }
}
