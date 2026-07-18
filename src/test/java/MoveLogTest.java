import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MoveLogTest {
    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    @Test
    public void pawnOpeningMoveIsLoggedInAlgebraicNotation() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[6][4] = GameConstants.W_PAWN; // e2

        GameEngine.RequestResult result = engine.requestMove(6, 4, 4, 4); // e2-e4
        assertTrue(result.accepted);

        engine.currentTime = engine.activeMoves.get(0).arrivalTime;
        engine.processMoves();

        GameSnapshot snapshot = engine.snapshot(-1, -1);
        assertEquals(1, snapshot.moveLog.size());
        MoveLogEntry entry = snapshot.moveLog.get(0);
        assertTrue(entry.white);
        assertEquals("e4", entry.notation);
    }

    @Test
    public void captureIsLoggedWithPieceLetterAndXAndTimestamp() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[4][3] = GameConstants.W_KNIGHT;
        engine.board[3][5] = GameConstants.B_PAWN; // enemy sitting on the knight's destination

        engine.currentTime = 2000;
        GameEngine.RequestResult result = engine.requestMove(4, 3, 3, 5);
        assertTrue(result.accepted);

        engine.currentTime = engine.activeMoves.get(0).arrivalTime;
        engine.processMoves();

        GameSnapshot snapshot = engine.snapshot(-1, -1);
        assertEquals(1, snapshot.moveLog.size());
        MoveLogEntry entry = snapshot.moveLog.get(0);
        assertTrue(entry.white);
        assertEquals("Nxf5", entry.notation);
        assertEquals(engine.currentTime, entry.gameTimeMillis);
    }

    @Test
    public void jumpsAndCanceledMovesAreNeverLogged() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[4][4] = GameConstants.W_ROOK;
        engine.board[4][5] = GameConstants.W_PAWN; // friendly - blocks a would-be move there

        engine.requestJump(4, 4);
        engine.currentTime = engine.activeMoves.get(0).arrivalTime;
        engine.processMoves();

        engine.addMove(new MovingPiece(GameConstants.W_ROOK, 4, 4, 4, 5, engine.currentTime + 1000));
        engine.currentTime += 1000;
        engine.processMoves();

        assertTrue(engine.snapshot(-1, -1).moveLog.isEmpty());
    }
}
