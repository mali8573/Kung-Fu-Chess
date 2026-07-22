import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import view.PieceAssetPaths;
import view.PieceVisualState;

public class PieceAssetPathsTest {

    @Test
    public void folderCodeSwapsPieceAndColorAndUppercases() {
        assertEquals("PW", PieceAssetPaths.folderCode("wP"));
        assertEquals("NB", PieceAssetPaths.folderCode("bN"));
        assertEquals("KW", PieceAssetPaths.folderCode("wK"));
    }

    @Test
    public void statePathUsesTheStateFolderName() {
        assertEquals("assets/pieces/PW/states/idle", PieceAssetPaths.statePath("wP", PieceVisualState.IDLE));
        assertEquals("assets/pieces/NB/states/short_rest",
                PieceAssetPaths.statePath("bN", PieceVisualState.SHORT_REST));
    }

    @Test
    public void configPathAppendsConfigJsonToTheStatePath() {
        assertEquals("assets/pieces/PW/states/idle/config.json",
                PieceAssetPaths.configPath("wP", PieceVisualState.IDLE));
    }

    @Test
    public void spritesDirAppendsSpritesToTheStatePath() {
        assertEquals("assets/pieces/PW/states/idle/sprites",
                PieceAssetPaths.spritesDir("wP", PieceVisualState.IDLE));
    }

    @Test
    public void frameCountFindsTheRealShippedSpritesForAKnownPieceAndState() {
        // Exercises real disk I/O against the asset pack actually shipped in this repo
        // (assets/pieces/PW/states/idle/sprites), not a mock - so it also verifies the
        // asset pack itself has not gone missing or been renamed.
        int count = PieceAssetPaths.frameCount("wP", PieceVisualState.IDLE);

        assertTrue(count > 0, "the white pawn's idle sprites should exist and be non-empty");
    }

    @Test
    public void frameCountIsZeroForANonExistentDirectory() {
        assertEquals(0, PieceAssetPaths.frameCount("zZ", PieceVisualState.IDLE));
    }
}
