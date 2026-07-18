package rules.pieces;

import model.Board;
import model.Piece;
import model.Position;

public class PawnMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        Board b = new Board(board);
        Position from = new Position(fr, fc);
        Position to = new Position(tr, tc);

        if (!b.isInBounds(from) || !b.isInBounds(to)) {
            return false;
        }

        Piece piece = b.pieceAt(from);
        if (piece == null) return false; // no piece to move

        int dir = piece.isWhite() ? -1 : 1;
        int dr = to.row - from.row;
        int dc = Math.abs(to.col - from.col);
        Piece atTarget = b.pieceAt(to);

        // Regular move
        if (dc == 0 && dr == dir && atTarget == null) return true;

        // First double step - white pawns start on row (height-2), black pawns on row 1;
        // the back rank itself (height-1 / 0) is where the other pieces start, not pawns.
        boolean isStart = (piece.isWhite() && from.row == b.getHeight() - 2) ||
                          (!piece.isWhite() && from.row == 1);
        Piece atStepThrough = b.pieceAt(new Position(from.row + dir, from.col));
        if (dc == 0 && isStart && dr == 2 * dir && atTarget == null && atStepThrough == null) return true;

        // Diagonal capture
        return dc == 1 && dr == dir && atTarget != null && piece.isWhite() != atTarget.isWhite();
    }
}
