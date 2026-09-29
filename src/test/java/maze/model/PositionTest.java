package maze.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void stepMovesByDirection() {
        Position p = new Position(3, 4);
        assertEquals(new Position(3, 3), p.step(Direction.UP));
        assertEquals(new Position(4, 4), p.step(Direction.RIGHT));
        assertEquals(new Position(3, 5), p.step(Direction.DOWN));
        assertEquals(new Position(2, 4), p.step(Direction.LEFT));
    }

    @Test
    void stepDoesNotChangeOriginal() {
        Position p = new Position(1, 1);
        p.step(Direction.RIGHT);
        assertEquals(new Position(1, 1), p);
    }

    @Test
    void textIsReadable() {
        assertEquals("(2, 7)", new Position(2, 7).toString());
    }
}
