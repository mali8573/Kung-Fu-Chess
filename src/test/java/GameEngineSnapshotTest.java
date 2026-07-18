import org.junit.jupiter.api.Test;
import java.util.List;
import model.GameConstants;
import engine.GameEngine;
import engine.GameSnapshot;
import engine.MovingPiece;
import engine.PieceSnapshot;
import view.PieceVisualState;
import static org.junit.jupiter.api.Assertions.*;

public class GameEngineSnapshotTest {

    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    private PieceSnapshot find(GameSnapshot snapshot, String pieceCode) {
        for (PieceSnapshot p : snapshot.pieces) {
            if (p.pieceCode.equals(pieceCode)) return p;
        }
        return null;
    }

    @Test
    public void snapshotCarriesSelectionThroughUnchanged() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);

        GameSnapshot snapshot = engine.snapshot(4, 6);

        assertEquals(4, snapshot.selectedRow);
        assertEquals(6, snapshot.selectedCol);
    }

    @Test
    public void snapshotReflectsGameOverAndWinner() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.B_ROOK;
        engine.board[0][3] = GameConstants.W_KING;
        engine.addMove(new MovingPiece(GameConstants.B_ROOK, 0, 0, 0, 3, 1L));
        engine.currentTime = 1;

        engine.processMoves(); // black rook captures the white king

        GameSnapshot snapshot = engine.snapshot(-1, -1);
        assertTrue(snapshot.gameOver);
        assertEquals("black", snapshot.winner);
    }

    @Test
    public void stationaryPieceReportsIdleAtItsCell() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[3][3] = GameConstants.W_QUEEN;

        PieceSnapshot p = find(engine.snapshot(-1, -1), GameConstants.W_QUEEN);

        assertNotNull(p);
        assertEquals(PieceVisualState.IDLE, p.state);
        assertEquals(3.0, p.row);
        assertEquals(3.0, p.col);
    }

    @Test
    public void movingPieceIsInterpolatedBetweenSourceAndDestination() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        engine.currentTime = 0;

        GameEngine.RequestResult result = engine.requestMove(0, 0, 0, 4);
        assertTrue(result.accepted);

        long arrival = engine.activeMoves.get(0).arrivalTime;
        engine.currentTime = arrival / 2; // partway through the flight

        PieceSnapshot p = find(engine.snapshot(-1, -1), GameConstants.W_ROOK);

        assertNotNull(p);
        assertEquals(PieceVisualState.MOVE, p.state);
        assertEquals(0.0, p.row);
        assertTrue(p.col > 0.0 && p.col < 4.0, "expected column strictly between start and destination, was " + p.col);
    }

    @Test
    public void pieceEntersLongRestImmediatelyAfterLanding() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[0][0] = GameConstants.W_ROOK;
        engine.currentTime = 0;

        engine.requestMove(0, 0, 0, 2);
        long arrival = engine.activeMoves.get(0).arrivalTime;
        engine.currentTime = arrival; // exactly at arrival

        engine.processMoves();
        PieceSnapshot p = find(engine.snapshot(-1, -1), GameConstants.W_ROOK);

        assertNotNull(p);
        assertEquals(PieceVisualState.LONG_REST, p.state);
        assertEquals(0, p.row, 0.001);
        assertEquals(2, p.col, 0.001);
    }
}
