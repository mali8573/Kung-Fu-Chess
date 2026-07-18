package view;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Physics/graphics config for one piece state (idle/move/jump/short_rest/long_rest),
 * read from the asset pack's config.json. Hand-rolled parsing (regex over 4 fixed
 * fields) instead of a JSON library, since the format is small and fixed-shape and
 * we don't want an extra dependency for this.
 */
public class PieceStateConfig {
    private static final Pattern SPEED = Pattern.compile("\"speed_m_per_sec\"\\s*:\\s*([0-9.]+)");
    private static final Pattern NEXT_STATE = Pattern.compile("\"next_state_when_finished\"\\s*:\\s*\"([a-zA-Z_]+)\"");
    private static final Pattern FPS = Pattern.compile("\"frames_per_sec\"\\s*:\\s*([0-9]+)");
    private static final Pattern IS_LOOP = Pattern.compile("\"is_loop\"\\s*:\\s*(true|false)");

    public final double speedMetersPerSec;
    public final String nextStateWhenFinished;
    public final int framesPerSec;
    public final boolean isLoop;

    public PieceStateConfig(double speedMetersPerSec, String nextStateWhenFinished, int framesPerSec, boolean isLoop) {
        this.speedMetersPerSec = speedMetersPerSec;
        this.nextStateWhenFinished = nextStateWhenFinished;
        this.framesPerSec = framesPerSec;
        this.isLoop = isLoop;
    }

    private static final Map<String, PieceStateConfig> CACHE = new HashMap<>();

    /** Reads and parses config.json, caching by path so repeated lookups don't re-hit disk. */
    public static PieceStateConfig readFrom(String path) {
        PieceStateConfig cached = CACHE.get(path);
        if (cached != null) return cached;

        String json;
        try {
            json = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load config: " + path);
        }

        PieceStateConfig config = new PieceStateConfig(
                extractDouble(json, SPEED, path),
                extractString(json, NEXT_STATE, path),
                (int) extractDouble(json, FPS, path),
                Boolean.parseBoolean(extractString(json, IS_LOOP, path)));
        CACHE.put(path, config);
        return config;
    }

    private static double extractDouble(String json, Pattern pattern, String path) {
        Matcher m = pattern.matcher(json);
        if (!m.find()) throw new IllegalArgumentException("Missing field " + pattern + " in " + path);
        return Double.parseDouble(m.group(1));
    }

    private static String extractString(String json, Pattern pattern, String path) {
        Matcher m = pattern.matcher(json);
        if (!m.find()) throw new IllegalArgumentException("Missing field " + pattern + " in " + path);
        return m.group(1);
    }
}
