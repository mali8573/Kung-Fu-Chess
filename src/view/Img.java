package view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Lightweight image-utility class using only standard JDK APIs.
 */
public class Img {

    private BufferedImage img;

    /* ----------- load & optional resize ----------- */
    public Img read(String path,
                    Dimension targetSize,
                    boolean keepAspect,
                    Object interpolation /*ignored*/) {

        try {
            img = ImageIO.read(new File(path));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load image: " + path);
        }
        if (img == null) throw new IllegalArgumentException("Unsupported image: " + path);

        if (targetSize != null) {
            img = scaled(img, targetSize, keepAspect);
        }
        return this;
    }

    public Img read(String path) { return read(path, null, false, null); }

    /**
     * Rescales this already-loaded image to a new size, purely in memory (no disk I/O).
     * Use this instead of read(path, ...) when you already have the source loaded and just
     * need a different size - e.g. on every window resize, so resizing doesn't re-hit the disk.
     */
    public Img resized(Dimension targetSize, boolean keepAspect) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Img result = new Img();
        result.img = scaled(img, targetSize, keepAspect);
        return result;
    }

    private static BufferedImage scaled(BufferedImage src, Dimension targetSize, boolean keepAspect) {
        int tw = targetSize.width, th = targetSize.height;
        int w = src.getWidth(), h = src.getHeight();

        int nw, nh;
        if (keepAspect) {
            double s = Math.min(tw / (double) w, th / (double) h);
            nw = (int) Math.round(w * s);
            nh = (int) Math.round(h * s);
        } else { nw = tw; nh = th; }

        BufferedImage dst = new BufferedImage(
                nw, nh,
                src.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB);

        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    /* ----------- draw this image onto another ----------- */
    public void drawOn(Img other, int x, int y) {
        if (img == null || other.img == null)
            throw new IllegalStateException("Both images must be loaded.");

        if (x + img.getWidth()  > other.img.getWidth()
         || y + img.getHeight() > other.img.getHeight())
            throw new IllegalArgumentException("Patch exceeds destination bounds.");

        Graphics2D g = other.img.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(img, x, y, null);
        g.dispose();
    }

    /* ----------- annotate with text ----------- */
    public void putText(String txt, int x, int y, float fontSize,
                        Color color, int thickness /*unused in Java2D*/) {

        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(img.getGraphics().getFont().deriveFont(fontSize * 12));
        g.drawString(txt, x, y);
        g.dispose();
    }

    /* ----------- filled rectangle, e.g. a translucent game-over overlay ----------- */
    public void fillRect(int x, int y, int w, int h, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(color);
        g.fillRect(x, y, w, h);
        g.dispose();
    }

    /* ----------- text centered on a point, e.g. a banner message ----------- */
    public void putTextCentered(String txt, int centerX, int centerY, float fontSize, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(g.getFont().deriveFont(fontSize * 12));
        FontMetrics fm = g.getFontMetrics();
        int x = centerX - fm.stringWidth(txt) / 2;
        int y = centerY + (fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(color);
        g.drawString(txt, x, y);
        g.dispose();
    }

    /* ----------- independent copy, so overlays don't permanently mark the source ----------- */
    public Img copy() {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        BufferedImage dst = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        Graphics2D g = dst.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        Img copy = new Img();
        copy.img = dst;
        return copy;
    }

    /* ----------- outlined rectangle, e.g. for a selection highlight ----------- */
    public void drawRectOutline(int x, int y, int w, int h, Color color, int thickness) {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawRect(x + thickness / 2, y + thickness / 2, w - thickness, h - thickness);
        g.dispose();
    }

    /* ----------- display in a Swing window ----------- */
    public void show() {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Image");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new JLabel(new ImageIcon(img)));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /* ----------- access (optional) ----------- */
    public BufferedImage get() { return img; }
}
