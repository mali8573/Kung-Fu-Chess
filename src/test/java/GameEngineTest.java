import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;
import engine.GameEngine;
import engine.MovingPiece;

public class GameEngineTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void processMoves_movesPieceOnArrival() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_PAWN;
        MovingPiece mp = new MovingPiece(GameConstants.W_PAWN,6,0,5,0,1L);
        g.addMove(mp);
        g.currentTime = 1;
        g.processMoves();
        assertEquals(GameConstants.W_PAWN, g.board[5][0]);
        assertEquals(GameConstants.EMPTY, g.board[6][0]);
        assertTrue(g.activeMoves.isEmpty());
    }

    @Test
    public void processMoves_capturedWhenDestinationHasStationaryEnemy() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[4][4] = GameConstants.B_ROOK;
        g.board[6][4] = GameConstants.W_ROOK;
        MovingPiece jumper = new MovingPiece(GameConstants.B_ROOK,4,4,4,4,0L);
        MovingPiece mover = new MovingPiece(GameConstants.W_ROOK,6,4,4,4,1L);
        g.addMove(jumper);
        g.addMove(mover);
        g.currentTime = 1;
        g.processMoves();
        assertEquals(GameConstants.EMPTY, g.board[6][4]);
        assertEquals(GameConstants.W_ROOK, g.board[4][4]);
        assertTrue(g.activeMoves.isEmpty());
    }

    @Test
    public void processMoves_doesNothingForMovesNotYetArrived() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_PAWN;
        MovingPiece mp = new MovingPiece(GameConstants.W_PAWN,6,0,5,0,5L);
        g.addMove(mp);
        g.currentTime = 1;

        g.processMoves();

        assertEquals(GameConstants.W_PAWN, g.board[6][0]);
        assertEquals(GameConstants.EMPTY, g.board[5][0]);
        assertFalse(g.activeMoves.isEmpty());
    }
}
