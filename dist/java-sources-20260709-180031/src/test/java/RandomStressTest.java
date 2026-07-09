import org.junit.jupiter.api.Test;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class RandomStressTest {
    @Test
    public void randomMoveStressNoExceptions() {
        GameEngine g = new GameEngine();
        int n = 8;
        String[][] board = new String[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) board[i][j] = GameConstants.EMPTY;

        // place some pieces
        board[6][0] = GameConstants.W_PAWN;
        board[6][1] = GameConstants.W_PAWN;
        board[1][0] = GameConstants.B_PAWN;
        board[0][0] = GameConstants.B_ROOK;
        g.board = board;

        Random rnd = new Random(12345);
        // generate many random moves and advance time
        for (int step = 0; step < 200; step++) {
            int fr = rnd.nextInt(n);
            int fc = rnd.nextInt(n);
            int tr = rnd.nextInt(n);
            int tc = rnd.nextInt(n);
            // occasionally add a no-op jump
            if (rnd.nextDouble() < 0.2) {
                g.addMove(new MovingPiece(g.board[fr][fc], fr, fc, fr, fc, g.currentTime + 1));
            } else {
                MovingPiece mp = new MovingPiece(g.board[fr][fc], fr, fc, tr, tc, g.currentTime + 5);
                g.addMove(mp);
            }
            g.currentTime += 5;
            g.processMoves();

            // invariants: board entries never null and are strings
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) assertNotNull(g.board[i][j]);
        }
    }
}
