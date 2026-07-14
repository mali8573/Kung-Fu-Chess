/**
 * Read-only view of a single piece for the renderer. Position is board-relative
 * (row/col as fractional cell coordinates, e.g. row=2.35), not device pixels —
 * converting to actual pixels is the View's job (BoardGeometry), same rule we
 * already applied to the board image itself.
 */
public final class PieceSnapshot {
    public final String id;
    public final String pieceCode;
    public final double row;
    public final double col;
    public final PieceVisualState state;
    public final long stateElapsedMillis;

    public PieceSnapshot(String id, String pieceCode, double row, double col,
                          PieceVisualState state, long stateElapsedMillis) {
        this.id = id;
        this.pieceCode = pieceCode;
        this.row = row;
        this.col = col;
        this.state = state;
        this.stateElapsedMillis = stateElapsedMillis;
    }
}
