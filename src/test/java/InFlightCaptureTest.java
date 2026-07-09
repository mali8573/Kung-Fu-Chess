import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InFlightCaptureTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void attackerCapturesPieceStillAtSource() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        // defender at 4,4 will move to 4,5 later
        g.board[4][4] = GameConstants.B_ROOK;
        g.board[6][4] = GameConstants.W_ROOK;

        // defender departs at time 100
        g.addMove(new MovingPiece(GameConstants.B_ROOK, 4,4, 4,5, 100L));
        // attacker arrives earlier at defender's source at time 50
        g.addMove(new MovingPiece(GameConstants.W_ROOK, 6,4, 4,4, 50L));

        g.currentTime = 50;
        g.processMoves();

        // attacker should have captured defender at source (defender had not left yet)
        assertEquals(GameConstants.W_ROOK, g.board[4][4]);
        assertEquals(GameConstants.EMPTY, g.board[6][4]);
    }
}
