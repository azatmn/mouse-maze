package maze.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Лабиринт: прямоугольник клеток, тонкие стены между ними, предметы (вода, ток), старт и сыр.
 * Граница лабиринта — всегда стена. Лабиринт меняется только редактором и генератором;
 * мышь его не меняет (выпитую воду помнит попытка, а не лабиринт).
 */
public final class Maze {

    public static final int MAX_SIZE = 100;

    private final int width;
    private final int height;
    /** eastWall[y][x] — стена между (x, y) и (x + 1, y). */
    private final boolean[][] eastWall;
    /** southWall[y][x] — стена между (x, y) и (x, y + 1). */
    private final boolean[][] southWall;
    private final CellType[][] items;
    private Position start;
    private Position cheese;

    /** Лабиринт без внутренних стен; старт — левый нижний угол, сыр — правый верхний. */
    public Maze(int width, int height) {
        checkSize(width, height);
        this.width = width;
        this.height = height;
        this.eastWall = new boolean[height][width];
        this.southWall = new boolean[height][width];
        this.items = new CellType[height][width];
        for (CellType[] row : items) {
            Arrays.fill(row, CellType.EMPTY);
        }
        this.start = new Position(0, height - 1);
        this.cheese = new Position(width - 1, 0);
    }

    static void checkSize(int width, int height) {
        if (width < 1 || height < 1 || width > MAX_SIZE || height > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "размер должен быть от 1 до " + MAX_SIZE + " по каждой стороне, а не " + width + "x" + height);
        }
        if (width * height < 2) {
            throw new IllegalArgumentException("нужно хотя бы 2 клетки: для старта и для сыра");
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(Position p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
    }

    public boolean hasWall(Position p, Direction d) {
        requireInside(p);
        Position n = p.step(d);
        if (!contains(n)) {
            return true;
        }
        return switch (d) {
            case RIGHT -> eastWall[p.y()][p.x()];
            case LEFT -> eastWall[n.y()][n.x()];
            case DOWN -> southWall[p.y()][p.x()];
            case UP -> southWall[n.y()][n.x()];
        };
    }

    /** Ставит или убирает стену между p и соседом в направлении d. Границу менять нельзя. */
    public void setWall(Position p, Direction d, boolean wall) {
        requireInside(p);
        Position n = p.step(d);
        if (!contains(n)) {
            throw new IllegalArgumentException("граница лабиринта всегда стена: " + p + " " + d);
        }
        switch (d) {
            case RIGHT -> eastWall[p.y()][p.x()] = wall;
            case LEFT -> eastWall[n.y()][n.x()] = wall;
            case DOWN -> southWall[p.y()][p.x()] = wall;
            case UP -> southWall[n.y()][n.x()] = wall;
        }
    }

    /** Ставит все внутренние стены. */
    public void closeAll() {
        for (int y = 0; y < height; y++) {
            Arrays.fill(eastWall[y], true);
            Arrays.fill(southWall[y], true);
        }
    }

    public CellType cellAt(Position p) {
        requireInside(p);
        return p.equals(cheese) ? CellType.CHEESE : items[p.y()][p.x()];
    }

    /** Кладёт в клетку воду, ток или убирает предмет (EMPTY). Старт и сыр заняты. */
    public void setItem(Position p, CellType type) {
        Objects.requireNonNull(type, "type");
        requireInside(p);
        if (type == CellType.CHEESE) {
            throw new IllegalArgumentException("сыр переставляется через setCheese");
        }
        if (p.equals(start) || p.equals(cheese)) {
            throw new IllegalArgumentException("на старт и сыр ничего класть нельзя: " + p);
        }
        items[p.y()][p.x()] = type;
    }

    public Position start() {
        return start;
    }

    public Position cheese() {
        return cheese;
    }

    /** Переносит старт; предмет в новой клетке пропадает. */
    public void setStart(Position p) {
        Objects.requireNonNull(p, "start");
        requireInside(p);
        if (p.equals(cheese)) {
            throw new IllegalArgumentException("старт не может совпадать с сыром: " + p);
        }
        items[p.y()][p.x()] = CellType.EMPTY;
        start = p;
    }

    /** Переносит сыр; предмет в новой клетке пропадает. */
    public void setCheese(Position p) {
        Objects.requireNonNull(p, "cheese");
        requireInside(p);
        if (p.equals(start)) {
            throw new IllegalArgumentException("сыр не может совпадать со стартом: " + p);
        }
        items[p.y()][p.x()] = CellType.EMPTY;
        cheese = p;
    }

    public boolean canMove(Position p, Direction d) {
        return !hasWall(p, d);
    }

    /** Куда попадёт мышь, шагнув из p в направлении d: соседняя клетка или та же, если там стена. */
    public Position next(Position p, Direction d) {
        return canMove(p, d) ? p.step(d) : p;
    }

    /**
     * Кратчайший по числу шагов путь от старта до сыра (поиск в ширину по карте),
     * включая обе клетки. Пустой список — сыр недостижим. Предметы путь не загораживают.
     */
    public List<Position> shortestPath() {
        Position[][] from = new Position[height][width];
        ArrayDeque<Position> queue = new ArrayDeque<>();
        from[start.y()][start.x()] = start;
        queue.add(start);
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            if (p.equals(cheese)) {
                break;
            }
            for (Direction d : Direction.values()) {
                if (canMove(p, d)) {
                    Position n = p.step(d);
                    if (from[n.y()][n.x()] == null) {
                        from[n.y()][n.x()] = p;
                        queue.add(n);
                    }
                }
            }
        }
        if (from[cheese.y()][cheese.x()] == null) {
            return List.of();
        }
        List<Position> path = new ArrayList<>();
        for (Position p = cheese; !p.equals(start); p = from[p.y()][p.x()]) {
            path.add(p);
        }
        path.add(start);
        Collections.reverse(path);
        return path;
    }

    public boolean isCheeseReachable() {
        return !shortestPath().isEmpty();
    }

    public int count(CellType type) {
        int n = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (cellAt(new Position(x, y)) == type) {
                    n++;
                }
            }
        }
        return n;
    }

    public Maze copy() {
        Maze c = new Maze(width, height);
        for (int y = 0; y < height; y++) {
            c.eastWall[y] = eastWall[y].clone();
            c.southWall[y] = southWall[y].clone();
            c.items[y] = items[y].clone();
        }
        c.start = start;
        c.cheese = cheese;
        return c;
    }

    private void requireInside(Position p) {
        Objects.requireNonNull(p, "position");
        if (!contains(p)) {
            throw new IllegalArgumentException("клетка " + p + " вне лабиринта " + width + "x" + height);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Maze m
                && width == m.width && height == m.height
                && start.equals(m.start) && cheese.equals(m.cheese)
                && Arrays.deepEquals(eastWall, m.eastWall)
                && Arrays.deepEquals(southWall, m.southWall)
                && Arrays.deepEquals(items, m.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, start, cheese,
                Arrays.deepHashCode(eastWall), Arrays.deepHashCode(southWall), Arrays.deepHashCode(items));
    }
}
