import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;
import engine.GameEngine;
import engine.MovingPiece;

public class KingCaptureEndsGameTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void capturingKingSetsGameOver() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[4][4] = GameConstants.B_KING;
        g.board[6][4] = GameConstants.W_ROOK;

        g.addMove(new MovingPiece(GameConstants.W_ROOK,6,4,4,4,1L));
        g.currentTime = 1;
        g.processMoves();

        assertTrue(g.gameOver);
        assertEquals(GameConstants.W_ROOK, g.board[4][4]);
    }
}
