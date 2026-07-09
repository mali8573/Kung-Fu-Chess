public class KingMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        if (!BoardUtils.isInBounds(fr, fc, board) || !BoardUtils.isInBounds(tr, tc, board)) {
            return false;
        }

        int absDr = Math.abs(tr - fr);
        int absDc = Math.abs(tc - fc);
        return absDr <= 1 && absDc <= 1;
    }
}