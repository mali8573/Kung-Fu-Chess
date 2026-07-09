public class BishopMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        return Math.abs(tr - fr) == Math.abs(tc - fc);
    }
}