import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import engine.MovingPiece;

public class MovingPieceTest {
    @Test
    public void fieldsAreStored() {
        MovingPiece mp = new MovingPiece("wP", 6, 0, 5, 0, 100L);
        assertEquals("wP", mp.piece);
        assertEquals(6, mp.fromRow);
        assertEquals(0, mp.fromCol);
        assertEquals(5, mp.toRow);
        assertEquals(0, mp.toCol);
        assertEquals(100L, mp.arrivalTime);
    }
}
