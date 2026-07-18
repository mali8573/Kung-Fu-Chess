import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.Position;

public class PositionTest {
    @Test
    public void samePositionsAreEqual() {
        assertEquals(new Position(2, 3), new Position(2, 3));
        assertEquals(new Position(2, 3).hashCode(), new Position(2, 3).hashCode());
    }

    @Test
    public void differentRowOrColAreNotEqual() {
        assertNotEquals(new Position(2, 3), new Position(2, 4));
        assertNotEquals(new Position(2, 3), new Position(5, 3));
    }

    @Test
    public void toStringIsReadable() {
        assertEquals("(2,3)", new Position(2, 3).toString());
    }
}
