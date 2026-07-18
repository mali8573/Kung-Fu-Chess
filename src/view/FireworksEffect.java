package view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A lightweight particle-based fireworks display, drawn on top of the board on a win. Owns no
 * timer of its own - the caller drives update(dt) from its own render loop and calls
 * spawnBurst(...) whenever a new firework should go off.
 */
public class FireworksEffect {
    private static final Color[] PALETTE = {
            new Color(255, 215, 0), new Color(255, 90, 90), new Color(90, 180, 255),
            new Color(130, 255, 130), new Color(255, 255, 255), new Color(255, 140, 255),
    };
    private static final double GRAVITY = 260; // px/s^2

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private static final class Particle {
        double x, y, vx, vy;
        final Color color;
        double life = 1.0;
        final double decayPerSecond;

        Particle(double x, double y, double vx, double vy, Color color, double decayPerSecond) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = color;
            this.decayPerSecond = decayPerSecond;
        }
    }

    public void spawnBurst(double centerX, double centerY) {
        Color color = PALETTE[random.nextInt(PALETTE.length)];
        int count = 55 + random.nextInt(25);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = 80 + random.nextDouble() * 220;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            particles.add(new Particle(centerX, centerY, vx, vy, color, 0.55 + random.nextDouble() * 0.35));
        }
    }

    public void update(double dtSeconds) {
        for (Particle p : particles) {
            p.x += p.vx * dtSeconds;
            p.y += p.vy * dtSeconds;
            p.vy += GRAVITY * dtSeconds;
            p.life -= p.decayPerSecond * dtSeconds;
        }
        particles.removeIf(p -> p.life <= 0);
    }

    public void clear() {
        particles.clear();
    }

    public void render(Graphics2D g) {
        for (Particle p : particles) {
            int alpha = (int) Math.round(Math.max(0, Math.min(1, p.life)) * 255);
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            int size = 5;
            g.fillOval((int) Math.round(p.x - size / 2.0), (int) Math.round(p.y - size / 2.0), size, size);
        }
    }
}
