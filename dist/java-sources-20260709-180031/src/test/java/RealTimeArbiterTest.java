import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RealTimeArbiterTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void advanceTimeProcessesArrivals() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_PAWN;
        g.addMove(new MovingPiece(GameConstants.W_PAWN,6,0,5,0,1000L));

        RealTimeArbiter arb = new RealTimeArbiter(g);
        arb.advanceTime(999);
        // not arrived yet
        assertEquals(GameConstants.W_PAWN, g.board[6][0]);

        arb.advanceTime(1);
        assertEquals(GameConstants.W_PAWN, g.board[5][0]);
        assertEquals(GameConstants.EMPTY, g.board[6][0]);
    }
}
