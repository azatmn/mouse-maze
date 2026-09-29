package maze.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MazeGeneratorTest {

    /** Сколько проходов (стенок, которых нет) внутри лабиринта. */
    private static int passages(Maze m) {
        int n = 0;
        for (int y = 0; y < m.height(); y++) {
            for (int x = 0; x < m.width(); x++) {
                Position p = new Position(x, y);
                if (x < m.width() - 1 && !m.hasWall(p, Direction.RIGHT)) n++;
                if (y < m.height() - 1 && !m.hasWall(p, Direction.DOWN)) n++;
            }
        }
        return n;
    }

    private static int innerWalls(int w, int h) {
        return (w - 1) * h + w * (h - 1);
    }

    /** Сколько клеток достижимо от старта. */
    private static int reachable(Maze m) {
        Set<Position> seen = new HashSet<>();
        ArrayDeque<Position> queue = new ArrayDeque<>();
        queue.add(m.start());
        seen.add(m.start());
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            for (Direction d : Direction.values()) {
                if (m.canMove(p, d) && seen.add(p.step(d))) {
                    queue.add(p.step(d));
                }
            }
        }
        return seen.size();
    }

    @Test
    void sizeStartAndCheeseFromSpec() {
        Maze m = MazeGenerator.generate(new MazeSpec(12, 9, 0, 0, 0, 1L));
        assertEquals(12, m.width());
        assertEquals(9, m.height());
        assertEquals(new Position(0, 8), m.start());
        assertEquals(new Position(11, 0), m.cheese());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 42, 777, -5})
    void withoutLoopsEveryCellReachableByExactlyOnePath(long seed) {
        // дерево: все клетки связаны, а проходов на один меньше, чем клеток — значит, циклов нет
        Maze m = MazeGenerator.generate(new MazeSpec(15, 11, 0, 0, 0, seed));
        assertEquals(15 * 11, reachable(m));
        assertEquals(15 * 11 - 1, passages(m));
    }

    @Test
    void fullLoopsOpenEverything() {
        Maze m = MazeGenerator.generate(new MazeSpec(6, 5, 0, 0, 100, 3L));
        assertEquals(innerWalls(6, 5), passages(m));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 25, 50})
    void loopPercentRemovesThatShareOfRemainingWalls(int percent) {
        int w = 10, h = 8;
        Maze m = MazeGenerator.generate(new MazeSpec(w, h, 0, 0, percent, 9L));
        int tree = w * h - 1;
        int remaining = innerWalls(w, h) - tree;
        assertEquals(tree + Math.round(remaining * percent / 100.0f), passages(m));
        assertEquals(w * h, reachable(m));
    }

    @Test
    void itemCountsExactAndNotOnStartOrCheese() {
        for (long seed = 0; seed < 30; seed++) {
            Maze m = MazeGenerator.generate(new MazeSpec(7, 6, 5, 8, 10, seed));
            assertEquals(5, m.count(CellType.WATER), "seed " + seed);
            assertEquals(8, m.count(CellType.SHOCK), "seed " + seed);
            assertEquals(CellType.EMPTY, m.cellAt(m.start()));
            assertEquals(CellType.CHEESE, m.cellAt(m.cheese()));
        }
    }

    @Test
    void everyFreeCellCanBeFilled() {
        Maze m = MazeGenerator.generate(new MazeSpec(3, 3, 4, 3, 0, 5L));
        assertEquals(0, m.count(CellType.EMPTY) - 1, "пустой остался только старт");
    }

    @Test
    void sameSeedSameMaze() {
        MazeSpec s = new MazeSpec(20, 15, 6, 6, 15, 123L);
        assertEquals(MazeGenerator.generate(s), MazeGenerator.generate(s));
    }

    @Test
    void differentSeedsDifferentMazes() {
        Set<Maze> mazes = new HashSet<>();
        for (long seed = 0; seed < 10; seed++) {
            mazes.add(MazeGenerator.generate(new MazeSpec(10, 10, 3, 3, 10, seed)));
        }
        assertEquals(10, mazes.size());
    }

    @Test
    void tinyMazes() {
        Maze m = MazeGenerator.generate(new MazeSpec(1, 2, 0, 0, 0, 1L));
        assertTrue(m.isCheeseReachable());
        Maze c = MazeGenerator.generate(new MazeSpec(2, 1, 0, 0, 100, 1L));
        assertTrue(c.isCheeseReachable());
    }

    @Test
    void largestMazeIsFastAndConnected() {
        long t = System.nanoTime();
        Maze m = MazeGenerator.generate(new MazeSpec(100, 100, 200, 200, 10, 8L));
        assertTrue(System.nanoTime() - t < 2_000_000_000L, "генерация 100x100 дольше 2 с");
        assertEquals(100 * 100, reachable(m));
    }

    @Test
    void wallsNotBiasedToOneDirection() {
        // идеальный лабиринт из «змейки» тоже дерево; проверяем, что есть и горизонтальные, и вертикальные проходы примерно поровну
        Maze m = MazeGenerator.generate(new MazeSpec(30, 30, 0, 0, 0, 4L));
        int right = 0, down = 0;
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 30; x++) {
                Position p = new Position(x, y);
                if (x < 29 && !m.hasWall(p, Direction.RIGHT)) right++;
                if (y < 29 && !m.hasWall(p, Direction.DOWN)) down++;
            }
        }
        assertTrue(right > 300 && down > 300, "right=" + right + " down=" + down);
    }
}
