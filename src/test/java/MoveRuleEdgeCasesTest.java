import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.GameConstants;
import rules.PieceFactory;
import rules.pieces.KingMoveStrategy;
import rules.pieces.PawnMoveStrategy;

public class MoveRuleEdgeCasesTest {
    private String[][] emptyBoard(int n) {
        String[][] b = new String[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                b[i][j] = GameConstants.EMPTY;
            }
        }
        return b;
    }

    @Test
    public void whitePawnCannotMoveForwardIntoOccupiedSquare() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        board[5][0] = GameConstants.W_ROOK;

        assertFalse(strategy.isValid(6, 0, 5, 0, board));
    }

    @Test
    public void whitePawnCannotDoubleJumpWhenBlocked() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        board[5][0] = GameConstants.B_PAWN;

        assertFalse(strategy.isValid(6, 0, 4, 0, board));
    }

    @Test
    public void pawnCannotCaptureIntoEmptyDiagonalSquare() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;

        assertFalse(strategy.isValid(6, 0, 5, 1, board));
    }

    @Test
    public void rookPathIsBlockedByAnotherPiece() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_ROOK;
        board[0][1] = GameConstants.W_PAWN;

        assertTrue(PieceFactory.isPathBlocked(0, 0, 0, 3, board));
    }

    @Test
    public void rookPathIsClearWhenNoObstaclesExist() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_ROOK;

        assertFalse(PieceFactory.isPathBlocked(0, 0, 0, 3, board));
    }

    @Test
    public void kingCannotMoveMoreThanOneSquare() {
        KingMoveStrategy strategy = new KingMoveStrategy();
        String[][] board = emptyBoard(8);

        assertTrue(strategy.isValid(4, 4, 5, 5, board));
        assertFalse(strategy.isValid(4, 4, 6, 6, board));
    }
}
