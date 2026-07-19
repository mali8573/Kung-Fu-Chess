package net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import engine.GameSnapshot;
import engine.MoveLogEntry;
import engine.PieceSnapshot;
import view.PieceVisualState;

/**
 * Turns a GameSnapshot into a line-based text payload for the server's STATE broadcast,
 * and back. Selection and legalMoves are per-client UI state, not part of the server's
 * authoritative game state, so they are never sent - decode() always comes back with
 * nothing selected, and the caller (the networked client) fills in its own local
 * selection before handing the snapshot to BoardRenderer.
 */
public final class SnapshotCodec {
    private static final String SEP = "|";
    private static final Pattern SEP_PATTERN = Pattern.compile(Pattern.quote(SEP));

    private SnapshotCodec() { }

    public static String encode(GameSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append(snapshot.boardRows).append(SEP).append(snapshot.boardCols).append('\n');
        sb.append(snapshot.gameOver ? "1" : "0").append(SEP)
                .append(snapshot.winner == null ? "-" : snapshot.winner).append(SEP)
                .append(snapshot.whiteScore).append(SEP).append(snapshot.blackScore).append('\n');

        sb.append(snapshot.pieces.size()).append('\n');
        for (PieceSnapshot p : snapshot.pieces) {
            sb.append(p.id).append(SEP).append(p.pieceCode).append(SEP)
                    .append(p.row).append(SEP).append(p.col).append(SEP)
                    .append(p.state.name()).append(SEP).append(p.stateElapsedMillis).append(SEP)
                    .append(p.restRemainingFraction).append('\n');
        }

        sb.append(snapshot.moveLog.size()).append('\n');
        for (MoveLogEntry entry : snapshot.moveLog) {
            sb.append(entry.white ? "1" : "0").append(SEP)
                    .append(entry.notation).append(SEP)
                    .append(entry.gameTimeMillis).append('\n');
        }
        return sb.toString();
    }

    public static GameSnapshot decode(String payload) {
        String[] lines = payload.split("\n", -1);
        int idx = 0;

        String[] dims = SEP_PATTERN.split(lines[idx++]);
        int boardRows = Integer.parseInt(dims[0]);
        int boardCols = Integer.parseInt(dims[1]);

        String[] meta = SEP_PATTERN.split(lines[idx++]);
        boolean gameOver = meta[0].equals("1");
        String winner = meta[1].equals("-") ? null : meta[1];
        int whiteScore = Integer.parseInt(meta[2]);
        int blackScore = Integer.parseInt(meta[3]);

        int pieceCount = Integer.parseInt(lines[idx++]);
        List<PieceSnapshot> pieces = new ArrayList<>();
        for (int i = 0; i < pieceCount; i++) {
            String[] f = SEP_PATTERN.split(lines[idx++]);
            pieces.add(new PieceSnapshot(f[0], f[1], Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                    PieceVisualState.valueOf(f[4]), Long.parseLong(f[5]), Double.parseDouble(f[6])));
        }

        int moveCount = Integer.parseInt(lines[idx++]);
        List<MoveLogEntry> moveLog = new ArrayList<>();
        for (int i = 0; i < moveCount; i++) {
            String[] f = SEP_PATTERN.split(lines[idx++]);
            moveLog.add(new MoveLogEntry(f[0].equals("1"), f[1], Long.parseLong(f[2])));
        }

        return new GameSnapshot(boardRows, boardCols, pieces, -1, -1, gameOver, winner,
                Collections.emptyList(), moveLog, whiteScore, blackScore);
    }
}
