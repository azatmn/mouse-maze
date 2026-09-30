package maze.controller;

import maze.controller.Viewport.Layout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ViewportTest {

    /** Холст 400x300 без полей, лабиринт 10x10: вписанная клетка 30 px. */
    private static final double W = 400, H = 300;
    private static final int COLS = 10, ROWS = 10;
    private Viewport v;

    @BeforeEach
    void setUp() {
        v = new Viewport(0);
    }

    private Layout layout() {
        return v.layout(W, H, COLS, ROWS);
    }

    @Test
    void fittedAndCenteredAtStart() {
        Layout l = layout();
        assertEquals(30, l.cell(), 1e-9);
        assertEquals(50, l.left(), 1e-9);   // (400 − 300) / 2
        assertEquals(0, l.top(), 1e-9);
        assertEquals(1, v.zoom());
    }

    @Test
    void zoomKeepsPointUnderCursor() {
        // точка (200, 150) — середина лабиринта (5, 5) в клетках
        v.zoomAt(2, 200, 150, W, H, COLS, ROWS);
        Layout l = layout();
        assertEquals(60, l.cell(), 1e-9);
        assertEquals(5, (200 - l.left()) / l.cell(), 1e-9);
        assertEquals(5, (150 - l.top()) / l.cell(), 1e-9);
    }

    @Test
    void zoomAroundCornerPoint() {
        v.zoomAt(3, 80, 60, W, H, COLS, ROWS);   // клетка (1, 2) под курсором
        Layout l = layout();
        assertEquals(1, (80 - l.left()) / l.cell(), 1e-9);
        assertEquals(2, (60 - l.top()) / l.cell(), 1e-9);
    }

    @Test
    void zoomLimits() {
        v.zoomAt(0.1, 200, 150, W, H, COLS, ROWS);
        assertEquals(1, v.zoom(), "меньше «вписать» не уменьшаем");
        v.zoomAt(1000, 200, 150, W, H, COLS, ROWS);
        assertEquals(Viewport.MAX_ZOOM, v.zoom());
        v.zoomAt(Double.NaN, 200, 150, W, H, COLS, ROWS);
        assertEquals(Viewport.MAX_ZOOM, v.zoom(), "мусор от жеста не ломает масштаб");
        v.zoomAt(0, 200, 150, W, H, COLS, ROWS);
        assertEquals(Viewport.MAX_ZOOM, v.zoom());
    }

    @Test
    void panMovesZoomedMaze() {
        v.zoomAt(2, 200, 150, W, H, COLS, ROWS);
        double before = layout().left();
        v.pan(-40, 0, W, H, COLS, ROWS);
        assertEquals(before - 40, layout().left(), 1e-9);
    }

    @Test
    void panStopsAtEdges() {
        v.zoomAt(4, 200, 150, W, H, COLS, ROWS);   // лабиринт 1200x1200
        v.pan(10_000, 10_000, W, H, COLS, ROWS);
        Layout l = layout();
        assertEquals(0, l.left(), 1e-9, "левый край лабиринта не уходит правее края холста");
        assertEquals(0, l.top(), 1e-9);
        v.pan(-100_000, -100_000, W, H, COLS, ROWS);
        l = layout();
        assertEquals(W, l.left() + COLS * l.cell(), 1e-9);
        assertEquals(H, l.top() + ROWS * l.cell(), 1e-9);
    }

    @Test
    void smallMazeStaysCentered() {
        // при масштабе 1 лабиринт помещается целиком: двигать некуда
        v.pan(123, -45, W, H, COLS, ROWS);
        assertEquals(50, layout().left(), 1e-9);
        assertEquals(0, layout().top(), 1e-9);
    }

    @Test
    void resetFitsAgain() {
        v.zoomAt(3, 10, 10, W, H, COLS, ROWS);
        v.pan(-50, -50, W, H, COLS, ROWS);
        v.reset();
        assertEquals(1, v.zoom());
        assertEquals(50, layout().left(), 1e-9);
    }

    @Test
    void marginAroundMaze() {
        Viewport m = new Viewport(6);
        Layout l = m.layout(412, 312, COLS, ROWS);
        assertEquals(30, l.cell(), 1e-9);
        assertEquals(6, l.top(), 1e-9);
    }

    @Test
    void tinyOrBrokenCanvasDoesNotBreak() {
        Layout l = v.layout(0, 0, COLS, ROWS);
        assertFalse(l.cell() > 0, "рисовать нечего");
        v.zoomAt(2, 1, 1, 0, 0, COLS, ROWS);
        v.pan(5, 5, Double.NaN, 1, COLS, ROWS);
        assertTrue(Double.isFinite(v.zoom()));
    }

    @Test
    void zoomNeverLeavesPanOutsideLimits() {
        v.zoomAt(4, 0, 0, W, H, COLS, ROWS);
        v.zoomAt(0.25, 400, 300, W, H, COLS, ROWS);   // обратно к вписанному — снова по центру
        assertEquals(50, layout().left(), 1e-9);
        assertEquals(0, layout().top(), 1e-9);
    }
}
