public class BishopMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        Position from = new Position(fr, fc);
        Position to = new Position(tr, tc);
        return Math.abs(to.row - from.row) == Math.abs(to.col - from.col);
    }
}
