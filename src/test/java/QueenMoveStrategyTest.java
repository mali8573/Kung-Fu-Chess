import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;
import rules.pieces.QueenMoveStrategy;

public class QueenMoveStrategyTest {
    @Test
    public void queenCombinesRookAndBishop() {
        QueenMoveStrategy s = new QueenMoveStrategy();
        String[][] board = new String[8][8];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = GameConstants.EMPTY;
            }
        }
        assertTrue(s.isValid(0,0,0,5,board));
        assertTrue(s.isValid(0,0,3,3,board));
        assertFalse(s.isValid(0,0,1,2,board));
        assertFalse(s.isValid(0,0,2,1,board));
    }
}
