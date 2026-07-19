package net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * One established WebSocket connection (after the HTTP upgrade handshake has already
 * happened). Per RFC 6455, frames sent by a client MUST be masked and frames sent by
 * a server MUST NOT be - isClientSide records which end of the wire we are, so the
 * right thing happens automatically.
 */
public class WebSocketConnection {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final boolean isClientSide;

    public WebSocketConnection(Socket socket, boolean isClientSide) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.isClientSide = isClientSide;
    }

    public synchronized void sendText(String text) throws IOException {
        WebSocketFrame.writeText(out, text, isClientSide);
    }

    /** Blocks until the next text message arrives. Throws when the peer disconnects. */
    public String readText() throws IOException {
        return WebSocketFrame.readText(in);
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) { /* already gone */ }
    }
}
