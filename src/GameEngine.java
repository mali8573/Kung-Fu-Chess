import java.util.*;

public class GameEngine {
    public String[][] board;
    public long currentTime = 0;
    public boolean gameOver = false;
    public int sR = -1, sCol = -1;
    public List<MovingPiece> activeMoves = new ArrayList<>();

    public void processMoves() {
        List<MovingPiece> toRemove = new ArrayList<>();
        Set<MovingPiece> capturedPieces = new HashSet<>();

        // 1. Detect captures (only if it's an enemy)
        for (MovingPiece move : activeMoves) {
            if (move.fromRow == move.toRow && move.fromCol == move.toCol) continue;

            for (MovingPiece jump : activeMoves) {
                if (jump.fromRow == jump.toRow && jump.fromCol == jump.toCol && 
                    jump.toRow == move.toRow && jump.toCol == move.toCol) {
                    
                    // Use color logic from constants instead of charAt(0)
                    if (GameConstants.isWhite(jump.piece) != GameConstants.isWhite(move.piece)) {
                        capturedPieces.add(move); 
                    }
                }
            }
        }

        // 2. Process landings
        for (MovingPiece mp : activeMoves) {
            if (currentTime >= mp.arrivalTime) {
                if (mp.fromRow != mp.toRow || mp.fromCol != mp.toCol) {
                    if (!capturedPieces.contains(mp)) {
                        // Normal landing
                        board[mp.toRow][mp.toCol] = mp.piece;
                        board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
                    } else {
                        // If captured, the piece disappears from the board
                        board[mp.fromRow][mp.fromCol] = GameConstants.EMPTY;
                    }
                }
                toRemove.add(mp);
            }
        }
        activeMoves.removeAll(toRemove);
    }
}