package rules;

import engine.GameEngine;
import model.Board;
import model.Piece;
import model.Position;

public class RuleEngine {
    public enum MoveResult { OK, SRC_EMPTY, SRC_BUSY, SRC_RESTING, TARGET_FRIENDLY, CANNOT_REACH }

    public static MoveResult checkMove(int fr, int fc, int tr, int tc, String[][] board) {
        Board b = new Board(board);
        Position from = new Position(fr, fc);
        Position to = new Position(tr, tc);

        if (!b.isInBounds(from) || !b.isInBounds(to)) {
            return MoveResult.CANNOT_REACH;
        }

        Piece src = b.pieceAt(from);
        if (src == null) return MoveResult.SRC_EMPTY;

        Piece tgt = b.pieceAt(to);
        if (tgt != null && tgt.isWhite() == src.isWhite()) {
            return MoveResult.TARGET_FRIENDLY;
        }

        if (!PieceFactory.isMoveLegal(fr, fc, tr, tc, board)) return MoveResult.CANNOT_REACH;
        if (PieceFactory.isPathBlocked(fr, fc, tr, tc, board)) return MoveResult.CANNOT_REACH;

        return MoveResult.OK;
    }

    /**
     * Same as checkMove, but first rejects commanding a piece that is already mid-move,
     * or that is still on its post-landing/post-jump rest cooldown.
     */
    public static MoveResult checkMove(int fr, int fc, int tr, int tc, String[][] board, GameEngine engine) {
        if (engine.isPieceInFlight(fr, fc)) return MoveResult.SRC_BUSY;
        if (engine.isPieceResting(fr, fc)) return MoveResult.SRC_RESTING;
        return checkMove(fr, fc, tr, tc, board);
    }
}
