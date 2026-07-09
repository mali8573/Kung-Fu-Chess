import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KingMoveStrategyTest {
    @Test
    public void kingMovesOneSquareOrLess() {
        KingMoveStrategy s = new KingMoveStrategy();
        String[][] board = new String[8][8];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = GameConstants.EMPTY;
            }
        }
        assertTrue(s.isValid(4,4,5,5,board));
        assertTrue(s.isValid(4,4,4,5,board));
        assertFalse(s.isValid(4,4,6,6,board));
    }
}