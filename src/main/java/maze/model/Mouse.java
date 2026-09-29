package maze.model;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Агент Q-learning. Карты не видит: знает только, в какой клетке стоит,
 * и учится по наградам, которые сообщает среда ({@link Attempt}).
 */
public final class Mouse {

    private static final Direction[] DIRECTIONS = Direction.values();

    private final QTable table;
    private final Random random;

    public Mouse(QTable table, Random random) {
        this.table = Objects.requireNonNull(table, "table");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Выбор шага (ε-жадная стратегия): с вероятностью epsilon — наугад, чтобы пробовать новые пути;
     * иначе — туда, где число больше. Среди равных лучших — случайное, чтобы вначале, когда всё по нулям,
     * мышь не шла всегда вверх.
     */
    public Direction choose(Position p, double epsilon) {
        if (random.nextDouble() < epsilon) {
            return DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        }
        List<Direction> best = table.bestAll(p);
        return best.get(random.nextInt(best.size()));
    }

    /**
     * Обучение после шага — формула Q-learning:
     * Q[s][a] = Q[s][a] + α · (r + γ · max Q[s'] − Q[s][a]).
     * «Награда сейчас + лучшее, что можно получить дальше»; у сыра «дальше» нет.
     */
    public void learn(StepResult step, double alpha, double gamma) {
        double future = step.terminal() ? 0 : table.max(step.to());
        double target = step.reward() + gamma * future;
        double old = table.get(step.from(), step.direction());
        table.set(step.from(), step.direction(), old + alpha * (target - old));
    }
}
