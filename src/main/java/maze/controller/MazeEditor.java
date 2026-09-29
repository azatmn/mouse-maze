package maze.controller;

import maze.model.CellType;
import maze.model.Direction;
import maze.model.Maze;
import maze.model.Position;

import java.util.Objects;

/**
 * Редактор лабиринта без JavaFX: клик в точку холста → какая это клетка или стенка → что с ней сделать.
 * Меняет текущий лабиринт контроллера; после любого изменения обучение начинается заново,
 * потому что выученное относится к старому лабиринту.
 */
public final class MazeEditor {

    /** Доля размера клетки у её границы, где клик считается кликом по стене. */
    public static final double EDGE_ZONE = 0.2;

    /** Инструмент, выбранный в панели редактора. */
    public enum Tool {
        WALL,
        WATER,
        SHOCK,
        START,
        CHEESE,
        ERASE
    }

    /** Куда попал клик. */
    public sealed interface Hit {
        /** Внутрь клетки. */
        record Cell(Position cell) implements Hit {
        }

        /**
         * На стену. Внутренняя стена всегда записана как правая или нижняя у левой/верхней клетки,
         * чтобы одна и та же стена не имела двух записей; внешняя граница — как сторона крайней клетки.
         */
        record Edge(Position cell, Direction side) implements Hit {
        }

        /** Мимо лабиринта. */
        record Outside() implements Hit {
        }
    }

    /** Что вышло из клика. */
    public sealed interface Result {
        record Changed() implements Result {
        }

        record Unchanged() implements Result {
        }

        /** Так нельзя; message — объяснение для пользователя. */
        record Rejected(String message) implements Result {
        }
    }

    private final TrainingController controller;

    public MazeEditor(TrainingController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    /**
     * Куда попадает точка (px, py) холста, если клетка на экране — квадрат cellSize пикселей,
     * а левый верхний угол лабиринта — (0, 0).
     */
    public static Hit hitTest(Maze maze, double px, double py, double cellSize) {
        Position cell = cellAt(maze, px, py, cellSize);
        if (cell == null) {
            return new Hit.Outside();
        }
        double inX = px - cell.x() * cellSize;  // расстояние от левой границы клетки
        double inY = py - cell.y() * cellSize;
        // до ближайшей вертикальной и горизонтальной границы
        double dx = Math.min(inX, cellSize - inX);
        double dy = Math.min(inY, cellSize - inY);
        if (Math.min(dx, dy) > cellSize * EDGE_ZONE) {
            return new Hit.Cell(cell);
        }
        Direction side = dx <= dy
                ? (inX < cellSize - inX ? Direction.LEFT : Direction.RIGHT)
                : (inY < cellSize - inY ? Direction.UP : Direction.DOWN);
        return normalized(maze, cell, side);
    }

    /**
     * Применить инструмент к точке холста. Стены ставятся и убираются кликом по границе,
     * предметы, старт и сыр — кликом в клетку (граница клетки тоже считается клеткой).
     */
    public Result apply(Tool tool, double px, double py, double cellSize) {
        Maze maze = controller.maze();
        Result result = switch (tool) {
            case WALL -> wall(maze, hitTest(maze, px, py, cellSize));
            case ERASE -> erase(maze, hitTest(maze, px, py, cellSize));
            case WATER -> item(maze, cellAt(maze, px, py, cellSize), CellType.WATER);
            case SHOCK -> item(maze, cellAt(maze, px, py, cellSize), CellType.SHOCK);
            case START -> start(maze, cellAt(maze, px, py, cellSize));
            case CHEESE -> cheese(maze, cellAt(maze, px, py, cellSize));
        };
        if (result instanceof Result.Changed) {
            controller.mazeEdited();
        }
        return result;
    }

    private static Result wall(Maze maze, Hit hit) {
        if (!(hit instanceof Hit.Edge(Position cell, Direction side))) {
            return new Result.Unchanged();
        }
        if (!maze.contains(cell.step(side))) {
            return new Result.Rejected("Граница лабиринта — всегда стена, её не убрать");
        }
        maze.setWall(cell, side, !maze.hasWall(cell, side));
        return new Result.Changed();
    }

    private static Result erase(Maze maze, Hit hit) {
        return switch (hit) {
            case Hit.Edge(Position cell, Direction side) -> {
                if (!maze.contains(cell.step(side)) || !maze.hasWall(cell, side)) {
                    yield new Result.Unchanged();
                }
                maze.setWall(cell, side, false);
                yield new Result.Changed();
            }
            case Hit.Cell(Position cell) -> {
                CellType type = maze.cellAt(cell);
                if (type != CellType.WATER && type != CellType.SHOCK) {
                    yield new Result.Unchanged();
                }
                maze.setItem(cell, CellType.EMPTY);
                yield new Result.Changed();
            }
            case Hit.Outside() -> new Result.Unchanged();
        };
    }

    /** Вода или ток: в пустую клетку или клетку с другим предметом — положить, с тем же — убрать. */
    private static Result item(Maze maze, Position cell, CellType type) {
        if (cell == null) {
            return new Result.Unchanged();
        }
        if (cell.equals(maze.start()) || cell.equals(maze.cheese())) {
            return new Result.Rejected("На старт и на сыр ничего класть нельзя");
        }
        maze.setItem(cell, maze.cellAt(cell) == type ? CellType.EMPTY : type);
        return new Result.Changed();
    }

    private static Result start(Maze maze, Position cell) {
        if (cell == null || cell.equals(maze.start())) {
            return new Result.Unchanged();
        }
        if (cell.equals(maze.cheese())) {
            return new Result.Rejected("Старт не может стоять на сыре");
        }
        maze.setStart(cell);
        return new Result.Changed();
    }

    private static Result cheese(Maze maze, Position cell) {
        if (cell == null || cell.equals(maze.cheese())) {
            return new Result.Unchanged();
        }
        if (cell.equals(maze.start())) {
            return new Result.Rejected("Сыр не может лежать на старте");
        }
        maze.setCheese(cell);
        return new Result.Changed();
    }

    /** Клетка под точкой или null, если точка вне лабиринта (или числа бессмысленные). */
    private static Position cellAt(Maze maze, double px, double py, double cellSize) {
        if (!(cellSize > 0) || !(px >= 0) || !(py >= 0)) {  // так отсекается и NaN
            return null;
        }
        double x = Math.floor(px / cellSize);
        double y = Math.floor(py / cellSize);
        if (x >= maze.width() || y >= maze.height()) {
            return null;
        }
        return new Position((int) x, (int) y);
    }

    /** Внутренняя стена — как правая или нижняя у левой/верхней из двух клеток. */
    private static Hit.Edge normalized(Maze maze, Position cell, Direction side) {
        Position other = cell.step(side);
        if (maze.contains(other) && (side == Direction.LEFT || side == Direction.UP)) {
            return new Hit.Edge(other, side.opposite());
        }
        return new Hit.Edge(cell, side);
    }
}
