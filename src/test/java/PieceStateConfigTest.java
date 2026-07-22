import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import view.PieceStateConfig;

public class PieceStateConfigTest {

    @Test
    public void readsTheRealIdlePawnConfigShippedInThisRepo() {
        // assets/pieces/PW/states/idle/config.json: speed 0.0, loops, next_state idle.
        PieceStateConfig config = PieceStateConfig.readFrom("assets/pieces/PW/states/idle/config.json");

        assertEquals(0.0, config.speedMetersPerSec);
        assertEquals("idle", config.nextStateWhenFinished);
        assertTrue(config.framesPerSec > 0);
        assertTrue(config.isLoop);
    }

    @Test
    public void readsTheRealMovePawnConfigWithNonZeroSpeed() {
        // A "move" state must have a positive travel speed, or no piece could ever cross the board.
        PieceStateConfig config = PieceStateConfig.readFrom("assets/pieces/PW/states/move/config.json");

        assertTrue(config.speedMetersPerSec > 0, "a move state's speed must be positive");
    }

    @Test
    public void repeatedReadsOfTheSamePathReturnEqualValues() {
        // readFrom caches by path - this just confirms the cache doesn't hand back stale
        // or inconsistent data on a second call.
        PieceStateConfig first = PieceStateConfig.readFrom("assets/pieces/PW/states/idle/config.json");
        PieceStateConfig second = PieceStateConfig.readFrom("assets/pieces/PW/states/idle/config.json");

        assertEquals(first.speedMetersPerSec, second.speedMetersPerSec);
        assertEquals(first.nextStateWhenFinished, second.nextStateWhenFinished);
        assertEquals(first.framesPerSec, second.framesPerSec);
        assertEquals(first.isLoop, second.isLoop);
    }

    @Test
    public void missingFileThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PieceStateConfig.readFrom("assets/pieces/DOES_NOT_EXIST/config.json"));
    }
}
