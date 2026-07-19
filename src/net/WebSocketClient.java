package net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Client side of the handshake: connect, send the HTTP Upgrade request, verify the reply. */
public class WebSocketClient {

    public static WebSocketConnection connect(String host, int port, String path) throws IOException {
        Socket socket = new Socket(host, port);
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        String key = Base64.getEncoder().encodeToString(keyBytes);

        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Key: " + key + "\r\n"
                + "Sec-WebSocket-Version: 13\r\n"
                + "\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        String statusLine = readHttpLine(in);
        if (!statusLine.contains("101")) {
            socket.close();
            throw new IOException("Server rejected the WebSocket upgrade: " + statusLine);
        }

        String acceptHeader = null;
        String line;
        while (!(line = readHttpLine(in)).isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).equalsIgnoreCase("Sec-WebSocket-Accept")) {
                acceptHeader = line.substring(colon + 1).trim();
            }
        }

        String expected = WebSocketHandshake.acceptValueFor(key);
        if (!expected.equals(acceptHeader)) {
            socket.close();
            throw new IOException("Sec-WebSocket-Accept did not match - handshake integrity check failed");
        }

        return new WebSocketConnection(socket, true);
    }

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
}
