import java.util.*;

public class GameEngine {
    public String[][] board;
    public long currentTime = 0;
    public boolean gameOver = false;
    public int sR = -1, sCol = -1;
    public List<MovingPiece> activeMoves = new ArrayList<>();

    public synchronized void addMove(MovingPiece mp) {
        activeMoves.add(mp);
    }

    /**
     * True if a move from (fromRow,fromCol) to (toRow,toCol) would share column
     * range with an existing, still-pending move belonging to the opposite color.
     * Jump moves (in-place, fromRow==toRow && fromCol==toCol) do not block routes.
     */
    public synchronized boolean hasOpposingRouteConflict(int fromRow, int fromCol, int toRow, int toCol) {
        String movingPiece = board[fromRow][fromCol];
        boolean movingIsWhite = GameConstants.isWhite(movingPiece);
        int lo = Math.min(fromCol, toCol), hi = Math.max(fromCol, toCol);

        for (MovingPiece mp : activeMoves) {
            if (mp.fromRow == mp.toRow && mp.fromCol == mp.toCol) continue; // jumps don't block routes
            if (GameConstants.isWhite(mp.piece) == movingIsWhite) continue; // only opposite color blocks

            int mlo = Math.min(mp.fromCol, mp.toCol), mhi = Math.max(mp.fromCol, mp.toCol);
            if (mlo <= hi && lo <= mhi) return true;
        }
        return false;
    }

    /**
     * Process any moves whose arrivalTime is <= currentTime.
     * Moves are applied in chronological order; each move is applied
     * only if its source still contains the expected piece.
     */
    public synchronized void processMoves() {
        // Sort by arrival time to simulate real-time arrivals
        activeMoves.sort(Comparator.comparingLong(m -> m.arrivalTime));

        List<MovingPiece> processed = new ArrayList<>();

        for (MovingPiece mp : new ArrayList<>(activeMoves)) {
            if (mp.arrivalTime > currentTime) break;

            // Validate source still holds the moving piece
            if (!BoardUtils.isInBounds(mp.fromRow, mp.fromCol, board)) {
                processed.add(mp);
                continue;
            }

            if (!board[mp.fromRow][mp.fromCol].equals(mp.piece)) {
                // Source no longer contains this piece (it was captured or moved)
                processed.add(mp);
                continue;
            }

            // Validate destination bounds
            if (!BoardUtils.isInBounds(mp.toRow, mp.toCol, board)) {
                // Invalid destination: clear source and drop the move
                board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
                processed.add(mp);
                continue;
            }

            boolean isJump = (mp.fromRow == mp.toRow && mp.fromCol == mp.toCol);
            String destPiece = board[mp.toRow][mp.toCol];

            if (!destPiece.equals(GameConstants.EMPTY)) {
                // Destination occupied
                if (GameConstants.isWhite(destPiece) == GameConstants.isWhite(mp.piece)) {
                    // Friendly piece on destination: cancel this move (leave board as-is)
                    processed.add(mp);
                    continue;
                } else if (!isJump && isDefenderAirborne(mp.toRow, mp.toCol, mp.arrivalTime)) {
                    // Target is mid-jump (airborne): the attacker is destroyed instead of capturing.
                    board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
                    processed.add(mp);
                    continue;
                } else {
                    // Capture enemy piece at destination
                    if (destPiece.endsWith("K")) {
                        gameOver = true;
                    }
                    board[mp.toRow][mp.toCol] = promote(mp.piece, mp.toRow);
                    board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
                }
            } else {
                // Normal landing
                board[mp.toRow][mp.toCol] = promote(mp.piece, mp.toRow);
                board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
            }

            processed.add(mp);
        }

        activeMoves.removeAll(processed);
    }

    /**
     * A defender at (row,col) counts as still airborne against an attacker arriving at
     * attackerArrival if there is a pending jump (in-place move) for that square whose own
     * arrival is not strictly earlier - i.e. it hasn't already landed before the attack lands.
     */
    private boolean isDefenderAirborne(int row, int col, long attackerArrival) {
        for (MovingPiece j : activeMoves) {
            if (j.fromRow == j.toRow && j.fromCol == j.toCol
                    && j.fromRow == row && j.fromCol == col
                    && j.arrivalTime >= attackerArrival) {
                return true;
            }
        }
        return false;
    }

    private String promote(String piece, int row) {
        if (!piece.endsWith("P")) return piece;
        boolean white = GameConstants.isWhite(piece);
        if (white && row == 0) return "wQ";
        if (!white && row == board.length - 1) return "bQ";
        return piece;
    }
}
