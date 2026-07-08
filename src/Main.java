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
        engine.board = rows.toArray(new String[0][0]);

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
                        if (PieceFactory.isMoveLegal(engine.sR, engine.sCol, r, c, engine.board) && 
                            !PieceFactory.isPathBlocked(engine.sR, engine.sCol, r, c, engine.board)) {
                            engine.activeMoves.add(new MovingPiece(engine.board[engine.sR][engine.sCol], engine.sR, engine.sCol, r, c, engine.currentTime + 1000));
                        }
                    }
                    engine.sR = -1; engine.sCol = -1;
                }
            } else if (cmd.equals("jump")) {
                int c = sc.nextInt() / 100;
                int r = sc.nextInt() / 100;
                if (!engine.gameOver && r >= 0 && r < engine.board.length && c >= 0 && c < engine.board[0].length && !engine.board[r][c].equals(GameConstants.EMPTY)) {
                    engine.activeMoves.add(new MovingPiece(engine.board[r][c], r, c, r, c, engine.currentTime + 1000));
                }
            } else if (cmd.equals("wait")) {
                engine.currentTime += sc.nextLong();
                engine.processMoves();
            } else if (cmd.equals("print")) {
                sc.next();
                engine.processMoves();
                for (String[] row : engine.board) {
                    System.out.println(String.join(" ", row));
                }
            }
        }
    }
}