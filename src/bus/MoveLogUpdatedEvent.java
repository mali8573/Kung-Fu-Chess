package bus;

import java.util.List;

import engine.MoveLogEntry;

/** Published whenever the move log has grown, carrying each side's moves already split apart -
 *  the move-log panel just displays whatever list it's handed, it never inspects the game. */
public final class MoveLogUpdatedEvent {
    public final List<MoveLogEntry> whiteMoves;
    public final List<MoveLogEntry> blackMoves;

    public MoveLogUpdatedEvent(List<MoveLogEntry> whiteMoves, List<MoveLogEntry> blackMoves) {
        this.whiteMoves = whiteMoves;
        this.blackMoves = blackMoves;
    }
}
