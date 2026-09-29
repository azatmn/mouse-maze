package maze.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Значки клеток, нарисованные линиями: капля, молния, кусок сыра. (x, y) — левый верхний угол клетки. */
final class Icons {

    private Icons() {
    }

    static void drop(GraphicsContext g, double x, double y, double size, Color color) {
        double cx = x + size / 2;
        double r = size * 0.18;
        double cy = y + size * 0.58;
        g.setFill(color);
        g.beginPath();
        g.moveTo(cx, y + size * 0.2);                       // острый кончик сверху
        g.bezierCurveTo(cx + r * 0.4, cy - r * 1.2, cx + r, cy - r * 0.6, cx + r, cy);
        g.arc(cx, cy, r, r, 0, -180);                       // круглое донышко
        g.bezierCurveTo(cx - r, cy - r * 0.6, cx - r * 0.4, cy - r * 1.2, cx, y + size * 0.2);
        g.closePath();
        g.fill();
    }

    static void bolt(GraphicsContext g, double x, double y, double size, Color color) {
        double[] px = {0.56, 0.30, 0.48, 0.40, 0.70, 0.52};
        double[] py = {0.18, 0.56, 0.56, 0.82, 0.44, 0.44};
        double[] xs = new double[px.length];
        double[] ys = new double[py.length];
        for (int i = 0; i < px.length; i++) {
            xs[i] = x + px[i] * size;
            ys[i] = y + py[i] * size;
        }
        g.setFill(color);
        g.fillPolygon(xs, ys, xs.length);
    }

    static void cheese(GraphicsContext g, double x, double y, double size, Color color) {
        // клин сыра и две дырки
        double[] xs = {x + size * 0.2, x + size * 0.8, x + size * 0.8};
        double[] ys = {y + size * 0.7, y + size * 0.3, y + size * 0.7};
        g.setFill(color);
        g.fillPolygon(xs, ys, 3);
        g.setFill(MazeCanvas.CHEESE_TILE);
        double h = size * 0.07;
        g.fillOval(x + size * 0.62 - h, y + size * 0.58 - h, 2 * h, 2 * h);
        g.fillOval(x + size * 0.70 - h * 0.7, y + size * 0.45 - h * 0.7, 1.4 * h, 1.4 * h);
    }
}
