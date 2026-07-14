import java.util.ArrayList;
import java.util.List;

/**
 * Owns the logical board: dimensions, occupancy, and piece placement/removal/movement.
 * Wraps the same String[][] grid the rest of the engine (RuleEngine, PieceFactory, the
 * real-time arbiter, text I/O) already uses and is tested against - Board does not keep
 * a second, separate copy of piece state that could drift out of sync with it. Piece
 * objects are derived from the grid on demand, not stored separately.
 *
 * Board knows nothing about chess move legality - that's RuleEngine's job. Board.movePiece
 * assumes the move has already been validated.
 */
public class Board {
    private final String[][] grid;

    public Board(String[][] grid) {
        this.grid = grid;
    }

    public int getWidth() {
        return grid.length == 0 ? 0 : grid[0].length;
    }

    public int getHeight() {
        return grid.length;
    }

    public boolean isInBounds(Position pos) {
        return pos.row >= 0 && pos.row < getHeight() && pos.col >= 0 && pos.col < getWidth();
    }

    /** The piece at this cell, or null if empty or out of bounds. */
    public Piece pieceAt(Position pos) {
        if (!isInBounds(pos)) return null;
        String code = grid[pos.row][pos.col];
        if (code.equals(GameConstants.EMPTY)) return null;
        return new Piece(pos.row + "_" + pos.col, code, pos);
    }

    public List<Piece> allPieces() {
        List<Piece> pieces = new ArrayList<>();
        for (int r = 0; r < getHeight(); r++) {
            for (int c = 0; c < getWidth(); c++) {
                Piece p = pieceAt(new Position(r, c));
                if (p != null) pieces.add(p);
            }
        }
        return pieces;
    }

    /** Places a piece at its cell. Rejects placing onto an already-occupied cell. */
    public void addPiece(Piece piece) {
        if (!isInBounds(piece.cell)) {
            throw new IllegalArgumentException("Position out of bounds: " + piece.cell);
        }
        if (!grid[piece.cell.row][piece.cell.col].equals(GameConstants.EMPTY)) {
            throw new IllegalStateException("Cell already occupied: " + piece.cell);
        }
        grid[piece.cell.row][piece.cell.col] = piece.code;
    }

    /** Clears whatever piece is at this cell (a no-op if it's already empty). */
    public void removePiece(Position pos) {
        if (isInBounds(pos)) {
            grid[pos.row][pos.col] = GameConstants.EMPTY;
        }
    }

    /** Moves whatever is at `from` to `to`, assuming legality has already been checked elsewhere. */
    public void movePiece(Position from, Position to) {
        if (!isInBounds(from) || !isInBounds(to)) {
            throw new IllegalArgumentException("Move out of bounds: " + from + " -> " + to);
        }
        String code = grid[from.row][from.col];
        grid[to.row][to.col] = code;
        grid[from.row][from.col] = GameConstants.EMPTY;
    }

    /** Overwrites whatever is at this cell with the given code, regardless of prior occupancy -
     *  used for cases like pawn promotion where a landing piece's own code changes. */
    public void placePieceCode(Position pos, String code) {
        if (isInBounds(pos)) {
            grid[pos.row][pos.col] = code;
        }
    }
}
