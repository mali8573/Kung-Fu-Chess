package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import engine.GameSnapshot;
import engine.PieceSnapshot;
import model.Position;

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

    /** The highlight color shown around a piece when it is clicked/selected - shared with
     *  MoveLogPanel so the moves-log text uses the same "you clicked this" accent color. */
    public static final Color SELECTION_COLOR = Color.YELLOW;
    private static final int SELECTION_THICKNESS = 4;
    private static final Color REST_COOLDOWN_COLOR = new Color(230, 200, 0, 150);
    private static final Color LEGAL_MOVE_COLOR = new Color(120, 120, 120, 130);
    private static final long IDLE_REBUILD_INTERVAL_MS = 80; // ~12fps, well above any state's own frames_per_sec
    /** Space reserved around the checkered squares for the file/rank labels (a-h, 1-8). */
    private static final int LABEL_MARGIN_PX = 26;
    private static final Color LABEL_COLOR = Color.WHITE;

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
        this.geometry = new BoardGeometry(rows, cols, LABEL_MARGIN_PX);
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
                .anyMatch(p -> p.state == PieceVisualState.MOVE || p.state == PieceVisualState.JUMP
                        || p.restRemainingFraction > 0);
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
        drawCoordinateLabels(g);
    }

    /** File letters (a-h) above and below the board, rank numbers (rows..1) to either side -
     *  drawn directly each paint() (not part of the cached frame image, since they live in the
     *  margin outside it) using whatever space LABEL_MARGIN_PX reserved in the geometry. */
    private void drawCoordinateLabels(Graphics2D g) {
        int margin = geometry.getMarginPx();
        int boardSize = geometry.getBoardSize();
        if (margin <= 0 || boardSize <= 0) return;

        int originX = geometry.getOriginX();
        int originY = geometry.getOriginY();
        double cellSize = geometry.getCellSize();

        g.setColor(LABEL_COLOR);
        g.setFont(g.getFont().deriveFont(Font.BOLD, (float) Math.max(10, margin * 0.55)));
        FontMetrics fm = g.getFontMetrics();
        int textAscentOffset = (fm.getAscent() - fm.getDescent()) / 2;

        for (int col = 0; col < geometry.getCols(); col++) {
            String label = String.valueOf((char) ('a' + col));
            int x = originX + (int) Math.round((col + 0.5) * cellSize) - fm.stringWidth(label) / 2;
            g.drawString(label, x, originY - margin / 2 + textAscentOffset);
            g.drawString(label, x, originY + boardSize + margin / 2 + textAscentOffset);
        }

        for (int row = 0; row < geometry.getRows(); row++) {
            String label = String.valueOf(geometry.getRows() - row);
            int y = originY + (int) Math.round((row + 0.5) * cellSize) + textAscentOffset;
            g.drawString(label, originX - margin / 2 - fm.stringWidth(label) / 2, y);
            g.drawString(label, originX + boardSize + margin / 2 - fm.stringWidth(label) / 2, y);
        }
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

                if (piece.restRemainingFraction > 0) {
                    // A yellow square overlay that starts covering the whole cell and shrinks
                    // downward as the cooldown elapses - its top edge sinks toward the bottom,
                    // like a level draining away, until nothing is left once it can act again.
                    int overlayHeight = (int) Math.round(cellSize * piece.restRemainingFraction);
                    int overlayY = cellY + (cellSize - overlayHeight);
                    frame.fillRect(cellX, overlayY, cellSize, overlayHeight, REST_COOLDOWN_COLOR);
                }
            }
        }

        if (selRow != -1) {
            double cellSizeD = geometry.getCellSize();
            int x = (int) Math.round(selCol * cellSizeD);
            int y = (int) Math.round(selRow * cellSizeD);
            frame.drawRectOutline(x, y, cellSize, cellSize, SELECTION_COLOR, SELECTION_THICKNESS);
        }

        if (snapshot != null) {
            for (Position move : snapshot.legalMoves) {
                int x = (int) Math.round(move.col * geometry.getCellSize());
                int y = (int) Math.round(move.row * geometry.getCellSize());
                frame.fillRect(x, y, cellSize, cellSize, LEGAL_MOVE_COLOR);
            }
        }

        if (snapshot != null && snapshot.gameOver) {
            drawGameOverBanner(frame, snapshot.winner);
        }

        return frame;
    }

    /** winner is normally "white"/"black"/null (local play), but a caller that knows a real
     *  player name (networked play) can pass that name instead, e.g. "Alice" -> "ALICE WINS". */
    private void drawGameOverBanner(Img frame, String winner) {
        int w = frame.get().getWidth();
        int h = frame.get().getHeight();
        frame.fillRect(0, 0, w, h, new Color(0, 0, 0, 160));

        String label = "white".equals(winner) ? "WHITE WINS"
                : "black".equals(winner) ? "BLACK WINS"
                : winner != null ? winner.toUpperCase() + " WINS"
                : "GAME OVER";
        frame.putTextCentered(label, w / 2, h / 2, 2.4f, Color.WHITE);
    }
}
