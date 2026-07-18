import java.util.*;

import controller.GameController;
import engine.GameEngine;
import engine.MovingPiece;
import io.BoardParser;
import io.BoardPrinter;
import model.GameConstants;
import view.BoardGeometry;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextLine()) return;
        sc.nextLine();
        List<String[]> rows = new ArrayList<>();

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.equals("Commands:")) break;
            rows.add(line.trim().split("\\s+"));
        }

        GameEngine engine = new GameEngine();
        try {
            engine.board = BoardParser.fromRows(rows);
        } catch (BoardParser.BoardFormatException e) {
            System.out.println("ERROR " + e.getMessage());
            return;
        }

        // Fixed CELL_SIZE=100 mapping, matching the text-DSL pixel convention exactly
        // (col = x / 100, row = y / 100, no letterboxing) - the GUI uses a separate,
        // dynamically-resizing BoardGeometry instead.
        BoardGeometry geometry = new BoardGeometry(engine.board.length, engine.board[0].length);
        geometry.resize(engine.board[0].length * 100, engine.board.length * 100);
        GameController controller = new GameController(engine, geometry);

        while (sc.hasNext()) {
            String cmd = sc.next();
            if (cmd.equals("click")) {
                int x = sc.nextInt();
                int y = sc.nextInt();
                controller.click(x, y);
            } else if (cmd.equals("jump")) {
                int c = sc.nextInt() / 100;
                int r = sc.nextInt() / 100;
                if (!engine.gameOver && r >= 0 && r < engine.board.length && c >= 0 && c < engine.board[0].length
                        && !engine.board[r][c].equals(GameConstants.EMPTY) && !engine.isPieceInFlight(r, c)) {
                    engine.addMove(new MovingPiece(engine.board[r][c], r, c, r, c, engine.currentTime + 1000));
                }
            } else if (cmd.equals("wait")) {
                long dt = sc.nextLong();
                engine.advanceTime(dt);
            } else if (cmd.equals("print")) {
                sc.next();
                engine.processMoves();
                BoardPrinter.print(engine.board);
            }
        }
    }
}
