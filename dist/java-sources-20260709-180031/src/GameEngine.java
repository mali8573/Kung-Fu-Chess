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

            String destPiece = board[mp.toRow][mp.toCol];

            if (!destPiece.equals(GameConstants.EMPTY)) {
                // Destination occupied
                if (GameConstants.isWhite(destPiece) == GameConstants.isWhite(mp.piece)) {
                    // Friendly piece on destination: cancel this move (leave board as-is)
                    processed.add(mp);
                    continue;
                } else {
                    // Capture enemy piece at destination
                    if (destPiece.endsWith("K")) {
                        gameOver = true;
                    }
                    board[mp.toRow][mp.toCol] = mp.piece;
                    board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
                }
            } else {
                // Normal landing
                board[mp.toRow][mp.toCol] = mp.piece;
                board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
            }

            processed.add(mp);
        }

        activeMoves.removeAll(processed);
    }
}