package engine;

public class RealTimeArbiter {
    private final GameEngine engine;

    public RealTimeArbiter(GameEngine engine) {
        this.engine = engine;
    }

    /** Advance the simulated time by dt and process moves that arrived. */
    public void advanceTime(long dt) {
        engine.currentTime += dt;
        engine.processMoves();
    }
}
