/**
 * Lifecycle state of a piece for rendering/animation purposes only. Names match the
 * asset pack's state folder names exactly (idle/move/jump/short_rest/long_rest).
 */
public enum PieceVisualState {
    IDLE,
    MOVE,
    JUMP,
    SHORT_REST,
    LONG_REST;

    /** Lowercase, matching the asset folder name for this state (e.g. "short_rest"). */
    public String folderName() {
        return name().toLowerCase();
    }
}
