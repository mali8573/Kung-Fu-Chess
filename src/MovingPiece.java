public class MovingPiece {
    public String piece;
    public int fromRow, fromCol, toRow, toCol;
    public long arrivalTime;
    
    public MovingPiece(String piece, int fromRow, int fromCol, int toRow, int toCol, long arrivalTime) {
        this.piece = piece;
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.arrivalTime = arrivalTime;
    }
}