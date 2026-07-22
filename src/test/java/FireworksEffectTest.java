import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import view.FireworksEffect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class FireworksEffectTest {

    private static BufferedImage blankCanvas() {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        return image;
    }

    /** True if at least one pixel differs from a freshly filled-black canvas - i.e. something
     *  was actually drawn on top of it. */
    private static boolean hasAnyNonBlackPixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0x00FFFFFF) != 0) return true;
            }
        }
        return false;
    }

    @Test
    public void freshlyConstructedEffectRendersNothing() {
        FireworksEffect fireworks = new FireworksEffect();
        BufferedImage canvas = blankCanvas();

        Graphics2D g = canvas.createGraphics();
        fireworks.render(g);
        g.dispose();

        assertFalse(hasAnyNonBlackPixel(canvas), "nothing should be drawn before any burst is spawned");
    }

    @Test
    public void spawnBurstProducesVisibleParticles() {
        FireworksEffect fireworks = new FireworksEffect();
        fireworks.spawnBurst(100, 100);
        BufferedImage canvas = blankCanvas();

        Graphics2D g = canvas.createGraphics();
        fireworks.render(g);
        g.dispose();

        assertTrue(hasAnyNonBlackPixel(canvas), "a freshly spawned burst should paint visible particles");
    }

    @Test
    public void clearRemovesAllParticlesImmediately() {
        FireworksEffect fireworks = new FireworksEffect();
        fireworks.spawnBurst(100, 100);
        fireworks.clear();
        BufferedImage canvas = blankCanvas();

        Graphics2D g = canvas.createGraphics();
        fireworks.render(g);
        g.dispose();

        assertFalse(hasAnyNonBlackPixel(canvas), "clear() should remove every particle");
    }

    @Test
    public void particlesEventuallyDecayAndDisappearOnTheirOwn() {
        FireworksEffect fireworks = new FireworksEffect();
        fireworks.spawnBurst(100, 100);

        // Every particle's decayPerSecond is at most 0.9, so life (starting at 1.0) is
        // guaranteed to reach zero well within 10 simulated seconds.
        for (int i = 0; i < 1000; i++) {
            fireworks.update(0.01);
        }
        BufferedImage canvas = blankCanvas();

        Graphics2D g = canvas.createGraphics();
        fireworks.render(g);
        g.dispose();

        assertFalse(hasAnyNonBlackPixel(canvas), "particles should have fully decayed and been removed by now");
    }
}
