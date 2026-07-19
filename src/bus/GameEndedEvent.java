package bus;

/** Published exactly once, the moment a match's snapshot first reports gameOver. */
public final class GameEndedEvent {
    public final String winner;

    public GameEndedEvent(String winner) {
        this.winner = winner;
    }
}
