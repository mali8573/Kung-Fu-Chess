/**
 * A piece's lifecycle flag only (per the course design doc, section 6) - not to be
 * confused with PieceVisualState, which is animation/rendering state (idle/move/jump/
 * short_rest/long_rest). This one just answers: is this piece still in play, currently
 * being relocated, or gone.
 */
public enum PieceLifecycleState {
    IDLE,
    MOVING,
    CAPTURED
}
