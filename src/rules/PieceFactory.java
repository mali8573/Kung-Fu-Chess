package rules;

import java.util.HashMap;
import java.util.Map;

import model.Board;
import model.Piece;
import model.Position;
import rules.pieces.BishopMoveStrategy;
import rules.pieces.KingMoveStrategy;
import rules.pieces.KnightMoveStrategy;
import rules.pieces.MoveStrategy;
import rules.pieces.PawnMoveStrategy;
import rules.pieces.QueenMoveStrategy;
import rules.pieces.RookMoveStrategy;

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
        Board b = new Board(board);
        Position from = new Position(fr, fc);
        Position to = new Position(tr, tc);

        if (!b.isInBounds(from) || !b.isInBounds(to)) {
            return false;
        }

        Piece piece = b.pieceAt(from);
        if (piece == null) return false;

        MoveStrategy strategy = strategies.get(String.valueOf(piece.kindLetter()));
        return (strategy != null) && strategy.isValid(fr, fc, tr, tc, board);
    }

    public static boolean isPathBlocked(int fr, int fc, int tr, int tc, String[][] board) {
        Board b = new Board(board);
        Piece piece = b.pieceAt(new Position(fr, fc));
        if (piece == null) return false;

        // Pieces that jump over others (like knights) or do not move far (like king/pawn) have no blocking
        char kind = piece.kindLetter();
        if (kind == 'N' || kind == 'K' || kind == 'P') return false;

        int dr = Integer.compare(tr, fr);
        int dc = Integer.compare(tc, fc);
        int r = fr + dr, c = fc + dc;

        while (r != tr || c != tc) {
            if (b.pieceAt(new Position(r, c)) != null) return true;
            r += dr; c += dc;
        }
        return false;
    }
}
