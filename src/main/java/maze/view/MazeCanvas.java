package maze.view;

import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import maze.controller.Viewport;
import maze.model.Attempt;
import maze.model.CellType;
import maze.model.Direction;
import maze.model.GreedyPath;
import maze.model.Maze;
import maze.model.Position;
import maze.model.QTable;

import java.util.List;

/**
 * Лабиринт на экране: клетки с водой, током и сыром, тонкие стены, мышь,
 * по желанию — выученный путь и стрелки лучших направлений.
 * Растягивается под размер окна, клетки остаются квадратными.
 * <p>
 * Масштаб и сдвиг — как в картах на Mac: щипок двумя пальцами приближает, движение двумя пальцами
 * (или колесо) двигает, Cmd + колесо приближает, перетаскивание мышью двигает (кроме режима редактора).
 */
public class MazeCanvas extends Pane {

    static final Color BACKGROUND = Color.web("#ffffff");
    static final Color FLOOR = Color.web("#f1efe8");
    static final Color WALL = Color.web("#444441");
    static final Color WATER_TILE = Color.web("#e6f1fb");
    static final Color WATER = Color.web("#185fa5");
    static final Color SHOCK_TILE = Color.web("#fcebeb");
    static final Color SHOCK = Color.web("#a32d2d");
    static final Color CHEESE_TILE = Color.web("#faeeda");
    static final Color CHEESE = Color.web("#ba7517");
    static final Color PATH = Color.web("#1d9e75");
    static final Color MOUSE = Color.web("#185fa5");
    static final Color ARROW = Color.web("#b4b2a9");
    static final Color START = Color.web("#888780");

    /** Отступ вокруг лабиринта, чтобы толстая внешняя стена не обрезалась. */
    private static final double MARGIN = 6;

    private final Canvas canvas = new Canvas();
    private final Viewport viewport = new Viewport(MARGIN);
    /** Можно ли двигать лабиринт перетаскиванием (в редакторе мышь ставит стены). */
    private boolean dragPan = true;
    private double dragX;
    private double dragY;
    private Runnable onViewChanged = () -> { };
    private Maze maze;
    private Attempt attempt;
    private QTable table;
    private GreedyPath path;
    private boolean arrows;

    /** Где сейчас нарисован лабиринт: левый верхний угол и размер клетки. */
    private double left;
    private double top;
    private double cell;

    public MazeCanvas() {
        getChildren().add(canvas);
        getStyleClass().add("maze-canvas");

        setOnZoom(e -> {
            zoomAt(e.getZoomFactor(), e.getX(), e.getY());
            e.consume();
        });
        setOnScroll(e -> {
            if (e.isShortcutDown() || e.isControlDown()) {
                zoomAt(Math.exp(e.getDeltaY() * 0.01), e.getX(), e.getY());  // Cmd + колесо
            } else {
                panBy(e.getDeltaX(), e.getDeltaY());                            // два пальца / колесо
            }
            e.consume();
        });
        setOnMousePressed(e -> {
            dragX = e.getX();
            dragY = e.getY();
        });
        setOnMouseDragged(e -> {
            if (dragPan) {
                setCursor(Cursor.CLOSED_HAND);
                panBy(e.getX() - dragX, e.getY() - dragY);
                dragX = e.getX();
                dragY = e.getY();
            }
        });
        setOnMouseReleased(e -> setCursor(Cursor.DEFAULT));
    }

    /** Двигать ли лабиринт перетаскиванием мыши (в редакторе — нет, там мышь рисует). */
    public void setDragPan(boolean dragPan) {
        this.dragPan = dragPan;
    }

    /** Вызывается после изменения масштаба или сдвига (окно обновляет надпись с масштабом). */
    public void setOnViewChanged(Runnable onViewChanged) {
        this.onViewChanged = onViewChanged;
    }

    /** Масштаб: 1 — лабиринт вписан целиком. */
    public double zoom() {
        return viewport.zoom();
    }

    /** Кнопка «Вписать»: снова весь лабиринт целиком. */
    public void resetView() {
        viewport.reset();
        redraw();
        onViewChanged.run();
    }

    private void zoomAt(double factor, double x, double y) {
        if (maze != null) {
            viewport.zoomAt(factor, x, y, canvas.getWidth(), canvas.getHeight(), maze.width(), maze.height());
            redraw();
            onViewChanged.run();
        }
    }

    private void panBy(double dx, double dy) {
        if (maze != null) {
            viewport.pan(dx, dy, canvas.getWidth(), canvas.getHeight(), maze.width(), maze.height());
            redraw();
        }
    }

    /**
     * Нарисовать состояние (запоминается — при изменении размера окна перерисуется само).
     *
     * @param path   выученный путь или null, если его не показывать
     * @param arrows рисовать ли стрелки лучших направлений
     */
    public void draw(Maze maze, Attempt attempt, QTable table, GreedyPath path, boolean arrows) {
        if (this.maze == null || this.maze.width() != maze.width() || this.maze.height() != maze.height()) {
            viewport.reset();  // новый размер — снова вписать целиком
            onViewChanged.run();
        }
        this.maze = maze;
        this.attempt = attempt;
        this.table = table;
        this.path = path;
        this.arrows = arrows;
        redraw();
    }

    /** Координата холста → координата внутри лабиринта (для редактора): x. */
    public double toMazeX(double canvasX) {
        return canvasX - left;
    }

    public double toMazeY(double canvasY) {
        return canvasY - top;
    }

    public double cellSize() {
        return cell;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    // Размер холста не должен влиять на желаемый размер панели — иначе окно нельзя уменьшить.
    @Override
    protected double computePrefWidth(double height) {
        return 720;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 540;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 120;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 90;
    }

    private void redraw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, width, height);
        if (maze == null) {
            return;
        }
        Viewport.Layout layout = viewport.layout(width, height, maze.width(), maze.height());
        if (!(layout.cell() > 0)) {
            return;
        }
        cell = layout.cell();
        left = Math.floor(layout.left());  // целые пиксели — стены чёткие, а не размытые
        top = Math.floor(layout.top());

        g.setFill(FLOOR);
        g.fillRect(left, top, cell * maze.width(), cell * maze.height());
        drawCells(g);
        if (arrows && table != null && cell >= 14) {
            drawArrows(g);
        }
        if (path != null) {
            drawPath(g, path.cells());
        }
        drawWalls(g);
        if (attempt != null) {
            drawMouse(g, attempt.position());
        }
    }

    private void drawCells(GraphicsContext g) {
        double inset = cell >= 8 ? Math.max(1, cell * 0.06) : 0;
        for (int y = 0; y < maze.height(); y++) {
            for (int x = 0; x < maze.width(); x++) {
                Position p = new Position(x, y);
                CellType type = maze.cellAt(p);
                if (type == CellType.EMPTY) {
                    continue;
                }
                double px = left + x * cell;
                double py = top + y * cell;
                boolean faded = type == CellType.WATER && attempt != null && !attempt.hasWater(p);
                g.setGlobalAlpha(faded ? 0.35 : 1);
                g.setFill(switch (type) {
                    case WATER -> WATER_TILE;
                    case SHOCK -> SHOCK_TILE;
                    default -> CHEESE_TILE;
                });
                g.fillRect(px + inset, py + inset, cell - 2 * inset, cell - 2 * inset);
                if (cell >= 10) {
                    switch (type) {
                        case WATER -> Icons.drop(g, px, py, cell, WATER);
                        case SHOCK -> Icons.bolt(g, px, py, cell, SHOCK);
                        default -> Icons.cheese(g, px, py, cell, CHEESE);
                    }
                } else {
                    // мелкие клетки: значок не разглядеть, остаётся насыщенный цвет
                    g.setFill(switch (type) {
                        case WATER -> WATER;
                        case SHOCK -> SHOCK;
                        default -> CHEESE;
                    });
                    g.fillRect(px, py, cell, cell);
                }
                g.setGlobalAlpha(1);
            }
        }
        // старт — пунктирное кольцо, чтобы было видно, откуда начинается каждая попытка
        Position s = maze.start();
        if (cell >= 8) {
            g.setStroke(START);
            g.setLineWidth(Math.max(1, cell * 0.05));
            g.setLineDashes(Math.max(2, cell * 0.12));
            double r = cell * 0.36;
            g.strokeOval(left + s.x() * cell + cell / 2 - r, top + s.y() * cell + cell / 2 - r, 2 * r, 2 * r);
            g.setLineDashes();
        }
    }

    private void drawArrows(GraphicsContext g) {
        g.setStroke(ARROW);
        g.setLineWidth(Math.max(1, cell * 0.06));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int y = 0; y < maze.height(); y++) {
            for (int x = 0; x < maze.width(); x++) {
                Position p = new Position(x, y);
                if (p.equals(maze.cheese()) || table.bestAll(p).size() == Direction.values().length) {
                    continue; // у сыра шагать некуда; все числа равны — мышь тут ещё ничего не знает
                }
                Direction d = table.best(p);
                double cx = left + x * cell + cell / 2;
                double cy = top + y * cell + cell / 2;
                double len = cell * 0.28;
                double ex = cx + d.dx() * len;
                double ey = cy + d.dy() * len;
                g.strokeLine(cx - d.dx() * len * 0.6, cy - d.dy() * len * 0.6, ex, ey);
                double head = cell * 0.12;
                // два уса наконечника: повернуть направление назад и вбок
                g.strokeLine(ex, ey, ex - d.dx() * head - d.dy() * head, ey - d.dy() * head + d.dx() * head);
                g.strokeLine(ex, ey, ex - d.dx() * head + d.dy() * head, ey - d.dy() * head - d.dx() * head);
            }
        }
    }

    private void drawPath(GraphicsContext g, List<Position> cells) {
        if (cells.size() < 2) {
            return;
        }
        g.setStroke(PATH);
        g.setLineWidth(Math.max(2, cell * 0.12));
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        g.beginPath();
        for (int i = 0; i < cells.size(); i++) {
            Position p = cells.get(i);
            double px = left + p.x() * cell + cell / 2;
            double py = top + p.y() * cell + cell / 2;
            if (i == 0) {
                g.moveTo(px, py);
            } else {
                g.lineTo(px, py);
            }
        }
        g.stroke();
    }

    private void drawWalls(GraphicsContext g) {
        g.setStroke(WALL);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(Math.max(1, Math.min(3, cell * 0.08)));
        g.beginPath();
        for (int y = 0; y < maze.height(); y++) {
            for (int x = 0; x < maze.width(); x++) {
                Position p = new Position(x, y);
                double px = left + x * cell;
                double py = top + y * cell;
                if (x < maze.width() - 1 && maze.hasWall(p, Direction.RIGHT)) {
                    g.moveTo(px + cell, py);
                    g.lineTo(px + cell, py + cell);
                }
                if (y < maze.height() - 1 && maze.hasWall(p, Direction.DOWN)) {
                    g.moveTo(px, py + cell);
                    g.lineTo(px + cell, py + cell);
                }
            }
        }
        g.stroke();
        g.setLineWidth(Math.max(2, Math.min(4, cell * 0.12)));
        g.strokeRect(left, top, cell * maze.width(), cell * maze.height());
    }

    private void drawMouse(GraphicsContext g, Position p) {
        double r = Math.max(2, cell * 0.28);
        double cx = left + p.x() * cell + cell / 2;
        double cy = top + p.y() * cell + cell / 2;
        g.setFill(MOUSE);
        if (cell >= 14) {
            // уши
            double ear = r * 0.45;
            g.fillOval(cx - r * 0.85 - ear, cy - r * 0.85 - ear, 2 * ear, 2 * ear);
            g.fillOval(cx + r * 0.85 - ear, cy - r * 0.85 - ear, 2 * ear, 2 * ear);
        }
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }
}
