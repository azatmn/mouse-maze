package maze.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void offsetsPointWhereTheNameSays() {
        // y растёт вниз, как в окне: вверх — это y - 1
        assertEquals(0, Direction.UP.dx());
        assertEquals(-1, Direction.UP.dy());
        assertEquals(1, Direction.RIGHT.dx());
        assertEquals(0, Direction.RIGHT.dy());
        assertEquals(0, Direction.DOWN.dx());
        assertEquals(1, Direction.DOWN.dy());
        assertEquals(-1, Direction.LEFT.dx());
        assertEquals(0, Direction.LEFT.dy());
    }

    @Test
    void oppositePairs() {
        assertEquals(Direction.DOWN, Direction.UP.opposite());
        assertEquals(Direction.UP, Direction.DOWN.opposite());
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite());
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite());
    }

    @Test
    void fourDirectionsInClockwiseOrder() {
        // порядок важен: индекс направления — номер столбца в Q-таблице
        assertArrayEquals(new Direction[]{Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT},
                Direction.values());
    }
}
