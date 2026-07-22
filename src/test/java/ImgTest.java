import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import view.Img;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class ImgTest {

    // A real asset shipped in this repo, so this also verifies loading doesn't regress.
    private static final String BOARD_IMAGE_PATH = "assets/board.png";

    @Test
    public void readLoadsARealImageFromDisk() {
        Img img = new Img().read(BOARD_IMAGE_PATH);

        BufferedImage loaded = img.get();
        assertNotNull(loaded);
        assertTrue(loaded.getWidth() > 0);
        assertTrue(loaded.getHeight() > 0);
    }

    @Test
    public void readingAMissingFileThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Img().read("assets/does_not_exist.png"));
    }

    @Test
    public void readWithATargetSizeResizesWithoutKeepingAspect() {
        Img img = new Img().read(BOARD_IMAGE_PATH, new Dimension(400, 250), false, null);

        assertEquals(400, img.get().getWidth());
        assertEquals(250, img.get().getHeight());
    }

    @Test
    public void resizedKeepingAspectFitsInsideTheTargetBox() {
        Img original = new Img().read(BOARD_IMAGE_PATH);
        int originalWidth = original.get().getWidth();
        int originalHeight = original.get().getHeight();

        Img resized = original.resized(new Dimension(100, 100), true);

        assertTrue(resized.get().getWidth() <= 100);
        assertTrue(resized.get().getHeight() <= 100);
        // At least one dimension should hit the target box exactly - that's what "fit" means.
        assertTrue(resized.get().getWidth() == 100 || resized.get().getHeight() == 100);
        // The original this was derived from must be untouched by the resize.
        assertEquals(originalWidth, original.get().getWidth());
        assertEquals(originalHeight, original.get().getHeight());
    }

    @Test
    public void fillRectPaintsTheRequestedAreaWithTheGivenColor() {
        Img img = new Img().read(BOARD_IMAGE_PATH, new Dimension(50, 50), false, null);

        img.fillRect(0, 0, 50, 50, Color.RED);

        assertEquals(Color.RED.getRGB(), img.get().getRGB(25, 25));
    }

    @Test
    public void drawOnCopiesPixelsFromOneImageOntoAnother() {
        Img patch = new Img().read(BOARD_IMAGE_PATH, new Dimension(10, 10), false, null);
        patch.fillRect(0, 0, 10, 10, Color.BLUE);
        Img canvas = new Img().read(BOARD_IMAGE_PATH, new Dimension(50, 50), false, null);
        canvas.fillRect(0, 0, 50, 50, Color.GREEN);

        patch.drawOn(canvas, 20, 20);

        assertEquals(Color.BLUE.getRGB(), canvas.get().getRGB(25, 25));
        assertEquals(Color.GREEN.getRGB(), canvas.get().getRGB(0, 0), "outside the patch, the canvas is untouched");
    }

    @Test
    public void drawOnRejectsAPatchThatExceedsTheDestinationBounds() {
        Img patch = new Img().read(BOARD_IMAGE_PATH, new Dimension(30, 30), false, null);
        Img canvas = new Img().read(BOARD_IMAGE_PATH, new Dimension(20, 20), false, null);

        assertThrows(IllegalArgumentException.class, () -> patch.drawOn(canvas, 0, 0));
    }

    @Test
    public void copyProducesAnIndependentImage() {
        Img original = new Img().read(BOARD_IMAGE_PATH, new Dimension(20, 20), false, null);
        original.fillRect(0, 0, 20, 20, Color.RED);

        Img copy = original.copy();
        copy.fillRect(0, 0, 20, 20, Color.BLUE);

        assertEquals(Color.RED.getRGB(), original.get().getRGB(5, 5), "modifying the copy must not affect the original");
        assertEquals(Color.BLUE.getRGB(), copy.get().getRGB(5, 5));
    }

    @Test
    public void drawRectOutlinePaintsTheBorderWithoutFillingTheInterior() {
        Img img = new Img().read(BOARD_IMAGE_PATH, new Dimension(40, 40), false, null);
        img.fillRect(0, 0, 40, 40, Color.BLACK);

        img.drawRectOutline(5, 5, 20, 20, Color.WHITE, 2);

        assertEquals(Color.WHITE.getRGB(), img.get().getRGB(6, 6), "the outline itself should be painted");
        assertEquals(Color.BLACK.getRGB(), img.get().getRGB(15, 15), "the interior must be left alone");
    }

    @Test
    public void operationsOnAnUnloadedImageThrowIllegalState() {
        Img img = new Img();

        assertThrows(IllegalStateException.class, () -> img.fillRect(0, 0, 10, 10, Color.RED));
        assertThrows(IllegalStateException.class, () -> img.resized(new Dimension(10, 10), false));
        assertThrows(IllegalStateException.class, img::copy);
    }
}
