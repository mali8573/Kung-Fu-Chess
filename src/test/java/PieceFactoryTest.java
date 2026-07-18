import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;
import rules.PieceFactory;

public class PieceFactoryTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void pathBlockedDetectsPieces() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_ROOK;
        board[0][1] = GameConstants.W_PAWN;
        assertTrue(PieceFactory.isPathBlocked(0,0,0,3,board));
    }

    @Test
    public void knightHasNoBlocking() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_KNIGHT;
        assertFalse(PieceFactory.isPathBlocked(0,0,2,1,board));
    }

    @Test
    public void isMoveLegalUsesStrategy() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_ROOK;
        assertTrue(PieceFactory.isMoveLegal(0,0,0,3,board));
    }
}
