/** One completed move, recorded for the on-screen moves log (not used by game logic itself). */
public final class MoveLogEntry {
    public final boolean white;
    public final String notation;
    public final long gameTimeMillis;

    public MoveLogEntry(boolean white, String notation, long gameTimeMillis) {
        this.white = white;
        this.notation = notation;
        this.gameTimeMillis = gameTimeMillis;
    }
}
