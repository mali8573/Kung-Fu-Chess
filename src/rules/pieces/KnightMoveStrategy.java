package rules.pieces;

import model.Position;

public class KnightMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        Position from = new Position(fr, fc);
        Position to = new Position(tr, tc);
        int absDr = Math.abs(to.row - from.row);
        int absDc = Math.abs(to.col - from.col);
        return (absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2);
    }
}
