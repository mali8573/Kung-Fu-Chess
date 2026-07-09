public class BoardPrinter {
    public static void print(String[][] board) {
        for (String[] row : board) {
            System.out.println(String.join(" ", row));
        }
    }
}
