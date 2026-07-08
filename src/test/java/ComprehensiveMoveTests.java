import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ComprehensiveMoveTests {
    private String[][] emptyBoard(int n) {
        String[][] board = new String[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] = GameConstants.EMPTY;
            }
        }
        return board;
    }

    @Test
    public void whitePawnMovesForwardAndDoubleStep() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;

        assertTrue(strategy.isValid(6, 0, 5, 0, board));
        assertTrue(strategy.isValid(6, 0, 4, 0, board));
    }

    @Test
    public void blackPawnMovesForwardAndDoubleStep() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[1][0] = GameConstants.B_PAWN;

        assertTrue(strategy.isValid(1, 0, 2, 0, board));
        assertTrue(strategy.isValid(1, 0, 3, 0, board));
    }

    @Test
    public void pawnCannotMoveIntoOccupiedSquare() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        board[5][0] = GameConstants.W_ROOK;

        assertFalse(strategy.isValid(6, 0, 5, 0, board));
    }

    @Test
    public void pawnCannotMoveDiagonallyToEmptySquare() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;

        assertFalse(strategy.isValid(6, 0, 5, 1, board));
    }

    @Test
    public void pawnCapturesEnemyDiagonally() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        board[5][1] = GameConstants.B_PAWN;

        assertTrue(strategy.isValid(6, 0, 5, 1, board));
    }

    @Test
    public void pawnCannotCaptureFriendlyPiece() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[6][0] = GameConstants.W_PAWN;
        board[5][1] = GameConstants.W_ROOK;

        assertFalse(strategy.isValid(6, 0, 5, 1, board));
    }

    @Test
    public void kingMovesOneSquareInAnyDirection() {
        KingMoveStrategy strategy = new KingMoveStrategy();
        String[][] board = emptyBoard(8);

        assertTrue(strategy.isValid(4, 4, 5, 5, board));
        assertTrue(strategy.isValid(4, 4, 4, 5, board));
        assertTrue(strategy.isValid(4, 4, 3, 4, board));
    }

    @Test
    public void kingCannotMoveMoreThanOneSquare() {
        KingMoveStrategy strategy = new KingMoveStrategy();
        String[][] board = emptyBoard(8);

        assertFalse(strategy.isValid(4, 4, 6, 6, board));
        assertFalse(strategy.isValid(4, 4, 4, 6, board));
    }

    @Test
    public void knightMovesInLShape() {
        KnightMoveStrategy strategy = new KnightMoveStrategy();
        String[][] board = emptyBoard(8);

        assertTrue(strategy.isValid(0, 0, 2, 1, board));
        assertTrue(strategy.isValid(0, 0, 1, 2, board));
    }

    @Test
    public void knightCannotMoveInStraightLine() {
        KnightMoveStrategy strategy = new KnightMoveStrategy();
        String[][] board = emptyBoard(8);

        assertFalse(strategy.isValid(0, 0, 0, 1, board));
        assertFalse(strategy.isValid(0, 0, 2, 2, board));
    }

    @Test
    public void bishopMovesDiagonally() {
        BishopMoveStrategy strategy = new BishopMoveStrategy();
        String[][] board = emptyBoard(8);

        assertTrue(strategy.isValid(2, 2, 5, 5, board));
        assertTrue(strategy.isValid(5, 5, 1, 1, board));
    }

    @Test
    public void bishopCannotMoveStraight() {
        BishopMoveStrategy strategy = new BishopMoveStrategy();
        String[][] board = emptyBoard(8);

        assertFalse(strategy.isValid(2, 2, 2, 5, board));
    }

    @Test
    public void rookMovesStraight() {
        RookMoveStrategy strategy = new RookMoveStrategy();
        String[][] board = emptyBoard(8);

        assertTrue(strategy.isValid(2, 3, 5, 3, board));
        assertTrue(strategy.isValid(2, 3, 2, 7, board));
    }

    @Test
    public void rookCannotMoveDiagonally() {
        RookMoveStrategy strategy = new RookMoveStrategy();
        String[][] board = emptyBoard(8);

        assertFalse(strategy.isValid(1, 1, 3, 3, board));
    }

    @Test
    public void queenMovesLikeRookOrBishop() {
        QueenMoveStrategy strategy = new QueenMoveStrategy();
        String[][] board = emptyBoard(8);

        assertTrue(strategy.isValid(0, 0, 0, 5, board));
        assertTrue(strategy.isValid(0, 0, 3, 3, board));
    }

    @Test
    public void queenCannotMoveAsKnight() {
        QueenMoveStrategy strategy = new QueenMoveStrategy();
        String[][] board = emptyBoard(8);

        assertFalse(strategy.isValid(0, 0, 2, 1, board));
    }

    @Test
    public void pieceFactoryRecognizesLegalMoves() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_ROOK;

        assertTrue(PieceFactory.isMoveLegal(0, 0, 0, 3, board));
        assertTrue(PieceFactory.isMoveLegal(0, 0, 2, 0, board));
    }

    @Test
    public void pieceFactoryRejectsIllegalMoves() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_KNIGHT;

        assertFalse(PieceFactory.isMoveLegal(0, 0, 0, 1, board));
    }

    @Test
    public void pathBlockedReturnsTrueWhenObstacleExists() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_ROOK;
        board[0][1] = GameConstants.W_PAWN;

        assertTrue(PieceFactory.isPathBlocked(0, 0, 0, 3, board));
    }

    @Test
    public void pathBlockedReturnsFalseWhenClear() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_ROOK;

        assertFalse(PieceFactory.isPathBlocked(0, 0, 0, 3, board));
    }

    @Test
    public void gameEngineAppliesMoveWhenArrivalTimeReached() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[6][0] = GameConstants.W_PAWN;
        engine.activeMoves.add(new MovingPiece(GameConstants.W_PAWN, 6, 0, 5, 0, 1L));
        engine.currentTime = 1;

        engine.processMoves();

        assertEquals(GameConstants.W_PAWN, engine.board[5][0]);
        assertEquals(GameConstants.EMPTY, engine.board[6][0]);
        assertTrue(engine.activeMoves.isEmpty());
    }

    @Test
    public void gameEngineDoesNotApplyMoveBeforeArrivalTime() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[6][0] = GameConstants.W_PAWN;
        engine.activeMoves.add(new MovingPiece(GameConstants.W_PAWN, 6, 0, 5, 0, 5L));
        engine.currentTime = 1;

        engine.processMoves();

        assertEquals(GameConstants.W_PAWN, engine.board[6][0]);
        assertEquals(GameConstants.EMPTY, engine.board[5][0]);
        assertFalse(engine.activeMoves.isEmpty());
    }

    @Test
    public void gameEngineCapturesEnemyAtDestination() {
        GameEngine engine = new GameEngine();
        engine.board = emptyBoard(8);
        engine.board[4][4] = GameConstants.B_ROOK;
        engine.activeMoves.add(new MovingPiece(GameConstants.B_ROOK, 4, 4, 4, 4, 0L));
        engine.activeMoves.add(new MovingPiece(GameConstants.W_ROOK, 6, 4, 4, 4, 1L));
        engine.currentTime = 1;

        engine.processMoves();

        assertEquals(GameConstants.EMPTY, engine.board[6][4]);
        assertEquals(GameConstants.B_ROOK, engine.board[4][4]);
        assertTrue(engine.activeMoves.isEmpty());
    }

    @Test
    public void pawnMoveReturnsFalseForOutOfBoundsCoordinates() {
        PawnMoveStrategy strategy = new PawnMoveStrategy();
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_PAWN;

        assertFalse(strategy.isValid(0, 0, -1, 0, board));
        assertFalse(strategy.isValid(0, 0, 8, 0, board));
    }

    @Test
    public void pieceFactoryRejectsOutOfBoundsMoves() {
        String[][] board = emptyBoard(8);
        board[0][0] = GameConstants.W_BISHOP;

        assertFalse(PieceFactory.isMoveLegal(0, 0, -1, 0, board));
        assertFalse(PieceFactory.isMoveLegal(0, 0, 8, 0, board));
    }

    @Test
    public void kingMoveReturnsFalseForOutOfBoundsCoordinates() {
        KingMoveStrategy strategy = new KingMoveStrategy();
        String[][] board = emptyBoard(8);

        assertFalse(strategy.isValid(0, 0, -1, 0, board));
        assertFalse(strategy.isValid(0, 0, 0, 8, board));
    }

    @Test
    public void gameConstantsRecognizesWhiteAndBlackPieces() {
        assertTrue(GameConstants.isWhite(GameConstants.W_KING));
        assertFalse(GameConstants.isWhite(GameConstants.B_KING));
    }

    @Test
    public void movingPieceStoresAllFields() {
        MovingPiece piece = new MovingPiece(GameConstants.W_PAWN, 6, 0, 5, 0, 100L);

        assertEquals(GameConstants.W_PAWN, piece.piece);
        assertEquals(6, piece.fromRow);
        assertEquals(0, piece.fromCol);
        assertEquals(5, piece.toRow);
        assertEquals(0, piece.toCol);
        assertEquals(100L, piece.arrivalTime);
    }
}
