package maze.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static maze.model.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

class AttemptTest {

    // 4x1: старт (0,0) · вода (1,0) · ток (2,0) · сыр (3,0)
    private Maze maze;
    private final Settings settings = Settings.builder()
            .cheeseReward(100).waterReward(10).shockReward(-50).stepReward(-1).wallReward(-5).maxSteps(20)
            .build();

    @BeforeEach
    void setUp() {
        maze = new Maze(4, 1);
        maze.setStart(new Position(0, 0));
        maze.setCheese(new Position(3, 0));
        maze.setItem(new Position(1, 0), CellType.WATER);
        maze.setItem(new Position(2, 0), CellType.SHOCK);
    }

    @Test
    void startsAtStartWithNothing() {
        Attempt a = new Attempt(maze, settings);
        assertEquals(new Position(0, 0), a.position());
        assertEquals(0, a.score());
        assertEquals(0, a.steps());
        assertFalse(a.isFinished());
        assertFalse(a.reachedCheese());
    }

    @Test
    void wallBumpCostsWallRewardAndKeepsPlace() {
        Attempt a = new Attempt(maze, settings);
        StepResult r = a.step(UP);
        assertEquals(new StepResult(new Position(0, 0), UP, new Position(0, 0), -5, false), r);
        assertEquals(new Position(0, 0), a.position());
        assertEquals(1, a.steps());
        assertEquals(-5, a.score());
    }

    @Test
    void emptyCellCostsOneStep() {
        Maze open = new Maze(3, 1);
        Attempt a = new Attempt(open, settings);
        StepResult r = a.step(RIGHT);
        assertEquals(-1, r.reward());
        assertEquals(new Position(1, 0), r.to());
        assertEquals(new Position(0, 0), r.from());
        assertEquals(RIGHT, r.direction());
    }

    @Test
    void waterOncePerAttempt() {
        Attempt a = new Attempt(maze, settings);
        assertTrue(a.hasWater(new Position(1, 0)));
        assertEquals(-1 + 10, a.step(RIGHT).reward());
        assertFalse(a.hasWater(new Position(1, 0)));
        assertEquals(-1, a.step(LEFT).reward());
        assertEquals(-1, a.step(RIGHT).reward(), "вода уже выпита");
        assertEquals(CellType.WATER, maze.cellAt(new Position(1, 0)), "лабиринт не меняется");

        Attempt next = new Attempt(maze, settings);
        assertTrue(next.hasWater(new Position(1, 0)), "в новой попытке вода снова есть");
        assertEquals(9, next.step(RIGHT).reward());
    }

    @Test
    void hasWaterOnlyWhereWaterIs() {
        Attempt a = new Attempt(maze, settings);
        assertFalse(a.hasWater(new Position(0, 0)));
        assertFalse(a.hasWater(new Position(2, 0)));
    }

    @Test
    void shockEveryTime() {
        Attempt a = new Attempt(maze, settings);
        a.step(RIGHT);
        assertEquals(-51, a.step(RIGHT).reward());
        a.step(LEFT);
        assertEquals(-51, a.step(RIGHT).reward());
    }

    @Test
    void cheeseEndsAttempt() {
        Attempt a = new Attempt(maze, settings);
        a.step(RIGHT);
        a.step(RIGHT);
        StepResult r = a.step(RIGHT);
        assertEquals(new StepResult(new Position(2, 0), RIGHT, new Position(3, 0), 99, true), r);
        assertTrue(a.isFinished());
        assertTrue(a.reachedCheese());
        assertEquals(9 - 51 + 99, a.score());
        assertEquals(3, a.steps());
    }

    @Test
    void stepLimitEndsAttemptWithoutCheese() {
        Attempt a = new Attempt(maze, settings.toBuilder().maxSteps(3).build());
        a.step(UP);
        a.step(UP);
        assertFalse(a.isFinished());
        StepResult r = a.step(UP);
        assertFalse(r.terminal(), "лимит — не сыр: будущее у клетки есть");
        assertTrue(a.isFinished());
        assertFalse(a.reachedCheese());
        assertEquals(-15, a.score());
    }

    @Test
    void noStepsAfterFinish() {
        Attempt a = new Attempt(maze, settings.toBuilder().maxSteps(1).build());
        a.step(UP);
        assertThrows(IllegalStateException.class, () -> a.step(RIGHT));
        assertEquals(1, a.steps());
    }

    @Test
    void nullDirectionRejected() {
        Attempt a = new Attempt(maze, settings);
        assertThrows(NullPointerException.class, () -> a.step(null));
        assertEquals(0, a.steps());
    }

    @Test
    void mazeUntouched() {
        Maze before = maze.copy();
        Attempt a = new Attempt(maze, settings);
        a.step(RIGHT);
        a.step(RIGHT);
        a.step(RIGHT);
        assertEquals(before, maze);
    }
}
