public interface MoveStrategy {
    boolean isValid(int fr, int fc, int tr, int tc, String[][] board);
}