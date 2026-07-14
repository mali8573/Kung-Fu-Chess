import java.io.File;

/**
 * Path/naming bridge between the engine's piece codes ("wP", color+piece) and the
 * asset pack's folder layout ("assets/pieces/PW/states/<state>/...", piece+color).
 * Shared by GameEngine (physics: speed) and the rendering side (graphics: frames),
 * so the mapping only lives in one place.
 */
public class PieceAssetPaths {
    private static final String BASE_PATH = "assets/pieces";

    public static String folderCode(String pieceCode) {
        char color = Character.toUpperCase(pieceCode.charAt(0));
        char piece = Character.toUpperCase(pieceCode.charAt(1));
        return "" + piece + color;
    }

    public static String statePath(String pieceCode, PieceVisualState state) {
        return BASE_PATH + "/" + folderCode(pieceCode) + "/states/" + state.folderName();
    }

    public static String configPath(String pieceCode, PieceVisualState state) {
        return statePath(pieceCode, state) + "/config.json";
    }

    public static String spritesDir(String pieceCode, PieceVisualState state) {
        return statePath(pieceCode, state) + "/sprites";
    }

    /** Number of numbered sprite frames (1.png, 2.png, ...) available for this piece/state. */
    public static int frameCount(String pieceCode, PieceVisualState state) {
        File dir = new File(spritesDir(pieceCode, state));
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        return files == null ? 0 : files.length;
    }
}
