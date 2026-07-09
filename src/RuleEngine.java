public class RuleEngine {
    public enum MoveResult { OK, SRC_EMPTY, TARGET_FRIENDLY, CANNOT_REACH }

    public static MoveResult checkMove(int fr, int fc, int tr, int tc, String[][] board) {
        if (!BoardUtils.isInBounds(fr, fc, board) || !BoardUtils.isInBounds(tr, tc, board)) {
            return MoveResult.CANNOT_REACH;
        }

        if (board[fr][fc].equals(GameConstants.EMPTY)) return MoveResult.SRC_EMPTY;

        String src = board[fr][fc];
        String tgt = board[tr][tc];
        if (!tgt.equals(GameConstants.EMPTY) && GameConstants.isWhite(tgt) == GameConstants.isWhite(src)) {
            return MoveResult.TARGET_FRIENDLY;
        }

        if (!PieceFactory.isMoveLegal(fr, fc, tr, tc, board)) return MoveResult.CANNOT_REACH;
        if (PieceFactory.isPathBlocked(fr, fc, tr, tc, board)) return MoveResult.CANNOT_REACH;

        return MoveResult.OK;
    }
}
