package bus;

import engine.MoveLogEntry;

/** Published once for each new entry appended to the move log - lets a listener (e.g. the
 *  sound player, reacting only to captures) inspect one move at a time instead of re-scanning
 *  the whole log itself on every tick. */
public final class MoveLoggedEvent {
    public final MoveLogEntry entry;

    public MoveLoggedEvent(MoveLogEntry entry) {
        this.entry = entry;
    }
}
