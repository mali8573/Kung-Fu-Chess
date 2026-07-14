import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Owns the board image, the pieces, and the on-screen geometry. All pixel work
 * (loading, scaling, highlighting, compositing pieces) goes through Img; this class
 * never touches a graphics library other than the single Graphics2D.drawImage blit
 * needed to hand the final raster to the Swing panel. The letterbox background is
 * the panel's own background color (set once in App), not something drawn here.
 *
 * masterImage is read from disk exactly once. resize() only rescales it in memory
 * (Img.resized), so live window resizing never re-hits the disk.
 *
 * The composed frame (board + pieces + selection) is cached and rebuilt only as
 * often as it actually needs to be:
 *  - while any piece is MOVE/JUMP (its board position is changing), rebuild every
 *    paint() - smooth motion genuinely needs that.
 *  - otherwise (everything idle/resting, only sprite frames cycling in place, or
 *    the selection changed), rebuild at a throttled ~12fps - plenty smooth for
 *    ambient animation without the GC pressure of a full board recomposite at 60fps.
 *
 * Each piece is drawn using its actual current state's sprite frame (idle/move/jump/
 * short_rest/long_rest), animated at that state's own frames_per_sec from config.json.
 */
public class BoardRenderer {

    private static final Color SELECTION_COLOR = Color.YELLOW;
    private static final int SELECTION_THICKNESS = 4;
    private static final long IDLE_REBUILD_INTERVAL_MS = 80; // ~12fps, well above any state's own frames_per_sec

    private final String imagePath;
    private final BoardGeometry geometry;
    private final PieceSprites pieceSprites = new PieceSprites();

    private Img masterImage;
    private Img cleanBoardImage;
    private Img cachedFrame;
    private int cachedSelectedRow = Integer.MIN_VALUE;
    private int cachedSelectedCol = Integer.MIN_VALUE;
    private long lastRebuildWallClock = 0;

    public BoardRenderer(String imagePath, int rows, int cols) {
        this.imagePath = imagePath;
        this.geometry = new BoardGeometry(rows, cols);
    }

    public BoardGeometry getGeometry() {
        return geometry;
    }

    /** Rescales the board image (loaded from disk only once, ever) to fit the current panel size. */
    public void resize(int panelWidth, int panelHeight) {
        if (masterImage == null) {
            masterImage = new Img().read(imagePath);
        }
        geometry.resize(panelWidth, panelHeight);
        int size = geometry.getBoardSize();
        cleanBoardImage = (size <= 0) ? null : masterImage.resized(new Dimension(size, size), false);
        cachedFrame = null; // board size changed, cache is stale
    }

    /** Draws the board, the pieces, and the selection highlight (if any) from the given snapshot. */
    public void paint(Graphics2D g, GameSnapshot snapshot, int panelWidth, int panelHeight) {
        if (cleanBoardImage == null) return;

        int selRow = (snapshot != null) ? snapshot.selectedRow : -1;
        int selCol = (snapshot != null) ? snapshot.selectedCol : -1;
        boolean anyPositionChanging = snapshot != null && snapshot.pieces.stream()
                .anyMatch(p -> p.state == PieceVisualState.MOVE || p.state == PieceVisualState.JUMP);
        boolean selectionChanged = selRow != cachedSelectedRow || selCol != cachedSelectedCol;

        long now = System.currentTimeMillis();
        boolean idleThrottleElapsed = now - lastRebuildWallClock >= IDLE_REBUILD_INTERVAL_MS;

        if (cachedFrame == null || selectionChanged || anyPositionChanging || idleThrottleElapsed) {
            cachedFrame = composeFrame(snapshot, selRow, selCol);
            cachedSelectedRow = selRow;
            cachedSelectedCol = selCol;
            lastRebuildWallClock = now;
        }

        g.drawImage(cachedFrame.get(), geometry.getOriginX(), geometry.getOriginY(), null);
    }

    private Img composeFrame(GameSnapshot snapshot, int selRow, int selCol) {
        Img frame = cleanBoardImage.copy();
        int cellSize = (int) Math.round(geometry.getCellSize());

        if (snapshot != null) {
            for (PieceSnapshot piece : snapshot.pieces) {
                Img sprite = pieceSprites.frameFor(piece.pieceCode, piece.state, piece.stateElapsedMillis, cellSize);
                int cellX = (int) Math.round(piece.col * geometry.getCellSize());
                int cellY = (int) Math.round(piece.row * geometry.getCellSize());
                int x = cellX + (cellSize - sprite.get().getWidth()) / 2;
                int y = cellY + (cellSize - sprite.get().getHeight()) / 2;
                x = Math.max(0, Math.min(x, frame.get().getWidth() - sprite.get().getWidth()));
                y = Math.max(0, Math.min(y, frame.get().getHeight() - sprite.get().getHeight()));
                sprite.drawOn(frame, x, y);
            }
        }

        if (selRow != -1) {
            double cellSizeD = geometry.getCellSize();
            int x = (int) Math.round(selCol * cellSizeD);
            int y = (int) Math.round(selRow * cellSizeD);
            frame.drawRectOutline(x, y, cellSize, cellSize, SELECTION_COLOR, SELECTION_THICKNESS);
        }

        if (snapshot != null && snapshot.gameOver) {
            drawGameOverBanner(frame, snapshot.winner);
        }

        return frame;
    }

    private void drawGameOverBanner(Img frame, String winner) {
        int w = frame.get().getWidth();
        int h = frame.get().getHeight();
        frame.fillRect(0, 0, w, h, new Color(0, 0, 0, 160));

        String label = "white".equals(winner) ? "WHITE WINS"
                : "black".equals(winner) ? "BLACK WINS"
                : "GAME OVER";
        frame.putTextCentered(label, w / 2, h / 2, 2.4f, Color.WHITE);
    }
}
