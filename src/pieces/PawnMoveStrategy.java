public class PawnMoveStrategy implements MoveStrategy {
    @Override
    public boolean isValid(int fr, int fc, int tr, int tc, String[][] board) {
        if (!BoardUtils.isInBounds(fr, fc, board) || !BoardUtils.isInBounds(tr, tc, board)) {
            return false;
        }

        String piece = board[fr][fc];
        int dir = GameConstants.isWhite(piece) ? -1 : 1;
        int dr = tr - fr;
        int dc = Math.abs(tc - fc);
        
        // Regular move
        if (dc == 0 && dr == dir && board[tr][tc].equals(GameConstants.EMPTY)) return true;
        
        // First double step
        boolean isStart = (GameConstants.isWhite(piece) && fr == board.length - 1) ||
                          (!GameConstants.isWhite(piece) && fr == 0);
        if (dc == 0 && isStart && dr == 2 * dir && board[tr][tc].equals(GameConstants.EMPTY) && 
            board[fr + dir][fc].equals(GameConstants.EMPTY)) return true;
        
        // Diagonal capture
        return dc == 1 && dr == dir && !board[tr][tc].equals(GameConstants.EMPTY) && 
               GameConstants.isWhite(piece) != GameConstants.isWhite(board[tr][tc]);
    }
}
