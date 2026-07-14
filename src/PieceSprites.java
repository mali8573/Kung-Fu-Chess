import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and caches sprite frame sequences per (piece type, visual state), and picks
 * the correct frame to show given how long the piece has been in that state.
 *
 * Master frames are read from disk once per (piece, state), ever. Scaled versions are
 * cached per cell size and rebuilt only when the cell size actually changes (same
 * lesson learned from BoardRenderer: don't reload/rescale from scratch every frame).
 */
public class PieceSprites {

    private final Map<String, List<Img>> masterFrames = new HashMap<>();
    private final Map<String, List<Img>> scaledFrames = new HashMap<>();
    private int cachedCellSize = -1;

    private static String key(String pieceCode, PieceVisualState state) {
        return pieceCode + "/" + state.folderName();
    }

    private List<Img> masterFramesFor(String pieceCode, PieceVisualState state) {
        return masterFrames.computeIfAbsent(key(pieceCode, state), k -> {
            List<Img> frames = new ArrayList<>();
            String spritesDir = PieceAssetPaths.spritesDir(pieceCode, state);
            int frameCount = PieceAssetPaths.frameCount(pieceCode, state);
            for (int i = 1; i <= frameCount; i++) {
                frames.add(new Img().read(spritesDir + "/" + i + ".png"));
            }
            if (frames.isEmpty()) {
                throw new IllegalStateException("No sprite frames found in " + spritesDir);
            }
            return frames;
        });
    }

    private List<Img> scaledFramesFor(String pieceCode, PieceVisualState state, int cellSize) {
        if (cellSize != cachedCellSize) {
            scaledFrames.clear();
            cachedCellSize = cellSize;
        }
        return scaledFrames.computeIfAbsent(key(pieceCode, state), k -> {
            List<Img> master = masterFramesFor(pieceCode, state);
            List<Img> scaled = new ArrayList<>();
            for (Img frame : master) {
                scaled.add(frame.resized(new Dimension(cellSize, cellSize), true));
            }
            return scaled;
        });
    }

    /**
     * The correct animation frame for this piece, scaled to fit within a cellSize x cellSize
     * box, given how long (in ms) the piece has been in its current state. Looping states
     * (is_loop=true, e.g. idle/move/rest) cycle forever; non-looping states (e.g. jump)
     * hold on the last frame once the animation has fully played.
     */
    public Img frameFor(String pieceCode, PieceVisualState state, long stateElapsedMillis, int cellSize) {
        List<Img> frames = scaledFramesFor(pieceCode, state, cellSize);
        PieceStateConfig config = PieceStateConfig.readFrom(PieceAssetPaths.configPath(pieceCode, state));

        int frameCount = frames.size();
        int rawIndex = (int) (stateElapsedMillis / 1000.0 * config.framesPerSec);
        int index = config.isLoop ? Math.floorMod(rawIndex, frameCount) : Math.min(rawIndex, frameCount - 1);

        return frames.get(Math.max(0, index));
    }
}
