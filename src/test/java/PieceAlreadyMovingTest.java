import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PieceAlreadyMovingTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void pieceIsInFlightWhileMoveIsPending() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_PAWN;

        assertFalse(g.isPieceInFlight(6, 0));

        g.addMove(new MovingPiece(GameConstants.W_PAWN, 6, 0, 5, 0, 1000L));
        assertTrue(g.isPieceInFlight(6, 0));

        g.currentTime = 1000;
        g.processMoves();

        // The move resolved, so the source square is free again (the piece is now at 5,0).
        assertFalse(g.isPieceInFlight(6, 0));
        assertFalse(g.isPieceInFlight(5, 0));
    }

    @Test
    public void ruleEngineRejectsCommandingAPieceAlreadyMidMove() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_ROOK;
        g.addMove(new MovingPiece(GameConstants.W_ROOK, 6, 0, 3, 0, 1000L));

        // A second command to the same still-moving piece must be rejected up front...
        assertEquals(RuleEngine.MoveResult.SRC_BUSY,
                RuleEngine.checkMove(6, 0, 6, 5, g.board, g));

        // ...even though the plain board-only check would have allowed it, since the board
        // array doesn't clear the source square until the move actually arrives.
        assertEquals(RuleEngine.MoveResult.OK, RuleEngine.checkMove(6, 0, 6, 5, g.board));

        // Only one move should ever be pending for that piece.
        long pending = g.activeMoves.stream().filter(m -> m.fromRow == 6 && m.fromCol == 0).count();
        assertEquals(1, pending);
    }
}
