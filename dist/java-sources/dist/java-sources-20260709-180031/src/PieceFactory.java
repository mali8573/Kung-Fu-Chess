import java.util.HashMap;
import java.util.Map;

public class PieceFactory {
    // Map holding movement strategies
    private static final Map<String, MoveStrategy> strategies = new HashMap<>();

    static {
        strategies.put("K", new KingMoveStrategy());
        strategies.put("R", new RookMoveStrategy());
        strategies.put("B", new BishopMoveStrategy());
        strategies.put("Q", new QueenMoveStrategy());
        strategies.put("N", new KnightMoveStrategy());
        strategies.put("P", new PawnMoveStrategy());
    }

    public static boolean isMoveLegal(int fr, int fc, int tr, int tc, String[][] board) {
        if (!BoardUtils.isInBounds(fr, fc, board) || !BoardUtils.isInBounds(tr, tc, board)) {
            return false;
        }

        String piece = board[fr][fc];
        String type = piece.substring(1); // Example: "K"
        
        MoveStrategy strategy = strategies.get(type);
        return (strategy != null) && strategy.isValid(fr, fc, tr, tc, board);
    }
    public static boolean isPathBlocked(int fr, int fc, int tr, int tc, String[][] board) {
    String type = board[fr][fc].substring(1);
    // Pieces that jump over others (like knights) or do not move far (like king/pawn) have no blocking
    if (type.equals("N") || type.equals("K") || type.equals("P")) return false;
    
    int dr = Integer.compare(tr, fr);
    int dc = Integer.compare(tc, fc);
    int r = fr + dr, c = fc + dc;
    
    while (r != tr || c != tc) {
        if (!board[r][c].equals(GameConstants.EMPTY)) return true;
        r += dr; c += dc;
    }
    return false;
}
}