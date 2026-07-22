import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import engine.GameSnapshot;
import engine.MoveLogEntry;
import engine.PieceSnapshot;
import model.Position;
import net.SnapshotCodec;
import view.PieceVisualState;

public class SnapshotCodecTest {

    @Test
    public void roundTripPreservesBoardDimensionsAndGameState() {
        GameSnapshot original = new GameSnapshot(8, 8, Collections.emptyList(),
                -1, -1, true, "white", Collections.emptyList(), Collections.emptyList(), 5, 3);

        GameSnapshot decoded = SnapshotCodec.decode(SnapshotCodec.encode(original));

        assertEquals(8, decoded.boardRows);
        assertEquals(8, decoded.boardCols);
        assertTrue(decoded.gameOver);
        assertEquals("white", decoded.winner);
        assertEquals(5, decoded.whiteScore);
        assertEquals(3, decoded.blackScore);
    }

    @Test
    public void noWinnerEncodesAndDecodesAsNull() {
        GameSnapshot original = new GameSnapshot(8, 8, Collections.emptyList(),
                -1, -1, false, null, Collections.emptyList(), Collections.emptyList(), 0, 0);

        GameSnapshot decoded = SnapshotCodec.decode(SnapshotCodec.encode(original));

        assertFalse(decoded.gameOver);
        assertNull(decoded.winner);
    }

    @Test
    public void roundTripPreservesEveryPieceField() {
        List<PieceSnapshot> pieces = new ArrayList<>();
        pieces.add(new PieceSnapshot("6_4", "wP", 6.0, 4.0, PieceVisualState.IDLE, 0, 0.0));
        pieces.add(new PieceSnapshot("3_2", "bN", 2.5, 2.0, PieceVisualState.MOVE, 250, 0.0));
        GameSnapshot original = new GameSnapshot(8, 8, pieces,
                -1, -1, false, null, Collections.emptyList(), Collections.emptyList(), 0, 0);

        GameSnapshot decoded = SnapshotCodec.decode(SnapshotCodec.encode(original));

        assertEquals(2, decoded.pieces.size());
        PieceSnapshot firstDecoded = decoded.pieces.get(0);
        assertEquals("wP", firstDecoded.pieceCode);
        assertEquals(6.0, firstDecoded.row);
        assertEquals(4.0, firstDecoded.col);
        assertEquals(PieceVisualState.IDLE, firstDecoded.state);

        PieceSnapshot secondDecoded = decoded.pieces.get(1);
        assertEquals("bN", secondDecoded.pieceCode);
        assertEquals(2.5, secondDecoded.row);
        assertEquals(PieceVisualState.MOVE, secondDecoded.state);
        assertEquals(250, secondDecoded.stateElapsedMillis);
    }

    @Test
    public void roundTripPreservesMoveLogOrderAndOwner() {
        List<MoveLogEntry> moveLog = new ArrayList<>();
        moveLog.add(new MoveLogEntry(true, "e4", 1000));
        moveLog.add(new MoveLogEntry(false, "Nxc6", 2500));
        GameSnapshot original = new GameSnapshot(8, 8, Collections.emptyList(),
                -1, -1, false, null, Collections.emptyList(), moveLog, 3, 0);

        GameSnapshot decoded = SnapshotCodec.decode(SnapshotCodec.encode(original));

        assertEquals(2, decoded.moveLog.size());
        assertTrue(decoded.moveLog.get(0).white);
        assertEquals("e4", decoded.moveLog.get(0).notation);
        assertFalse(decoded.moveLog.get(1).white);
        assertEquals("Nxc6", decoded.moveLog.get(1).notation);
        assertEquals(2500, decoded.moveLog.get(1).gameTimeMillis);
    }

    @Test
    public void decodedSnapshotNeverCarriesSelectionOrLegalMoves() {
        // Selection/legalMoves are per-client UI state, deliberately never sent over the wire.
        GameSnapshot original = new GameSnapshot(8, 8, Collections.emptyList(),
                3, 4, false, null, Collections.singletonList(new Position(4, 4)), Collections.emptyList(), 0, 0);

        GameSnapshot decoded = SnapshotCodec.decode(SnapshotCodec.encode(original));

        assertEquals(-1, decoded.selectedRow);
        assertEquals(-1, decoded.selectedCol);
        assertTrue(decoded.legalMoves.isEmpty());
    }
}
