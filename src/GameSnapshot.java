import java.util.Collections;
import java.util.List;

/** Read-only, immutable data handed from GameEngine to the renderer. No game logic here. */
public final class GameSnapshot {
    public final int boardRows;
    public final int boardCols;
    public final List<PieceSnapshot> pieces;
    public final int selectedRow;
    public final int selectedCol;
    public final boolean gameOver;
    public final String winner;

    public GameSnapshot(int boardRows, int boardCols, List<PieceSnapshot> pieces,
                         int selectedRow, int selectedCol, boolean gameOver, String winner) {
        this.boardRows = boardRows;
        this.boardCols = boardCols;
        this.pieces = Collections.unmodifiableList(pieces);
        this.selectedRow = selectedRow;
        this.selectedCol = selectedCol;
        this.gameOver = gameOver;
        this.winner = winner;
    }
}
