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
    /** 1.0 = cooldown just started, 0.0 = expired (can act again); 0.0 when not resting. */
    public final double restRemainingFraction;

    public PieceSnapshot(String id, String pieceCode, double row, double col,
                          PieceVisualState state, long stateElapsedMillis, double restRemainingFraction) {
        this.id = id;
        this.pieceCode = pieceCode;
        this.row = row;
        this.col = col;
        this.state = state;
        this.stateElapsedMillis = stateElapsedMillis;
        this.restRemainingFraction = restRemainingFraction;
    }
}
