package maze.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Все параметры обучения в одном месте: награды среды, параметры Q-learning, лимиты.
 * Объект неизменяемый: создаётся через {@link Builder} и проверяется при создании,
 * поэтому некорректные настройки не могут попасть в модель.
 *
 * @param cheeseReward   награда за сыр (+Z), больше нуля
 * @param waterReward    награда за воду (+x), меньше награды за сыр
 * @param shockReward    награда за удар током (−y), не больше нуля
 * @param stepReward     плата за каждый шаг, не больше нуля: чтобы короткий путь был выгоднее длинного
 * @param wallReward     награда за удар о стену (мышь остаётся на месте), не больше нуля
 * @param alpha          α — насколько сильно число сдвигается к новой оценке за один шаг, (0, 1]
 * @param gamma          γ — во сколько раз награда на шаг дальше ценится меньше, [0, 1]
 * @param epsilonStart   доля случайных шагов в начале обучения
 * @param epsilonEnd     доля случайных шагов после {@code decayEpisodes} попыток (если она убывает)
 * @param epsilonDecay   убывает ли доля случайных шагов; если нет — всегда {@code epsilonStart}
 * @param decayEpisodes  за сколько попыток доля случайных шагов доходит до {@code epsilonEnd}
 * @param maxSteps       лимит шагов в одной попытке
 * @param stableEpisodes сколько попыток подряд лучший путь не должен меняться, чтобы считать его выученным
 * @param seed           зерно генератора случайных шагов мыши
 */
public record Settings(
        double cheeseReward,
        double waterReward,
        double shockReward,
        double stepReward,
        double wallReward,
        double alpha,
        double gamma,
        double epsilonStart,
        double epsilonEnd,
        boolean epsilonDecay,
        int decayEpisodes,
        int maxSteps,
        int stableEpisodes,
        long seed) {

    public static final double MAX_REWARD = 1_000_000;
    public static final int MAX_COUNT = 1_000_000;

    public Settings {
        Checker check = new Checker();

        boolean cheeseOk = check.range("cheeseReward", "Награда за сыр", cheeseReward, 0, false, MAX_REWARD);
        boolean waterOk = check.range("waterReward", "Награда за воду", waterReward, 0, true, MAX_REWARD);
        if (cheeseOk && waterOk && waterReward >= cheeseReward) {
            check.fail("waterReward", "Награда за воду (" + format(waterReward)
                    + ") должна быть меньше награды за сыр (" + format(cheeseReward) + ")");
        }
        check.range("shockReward", "Награда за удар током", shockReward, -MAX_REWARD, true, 0);
        boolean stepOk = check.range("stepReward", "Награда за шаг", stepReward, -MAX_REWARD, true, 0);
        check.range("wallReward", "Награда за удар о стену", wallReward, -MAX_REWARD, true, 0);

        check.range("alpha", "Скорость обучения α", alpha, 0, false, 1);
        boolean gammaOk = check.range("gamma", "Коэффициент дисконтирования γ", gamma, 0, true, 1);
        if (stepOk && gammaOk && gamma == 1 && stepReward == 0) {
            // без скидки на даль и без платы за шаг длинный путь не хуже короткого:
            // мышь бегает туда-обратно у воды (выпитую воду она не помнит) и не идёт к сыру
            check.fail("stepReward", "Если γ = 1, награда за шаг должна быть меньше 0, "
                    + "иначе мыши незачем торопиться к сыру");
        }
        boolean startOk = check.range("epsilonStart", "Доля случайных шагов в начале", epsilonStart, 0, true, 1);
        boolean endOk = check.range("epsilonEnd", "Доля случайных шагов в конце", epsilonEnd, 0, true, 1);
        if (startOk && endOk && epsilonDecay && epsilonEnd > epsilonStart) {
            check.fail("epsilonEnd", "Доля случайных шагов убывает, поэтому в конце (" + format(epsilonEnd)
                    + ") должна быть не больше, чем в начале (" + format(epsilonStart) + ")");
        }

        check.count("decayEpisodes", "Число попыток до конечной доли случайных шагов", decayEpisodes);
        check.count("maxSteps", "Лимит шагов в попытке", maxSteps);
        check.count("stableEpisodes", "Число попыток без изменений пути для автостопа", stableEpisodes);

        check.throwIfFailed();
    }

    /**
     * Не ошибка, но мышь при таких настройках может не выучить маршрут. Пороги — из замера
     * на 12 случайных лабиринтах: при бесплатных шагах или при γ ≥ 0.99 и шаге дешевле 0.1
     * мышь не знает, какая дальняя вода уже выпита, её оценки раздуваются и ничем не гасятся.
     */
    public List<String> warnings() {
        if (stepReward == 0 || (gamma >= 0.99 && stepReward > -0.1)) {
            return List.of("Шаги почти бесплатные: мышь может не выучить маршрут. "
                    + "Обычно помогает награда за шаг −0.5 или −1.");
        }
        return List.of();
    }

    /** Доля случайных шагов в попытке, перед которой закончено {@code finished} попыток. */
    public double epsilonAt(int finished) {
        if (!epsilonDecay) {
            return epsilonStart;
        }
        double progress = Math.min(1.0, (double) finished / decayEpisodes);
        return epsilonStart + (epsilonEnd - epsilonStart) * progress;
    }

    private static String format(double v) {
        return v == Math.rint(v) && Math.abs(v) < 1e15 ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Копит найденные проблемы, чтобы сообщить обо всех сразу. */
    private static final class Checker {
        private final List<InvalidSettingsException.Problem> problems = new ArrayList<>();

        /** Проверяет, что value в (min, max] или [min, max]. Возвращает true, если всё в порядке. */
        boolean range(String field, String title, double value, double min, boolean minIncluded, double max) {
            // NaN не проходит ни одно сравнение, бесконечности — за границами, поэтому отсекаются здесь же
            boolean aboveMin = minIncluded ? value >= min : value > min;
            if (aboveMin && value <= max) {
                return true;
            }
            String allowed = (minIncluded ? "от " : "больше ") + format(min) + " до " + format(max);
            String given = Double.isFinite(value) ? format(value) : "не число";
            fail(field, title + ": допустимо " + allowed + ", указано " + given);
            return false;
        }

        void count(String field, String title, int value) {
            if (value < 1 || value > MAX_COUNT) {
                fail(field, title + ": допустимо от 1 до " + MAX_COUNT + ", указано " + value);
            }
        }

        void fail(String field, String message) {
            problems.add(new InvalidSettingsException.Problem(field, message));
        }

        void throwIfFailed() {
            if (!problems.isEmpty()) {
                throw new InvalidSettingsException(problems);
            }
        }
    }

    public static Settings defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Билдер с текущими значениями — удобно поменять пару полей у готовых настроек. */
    public Builder toBuilder() {
        return new Builder()
                .cheeseReward(cheeseReward).waterReward(waterReward).shockReward(shockReward)
                .stepReward(stepReward).wallReward(wallReward)
                .alpha(alpha).gamma(gamma)
                .epsilonStart(epsilonStart).epsilonEnd(epsilonEnd).epsilonDecay(epsilonDecay)
                .decayEpisodes(decayEpisodes).maxSteps(maxSteps).stableEpisodes(stableEpisodes)
                .seed(seed);
    }

    /** Пошаговая сборка настроек: меняем только нужные поля, остальные берутся по умолчанию. */
    public static final class Builder {
        private double cheeseReward = 100;
        private double waterReward = 10;
        private double shockReward = -50;
        private double stepReward = -1;
        private double wallReward = -5;
        private double alpha = 0.5;
        private double gamma = 0.95;
        private double epsilonStart = 0.3;
        private double epsilonEnd = 0.01;
        private boolean epsilonDecay = true;
        private int decayEpisodes = 300;
        private int maxSteps = 1000;
        private int stableEpisodes = 50;
        private long seed = 42;

        private Builder() {
        }

        public Builder cheeseReward(double v) {
            cheeseReward = v;
            return this;
        }

        public Builder waterReward(double v) {
            waterReward = v;
            return this;
        }

        public Builder shockReward(double v) {
            shockReward = v;
            return this;
        }

        public Builder stepReward(double v) {
            stepReward = v;
            return this;
        }

        public Builder wallReward(double v) {
            wallReward = v;
            return this;
        }

        public Builder alpha(double v) {
            alpha = v;
            return this;
        }

        public Builder gamma(double v) {
            gamma = v;
            return this;
        }

        public Builder epsilonStart(double v) {
            epsilonStart = v;
            return this;
        }

        public Builder epsilonEnd(double v) {
            epsilonEnd = v;
            return this;
        }

        public Builder epsilonDecay(boolean v) {
            epsilonDecay = v;
            return this;
        }

        public Builder decayEpisodes(int v) {
            decayEpisodes = v;
            return this;
        }

        public Builder maxSteps(int v) {
            maxSteps = v;
            return this;
        }

        public Builder stableEpisodes(int v) {
            stableEpisodes = v;
            return this;
        }

        public Builder seed(long v) {
            seed = v;
            return this;
        }

        public Settings build() {
            return new Settings(cheeseReward, waterReward, shockReward, stepReward, wallReward,
                    alpha, gamma, epsilonStart, epsilonEnd, epsilonDecay,
                    decayEpisodes, maxSteps, stableEpisodes, seed);
        }
    }
}
