package maze.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Обучение: попытки одна за другой. Каждый шаг — мышь выбирает направление, среда отвечает наградой,
 * мышь учится. Кончилась попытка — её сумма идёт в историю, мышь снова на старте, выученное остаётся.
 */
public final class Trainer {

    private final Maze maze;
    private final Settings settings;
    private final QTable table;
    private final Mouse mouse;
    private final List<Double> history = new ArrayList<>();
    private Attempt attempt;
    private GreedyPath lastPath;
    private int stableCount;
    private int learnedAt = -1;

    public Trainer(Maze maze, Settings settings) {
        this.maze = Objects.requireNonNull(maze, "maze");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.table = new QTable(maze.width(), maze.height());
        this.mouse = new Mouse(table, new Random(settings.seed()));
        this.attempt = new Attempt(maze, settings);
    }

    public Maze maze() {
        return maze;
    }

    public Settings settings() {
        return settings;
    }

    public QTable table() {
        return table;
    }

    /** Текущая (ещё не законченная) попытка. */
    public Attempt attempt() {
        return attempt;
    }

    /** Номер текущей попытки, с 1. */
    public int episode() {
        return history.size() + 1;
    }

    public int finishedEpisodes() {
        return history.size();
    }

    /** Доля случайных шагов в текущей попытке. */
    public double epsilon() {
        return settings.epsilonAt(history.size());
    }

    /** Один шаг мыши. Если попытка на нём закончилась — сразу начинается следующая. */
    public StepResult step() {
        StepResult result = attempt.step(mouse.choose(attempt.position(), epsilon()));
        mouse.learn(result, settings.alpha(), settings.gamma());
        if (attempt.isFinished()) {
            finishAttempt();
        }
        return result;
    }

    /** Доводит текущую попытку до конца. */
    public void runEpisode() {
        int target = episode() + 1;
        while (episode() < target) {
            step();
        }
    }

    public void runEpisodes(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("число попыток не может быть отрицательным: " + count);
        }
        for (int i = 0; i < count; i++) {
            runEpisode();
        }
    }

    /** Суммы выигрыша законченных попыток по порядку. */
    public List<Double> history() {
        return Collections.unmodifiableList(history);
    }

    /** Сумма последней законченной попытки; NaN, пока ни одной не было. */
    public double lastScore() {
        return history.isEmpty() ? Double.NaN : history.getLast();
    }

    /** Лучший путь не менялся (и доходил до сыра) {@code stableEpisodes} попыток подряд. */
    public boolean isLearned() {
        return stableCount >= settings.stableEpisodes();
    }

    /** Номер попытки, после которой путь впервые признан выученным; −1 — ещё не выучен. */
    public int learnedAt() {
        return learnedAt;
    }

    public GreedyPath bestPath() {
        return table.greedyPath(maze);
    }

    private void finishAttempt() {
        history.add(attempt.score());
        GreedyPath path = bestPath();
        if (!path.reachesCheese()) {
            stableCount = 0;
        } else if (path.equals(lastPath)) {
            stableCount++;
        } else {
            stableCount = 1;
        }
        lastPath = path;
        if (learnedAt < 0 && isLearned()) {
            learnedAt = history.size();
        }
        attempt = new Attempt(maze, settings);
    }
}
