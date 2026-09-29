package maze.controller;

import maze.controller.MazeEditor.Hit;
import maze.controller.MazeEditor.Result;
import maze.controller.MazeEditor.Tool;
import maze.model.CellType;
import maze.model.Maze;
import maze.model.Position;
import maze.model.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static maze.model.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

class MazeEditorTest {

    /** Размер клетки на экране в пикселях. */
    private static final double S = 40;

    private static Position p(int x, int y) {
        return new Position(x, y);
    }

    /** Центр клетки (x, y) в пикселях. */
    private static double cx(int x) {
        return x * S + S / 2;
    }

    @Nested
    class HitTest {

        private final Maze maze = new Maze(4, 3);

        @Test
        void centerIsCell() {
            assertEquals(new Hit.Cell(p(1, 2)), MazeEditor.hitTest(maze, cx(1), cx(2), S));
        }

        @Test
        void nearBorderIsEdge() {
            // правая граница клетки (1,1) — x = 80; 3 px левее и правее — та же стена
            assertEquals(new Hit.Edge(p(1, 1), RIGHT), MazeEditor.hitTest(maze, 77, cx(1), S));
            assertEquals(new Hit.Edge(p(1, 1), RIGHT), MazeEditor.hitTest(maze, 83, cx(1), S));
            // нижняя граница клетки (2,0) — y = 40
            assertEquals(new Hit.Edge(p(2, 0), DOWN), MazeEditor.hitTest(maze, cx(2), 42, S));
            assertEquals(new Hit.Edge(p(2, 0), DOWN), MazeEditor.hitTest(maze, cx(2), 38, S));
        }

        @Test
        void edgeZoneIsAFifthOfCell() {
            // 0.2 * 40 = 8 px от границы — ещё стена, 9 px — уже клетка
            assertEquals(new Hit.Edge(p(1, 1), RIGHT), MazeEditor.hitTest(maze, 72, cx(1), S));
            assertEquals(new Hit.Cell(p(1, 1)), MazeEditor.hitTest(maze, 71, cx(1), S));
        }

        @Test
        void outerBorderIsEdgeOfBorderCell() {
            assertEquals(new Hit.Edge(p(0, 1), LEFT), MazeEditor.hitTest(maze, 2, cx(1), S));
            assertEquals(new Hit.Edge(p(3, 2), DOWN), MazeEditor.hitTest(maze, cx(3), 3 * S - 1, S));
            assertEquals(new Hit.Edge(p(2, 0), UP), MazeEditor.hitTest(maze, cx(2), 1, S));
            assertEquals(new Hit.Edge(p(3, 0), RIGHT), MazeEditor.hitTest(maze, 4 * S - 1, cx(0), S));
        }

        @Test
        void cornerPicksNearerEdge() {
            // клетка (1,1), рядом с её углом (80, 40): выигрывает та граница, что ближе
            assertEquals(new Hit.Edge(p(1, 1), RIGHT), MazeEditor.hitTest(maze, 79, 45, S));
            assertEquals(new Hit.Edge(p(1, 0), DOWN), MazeEditor.hitTest(maze, 73, 41, S));
        }

        @Test
        void outsideIsNothing() {
            assertEquals(new Hit.Outside(), MazeEditor.hitTest(maze, -1, 10, S));
            assertEquals(new Hit.Outside(), MazeEditor.hitTest(maze, 10, -0.5, S));
            assertEquals(new Hit.Outside(), MazeEditor.hitTest(maze, 4 * S, 10, S));
            assertEquals(new Hit.Outside(), MazeEditor.hitTest(maze, 10, 3 * S + 5, S));
            assertEquals(new Hit.Outside(), MazeEditor.hitTest(maze, Double.NaN, 10, S));
            assertEquals(new Hit.Outside(), MazeEditor.hitTest(maze, 10, 10, 0));
        }
    }

    @Nested
    class Editing {

        private TrainingController controller;
        private MazeEditor editor;
        private Maze maze;

        @BeforeEach
        void setUp() {
            maze = new Maze(4, 3);   // старт (0,2), сыр (3,0)
            controller = new TrainingController(maze, Settings.defaults());
            editor = new MazeEditor(controller);
        }

        private Result click(Tool tool, double x, double y) {
            return editor.apply(tool, x, y, S);
        }

        @Test
        void wallToolTogglesInnerWall() {
            assertEquals(new Result.Changed(), click(Tool.WALL, 80, cx(1)));
            assertTrue(maze.hasWall(p(1, 1), RIGHT));
            assertEquals(new Result.Changed(), click(Tool.WALL, 80, cx(1)));
            assertFalse(maze.hasWall(p(1, 1), RIGHT));
        }

        @Test
        void wallToolOnBorderRejected() {
            Result r = click(Tool.WALL, 1, cx(1));
            assertInstanceOf(Result.Rejected.class, r);
            assertTrue(maze.hasWall(p(0, 1), LEFT));
        }

        @Test
        void wallToolInCellCenterDoesNothing() {
            Maze before = maze.copy();
            assertEquals(new Result.Unchanged(), click(Tool.WALL, cx(1), cx(1)));
            assertEquals(before, maze);
        }

        @Test
        void waterAndShockToggleInCell() {
            assertEquals(new Result.Changed(), click(Tool.WATER, cx(1), cx(1)));
            assertEquals(CellType.WATER, maze.cellAt(p(1, 1)));
            assertEquals(new Result.Changed(), click(Tool.SHOCK, cx(1), cx(1)));
            assertEquals(CellType.SHOCK, maze.cellAt(p(1, 1)), "другой предмет заменяет");
            assertEquals(new Result.Changed(), click(Tool.SHOCK, cx(1), cx(1)));
            assertEquals(CellType.EMPTY, maze.cellAt(p(1, 1)), "тот же предмет убирает");
        }

        @Test
        void itemToolUsesCellEvenNearBorder() {
            assertEquals(new Result.Changed(), click(Tool.WATER, 78, cx(1)));
            assertEquals(CellType.WATER, maze.cellAt(p(1, 1)));
        }

        @Test
        void itemsNotOnStartOrCheese() {
            Result r = click(Tool.WATER, cx(0), cx(2));
            assertInstanceOf(Result.Rejected.class, r);
            assertInstanceOf(Result.Rejected.class, click(Tool.SHOCK, cx(3), cx(0)));
            assertEquals(CellType.EMPTY, maze.cellAt(p(0, 2)));
            assertEquals(CellType.CHEESE, maze.cellAt(p(3, 0)));
        }

        @Test
        void moveStartAndCheese() {
            maze.setItem(p(1, 1), CellType.SHOCK);
            assertEquals(new Result.Changed(), click(Tool.START, cx(1), cx(1)));
            assertEquals(p(1, 1), maze.start());
            assertEquals(CellType.EMPTY, maze.cellAt(p(1, 1)));
            assertEquals(new Result.Changed(), click(Tool.CHEESE, cx(0), cx(0)));
            assertEquals(p(0, 0), maze.cheese());
        }

        @Test
        void startOntoCheeseRejectedAndSamePlaceUnchanged() {
            assertInstanceOf(Result.Rejected.class, click(Tool.START, cx(3), cx(0)));
            assertInstanceOf(Result.Rejected.class, click(Tool.CHEESE, cx(0), cx(2)));
            assertEquals(new Result.Unchanged(), click(Tool.START, cx(0), cx(2)));
            assertEquals(new Result.Unchanged(), click(Tool.CHEESE, cx(3), cx(0)));
        }

        @Test
        void eraseRemovesWallOrItem() {
            maze.setWall(p(1, 1), RIGHT, true);
            maze.setItem(p(2, 2), CellType.WATER);
            assertEquals(new Result.Changed(), click(Tool.ERASE, 80, cx(1)));
            assertFalse(maze.hasWall(p(1, 1), RIGHT));
            assertEquals(new Result.Unchanged(), click(Tool.ERASE, 80, cx(1)), "стены уже нет");
            assertEquals(new Result.Changed(), click(Tool.ERASE, cx(2), cx(2)));
            assertEquals(CellType.EMPTY, maze.cellAt(p(2, 2)));
            assertEquals(new Result.Unchanged(), click(Tool.ERASE, cx(2), cx(2)));
            assertEquals(new Result.Unchanged(), click(Tool.ERASE, 1, cx(1)), "границу не стереть, но и не ошибка");
        }

        @Test
        void outsideUnchanged() {
            for (Tool t : Tool.values()) {
                assertEquals(new Result.Unchanged(), click(t, -10, -10), t.toString());
            }
        }

        @Test
        void changeResetsTrainingAndPauses() {
            controller.trainEpisodes(5);
            controller.start();
            click(Tool.WATER, cx(1), cx(1));
            assertFalse(controller.isRunning());
            assertEquals(0, controller.trainer().finishedEpisodes());
        }

        @Test
        void noChangeKeepsTraining() {
            controller.trainEpisodes(5);
            controller.start();
            click(Tool.WALL, cx(1), cx(1));
            click(Tool.WATER, cx(0), cx(2));
            click(Tool.START, cx(0), cx(2));
            assertTrue(controller.isRunning());
            assertEquals(5, controller.trainer().finishedEpisodes());
        }

        @Test
        void editsGoToCurrentMazeAfterReplacing() {
            Maze other = new Maze(4, 3);
            controller.setMaze(other);
            click(Tool.WATER, cx(1), cx(1));
            assertEquals(CellType.WATER, other.cellAt(p(1, 1)));
            assertEquals(CellType.EMPTY, maze.cellAt(p(1, 1)));
        }

        @Test
        void rejectionMessagesAreHuman() {
            Result.Rejected r = (Result.Rejected) click(Tool.WATER, cx(0), cx(2));
            assertFalse(r.message().isBlank());
            Result.Rejected b = (Result.Rejected) click(Tool.WALL, 1, cx(1));
            assertFalse(b.message().isBlank());
            assertNotEquals(r.message(), b.message());
        }
    }
}
