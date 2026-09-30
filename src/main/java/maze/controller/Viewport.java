package maze.controller;

/**
 * Масштаб и сдвиг лабиринта на холсте — чистая математика без JavaFX.
 * При масштабе 1 лабиринт вписан в холст и стоит по центру; при увеличении его можно двигать,
 * но край лабиринта не уходит внутрь холста (за лабиринтом нет ничего интересного).
 */
public final class Viewport {

    public static final double MAX_ZOOM = 16;

    /** Где рисовать: левый верхний угол лабиринта и размер клетки, в пикселях холста. */
    public record Layout(double left, double top, double cell) {
    }

    private final double margin;
    private double zoom = 1;
    /** Сдвиг от положения «по центру», в пикселях. */
    private double panX;
    private double panY;

    /** margin — отступ вокруг лабиринта, чтобы толстая внешняя стена не обрезалась. */
    public Viewport(double margin) {
        this.margin = margin;
    }

    public double zoom() {
        return zoom;
    }

    /** Раскладка для холста w x h и лабиринта cols x rows; заодно поправляет сдвиг, если он вышел за края. */
    public Layout layout(double w, double h, int cols, int rows) {
        double base = Math.min((w - 2 * margin) / cols, (h - 2 * margin) / rows);
        if (!(base > 0)) {
            return new Layout(0, 0, 0);
        }
        double cell = base * zoom;
        panX = clampPan(panX, w, cols * cell);
        panY = clampPan(panY, h, rows * cell);
        return new Layout((w - cols * cell) / 2 + panX, (h - rows * cell) / 2 + panY, cell);
    }

    /** Умножить масштаб на factor так, чтобы точка (px, py) холста осталась над той же точкой лабиринта. */
    public void zoomAt(double factor, double px, double py, double w, double h, int cols, int rows) {
        if (!(factor > 0) || !Double.isFinite(factor)) {
            return;  // жест иногда присылает 0, NaN или бесконечность
        }
        Layout before = layout(w, h, cols, rows);
        if (!(before.cell() > 0)) {
            return;
        }
        double mazeX = (px - before.left()) / before.cell();
        double mazeY = (py - before.top()) / before.cell();
        zoom = Math.clamp(zoom * factor, 1, MAX_ZOOM);
        double cell = baseCell(w, h, cols, rows) * zoom;
        panX = px - mazeX * cell - (w - cols * cell) / 2;
        panY = py - mazeY * cell - (h - rows * cell) / 2;
        layout(w, h, cols, rows);  // прижать сдвиг к краям
    }

    /** Сдвинуть лабиринт на (dx, dy) пикселей. */
    public void pan(double dx, double dy, double w, double h, int cols, int rows) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) {
            return;
        }
        panX += dx;
        panY += dy;
        layout(w, h, cols, rows);
    }

    /** Снова вписать лабиринт целиком. */
    public void reset() {
        zoom = 1;
        panX = 0;
        panY = 0;
    }

    private double baseCell(double w, double h, int cols, int rows) {
        return Math.min((w - 2 * margin) / cols, (h - 2 * margin) / rows);
    }

    /** Лабиринт помещается — по центру; не помещается — край не отходит от края холста дальше отступа. */
    private double clampPan(double pan, double size, double content) {
        if (!Double.isFinite(pan) || content <= size - 2 * margin) {
            return 0;
        }
        double limit = (content - size) / 2 + margin;
        return Math.clamp(pan, -limit, limit);
    }
}
