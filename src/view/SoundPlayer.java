package view;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Short procedurally-synthesized sound effects (no audio asset files needed). Every method
 * returns immediately - the actual tone plays on its own background thread, so gameplay and
 * rendering are never blocked by audio. Playback failures (e.g. no audio device) are swallowed;
 * sound is a nice-to-have, never a gameplay requirement.
 */
public class SoundPlayer {
    private static final float SAMPLE_RATE = 44100f;

    public static void playMove() {
        playToneAsync(700, 50, 0.2);
    }

    public static void playCapture() {
        playToneAsync(180, 130, 0.35);
    }

    public static void playJump() {
        playSweepAsync(400, 950, 140, 0.3);
    }

    public static void playVictoryFanfare() {
        new Thread(() -> {
            int[] notes = {523, 659, 784, 1047}; // C5 E5 G5 C6 major arpeggio
            for (int freq : notes) {
                playToneBlocking(freq, 160, 0.3);
            }
            playToneBlocking(1047, 500, 0.35);
        }).start();
    }

    private static void playToneAsync(int freq, int durationMs, double volume) {
        new Thread(() -> playToneBlocking(freq, durationMs, volume)).start();
    }

    private static void playToneBlocking(int freq, int durationMs, double volume) {
        playSamples(sineWave(freq, freq, durationMs, volume));
    }

    private static void playSweepAsync(int startFreq, int endFreq, int durationMs, double volume) {
        new Thread(() -> playSamples(sineWave(startFreq, endFreq, durationMs, volume))).start();
    }

    /** A sine wave sweeping linearly from startFreq to endFreq, with a short fade in/out to avoid clicks. */
    private static byte[] sineWave(int startFreq, int endFreq, int durationMs, double volume) {
        int samples = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[samples];
        double phase = 0;
        int fade = Math.min(200, samples / 4);
        for (int i = 0; i < samples; i++) {
            double t = i / (double) samples;
            double freq = startFreq + (endFreq - startFreq) * t;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            double envelope = Math.min(1.0, Math.min(i / (double) fade, (samples - i) / (double) fade));
            buffer[i] = (byte) (Math.sin(phase) * 127 * volume * envelope);
        }
        return buffer;
    }

    private static void playSamples(byte[] buffer) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(buffer, 0, buffer.length);
            line.drain();
        } catch (Exception ignored) {
            // No audio device or line unavailable - sound is best-effort only.
        }
    }
}
