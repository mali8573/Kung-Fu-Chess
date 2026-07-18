package model;

/**
 * A chess piece: identity, color/kind (encoded together in "code", e.g. "wK" - the
 * engine's existing convention), its cell, and its lifecycle state.
 *
 * Piece never knows about the renderer, mouse clicks, pixels, or text-test syntax.
 */
public class Piece {
    public final String id;
    public final String code; // e.g. "wK": color+kind, matches GameConstants' existing convention
    public Position cell;
    public PieceLifecycleState state;

    public Piece(String id, String code, Position cell) {
        this.id = id;
        this.code = code;
        this.cell = cell;
        this.state = PieceLifecycleState.IDLE;
    }

    public boolean isWhite() {
        return GameConstants.isWhite(code);
    }

    public char kindLetter() {
        return code.charAt(1);
    }
}
