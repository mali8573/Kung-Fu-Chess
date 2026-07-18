import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;

public class GameConstantsTest {
    @Test
    public void isWhiteDetectsWhiteAndBlack() {
        assertTrue(GameConstants.isWhite(GameConstants.W_KING));
        assertFalse(GameConstants.isWhite(GameConstants.B_KING));
    }
}
