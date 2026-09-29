package maze.model;

import java.util.Objects;

/**
 * Среда: одна попытка мыши от старта до сыра или до лимита шагов.
 * Считает награды и помнит выпитую воду; сам лабиринт не меняет,
 * поэтому в следующей попытке вода снова на месте.
 */
public final class Attempt {

    private final Maze maze;
    private final Settings settings;
    private final boolean[][] drunk;
    private Position position;
    private double score;
    private int steps;
    private boolean reachedCheese;

    public Attempt(Maze maze, Settings settings) {
        this.maze = Objects.requireNonNull(maze, "maze");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.drunk = new boolean[maze.height()][maze.width()];
        this.position = maze.start();
    }

    public Position position() {
        return position;
    }

    /** Сумма выигрыша за попытку (условие: мышь должна знать сумму выигрыша или проигрыша). */
    public double score() {
        return score;
    }

    public int steps() {
        return steps;
    }

    public boolean isFinished() {
        return reachedCheese || steps >= settings.maxSteps();
    }

    public boolean reachedCheese() {
        return reachedCheese;
    }

    /** Есть ли в клетке вода, которую ещё не выпили в этой попытке. */
    public boolean hasWater(Position p) {
        return maze.cellAt(p) == CellType.WATER && !drunk[p.y()][p.x()];
    }

    /** Мышь шагает в направлении d, среда отвечает наградой. */
    public StepResult step(Direction d) {
        Objects.requireNonNull(d, "direction");
        if (isFinished()) {
            throw new IllegalStateException("попытка уже закончена");
        }
        Position from = position;
        double reward;
        if (!maze.canMove(from, d)) {
            reward = settings.wallReward();
        } else {
            position = from.step(d);
            reward = settings.stepReward() + rewardFor(position);
        }
        steps++;
        score += reward;
        return new StepResult(from, d, position, reward, reachedCheese);
    }

    private double rewardFor(Position p) {
        return switch (maze.cellAt(p)) {
            case CHEESE -> {
                reachedCheese = true;
                yield settings.cheeseReward();
            }
            case WATER -> {
                if (drunk[p.y()][p.x()]) {
                    yield 0;
                }
                drunk[p.y()][p.x()] = true;
                yield settings.waterReward();
            }
            case SHOCK -> settings.shockReward();
            case EMPTY -> 0;
        };
    }
}
