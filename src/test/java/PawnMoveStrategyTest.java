import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PawnMoveStrategyTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void whitePawnMovesForwardAndDouble() {
        PawnMoveStrategy s = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        assertTrue(s.isValid(6,0,5,0,board));
        assertTrue(s.isValid(6,0,4,0,board));
    }

    @Test
    public void pawnCapturesDiagonally() {
        PawnMoveStrategy s = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        board[5][1] = GameConstants.B_PAWN;
        assertTrue(s.isValid(6,0,5,1,board));
    }

    @Test
    public void pawnCannotDoubleStepAfterLeavingItsStartingRow() {
        PawnMoveStrategy s = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[5][0] = GameConstants.W_PAWN; // already moved once, off row 6
        assertFalse(s.isValid(5, 0, 3, 0, board));
    }

    @Test
    public void pawnCannotMoveBackward() {
        PawnMoveStrategy s = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        assertFalse(s.isValid(6,0,7,0,board));
    }
}
