import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RuleEngineTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void returnsSrcEmpty() {
        String[][] b = emptyBoard(8);
        assertEquals(RuleEngine.MoveResult.SRC_EMPTY, RuleEngine.checkMove(6,0,5,0,b));
    }

    @Test
    public void returnsTargetFriendly() {
        String[][] b = emptyBoard(8);
        b[6][0] = GameConstants.W_PAWN;
        b[5][0] = GameConstants.W_ROOK;
        assertEquals(RuleEngine.MoveResult.TARGET_FRIENDLY, RuleEngine.checkMove(6,0,5,0,b));
    }

    @Test
    public void returnsOkForSimpleMove() {
        String[][] b = emptyBoard(8);
        b[6][0] = GameConstants.W_PAWN;
        assertEquals(RuleEngine.MoveResult.OK, RuleEngine.checkMove(6,0,5,0,b));
    }
}
