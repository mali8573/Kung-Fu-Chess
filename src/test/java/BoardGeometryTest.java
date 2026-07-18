import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import view.BoardGeometry;

public class BoardGeometryTest {

    @Test
    public void squarePanelHasNoLetterboxing() {
        BoardGeometry geo = new BoardGeometry(8, 8);
        geo.resize(800, 800);

        assertEquals(0, geo.getOriginX());
        assertEquals(0, geo.getOriginY());
        assertEquals(100.0, geo.getCellSize());
    }

    @Test
    public void widePanelLetterboxesHorizontally() {
        BoardGeometry geo = new BoardGeometry(8, 8);
        geo.resize(900, 600);

        assertEquals(600, geo.getBoardSize());
        assertEquals(150, geo.getOriginX()); // (900-600)/2
        assertEquals(0, geo.getOriginY());
    }

    @Test
    public void pixelToCellMapsCorrectlyOnASquarePanel() {
        BoardGeometry geo = new BoardGeometry(8, 8);
        geo.resize(800, 800);

        assertArrayEquals(new int[]{0, 0}, geo.pixelToCell(50, 50));
        assertArrayEquals(new int[]{0, 1}, geo.pixelToCell(150, 50));
        assertArrayEquals(new int[]{1, 0}, geo.pixelToCell(50, 150));
        assertArrayEquals(new int[]{7, 7}, geo.pixelToCell(799, 799));
    }

    @Test
    public void pixelToCellReturnsNullOutsideTheBoard() {
        BoardGeometry geo = new BoardGeometry(8, 8);
        geo.resize(900, 600); // letterboxed: board occupies x in [150, 750]

        assertNull(geo.pixelToCell(50, 300));  // in the left letterbox margin
        assertNull(geo.pixelToCell(-5, 50));   // negative
        assertNull(geo.pixelToCell(800, 800)); // below the panel entirely
    }

    @Test
    public void cellTopLeftRoundTripsWithPixelToCell() {
        BoardGeometry geo = new BoardGeometry(8, 8);
        geo.resize(800, 800);

        int x = geo.cellTopLeftX(3);
        int y = geo.cellTopLeftY(5);
        assertArrayEquals(new int[]{5, 3}, geo.pixelToCell(x + 1, y + 1));
    }

    @Test
    public void marginShrinksTheCheckeredAreaAndOffsetsItsOrigin() {
        BoardGeometry geo = new BoardGeometry(8, 8, 20);
        geo.resize(800, 800);

        assertEquals(760, geo.getBoardSize()); // 800 - 2*20
        assertEquals(20, geo.getOriginX());
        assertEquals(20, geo.getOriginY());
        assertEquals(20, geo.getMarginPx());
    }

    @Test
    public void marginedGeometryStillExcludesClicksInTheMarginBand() {
        BoardGeometry geo = new BoardGeometry(8, 8, 20);
        geo.resize(800, 800);

        assertNull(geo.pixelToCell(10, 400)); // inside the left margin band
        assertArrayEquals(new int[]{4, 0}, geo.pixelToCell(21, 400)); // just past the margin
    }
}
