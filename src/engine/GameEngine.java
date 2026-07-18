package engine;

import java.util.*;

import model.Board;
import model.GameConstants;
import model.Piece;
import model.Position;
import rules.RuleEngine;
import view.PieceAssetPaths;
import view.PieceStateConfig;
import view.PieceVisualState;

public class GameEngine {
    public String[][] board;
    public long currentTime = 0;
    public boolean gameOver = false;
    public String winner = null; // "white" | "black" | null
    public List<MovingPiece> activeMoves = new ArrayList<>();

    private final RealTimeArbiter arbiter = new RealTimeArbiter(this);

    /**
     * Object-oriented view of the same board data (Position/Piece instead of int/String).
     * Wraps this.board directly - not a copy - so it's always in sync with the grid the
     * rest of the engine reads and writes.
     */
    public Board asBoard() {
        return new Board(board);
    }

    /** Starts a brand new game on freshBoard, clearing all in-flight moves, cooldowns, the
     *  moves log, and both scores. Lets the same GameEngine/GameController/Timer instances
     *  keep running instead of tearing down and rebuilding the whole GUI for "play again". */
    public synchronized void reset(String[][] freshBoard) {
        board = freshBoard;
        currentTime = 0;
        gameOver = false;
        winner = null;
        activeMoves.clear();
        restStates.clear();
        moveLog.clear();
        whiteScore = 0;
        blackScore = 0;
    }

    /** Cooldown after landing from a normal move: 1 second per square actually traveled
     *  (e.g. a rook that moved 6 squares rests for 6 seconds). */
    private static long moveRestMs(int fromRow, int fromCol, int toRow, int toCol) {
        long distance = Math.max(Math.abs(toRow - fromRow), Math.abs(toCol - fromCol));
        return distance * 1000L;
    }

    /** Cooldown after completing a jump, before the piece can move or jump again. */
    private static final long JUMP_REST_MS = 500L;

    /** Post-landing rest state per cell ("row_col"), read by snapshot() and self-cleaned once expired.
     *  The same durationMs also gates new commands via isPieceResting - the rest pose lasts exactly
     *  as long as the piece is actually unable to act. */
    private static final class RestInfo {
        final PieceVisualState state;
        final long startTime;
        final long durationMs;

        RestInfo(PieceVisualState state, long startTime, long durationMs) {
            this.state = state;
            this.startTime = startTime;
            this.durationMs = durationMs;
        }
    }

    private final Map<String, RestInfo> restStates = new HashMap<>();

    /** Every completed (non-jump) move, in order - for the on-screen moves log only. */
    private final List<MoveLogEntry> moveLog = new ArrayList<>();

    /** Score = total point-value of enemy pieces a side has captured (classic chess values). */
    public int whiteScore = 0;
    public int blackScore = 0;

    private static int pieceValue(char kindLetter) {
        switch (kindLetter) {
            case 'P': return 1;
            case 'N': return 3;
            case 'B': return 3;
            case 'R': return 5;
            case 'Q': return 9;
            default: return 0; // king - capturing it ends the game, not a scored trade
        }
    }

    private String squareName(int row, int col) {
        char file = (char) ('a' + col);
        int rank = board.length - row;
        return "" + file + rank;
    }

    /** Simplified algebraic notation: no check/disambiguation, since this engine tracks neither. */
    private String notationFor(MovingPiece mp, boolean wasCapture, String landedPiece) {
        char kind = mp.piece.charAt(1);
        String dest = squareName(mp.toRow, mp.toCol);
        StringBuilder sb = new StringBuilder();

        if (kind == 'P') {
            if (wasCapture) sb.append((char) ('a' + mp.fromCol)).append('x');
            sb.append(dest);
            if (!landedPiece.equals(mp.piece)) sb.append("=Q"); // this engine only ever promotes to queen
        } else {
            sb.append(kind);
            if (wasCapture) sb.append('x');
            sb.append(dest);
        }
        return sb.toString();
    }

    private void startRest(String pieceCode, int row, int col, PieceVisualState restState, long durationMs) {
        if (pieceCode.equals(GameConstants.EMPTY) || pieceCode.length() < 2) return; // not a real piece
        restStates.put(row + "_" + col, new RestInfo(restState, currentTime, durationMs));
    }

    /** True if the piece at (row,col) is still on its post-landing/post-jump cooldown. */
    public synchronized boolean isPieceResting(int row, int col) {
        RestInfo rest = restStates.get(row + "_" + col);
        if (rest == null) return false;
        return currentTime - rest.startTime < rest.durationMs;
    }

    public synchronized void addMove(MovingPiece mp) {
        activeMoves.add(mp);
        resolveSameColorPathConflicts();
    }

    /** Advances simulated game time by dt milliseconds and resolves any moves that arrived. */
    public void advanceTime(long dt) {
        arbiter.advanceTime(dt);
    }

    /** Outcome of a requestMove call: whether it was accepted, and a stable machine-readable reason. */
    public static final class RequestResult {
        public final boolean accepted;
        public final String reason;

        public RequestResult(boolean accepted, String reason) {
            this.accepted = accepted;
            this.reason = reason;
        }
    }

    private static String reasonFor(RuleEngine.MoveResult result) {
        switch (result) {
            case SRC_EMPTY: return "empty_source";
            case SRC_BUSY: return "motion_in_progress";
            case SRC_RESTING: return "resting";
            case TARGET_FRIENDLY: return "friendly_destination";
            case CANNOT_REACH: return "illegal_piece_move";
            default: return "ok";
        }
    }

    /**
     * Public command boundary for a move attempt (source -> destination), used by both the
     * Controller and any future text-test runner. Rejects game_over up front; otherwise
     * delegates legality to RuleEngine and, if legal, starts the move via addMove.
     */
    public synchronized RequestResult requestMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (gameOver) return new RequestResult(false, "game_over");

        RuleEngine.MoveResult result = RuleEngine.checkMove(fromRow, fromCol, toRow, toCol, board, this);
        if (result != RuleEngine.MoveResult.OK) {
            return new RequestResult(false, reasonFor(result));
        }

        String pieceCode = asBoard().pieceAt(new Position(fromRow, fromCol)).code;
        long distance = Math.max(Math.abs(toRow - fromRow), Math.abs(toCol - fromCol));
        PieceStateConfig moveConfig = PieceStateConfig.readFrom(PieceAssetPaths.configPath(pieceCode, PieceVisualState.MOVE));
        long durationMs = Math.round(distance / moveConfig.speedMetersPerSec * 1000);

        addMove(new MovingPiece(pieceCode, fromRow, fromCol, toRow, toCol, currentTime + durationMs));
        return new RequestResult(true, "ok");
    }

    /**
     * Public command boundary for a jump attempt (in-place, e.g. a right-click action).
     * Rejects game_over and an empty source up front, and - like requestMove - rejects a
     * piece that is already mid-move via isPieceInFlight, since a piece can never be given
     * a new command while it is still executing an earlier one.
     */
    public synchronized RequestResult requestJump(int row, int col) {
        if (gameOver) return new RequestResult(false, "game_over");

        Position pos = new Position(row, col);
        Piece piece = asBoard().pieceAt(pos);
        if (piece == null) return new RequestResult(false, "empty_source");
        if (isPieceInFlight(row, col)) return new RequestResult(false, "motion_in_progress");
        if (isPieceResting(row, col)) return new RequestResult(false, "resting");

        addMove(new MovingPiece(piece.code, row, col, row, col, currentTime + 1000));
        return new RequestResult(true, "ok");
    }

    /**
     * Read-only snapshot for the renderer. Board-relative coordinates only (no pixels - that's
     * the View's job). Selection is passed in because GameEngine doesn't own selection state;
     * the Controller does.
     */
    public synchronized GameSnapshot snapshot(int selectedRow, int selectedCol) {
        List<PieceSnapshot> pieces = new ArrayList<>();
        boolean[][] inFlight = new boolean[board.length][board[0].length];
        for (MovingPiece mp : activeMoves) {
            inFlight[mp.fromRow][mp.fromCol] = true;
        }

        for (Piece piece : asBoard().allPieces()) {
            int r = piece.cell.row, c = piece.cell.col;
            if (inFlight[r][c]) continue;

            String key = r + "_" + c;
            RestInfo rest = restStates.get(key);

            if (rest != null) {
                long elapsed = currentTime - rest.startTime;
                if (elapsed < rest.durationMs) {
                    double remaining = 1.0 - elapsed / (double) rest.durationMs;
                    pieces.add(new PieceSnapshot(key, piece.code, r, c, rest.state, elapsed, remaining));
                    continue;
                }
                restStates.remove(key); // rest expired - fall through to idle, and stop tracking it
            }

            // currentTime as a phase input is fine for a state that just loops forever (idle
            // never "starts" from the renderer's point of view - it's ambient).
            pieces.add(new PieceSnapshot(key, piece.code, r, c, PieceVisualState.IDLE, currentTime, 0.0));
        }

        for (MovingPiece mp : activeMoves) {
            if (mp.piece.equals(GameConstants.EMPTY) || mp.piece.length() < 2) continue; // not a real piece

            boolean isJump = (mp.fromRow == mp.toRow && mp.fromCol == mp.toCol);
            PieceVisualState state = isJump ? PieceVisualState.JUMP : PieceVisualState.MOVE;

            long duration;
            if (isJump) {
                duration = 1000L; // matches the fixed duration the "jump" text command uses today
            } else {
                PieceStateConfig moveConfig = PieceStateConfig.readFrom(PieceAssetPaths.configPath(mp.piece, PieceVisualState.MOVE));
                long distance = Math.max(Math.abs(mp.toRow - mp.fromRow), Math.abs(mp.toCol - mp.fromCol));
                duration = Math.round(distance / moveConfig.speedMetersPerSec * 1000);
            }

            long startTime = mp.arrivalTime - duration;
            long elapsed = Math.max(0, currentTime - startTime);
            double progress = (duration == 0) ? 1.0 : Math.min(1.0, elapsed / (double) duration);

            double row = mp.fromRow + (mp.toRow - mp.fromRow) * progress;
            double col = mp.fromCol + (mp.toCol - mp.fromCol) * progress;
            pieces.add(new PieceSnapshot(mp.fromRow + "_" + mp.fromCol, mp.piece, row, col, state, elapsed, 0.0));
        }

        List<Position> legalMoves = (selectedRow == -1)
                ? Collections.emptyList()
                : legalDestinations(selectedRow, selectedCol);

        return new GameSnapshot(board.length, board[0].length, pieces, selectedRow, selectedCol, gameOver, winner,
                legalMoves, new ArrayList<>(moveLog), whiteScore, blackScore);
    }

    /** Every square the piece at (row,col) could legally move to right now (per RuleEngine). */
    public synchronized List<Position> legalDestinations(int row, int col) {
        List<Position> destinations = new ArrayList<>();
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (r == row && c == col) continue;
                if (RuleEngine.checkMove(row, col, r, c, board) == RuleEngine.MoveResult.OK) {
                    destinations.add(new Position(r, c));
                }
            }
        }
        return destinations;
    }

    /** True if the piece at (row,col) already has a pending move that hasn't been resolved yet. */
    public synchronized boolean isPieceInFlight(int row, int col) {
        for (MovingPiece mp : activeMoves) {
            if (mp.fromRow == row && mp.fromCol == col) return true;
        }
        return false;
    }

    /**
     * Two same-color pieces can never share a square. If their straight-line paths cross
     * a common square at nearly the same time (within one step), the one that would get
     * there later is truncated to stop one square short of it instead of colliding.
     * On an exact tie, the move added later is the one truncated.
     */
    private void resolveSameColorPathConflicts() {
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < activeMoves.size() * 2 + 5) {
            changed = false;
            List<MovingPiece> snapshot = new ArrayList<>(activeMoves);
            outer:
            for (int i = 0; i < snapshot.size(); i++) {
                MovingPiece a = snapshot.get(i);
                if (isJump(a)) continue;
                for (int j = i + 1; j < snapshot.size(); j++) {
                    MovingPiece b = snapshot.get(j);
                    if (isJump(b)) continue;
                    if (GameConstants.isWhite(a.piece) != GameConstants.isWhite(b.piece)) continue;

                    if (stopAtNearCollision(a, b)) {
                        changed = true;
                        break outer;
                    }
                }
            }
        }
    }

    private boolean isJump(MovingPiece mp) {
        return mp.fromRow == mp.toRow && mp.fromCol == mp.toCol;
    }

    /** Real per-square travel time from this piece's own move config - must match what requestMove
     *  actually scheduled, or this whole near-collision reconstruction is measuring against a
     *  timeline that never existed. */
    private long msPerSquare(String pieceCode) {
        PieceStateConfig config = PieceStateConfig.readFrom(PieceAssetPaths.configPath(pieceCode, PieceVisualState.MOVE));
        return Math.round(1000 / config.speedMetersPerSec);
    }

    private boolean stopAtNearCollision(MovingPiece a, MovingPiece b) {
        List<int[]> pathA = computePath(a.fromRow, a.fromCol, a.toRow, a.toCol);
        List<int[]> pathB = computePath(b.fromRow, b.fromCol, b.toRow, b.toCol);
        if (pathA.isEmpty() || pathB.isEmpty()) return false;

        long stepMsA = msPerSquare(a.piece);
        long stepMsB = msPerSquare(b.piece);
        long startA = a.arrivalTime - pathA.size() * stepMsA;
        long startB = b.arrivalTime - pathB.size() * stepMsB;
        long closeThreshold = Math.max(stepMsA, stepMsB);

        for (int ia = 0; ia < pathA.size(); ia++) {
            for (int ib = 0; ib < pathB.size(); ib++) {
                if (pathA.get(ia)[0] != pathB.get(ib)[0] || pathA.get(ia)[1] != pathB.get(ib)[1]) continue;

                long timeAtA = startA + (ia + 1) * stepMsA;
                long timeAtB = startB + (ib + 1) * stepMsB;

                // Both pieces actually landing on the same square is a guaranteed conflict -
                // same-color pieces can never share a square - no matter how far apart their
                // arrival times are. A shared square that's only a waypoint for one of them,
                // on the other hand, is just a near-miss and only matters if the times are close.
                boolean bothLandHere = (ia == pathA.size() - 1) && (ib == pathB.size() - 1);
                if (!bothLandHere && Math.abs(timeAtA - timeAtB) >= closeThreshold) continue;

                if (timeAtA > timeAtB) {
                    truncateBefore(a, pathA, ia, startA, stepMsA);
                } else {
                    truncateBefore(b, pathB, ib, startB, stepMsB);
                }
                return true;
            }
        }
        return false;
    }

    private void truncateBefore(MovingPiece mp, List<int[]> path, int collisionIndex, long startTime, long stepMs) {
        if (collisionIndex == 0) {
            mp.toRow = mp.fromRow;
            mp.toCol = mp.fromCol;
            mp.arrivalTime = startTime;
        } else {
            int[] stopSquare = path.get(collisionIndex - 1);
            mp.toRow = stopSquare[0];
            mp.toCol = stopSquare[1];
            mp.arrivalTime = startTime + collisionIndex * stepMs;
        }
    }

    /** Ordered squares from just after (fr,fc) to (tr,tc). Straight lines only; other shapes (e.g. knight) jump directly. */
    private static List<int[]> computePath(int fr, int fc, int tr, int tc) {
        List<int[]> path = new ArrayList<>();
        int dr = tr - fr, dc = tc - fc;
        int steps = Math.max(Math.abs(dr), Math.abs(dc));
        if (steps == 0) return path;

        boolean straightLine = (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc));
        if (!straightLine) {
            path.add(new int[]{tr, tc});
            return path;
        }

        int stepR = Integer.compare(dr, 0);
        int stepC = Integer.compare(dc, 0);
        int r = fr, c = fc;
        for (int i = 0; i < steps; i++) {
            r += stepR; c += stepC;
            path.add(new int[]{r, c});
        }
        return path;
    }

    /**
     * Process any moves whose arrivalTime is <= currentTime.
     * Moves are applied in chronological order; each move is applied
     * only if its source still contains the expected piece.
     */
    public synchronized void processMoves() {
        // Sort by arrival time to simulate real-time arrivals
        activeMoves.sort(Comparator.comparingLong(m -> m.arrivalTime));

        Board b = asBoard();
        List<MovingPiece> processed = new ArrayList<>();

        for (MovingPiece mp : new ArrayList<>(activeMoves)) {
            if (mp.arrivalTime > currentTime) break;

            Position from = new Position(mp.fromRow, mp.fromCol);
            Position to = new Position(mp.toRow, mp.toCol);

            // Validate source still holds the moving piece
            if (!b.isInBounds(from)) {
                processed.add(mp);
                continue;
            }

            Piece atSource = b.pieceAt(from);
            if (atSource == null || !atSource.code.equals(mp.piece)) {
                // Source no longer contains this piece (it was captured or moved)
                processed.add(mp);
                continue;
            }

            // Validate destination bounds
            if (!b.isInBounds(to)) {
                // Invalid destination: clear source and drop the move
                b.removePiece(from);
                processed.add(mp);
                continue;
            }

            boolean isJump = (mp.fromRow == mp.toRow && mp.fromCol == mp.toCol);

            if (isJump) {
                // A jump never leaves its square, so there is no "destination" to check against -
                // it always completes in place and starts its (longer) rest cooldown.
                startRest(atSource.code, mp.toRow, mp.toCol, PieceVisualState.SHORT_REST, JUMP_REST_MS);
                processed.add(mp);
                continue;
            }

            Piece defender = b.pieceAt(to);

            if (defender != null) {
                // Destination occupied
                if (defender.isWhite() == atSource.isWhite()) {
                    // Friendly piece on destination: cancel this move (leave board as-is)
                    processed.add(mp);
                    continue;
                } else if (isDefenderAirborne(mp.toRow, mp.toCol, mp.arrivalTime)) {
                    // Target is mid-jump (airborne): the attacker is destroyed instead of capturing.
                    b.removePiece(from);
                    processed.add(mp);
                    continue;
                } else {
                    // Capture enemy piece at destination
                    if (defender.kindLetter() == 'K') {
                        gameOver = true;
                        winner = atSource.isWhite() ? "white" : "black";
                    }
                    if (atSource.isWhite()) {
                        whiteScore += pieceValue(defender.kindLetter());
                    } else {
                        blackScore += pieceValue(defender.kindLetter());
                    }
                    String landedPiece = promote(mp.piece, mp.toRow);
                    b.removePiece(from);
                    b.placePieceCode(to, landedPiece);
                    startRest(landedPiece, mp.toRow, mp.toCol, PieceVisualState.LONG_REST,
                        moveRestMs(mp.fromRow, mp.fromCol, mp.toRow, mp.toCol));
                    moveLog.add(new MoveLogEntry(atSource.isWhite(), notationFor(mp, true, landedPiece), currentTime));
                }
            } else {
                // Normal landing
                String landedPiece = promote(mp.piece, mp.toRow);
                b.removePiece(from);
                b.placePieceCode(to, landedPiece);
                startRest(landedPiece, mp.toRow, mp.toCol, PieceVisualState.LONG_REST,
                        moveRestMs(mp.fromRow, mp.fromCol, mp.toRow, mp.toCol));
                moveLog.add(new MoveLogEntry(atSource.isWhite(), notationFor(mp, false, landedPiece), currentTime));
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
