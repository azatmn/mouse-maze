package maze.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static maze.model.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

class QTableTest {

    private static Position p(int x, int y) {
        return new Position(x, y);
    }

    @Test
    void startsWithZeros() {
        QTable q = new QTable(3, 2);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                for (Direction d : Direction.values()) {
                    assertEquals(0, q.get(p(x, y), d));
                }
            }
        }
        assertEquals(3, q.width());
        assertEquals(2, q.height());
    }

    @Test
    void setChangesOnlyOneNumber() {
        QTable q = new QTable(3, 2);
        q.set(p(1, 1), LEFT, 7.5);
        assertEquals(7.5, q.get(p(1, 1), LEFT));
        assertEquals(0, q.get(p(1, 1), RIGHT));
        assertEquals(0, q.get(p(0, 1), LEFT));
        assertEquals(0, q.get(p(1, 0), LEFT));
    }

    @Test
    void maxAndBest() {
        QTable q = new QTable(2, 2);
        q.set(p(0, 0), UP, -3);
        q.set(p(0, 0), RIGHT, 5);
        q.set(p(0, 0), DOWN, 2);
        q.set(p(0, 0), LEFT, -1);
        assertEquals(5, q.max(p(0, 0)));
        assertEquals(RIGHT, q.best(p(0, 0)));
        assertEquals(List.of(RIGHT), q.bestAll(p(0, 0)));
    }

    @Test
    void maxOfNegatives() {
        QTable q = new QTable(1, 2);
        for (Direction d : Direction.values()) {
            q.set(p(0, 0), d, -10 - d.ordinal());
        }
        assertEquals(-10, q.max(p(0, 0)));
        assertEquals(UP, q.best(p(0, 0)));
    }

    @Test
    void tiesListedAndFirstWins() {
        QTable q = new QTable(2, 2);
        q.set(p(1, 1), UP, -1);
        q.set(p(1, 1), RIGHT, 4);
        q.set(p(1, 1), LEFT, 4);
        assertEquals(List.of(RIGHT, LEFT), q.bestAll(p(1, 1)));
        assertEquals(RIGHT, q.best(p(1, 1)));
        assertEquals(List.of(UP, RIGHT, DOWN, LEFT), new QTable(2, 2).bestAll(p(0, 0)));
    }

    @Test
    void outsideRejected() {
        QTable q = new QTable(2, 2);
        assertThrows(IllegalArgumentException.class, () -> q.get(p(2, 0), UP));
        assertThrows(IllegalArgumentException.class, () -> q.set(p(0, -1), UP, 1));
        assertThrows(IllegalArgumentException.class, () -> q.max(p(0, 2)));
    }

    @Test
    void greedyPathFollowsBestArrows() {
        // 3x2, старт (0,1), сыр (2,0): вправо, вправо, вверх
        Maze m = new Maze(3, 2);
        QTable q = new QTable(3, 2);
        q.set(p(0, 1), RIGHT, 1);
        q.set(p(1, 1), RIGHT, 1);
        q.set(p(2, 1), UP, 1);
        GreedyPath path = q.greedyPath(m);
        assertTrue(path.reachesCheese());
        assertEquals(List.of(p(0, 1), p(1, 1), p(2, 1), p(2, 0)), path.cells());
    }

    @Test
    void greedyPathStopsOnLoop() {
        Maze m = new Maze(3, 2);
        QTable q = new QTable(3, 2);
        q.set(p(0, 1), RIGHT, 1);
        q.set(p(1, 1), LEFT, 1);
        GreedyPath path = q.greedyPath(m);
        assertFalse(path.reachesCheese());
        assertEquals(List.of(p(0, 1), p(1, 1)), path.cells());
    }

    @Test
    void greedyPathStopsAtWall() {
        // лучшая стрелка упирается в стену — мышь стоит на месте, это тоже петля
        Maze m = new Maze(3, 2);
        m.setWall(p(0, 1), RIGHT, true);
        QTable q = new QTable(3, 2);
        q.set(p(0, 1), RIGHT, 1);
        GreedyPath path = q.greedyPath(m);
        assertFalse(path.reachesCheese());
        assertEquals(List.of(p(0, 1)), path.cells());
    }

    @Test
    void sizeMustMatchMaze() {
        assertThrows(IllegalArgumentException.class, () -> new QTable(3, 3).greedyPath(new Maze(3, 2)));
        assertThrows(IllegalArgumentException.class, () -> new QTable(0, 3));
    }
}
