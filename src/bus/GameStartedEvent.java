package bus;

/** Published the moment a match begins (or a reconnect drops the player back into one) -
 *  published right where that's actually known (the COLOR message), not guessed later from
 *  a snapshot diff. */
public final class GameStartedEvent {
}
