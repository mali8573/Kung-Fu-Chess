package net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal WebSocket text-frame encode/decode (RFC 6455), just enough for our own
 * client and server to exchange single-frame text messages. No fragmentation, no
 * binary frames, no compression extensions - this is a private wire protocol
 * between our own two ends, not a general-purpose library.
 */
public class WebSocketFrame {
    private static final int OPCODE_TEXT = 0x1;
    private static final int OPCODE_CLOSE = 0x8;

    /** Thrown when the peer sent a close frame or the stream ended. */
    public static class ConnectionClosedException extends IOException {
        public ConnectionClosedException(String message) { super(message); }
    }

    public static void writeText(OutputStream out, String text, boolean masked) throws IOException {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        writeFrame(out, OPCODE_TEXT, payload, masked);
    }

    private static void writeFrame(OutputStream out, int opcode, byte[] payload, boolean masked) throws IOException {
        out.write(0x80 | opcode); // FIN=1, opcode

        int len = payload.length;
        int maskBit = masked ? 0x80 : 0x00;
        if (len < 126) {
            out.write(maskBit | len);
        } else if (len <= 0xFFFF) {
            out.write(maskBit | 126);
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            throw new IOException("Message too large for this minimal implementation: " + len + " bytes");
        }

        if (masked) {
            byte[] maskKey = new byte[4];
            new java.security.SecureRandom().nextBytes(maskKey);
            out.write(maskKey);
            byte[] masked_ = new byte[payload.length];
            for (int i = 0; i < payload.length; i++) {
                masked_[i] = (byte) (payload[i] ^ maskKey[i % 4]);
            }
            out.write(masked_);
        } else {
            out.write(payload);
        }
        out.flush();
    }

    /** Blocks until the next text frame arrives; throws ConnectionClosedException on close/EOF. */
    public static String readText(InputStream in) throws IOException {
        while (true) {
            int b0 = readByteOrThrow(in);
            int opcode = b0 & 0x0F;

            int b1 = readByteOrThrow(in);
            boolean masked = (b1 & 0x80) != 0;
            int len = b1 & 0x7F;

            if (len == 126) {
                len = (readByteOrThrow(in) << 8) | readByteOrThrow(in);
            } else if (len == 127) {
                throw new IOException("Message too large for this minimal implementation");
            }

            byte[] maskKey = null;
            if (masked) {
                maskKey = new byte[4];
                readFully(in, maskKey);
            }

            byte[] payload = new byte[len];
            readFully(in, payload);
            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= maskKey[i % 4];
                }
            }

            if (opcode == OPCODE_CLOSE) {
                throw new ConnectionClosedException("Peer sent a close frame");
            }
            if (opcode == OPCODE_TEXT) {
                return new String(payload, StandardCharsets.UTF_8);
            }
            // Any other opcode (ping/pong/continuation) - not needed for our own private
            // protocol, so just ignore it and read the next frame.
        }
    }

    private static int readByteOrThrow(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) throw new ConnectionClosedException("Stream ended");
        return b;
    }

    private static void readFully(InputStream in, byte[] dst) throws IOException {
        int off = 0;
        while (off < dst.length) {
            int n = in.read(dst, off, dst.length - off);
            if (n == -1) throw new ConnectionClosedException("Stream ended mid-frame");
            off += n;
        }
    }
}
