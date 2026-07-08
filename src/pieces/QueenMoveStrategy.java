public class QueenMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        return (tr == fr || tc == fc) || (Math.abs(tr - fr) == Math.abs(tc - fc));
    }
}