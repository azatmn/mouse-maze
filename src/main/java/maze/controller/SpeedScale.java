package maze.controller;

/**
 * Перевод положения слайдера скорости в шаги в секунду и обратно.
 * Шкала логарифмическая: на линейной от 1 до 5000 медленные скорости заняли бы крошечный кусочек слайдера.
 */
public final class SpeedScale {

    public static final double SLIDER_MIN = 0;
    public static final double SLIDER_MAX = 100;

    private static final double LOG_MIN = Math.log(TrainingController.MIN_SPEED);
    private static final double LOG_MAX = Math.log(TrainingController.MAX_SPEED);

    private SpeedScale() {
    }

    /** Положение слайдера → скорость. */
    public static int toSpeed(double slider) {
        double t = (slider - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN);
        long speed = Math.round(Math.exp(LOG_MIN + t * (LOG_MAX - LOG_MIN)));
        // За краями слайдера exp даёт почти 0 или бесконечность, NaN округляется в 0 — всё прижимается к диапазону
        return (int) Math.clamp(speed, TrainingController.MIN_SPEED, TrainingController.MAX_SPEED);
    }

    /** Скорость → положение слайдера. */
    public static double toSlider(int speed) {
        int clamped = Math.clamp(speed, TrainingController.MIN_SPEED, TrainingController.MAX_SPEED);
        return SLIDER_MIN + (Math.log(clamped) - LOG_MIN) / (LOG_MAX - LOG_MIN) * (SLIDER_MAX - SLIDER_MIN);
    }
}
