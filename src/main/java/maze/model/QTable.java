package maze.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Q-таблица — «числа на стрелках»: для каждой клетки и каждого из 4 направлений
 * оценка, сколько очков в сумме мышь получит, если шагнёт туда. Вначале все нули.
 */
public final class QTable {

    private final int width;
    private final int height;
    /** values[y][x][направление]. */
    private final double[][][] values;

    public QTable(int width, int height) {
        Maze.checkSize(width, height);
        this.width = width;
        this.height = height;
        this.values = new double[height][width][Direction.values().length];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double get(Position p, Direction d) {
        return cell(p)[d.ordinal()];
    }

    public void set(Position p, Direction d, double value) {
        cell(p)[d.ordinal()] = value;
    }

    /** Самое большое число среди 4 стрелок клетки. */
    public double max(Position p) {
        double[] q = cell(p);
        double m = q[0];
        for (int i = 1; i < q.length; i++) {
            m = Math.max(m, q[i]);
        }
        return m;
    }

    /** Направление с самым большим числом; при равенстве — первое по часовой стрелке от «вверх». */
    public Direction best(Position p) {
        return bestAll(p).getFirst();
    }

    /** Все направления с самым большим числом. */
    public List<Direction> bestAll(Position p) {
        double m = max(p);
        List<Direction> best = new ArrayList<>(4);
        for (Direction d : Direction.values()) {
            if (get(p, d) == m) {
                best.add(d);
            }
        }
        return best;
    }

    /**
     * Путь мыши без случайных шагов: от старта каждый раз по лучшей стрелке.
     * Обрывается, если мышь вернулась в уже пройденную клетку (дальше она ходила бы по кругу).
     */
    public GreedyPath greedyPath(Maze maze) {
        if (maze.width() != width || maze.height() != height) {
            throw new IllegalArgumentException("таблица " + width + "x" + height
                    + " не подходит к лабиринту " + maze.width() + "x" + maze.height());
        }
        List<Position> cells = new ArrayList<>();
        Set<Position> seen = new HashSet<>();
        Position p = maze.start();
        while (seen.add(p)) {
            cells.add(p);
            if (p.equals(maze.cheese())) {
                return new GreedyPath(cells, true);
            }
            p = maze.next(p, best(p));
        }
        return new GreedyPath(cells, false);
    }

    private double[] cell(Position p) {
        if (p.x() < 0 || p.y() < 0 || p.x() >= width || p.y() >= height) {
            throw new IllegalArgumentException("клетка " + p + " вне таблицы " + width + "x" + height);
        }
        return values[p.y()][p.x()];
    }
}
