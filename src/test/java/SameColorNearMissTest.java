import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SameColorNearMissTest {
    private String[][] emptyBoard(int n){
        String[][] b = new String[n][n];
        for(int i=0;i<n;i++) for(int j=0;j<n;j++) b[i][j] = GameConstants.EMPTY;
        return b;
    }

    // Rook travels down column 4 (rows 7 -> 0) while a same-color queen crosses that same
    // column along row 3 (cols 0 -> 7). Their paths cross at (3,4). The rook would reach that
    // square close enough behind the queen to count as "almost" meeting her there (within one
    // square's worth of travel time, per the real move-config speed) - so it must get stuck
    // one square earlier, at (4,4). The queen herself is unaffected, since she gets there first.
    // Reconstructed timing depends on the move config's speed_m_per_sec, not a hardcoded
    // 1 second/square - that's exactly the bug this test now guards against.
    @Test
    public void laterPieceStopsOneSquareBeforeNearMissWithFriendlyPiece() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[7][4] = GameConstants.W_ROOK;
        g.board[3][0] = GameConstants.W_QUEEN;

        g.addMove(new MovingPiece(GameConstants.W_QUEEN, 3, 0, 3, 7, 7000L));
        MovingPiece rookMove = new MovingPiece(GameConstants.W_ROOK, 7, 4, 0, 4, 7500L);
        g.addMove(rookMove);

        // The rook's move should have been truncated in-place to stop at (4,4).
        assertEquals(4, rookMove.toRow);
        assertEquals(4, rookMove.toCol);
        assertEquals(4832L, rookMove.arrivalTime);

        g.currentTime = 4832;
        g.processMoves();

        assertEquals(GameConstants.W_ROOK, g.board[4][4]);
        assertEquals(GameConstants.EMPTY, g.board[7][4]);
        assertEquals(GameConstants.EMPTY, g.board[3][4]); // never reached the crossing square

        // The queen keeps going undisturbed and reaches her real destination.
        g.currentTime = 7000;
        g.processMoves();

        assertEquals(GameConstants.W_QUEEN, g.board[3][7]);
        assertTrue(g.activeMoves.isEmpty());
    }

    // When two same-color pieces would land on the exact same square at the exact same
    // time, that also counts as a collision - the more recently commanded move is the one
    // that gets stuck instead.
    @Test
    public void exactSameSquareSameTimeSticksTheLaterAddedMove() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[6][0] = GameConstants.W_ROOK;
        g.board[6][1] = GameConstants.W_BISHOP;

        g.addMove(new MovingPiece(GameConstants.W_ROOK, 6, 0, 4, 0, 2000L));
        MovingPiece bishopMove = new MovingPiece(GameConstants.W_BISHOP, 6, 1, 4, 0, 2000L);
        g.addMove(bishopMove);

        // Added later with an identical arrival time -> the bishop is the one that sticks.
        assertEquals(6, bishopMove.toRow);
        assertEquals(1, bishopMove.toCol);

        g.currentTime = 2000;
        g.processMoves();

        assertEquals(GameConstants.W_ROOK, g.board[4][0]);
    }

    // Two same-color moves ending on the exact same square is a guaranteed conflict - it must
    // be resolved even if the arrival times are far apart (nowhere near "almost colliding" in
    // time). Before this was fixed, only near-in-time conflicts were caught, so a same-color
    // piece with a much slower move than another one heading to its exact destination would
    // fly all the way there, find it occupied by its own side on arrival, and get silently
    // cancelled back to its start square instead of stopping short along the way.
    @Test
    public void sameFinalDestinationIsResolvedEvenWhenArrivalTimesAreFarApart() {
        GameEngine g = new GameEngine();
        g.board = emptyBoard(8);
        g.board[0][5] = GameConstants.W_BISHOP; // long diagonal flight toward (5,0)
        g.board[7][0] = GameConstants.W_PAWN;   // short flight, same destination, arrives much sooner

        MovingPiece bishopMove = new MovingPiece(GameConstants.W_BISHOP, 0, 5, 5, 0, 10000L);
        g.addMove(bishopMove);
        g.addMove(new MovingPiece(GameConstants.W_PAWN, 7, 0, 5, 0, 1500L));

        // The bishop must have been truncated immediately, not left heading for (5,0).
        assertNotEquals(5, bishopMove.toRow);
        assertNotEquals(0, bishopMove.toCol);

        g.currentTime = 1500;
        g.processMoves();
        assertEquals(GameConstants.W_PAWN, g.board[5][0]);

        g.currentTime = 10000;
        g.processMoves();

        // The bishop must NOT have reverted to its original square - it should have landed at
        // its truncated (shorter) destination instead.
        assertEquals(GameConstants.EMPTY, g.board[0][5]);
        assertNotEquals(GameConstants.EMPTY, g.board[bishopMove.toRow][bishopMove.toCol]);
    }
}
