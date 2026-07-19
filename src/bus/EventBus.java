package bus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A tiny generic publish/subscribe bus. Whoever notices a game change (a score going up, a
 * move being logged, a game starting or ending) just calls publish() with an event object -
 * it doesn't need to know or care who's listening. Whoever reacts to that change (the score
 * label, the move log table, the sound player, the win animation) just subscribes to the
 * event type it cares about - it doesn't need to know who published it. That's the whole
 * point: those four reactions used to all be wired together inline in one place; now each
 * one only depends on the event type, not on each other or on how the change was detected.
 */
public class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();

    /** Registers a listener for exactly one event type (by class). Every subscribe() for that
     *  type gets called, in registration order, whenever a matching event is published. */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        subscribers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) listener);
    }

    /** Notifies every listener subscribed to this exact event's class. A no-op if nobody's
     *  subscribed to it - the publisher never needs to check that itself. */
    public void publish(Object event) {
        List<Consumer<Object>> listeners = subscribers.get(event.getClass());
        if (listeners == null) return;
        for (Consumer<Object> listener : listeners) {
            listener.accept(event);
        }
    }
}
