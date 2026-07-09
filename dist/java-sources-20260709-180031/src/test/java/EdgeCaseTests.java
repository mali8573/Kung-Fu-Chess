import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EdgeCaseTests {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void swapInsertionOrder_firstAddedWins() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[0][0] = GameConstants.W_ROOK; // A
        g.board[0][7] = GameConstants.B_ROOK; // B

        // Both arrive at same time to other's square
        g.addMove(new MovingPiece(GameConstants.W_ROOK,0,0,0,7,10L)); // added first
        g.addMove(new MovingPiece(GameConstants.B_ROOK,0,7,0,0,10L)); // added second

        g.currentTime = 10;
        g.processMoves();

        // First added (W_ROOK) captures B at 0,7; B's move is canceled because its source no longer matches
        assertEquals(GameConstants.W_ROOK, g.board[0][7]);
        assertEquals(GameConstants.EMPTY, g.board[0][0]);
    }

    @Test
    public void swapInsertionOrder_secondAddedWinsWhenReversed() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[0][0] = GameConstants.W_ROOK; // A
        g.board[0][7] = GameConstants.B_ROOK; // B

        // Add in opposite order
        g.addMove(new MovingPiece(GameConstants.B_ROOK,0,7,0,0,10L)); // added first
        g.addMove(new MovingPiece(GameConstants.W_ROOK,0,0,0,7,10L)); // added second

        g.currentTime = 10;
        g.processMoves();

        // Now B (first added) captures A at 0,0; second move canceled
        assertEquals(GameConstants.B_ROOK, g.board[0][0]);
        assertEquals(GameConstants.EMPTY, g.board[0][7]);
    }

    @Test
    public void threeWayArrival_lastAddedSurvives() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_ROOK;
        g.board[6][7] = GameConstants.B_ROOK;
        g.board[5][6] = GameConstants.W_BISHOP;

        g.addMove(new MovingPiece(GameConstants.W_ROOK,6,0,4,4,20L));
        g.addMove(new MovingPiece(GameConstants.B_ROOK,6,7,4,4,20L));
        g.addMove(new MovingPiece(GameConstants.W_BISHOP,5,6,4,4,20L)); // last added (white)

        g.currentTime = 20;
        g.processMoves();

        // The last added mover will be the final occupant under current engine behavior
        assertEquals(GameConstants.W_BISHOP, g.board[4][4]);
    }

    @Test
    public void canceledMoveWhenSourceCapturedEarlier() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[4][4] = GameConstants.W_ROOK; // A
        g.board[6][4] = GameConstants.B_ROOK; // B

        // B captures A at time 1
        g.addMove(new MovingPiece(GameConstants.B_ROOK,6,4,4,4,1L));
        // A attempts to move away at time 2 (but will have been captured)
        g.addMove(new MovingPiece(GameConstants.W_ROOK,4,4,4,5,2L));

        g.currentTime = 1;
        g.processMoves();

        assertEquals(GameConstants.B_ROOK, g.board[4][4]);

        g.currentTime = 2;
        g.processMoves();

        // A's move should not execute because source no longer contains W_ROOK
        assertEquals(GameConstants.B_ROOK, g.board[4][4]);
        assertEquals(GameConstants.EMPTY, g.board[4][5]);
    }
}
