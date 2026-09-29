package maze.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Строит случайный лабиринт по {@link MazeSpec}.
 * <ol>
 *   <li>Все стены на месте; обход в глубину со случайным выбором соседа пробивает проходы —
 *       получается «идеальный» лабиринт: к любой клетке ровно один путь.</li>
 *   <li>Убирается {@code loopPercent}% оставшихся стен — появляются обходные пути,
 *       и мыши есть из чего выбирать.</li>
 *   <li>Вода и ток ставятся в случайные клетки, кроме старта и сыра.</li>
 * </ol>
 */
public final class MazeGenerator {

    private MazeGenerator() {
    }

    public static Maze generate(MazeSpec spec) {
        Random random = new Random(spec.seed());
        Maze maze = new Maze(spec.width(), spec.height());
        maze.closeAll();
        carve(maze, random);
        openLoops(maze, spec.loopPercent(), random);
        placeItems(maze, spec, random);
        return maze;
    }

    /** Обход в глубину без рекурсии (стек), чтобы 100x100 не переполнил стек вызовов. */
    private static void carve(Maze maze, Random random) {
        boolean[][] visited = new boolean[maze.height()][maze.width()];
        List<Position> stack = new ArrayList<>();
        Position first = new Position(random.nextInt(maze.width()), random.nextInt(maze.height()));
        visited[first.y()][first.x()] = true;
        stack.add(first);
        while (!stack.isEmpty()) {
            Position current = stack.getLast();
            List<Direction> options = new ArrayList<>(4);
            for (Direction d : Direction.values()) {
                Position n = current.step(d);
                if (maze.contains(n) && !visited[n.y()][n.x()]) {
                    options.add(d);
                }
            }
            if (options.isEmpty()) {
                stack.removeLast();
                continue;
            }
            Direction d = options.get(random.nextInt(options.size()));
            Position n = current.step(d);
            maze.setWall(current, d, false);
            visited[n.y()][n.x()] = true;
            stack.add(n);
        }
    }

    /** Внутренняя стена: справа или снизу от клетки. */
    private record Wall(Position cell, Direction side) {
    }

    private static void openLoops(Maze maze, int percent, Random random) {
        List<Wall> walls = new ArrayList<>();
        for (int y = 0; y < maze.height(); y++) {
            for (int x = 0; x < maze.width(); x++) {
                Position p = new Position(x, y);
                if (x < maze.width() - 1 && maze.hasWall(p, Direction.RIGHT)) {
                    walls.add(new Wall(p, Direction.RIGHT));
                }
                if (y < maze.height() - 1 && maze.hasWall(p, Direction.DOWN)) {
                    walls.add(new Wall(p, Direction.DOWN));
                }
            }
        }
        Collections.shuffle(walls, random);
        int toOpen = Math.round(walls.size() * percent / 100.0f);
        for (Wall w : walls.subList(0, toOpen)) {
            maze.setWall(w.cell(), w.side(), false);
        }
    }

    private static void placeItems(Maze maze, MazeSpec spec, Random random) {
        List<Position> free = new ArrayList<>();
        for (int y = 0; y < maze.height(); y++) {
            for (int x = 0; x < maze.width(); x++) {
                Position p = new Position(x, y);
                if (!p.equals(maze.start()) && !p.equals(maze.cheese())) {
                    free.add(p);
                }
            }
        }
        Collections.shuffle(free, random);
        for (int i = 0; i < spec.water(); i++) {
            maze.setItem(free.get(i), CellType.WATER);
        }
        for (int i = 0; i < spec.shock(); i++) {
            maze.setItem(free.get(spec.water() + i), CellType.SHOCK);
        }
    }
}
