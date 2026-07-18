import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;
import engine.GameEngine;
import engine.MovingPiece;

public class SimultaneousArrivalTieTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void arrivalsWithSameTimeRespectInsertionOrder() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_ROOK;
        g.board[6][7] = GameConstants.B_ROOK;

        // both will arrive at (4,4) at time 10
        g.addMove(new MovingPiece(GameConstants.W_ROOK,6,0,4,4,10L));
        g.addMove(new MovingPiece(GameConstants.B_ROOK,6,7,4,4,10L));

        g.currentTime = 10;
        g.processMoves();

        // stable sort + insertion order -> W_ROOK placed first, then B_ROOK captures it
        // according to current engine behavior, the second mover will overwrite the first
        assertEquals(GameConstants.B_ROOK, g.board[4][4]);
    }
}
