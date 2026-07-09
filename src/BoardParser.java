import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoardParser {
    public static class BoardFormatException extends RuntimeException {
        public BoardFormatException(String message) { super(message); }
    }

    private static final Set<String> VALID_TOKENS = new HashSet<>(Arrays.asList(
        "wK", "wQ", "wR", "wB", "wN", "wP",
        "bK", "bQ", "bR", "bB", "bN", "bP"
    ));

    public static String[][] fromRows(List<String[]> rows) {
        if (!rows.isEmpty()) {
            int width = rows.get(0).length;
            for (String[] row : rows) {
                if (row.length != width) {
                    throw new BoardFormatException("ROW_WIDTH_MISMATCH");
                }
            }
        }

        for (String[] row : rows) {
            for (String token : row) {
                if (!token.equals(GameConstants.EMPTY) && !VALID_TOKENS.contains(token)) {
                    throw new BoardFormatException("UNKNOWN_TOKEN");
                }
            }
        }

        return rows.toArray(new String[0][0]);
    }
}
