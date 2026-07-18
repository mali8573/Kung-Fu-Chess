package model;

public class GameConstants {
    public static final String EMPTY = ".";
    
    public static final String W_KING = "wK";
    public static final String W_QUEEN = "wQ";
    public static final String W_ROOK = "wR";
    public static final String W_BISHOP = "wB";
    public static final String W_KNIGHT = "wN";
    public static final String W_PAWN = "wP";
    
    public static final String B_KING = "bK";
    public static final String B_QUEEN = "bQ";
    public static final String B_ROOK = "bR";
    public static final String B_BISHOP = "bB";
    public static final String B_KNIGHT = "bN";
    public static final String B_PAWN = "bP";

    // Helper for checking piece color
    public static boolean isWhite(String piece) {
        return piece.startsWith("w");
    }
}
