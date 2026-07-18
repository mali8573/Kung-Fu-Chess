package engine;

import java.util.Collections;
import java.util.List;

import model.Position;

/** Read-only, immutable data handed from GameEngine to the renderer. No game logic here. */
public final class GameSnapshot {
    public final int boardRows;
    public final int boardCols;
    public final List<PieceSnapshot> pieces;
    public final int selectedRow;
    public final int selectedCol;
    public final boolean gameOver;
    public final String winner;
    /** Squares the selected piece can legally move to right now; empty when nothing is selected. */
    public final List<Position> legalMoves;
    /** Every completed move so far, in order, for the moves-log panel. */
    public final List<MoveLogEntry> moveLog;
    /** Total point-value of enemy pieces each side has captured so far. */
    public final int whiteScore;
    public final int blackScore;

    public GameSnapshot(int boardRows, int boardCols, List<PieceSnapshot> pieces,
                         int selectedRow, int selectedCol, boolean gameOver, String winner,
                         List<Position> legalMoves, List<MoveLogEntry> moveLog,
                         int whiteScore, int blackScore) {
        this.boardRows = boardRows;
        this.boardCols = boardCols;
        this.pieces = Collections.unmodifiableList(pieces);
        this.selectedRow = selectedRow;
        this.selectedCol = selectedCol;
        this.gameOver = gameOver;
        this.winner = winner;
        this.legalMoves = Collections.unmodifiableList(legalMoves);
        this.moveLog = Collections.unmodifiableList(moveLog);
        this.whiteScore = whiteScore;
        this.blackScore = blackScore;
    }
}
