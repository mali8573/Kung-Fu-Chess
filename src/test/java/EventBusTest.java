import bus.EventBus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EventBusTest {

    private static final class Ping { final String text; Ping(String text) { this.text = text; } }
    private static final class Pong { }

    @Test
    public void subscriberReceivesPublishedEventOfItsExactType() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();
        bus.subscribe(Ping.class, ping -> received.add(ping.text));

        bus.publish(new Ping("hello"));

        assertEquals(1, received.size());
        assertEquals("hello", received.get(0));
    }

    @Test
    public void subscriberNeverReceivesADifferentEventType() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();
        bus.subscribe(Ping.class, ping -> received.add(ping.text));

        bus.publish(new Pong());

        assertTrue(received.isEmpty(), "a Ping subscriber must not react to a Pong event");
    }

    @Test
    public void everySubscriberToTheSameTypeIsNotified() {
        EventBus bus = new EventBus();
        List<String> firstListener = new ArrayList<>();
        List<String> secondListener = new ArrayList<>();
        bus.subscribe(Ping.class, ping -> firstListener.add(ping.text));
        bus.subscribe(Ping.class, ping -> secondListener.add(ping.text));

        bus.publish(new Ping("hi"));

        assertEquals(1, firstListener.size());
        assertEquals(1, secondListener.size());
    }

    @Test
    public void publishingWithNoSubscribersDoesNothing() {
        EventBus bus = new EventBus();
        assertDoesNotThrow(() -> bus.publish(new Ping("nobody's listening")));
    }
}
