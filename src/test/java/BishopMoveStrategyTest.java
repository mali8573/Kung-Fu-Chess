import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BishopMoveStrategyTest {
    @Test
    public void bishopValidatesDiagonals() {
        BishopMoveStrategy s = new BishopMoveStrategy();
        String[][] board = new String[8][8];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = GameConstants.EMPTY;
            }
        }
        assertTrue(s.isValid(0,0,3,3,board));
        assertFalse(s.isValid(0,0,0,1,board));
        assertFalse(s.isValid(0,0,2,1,board));
    }
}