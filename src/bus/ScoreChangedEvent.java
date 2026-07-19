package bus;

/** Published whenever either side's captured-points total changes. */
public final class ScoreChangedEvent {
    public final int whiteScore;
    public final int blackScore;

    public ScoreChangedEvent(int whiteScore, int blackScore) {
        this.whiteScore = whiteScore;
        this.blackScore = blackScore;
    }
}
