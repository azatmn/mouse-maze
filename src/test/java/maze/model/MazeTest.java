package maze.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static maze.model.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

class MazeTest {

    private static Position p(int x, int y) {
        return new Position(x, y);
    }

    @Nested
    class Creation {

        @Test
        void newMazeHasSizeAndNoInnerWalls() {
            Maze m = new Maze(4, 3);
            assertEquals(4, m.width());
            assertEquals(3, m.height());
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    assertFalse(m.hasWall(p(x, y), RIGHT), "стена справа от " + p(x, y));
                }
            }
            for (int y = 0; y < 2; y++) {
                for (int x = 0; x < 4; x++) {
                    assertFalse(m.hasWall(p(x, y), DOWN), "стена снизу от " + p(x, y));
                }
            }
        }

        @Test
        void startBottomLeftCheeseTopRight() {
            Maze m = new Maze(5, 4);
            assertEquals(p(0, 3), m.start());
            assertEquals(p(4, 0), m.cheese());
        }

        @Test
        void allCellsEmptyExceptCheese() {
            Maze m = new Maze(3, 2);
            assertEquals(CellType.CHEESE, m.cellAt(p(2, 0)));
            assertEquals(CellType.EMPTY, m.cellAt(p(0, 1)));
            assertEquals(CellType.EMPTY, m.cellAt(p(1, 1)));
            assertEquals(1, m.count(CellType.CHEESE));
            assertEquals(5, m.count(CellType.EMPTY));
        }

        @ParameterizedTest
        @CsvSource({"1, 2", "2, 1", "100, 100", "1, 100"})
        void smallestAndLargestSizesAllowed(int w, int h) {
            Maze m = new Maze(w, h);
            assertNotEquals(m.start(), m.cheese());
        }

        @ParameterizedTest
        @CsvSource({"0, 5", "5, 0", "-1, 5", "1, 1", "101, 5", "5, 101", "2147483647, 2"})
        void badSizesRejected(int w, int h) {
            assertThrows(IllegalArgumentException.class, () -> new Maze(w, h));
        }

        @Test
        void corridorOneCellWide() {
            Maze m = new Maze(1, 3);
            assertEquals(p(0, 2), m.start());
            assertEquals(p(0, 0), m.cheese());
        }
    }

    @Nested
    class Walls {

        @Test
        void borderIsAlwaysWall() {
            Maze m = new Maze(3, 3);
            assertTrue(m.hasWall(p(0, 1), LEFT));
            assertTrue(m.hasWall(p(2, 1), RIGHT));
            assertTrue(m.hasWall(p(1, 0), UP));
            assertTrue(m.hasWall(p(1, 2), DOWN));
        }

        @Test
        void wallIsSeenFromBothSides() {
            Maze m = new Maze(3, 3);
            m.setWall(p(1, 1), RIGHT, true);
            assertTrue(m.hasWall(p(1, 1), RIGHT));
            assertTrue(m.hasWall(p(2, 1), LEFT));
            m.setWall(p(1, 1), UP, true);
            assertTrue(m.hasWall(p(1, 0), DOWN));
        }

        @Test
        void setWallTouchesOnlyThatWall() {
            Maze m = new Maze(3, 3);
            m.setWall(p(1, 1), LEFT, true);
            assertTrue(m.hasWall(p(0, 1), RIGHT));
            assertFalse(m.hasWall(p(1, 1), RIGHT));
            assertFalse(m.hasWall(p(1, 1), UP));
            assertFalse(m.hasWall(p(1, 1), DOWN));
            assertFalse(m.hasWall(p(0, 0), RIGHT));
        }

        @Test
        void wallCanBeRemoved() {
            Maze m = new Maze(3, 3);
            m.setWall(p(1, 1), DOWN, true);
            m.setWall(p(1, 2), UP, false);
            assertFalse(m.hasWall(p(1, 1), DOWN));
        }

        @Test
        void borderCannotBeChanged() {
            Maze m = new Maze(3, 3);
            assertThrows(IllegalArgumentException.class, () -> m.setWall(p(0, 0), LEFT, false));
            assertThrows(IllegalArgumentException.class, () -> m.setWall(p(2, 2), DOWN, false));
            assertTrue(m.hasWall(p(0, 0), LEFT));
        }

        @Test
        void closeAllPutsEveryInnerWall() {
            Maze m = new Maze(3, 2);
            m.closeAll();
            for (int y = 0; y < 2; y++) {
                for (int x = 0; x < 3; x++) {
                    for (Direction d : Direction.values()) {
                        assertTrue(m.hasWall(p(x, y), d), p(x, y) + " " + d);
                    }
                }
            }
        }

        @Test
        void positionOutsideRejected() {
            Maze m = new Maze(3, 3);
            assertThrows(IllegalArgumentException.class, () -> m.hasWall(p(3, 0), LEFT));
            assertThrows(IllegalArgumentException.class, () -> m.hasWall(p(0, -1), DOWN));
            assertThrows(IllegalArgumentException.class, () -> m.setWall(p(-1, 0), RIGHT, true));
        }

        @Test
        void containsChecksAllFourEdges() {
            Maze m = new Maze(4, 3);
            assertTrue(m.contains(p(0, 0)));
            assertTrue(m.contains(p(3, 2)));
            assertFalse(m.contains(p(-1, 0)));
            assertFalse(m.contains(p(0, -1)));
            assertFalse(m.contains(p(4, 0)));
            assertFalse(m.contains(p(0, 3)));
        }
    }

    @Nested
    class Moving {

        @Test
        void moveThroughOpenPassage() {
            Maze m = new Maze(3, 3);
            assertTrue(m.canMove(p(1, 1), UP));
            assertEquals(p(1, 0), m.next(p(1, 1), UP));
        }

        @Test
        void wallKeepsMouseInPlace() {
            Maze m = new Maze(3, 3);
            m.setWall(p(1, 1), RIGHT, true);
            assertFalse(m.canMove(p(1, 1), RIGHT));
            assertEquals(p(1, 1), m.next(p(1, 1), RIGHT));
        }

        @Test
        void borderKeepsMouseInPlace() {
            Maze m = new Maze(3, 3);
            assertFalse(m.canMove(p(0, 0), LEFT));
            assertEquals(p(0, 0), m.next(p(0, 0), UP));
        }
    }

    @Nested
    class Items {

        @Test
        void putWaterAndShock() {
            Maze m = new Maze(3, 3);
            m.setItem(p(1, 1), CellType.WATER);
            m.setItem(p(1, 2), CellType.SHOCK);
            assertEquals(CellType.WATER, m.cellAt(p(1, 1)));
            assertEquals(CellType.SHOCK, m.cellAt(p(1, 2)));
            assertEquals(1, m.count(CellType.WATER));
            assertEquals(1, m.count(CellType.SHOCK));
            m.setItem(p(1, 1), CellType.EMPTY);
            assertEquals(CellType.EMPTY, m.cellAt(p(1, 1)));
        }

        @Test
        void noItemsOnStartOrCheese() {
            Maze m = new Maze(3, 3);
            assertThrows(IllegalArgumentException.class, () -> m.setItem(m.start(), CellType.WATER));
            assertThrows(IllegalArgumentException.class, () -> m.setItem(m.cheese(), CellType.SHOCK));
            assertEquals(CellType.CHEESE, m.cellAt(m.cheese()));
        }

        @Test
        void cheeseIsNotAnItem() {
            Maze m = new Maze(3, 3);
            assertThrows(IllegalArgumentException.class, () -> m.setItem(p(1, 1), CellType.CHEESE));
        }

        @Test
        void nullRejected() {
            Maze m = new Maze(3, 3);
            assertThrows(NullPointerException.class, () -> m.setItem(p(1, 1), null));
            assertThrows(NullPointerException.class, () -> m.setStart(null));
        }

        @Test
        void moveStartAndCheese() {
            Maze m = new Maze(3, 3);
            m.setStart(p(1, 1));
            m.setCheese(p(0, 0));
            assertEquals(p(1, 1), m.start());
            assertEquals(p(0, 0), m.cheese());
            assertEquals(CellType.CHEESE, m.cellAt(p(0, 0)));
            assertEquals(CellType.EMPTY, m.cellAt(p(2, 0)), "старое место сыра опустело");
            assertEquals(1, m.count(CellType.CHEESE));
        }

        @Test
        void startAndCheeseCannotMeet() {
            Maze m = new Maze(3, 3);
            assertThrows(IllegalArgumentException.class, () -> m.setStart(m.cheese()));
            assertThrows(IllegalArgumentException.class, () -> m.setCheese(m.start()));
        }

        @Test
        void startOrCheeseOnItemClearsIt() {
            Maze m = new Maze(3, 3);
            m.setItem(p(1, 1), CellType.SHOCK);
            m.setItem(p(1, 0), CellType.WATER);
            m.setStart(p(1, 1));
            m.setCheese(p(1, 0));
            m.setStart(p(0, 0));
            assertEquals(CellType.EMPTY, m.cellAt(p(1, 1)), "молния не вернулась, когда старт ушёл");
            assertEquals(0, m.count(CellType.SHOCK));
            assertEquals(0, m.count(CellType.WATER));
        }

        @Test
        void outsideRejected() {
            Maze m = new Maze(3, 3);
            assertThrows(IllegalArgumentException.class, () -> m.setItem(p(3, 3), CellType.WATER));
            assertThrows(IllegalArgumentException.class, () -> m.setStart(p(-1, 0)));
            assertThrows(IllegalArgumentException.class, () -> m.setCheese(p(0, 3)));
            assertThrows(IllegalArgumentException.class, () -> m.cellAt(p(0, 5)));
        }
    }

    @Nested
    class Paths {

        @Test
        void openFieldShortestPathLength() {
            Maze m = new Maze(4, 3);
            List<Position> path = m.shortestPath();
            // от (0,2) до (3,0): 3 шага вправо + 2 вверх = 5 шагов, 6 клеток
            assertEquals(6, path.size());
            assertEquals(m.start(), path.getFirst());
            assertEquals(m.cheese(), path.getLast());
            assertValidPath(m, path);
        }

        @Test
        void pathGoesAroundWall() {
            // 3x2: старт (0,1), сыр (2,0); стена между верхними (0,0)-(1,0) и нижними (1,1)-(2,1)
            Maze m = new Maze(3, 2);
            m.setWall(p(1, 1), RIGHT, true);
            m.setWall(p(1, 0), DOWN, true);
            List<Position> path = m.shortestPath();
            assertEquals(List.of(p(0, 1), p(0, 0), p(1, 0), p(2, 0)), path);
        }

        @Test
        void unreachableCheese() {
            Maze m = new Maze(3, 3);
            m.setWall(p(2, 0), LEFT, true);
            m.setWall(p(2, 0), DOWN, true);
            assertFalse(m.isCheeseReachable());
            assertTrue(m.shortestPath().isEmpty());
        }

        @Test
        void reachableWhenOnlyOnePassageLeft() {
            Maze m = new Maze(3, 3);
            m.setWall(p(2, 0), LEFT, true);
            assertTrue(m.isCheeseReachable());
            assertEquals(p(2, 1), m.shortestPath().get(m.shortestPath().size() - 2));
        }

        @Test
        void itemsDoNotBlockPath() {
            Maze m = new Maze(2, 1);
            assertTrue(m.isCheeseReachable());
            Maze m2 = new Maze(3, 1);
            m2.setItem(p(1, 0), CellType.SHOCK);
            assertEquals(3, m2.shortestPath().size());
        }

        private void assertValidPath(Maze m, List<Position> path) {
            for (int i = 1; i < path.size(); i++) {
                Position a = path.get(i - 1), b = path.get(i);
                boolean ok = false;
                for (Direction d : Direction.values()) {
                    if (a.step(d).equals(b) && m.canMove(a, d)) {
                        ok = true;
                    }
                }
                assertTrue(ok, a + " -> " + b);
            }
        }
    }

    @Nested
    class Copy {

        @Test
        void copyIsEqualButIndependent() {
            Maze m = new Maze(3, 3);
            m.setWall(p(1, 1), RIGHT, true);
            m.setItem(p(1, 0), CellType.WATER);
            m.setStart(p(0, 0));
            Maze c = m.copy();
            assertTrue(c.hasWall(p(1, 1), RIGHT));
            assertEquals(CellType.WATER, c.cellAt(p(1, 0)));
            assertEquals(p(0, 0), c.start());
            assertEquals(m.cheese(), c.cheese());

            c.setWall(p(1, 1), RIGHT, false);
            c.setItem(p(1, 0), CellType.EMPTY);
            c.setWall(p(0, 1), DOWN, true);
            assertTrue(m.hasWall(p(1, 1), RIGHT));
            assertFalse(m.hasWall(p(0, 1), DOWN));
            assertEquals(CellType.WATER, m.cellAt(p(1, 0)));
        }

        @Test
        void equalsComparesEverything() {
            Maze a = new Maze(3, 3);
            assertEquals(a, a.copy());
            assertEquals(a.hashCode(), a.copy().hashCode());
            assertNotEquals(a, new Maze(3, 4));

            Maze wall = a.copy();
            wall.setWall(p(0, 0), RIGHT, true);
            assertNotEquals(a, wall);
            Maze down = a.copy();
            down.setWall(p(0, 0), DOWN, true);
            assertNotEquals(a, down);
            Maze item = a.copy();
            item.setItem(p(1, 1), CellType.WATER);
            assertNotEquals(a, item);
            Maze start = a.copy();
            start.setStart(p(1, 1));
            assertNotEquals(a, start);
            Maze cheese = a.copy();
            cheese.setCheese(p(1, 1));
            assertNotEquals(a, cheese);
            assertNotEquals(a, null);
        }
    }
}
