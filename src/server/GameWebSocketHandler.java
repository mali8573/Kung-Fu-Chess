package server;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Spring's WebSocketHandler session lifecycle to GameServer's existing protocol
 * handling. Every session is wrapped in a ConcurrentWebSocketSessionDecorator before
 * GameServer ever sees it: Match.runLoop's 100ms tick broadcasts run on their own thread with
 * no lock held, and can race a client's MOVE/JUMP triggering a send on the same session - the
 * old hand-rolled WebSocketConnection.sendText was synchronized, but Tomcat's raw
 * WebSocketSession.sendMessage has no such guarantee and can throw under concurrent writes.
 * The raw-session -> decorated-session map lets later callbacks for the same connection
 * (handleTextMessage, afterConnectionClosed) keep reusing that same decorated instance, since
 * Spring always calls back with the raw session, not the decorator.
 */
class GameWebSocketHandler extends TextWebSocketHandler {
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final GameServer gameServer;
    private final Map<WebSocketSession, WebSocketSession> decoratedByRawSession = new ConcurrentHashMap<>();

    GameWebSocketHandler(GameServer gameServer) {
        this.gameServer = gameServer;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        decoratedByRawSession.put(session, decorated);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        gameServer.handleMessage(decoratedByRawSession.get(session), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketSession decorated = decoratedByRawSession.remove(session);
        if (decorated != null) {
            gameServer.removeConnection(decorated);
        }
    }
}
