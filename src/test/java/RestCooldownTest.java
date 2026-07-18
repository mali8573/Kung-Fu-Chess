import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RestCooldownTest {
    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void moveLandingStartsOneSecondRestCooldown() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[6][0] = GameConstants.W_PAWN;

        engine.addMove(new MovingPiece(GameConstants.W_PAWN, 6, 0, 5, 0, 500L));
        engine.currentTime = 500;
        engine.processMoves(); // lands at (5,0); rest cooldown starts now

        assertTrue(engine.isPieceResting(5, 0));
        assertEquals("resting", engine.requestMove(5, 0, 4, 0).reason);
        assertEquals("resting", engine.requestJump(5, 0).reason);

        engine.currentTime = 500 + 999;
        assertTrue(engine.isPieceResting(5, 0));

        engine.currentTime = 500 + 1000;
        assertFalse(engine.isPieceResting(5, 0));
        assertTrue(engine.requestMove(5, 0, 4, 0).accepted);
    }

    @Test
    public void restCooldownScalesWithDistanceTraveled() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[7][0] = GameConstants.W_ROOK;

        // 6 squares in a straight line -> 6 seconds of rest
        engine.addMove(new MovingPiece(GameConstants.W_ROOK, 7, 0, 1, 0, 4000L));
        engine.currentTime = 4000;
        engine.processMoves();

        assertTrue(engine.isPieceResting(1, 0));
        assertEquals("resting", engine.requestMove(1, 0, 0, 0).reason);

        engine.currentTime = 4000 + 5999;
        assertTrue(engine.isPieceResting(1, 0));

        engine.currentTime = 4000 + 6000;
        assertFalse(engine.isPieceResting(1, 0));
        assertTrue(engine.requestMove(1, 0, 0, 0).accepted);
    }

    @Test
    public void jumpLandingStartsHalfSecondRestCooldown() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[4][4] = GameConstants.W_ROOK;
        engine.currentTime = 0;

        GameEngine.RequestResult jumpResult = engine.requestJump(4, 4);
        assertTrue(jumpResult.accepted);

        long arrival = engine.activeMoves.get(0).arrivalTime;
        engine.currentTime = arrival;
        engine.processMoves(); // jump completes; rest cooldown starts now

        assertTrue(engine.isPieceResting(4, 4));
        assertEquals("resting", engine.requestMove(4, 4, 3, 4).reason);
        assertEquals("resting", engine.requestJump(4, 4).reason);

        engine.currentTime = arrival + 499;
        assertTrue(engine.isPieceResting(4, 4));

        engine.currentTime = arrival + 500;
        assertFalse(engine.isPieceResting(4, 4));
        assertTrue(engine.requestJump(4, 4).accepted);
    }
}
