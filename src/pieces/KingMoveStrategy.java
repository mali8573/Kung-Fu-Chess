public class KingMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        Board b = new Board(board);
        Position from = new Position(fr, fc);
        Position to = new Position(tr, tc);

        if (!b.isInBounds(from) || !b.isInBounds(to)) {
            return false;
        }

        int absDr = Math.abs(to.row - from.row);
        int absDc = Math.abs(to.col - from.col);
        return absDr <= 1 && absDc <= 1;
    }
}
