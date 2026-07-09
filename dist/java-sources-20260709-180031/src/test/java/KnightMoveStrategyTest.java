import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KnightMoveStrategyTest {
    @Test
    public void knightMovesInLShape() {
        KnightMoveStrategy s = new KnightMoveStrategy();
        String[][] board = new String[8][8];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = GameConstants.EMPTY;
            }
        }
        assertTrue(s.isValid(0,0,2,1,board));
        assertTrue(s.isValid(0,0,1,2,board));
        assertFalse(s.isValid(0,0,2,2,board));
        assertFalse(s.isValid(0,0,0,1,board));
    }
}