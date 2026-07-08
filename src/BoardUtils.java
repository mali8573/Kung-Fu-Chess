public class BoardUtils {
    public static boolean isInBounds(int row, int col, String[][] board) {
        return board != null && row >= 0 && row < board.length && col >= 0 && col < board[0].length;
    }
}
