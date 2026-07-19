package client;

import net.WebSocketConnection;
import net.WebSocketClient;
import net.WebSocketFrame;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Connects to a GameServer and speaks its simple text protocol. This is the piece a
 * real GUI (or a test) uses instead of talking to a local GameEngine directly.
 */
public class GameClient {
    private final WebSocketConnection connection;
    private volatile String assignedColor;

    private GameClient(WebSocketConnection connection) {
        this.connection = connection;
    }

    /** Connects and starts listening; onMessage is called once per line received from the server
     *  ("COLOR white", "STATE\n...", "REJECTED ..."), on a background thread. */
    public static GameClient connect(String host, int port, Consumer<String> onMessage) throws IOException {
        WebSocketConnection connection = WebSocketClient.connect(host, port, "/");
        GameClient client = new GameClient(connection);

        Thread listener = new Thread(() -> {
            try {
                while (true) {
                    String message = connection.readText();
                    if (message.startsWith("COLOR ")) {
                        client.assignedColor = message.substring("COLOR ".length()).trim();
                    }
                    onMessage.accept(message);
                }
            } catch (WebSocketFrame.ConnectionClosedException e) {
                // server closed the connection - nothing more to do
            } catch (IOException e) {
                System.err.println("GameClient connection error: " + e.getMessage());
            }
        });
        listener.setDaemon(true);
        listener.start();

        return client;
    }

    public String getAssignedColor() {
        return assignedColor;
    }

    /** Logs into (or, on first use, registers) an account. Send once, right after connecting;
     *  can be sent again after a "LOGIN_FAILED" to retry with different credentials. */
    public void sendLogin(String username, String password) throws IOException {
        connection.sendText("LOGIN " + username + " " + password);
    }

    /** Joins the matchmaking queue - send once, after "LOGIN_OK". */
    public void sendPlay() throws IOException {
        connection.sendText("PLAY");
    }

    /** Sets aside a private room and waits for a friend to join it with the code from "ROOM_CREATED". */
    public void sendCreateRoom() throws IOException {
        connection.sendText("CREATE_ROOM");
    }

    /** Joins a room by its code - becomes the Black player if the room's still waiting for
     *  one, or a read-only spectator if it's already got both players seated. */
    public void sendJoinRoom(String code) throws IOException {
        connection.sendText("JOIN_ROOM " + code);
    }

    /** e.g. sendMove("e2", "e5") */
    public void sendMove(String fromSquare, String toSquare) throws IOException {
        connection.sendText("MOVE " + fromSquare + toSquare);
    }

    public void sendJump(String square) throws IOException {
        connection.sendText("JUMP " + square);
    }

    public void close() {
        connection.close();
    }
}
