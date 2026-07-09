import java.util.*;

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
        engine.board = BoardParser.fromRows(rows);
        RealTimeArbiter arbiter = new RealTimeArbiter(engine);

        while (sc.hasNext()) {
            String cmd = sc.next();
            if (cmd.equals("click")) {
                int c = sc.nextInt() / 100;
                int r = sc.nextInt() / 100;
                if (engine.gameOver || r < 0 || r >= engine.board.length || c < 0 || c >= engine.board[0].length) continue;
                
                if (engine.sR == -1) {
                    if (!engine.board[r][c].equals(GameConstants.EMPTY)) {
                        engine.sR = r; engine.sCol = c;
                    }
                } else {
                    boolean isSameSpot = (r == engine.sR && c == engine.sCol);
                    boolean isFriendly = (!engine.board[r][c].equals(GameConstants.EMPTY) && 
                                        GameConstants.isWhite(engine.board[r][c]) == GameConstants.isWhite(engine.board[engine.sR][engine.sCol]));

                    if (!isSameSpot && !isFriendly) {
                        RuleEngine.MoveResult res = RuleEngine.checkMove(engine.sR, engine.sCol, r, c, engine.board);
                        if (res == RuleEngine.MoveResult.OK) {
                            engine.addMove(new MovingPiece(engine.board[engine.sR][engine.sCol], engine.sR, engine.sCol, r, c, engine.currentTime + 1000));
                        }
                    }
                    engine.sR = -1; engine.sCol = -1;
                }
            } else if (cmd.equals("jump")) {
                int c = sc.nextInt() / 100;
                int r = sc.nextInt() / 100;
                if (!engine.gameOver && r >= 0 && r < engine.board.length && c >= 0 && c < engine.board[0].length && !engine.board[r][c].equals(GameConstants.EMPTY)) {
                    engine.addMove(new MovingPiece(engine.board[r][c], r, c, r, c, engine.currentTime + 1000));
                }
            } else if (cmd.equals("wait")) {
                long dt = sc.nextLong();
                arbiter.advanceTime(dt);
            } else if (cmd.equals("print")) {
                sc.next();
                engine.processMoves();
                BoardPrinter.print(engine.board);
            }
        }
    }
}