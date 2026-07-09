import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RookMoveStrategyTest {
    @Test
    public void rookValidatesStraightMoves() {
        RookMoveStrategy s = new RookMoveStrategy();
        String[][] board = new String[8][8];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = GameConstants.EMPTY;
            }
        }
        assertTrue(s.isValid(0,0,0,5,board));
        assertTrue(s.isValid(2,3,5,3,board));
        assertFalse(s.isValid(1,1,2,2,board));
        assertFalse(s.isValid(1,1,3,4,board));
    }
}