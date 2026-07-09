public class KnightMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        int absDr = Math.abs(tr - fr);
        int absDc = Math.abs(tc - fc);
        return (absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2);
    }
}