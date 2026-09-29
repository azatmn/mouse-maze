package maze.controller;

import maze.model.Maze;
import maze.model.Settings;
import maze.model.Trainer;

import java.util.Objects;

/**
 * Управление обучением для окна: старт, пауза, шаг, «обучить N попыток», сброс, скорость, автостоп.
 * Не знает ничего про JavaFX — окно только вызывает методы и рисует состояние,
 * поэтому всю логику кнопок можно проверить обычными тестами.
 * <p>
 * Время приходит снаружи через {@link #tick(long)} (окно вызывает его на каждом кадре),
 * а контроллер сам решает, сколько шагов сделать, исходя из скорости.
 */
public class TrainingController {

    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 5000;
    public static final int DEFAULT_SPEED = 20;
    /** Больше шагов за один кадр не делаем, чтобы окно не подвисало. */
    public static final int MAX_STEPS_PER_TICK = 500;
    /** Больше попыток за одно нажатие «Обучить» не делаем. */
    public static final int MAX_EPISODES_AT_ONCE = 100_000;
    /** «Обучить» останавливается, когда шагов набралось столько (иначе большой лабиринт повесит окно). */
    public static final long STEP_BUDGET = 5_000_000;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private Maze maze;
    private Settings settings;
    private Trainer trainer;

    private boolean running;
    private boolean autoStop = true;
    private int speed = DEFAULT_SPEED;
    /** Время прошлого кадра; пока его нет (сразу после старта), первый кадр только запоминает время. */
    private boolean hasLastTick;
    private long lastTick;
    /** Накопленное время × скорость: целая часть от деления на секунду — сколько шагов пора сделать. */
    private long stepDebt;

    public TrainingController(Maze maze, Settings settings) {
        this.maze = Objects.requireNonNull(maze, "maze");
        this.settings = Objects.requireNonNull(settings, "settings");
        reset();
    }

    /** Запустить. Повторное нажатие ничего не меняет. */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        hasLastTick = false; // время, проведённое на паузе, не должно превратиться в шаги
        stepDebt = 0;
    }

    public void pause() {
        running = false;
    }

    /** Одна кнопка «Старт/Пауза». */
    public void toggleRunning() {
        if (running) {
            pause();
        } else {
            start();
        }
    }

    public boolean isRunning() {
        return running;
    }

    /** Кнопка «Шаг»: ставит на паузу и делает ровно один шаг мыши. */
    public void stepOnce() {
        pause();
        trainer.step();
    }

    /** Очередной кадр окна. nowNanos — текущее время в наносекундах. */
    public void tick(long nowNanos) {
        if (!running) {
            return;
        }
        if (!hasLastTick || nowNanos < lastTick) {
            hasLastTick = true;
            lastTick = nowNanos;
            return;
        }
        // Больше секунды за кадр не учитываем: иначе после долгого «замерзания» набежит гигантский долг
        long elapsed = Math.min(nowNanos - lastTick, NANOS_PER_SECOND);
        lastTick = nowNanos;
        stepDebt += elapsed * speed;

        long due = stepDebt / NANOS_PER_SECOND;
        stepDebt %= NANOS_PER_SECOND;
        if (due > MAX_STEPS_PER_TICK) {
            due = MAX_STEPS_PER_TICK;
            stepDebt = 0; // не успели — не догоняем, просто идём медленнее
        }
        for (int i = 0; i < due; i++) {
            boolean wasLearned = trainer.learnedAt() >= 0;
            trainer.step();
            if (autoStop && !wasLearned && trainer.learnedAt() >= 0) {
                pause();
                return;
            }
        }
    }

    /** Скорость в шагах в секунду; значения вне допустимого диапазона прижимаются к границе. */
    public void setSpeed(int stepsPerSecond) {
        speed = Math.clamp(stepsPerSecond, MIN_SPEED, MAX_SPEED);
    }

    public int getSpeed() {
        return speed;
    }

    /** Останавливаться ли, когда мышь выучила маршрут (срабатывает один раз за обучение). */
    public void setAutoStop(boolean autoStop) {
        this.autoStop = autoStop;
    }

    public boolean isAutoStop() {
        return autoStop;
    }

    /**
     * Кнопка «Обучить N попыток»: ставит на паузу и мгновенно прогоняет попытки.
     * Не больше {@link #MAX_EPISODES_AT_ONCE} попыток и примерно {@link #STEP_BUDGET} шагов:
     * бюджет проверяется между попытками, поэтому попытка не обрывается посередине.
     *
     * @return сколько попыток сделано на самом деле
     */
    public int trainEpisodes(int count) {
        pause();
        int target = Math.min(Math.max(count, 0), MAX_EPISODES_AT_ONCE);
        long start = trainer.totalSteps();
        int done = 0;
        // текущая попытка могла начаться кнопкой «Шаг»: доводим её до конца, это тоже одна из N
        while (done < target && trainer.totalSteps() - start < STEP_BUDGET) {
            trainer.runEpisode();
            done++;
        }
        return done;
    }

    /** Кнопка «Сброс»: мышь забывает всё выученное; лабиринт и настройки те же. */
    public void reset() {
        pause();
        trainer = new Trainer(maze, settings);
    }

    /** Новые настройки из окна параметров: обучение начинается заново. */
    public void applySettings(Settings newSettings) {
        settings = Objects.requireNonNull(newSettings, "settings");
        reset();
    }

    /** Новый лабиринт (сгенерированный): обучение начинается заново. */
    public void setMaze(Maze newMaze) {
        maze = Objects.requireNonNull(newMaze, "maze");
        reset();
    }

    /** Лабиринт поменяли в редакторе: выученное больше не соответствует ему. */
    public void mazeEdited() {
        reset();
    }

    public Maze maze() {
        return maze;
    }

    public Settings settings() {
        return settings;
    }

    public Trainer trainer() {
        return trainer;
    }

    /** Что сообщить пользователю: сыр недостижим, мышь выучила маршрут или ничего. */
    public String statusMessage() {
        if (!maze.isCheeseReachable()) {
            return "Сыр недостижим: мышь не сможет его найти";
        }
        if (trainer.learnedAt() >= 0) {
            return "Мышь выучила маршрут за " + Texts.attempts(trainer.learnedAt());
        }
        return "";
    }
}
