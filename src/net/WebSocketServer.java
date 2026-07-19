package net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Listens on a port and performs the server side of the WebSocket handshake for each
 * incoming connection, then hands the resulting WebSocketConnection off to a callback.
 * One thread per accepted connection is spawned by the caller-supplied handler (this
 * class itself only owns the accept loop).
 */
public class WebSocketServer {
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public WebSocketServer(int port) {
        this.port = port;
    }

    /** The actual bound port - useful when constructed with port 0 (auto-assign), e.g. in tests. */
    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    /** Starts accepting connections in a background thread. onConnection runs once per client. */
    public void start(Consumer<WebSocketConnection> onConnection) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handshakeAndDispatch(socket, onConnection)).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("WebSocketServer accept failed: " + e.getMessage());
                    }
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void handshakeAndDispatch(Socket socket, Consumer<WebSocketConnection> onConnection) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String key = null;
            String line;
            while (!(line = readHttpLine(in)).isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).equalsIgnoreCase("Sec-WebSocket-Key")) {
                    key = line.substring(colon + 1).trim();
                }
            }
            if (key == null) {
                socket.close();
                return;
            }

            String accept = WebSocketHandshake.acceptValueFor(key);
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n"
                    + "\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();

            onConnection.accept(new WebSocketConnection(socket, false));
        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    /** Reads one HTTP header line (up to, not including, the trailing CRLF). Byte-at-a-time
     *  on purpose - a buffered reader could overread into the binary frame data that follows
     *  the handshake on the same stream. */
    private static String readHttpLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int prev = -1, curr;
        while ((curr = in.read()) != -1) {
            if (prev == '\r' && curr == '\n') {
                byte[] bytes = line.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8);
            }
            line.write(curr);
            prev = curr;
        }
        throw new IOException("Connection closed during handshake");
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
    }
}
