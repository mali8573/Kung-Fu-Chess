import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    private String[][] emptyGrid(int rows, int cols) {
        String[][] grid = new String[rows][cols];
        for (String[] row : grid) java.util.Arrays.fill(row, GameConstants.EMPTY);
        return grid;
    }

    @Test
    public void dimensionsAreDerivedCorrectly() {
        Board board = new Board(emptyGrid(3, 5));
        assertEquals(3, board.getHeight());
        assertEquals(5, board.getWidth());
    }

    @Test
    public void emptyCellReturnsNoPiece() {
        Board board = new Board(emptyGrid(3, 3));
        assertNull(board.pieceAt(new Position(1, 1)));
    }

    @Test
    public void occupiedCellReturnsCorrectPiece() {
        String[][] grid = emptyGrid(3, 3);
        grid[1][1] = GameConstants.W_KING;
        Board board = new Board(grid);

        Piece piece = board.pieceAt(new Position(1, 1));
        assertNotNull(piece);
        assertEquals(GameConstants.W_KING, piece.code);
        assertTrue(piece.isWhite());
    }

    @Test
    public void addingTwoPiecesToSameCellFails() {
        Board board = new Board(emptyGrid(3, 3));
        board.addPiece(new Piece("p1", GameConstants.W_KING, new Position(1, 1)));

        assertThrows(IllegalStateException.class, () ->
                board.addPiece(new Piece("p2", GameConstants.B_KING, new Position(1, 1))));
    }

    @Test
    public void movingPieceUpdatesSourceAndDestination() {
        String[][] grid = emptyGrid(3, 3);
        grid[0][0] = GameConstants.W_ROOK;
        Board board = new Board(grid);

        board.movePiece(new Position(0, 0), new Position(2, 2));

        assertNull(board.pieceAt(new Position(0, 0)));
        assertEquals(GameConstants.W_ROOK, board.pieceAt(new Position(2, 2)).code);
    }

    @Test
    public void removingPieceClearsItsCell() {
        String[][] grid = emptyGrid(3, 3);
        grid[1][1] = GameConstants.B_PAWN;
        Board board = new Board(grid);

        board.removePiece(new Position(1, 1));

        assertNull(board.pieceAt(new Position(1, 1)));
    }

    @Test
    public void boardWrapsTheSameGridSoEngineWritesStayInSync() {
        String[][] grid = emptyGrid(2, 2);
        Board board = new Board(grid);

        grid[0][0] = GameConstants.W_QUEEN; // external mutation, e.g. by GameEngine

        assertEquals(GameConstants.W_QUEEN, board.pieceAt(new Position(0, 0)).code);
    }
}
