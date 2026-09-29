package maze.controller;

import maze.model.Settings;

import java.util.List;
import java.util.Map;

import static maze.controller.FormField.Kind.*;

/**
 * Форма настроек обучения без JavaFX: тексты из полей ввода превращаются в {@link Settings}
 * или в список понятных проблем. Окно параметров только показывает поля и результат.
 */
public final class SettingsForm {

    /** Поля в том же порядке, что и параметры в {@link Settings}. */
    public static final List<FormField> FIELDS = List.of(
            new FormField("cheeseReward", "Награда за сыр", DECIMAL),
            new FormField("waterReward", "Награда за воду", DECIMAL),
            new FormField("shockReward", "Награда за удар током", DECIMAL),
            new FormField("stepReward", "Награда за шаг", DECIMAL),
            new FormField("wallReward", "Награда за удар о стену", DECIMAL),
            new FormField("alpha", "Скорость обучения α", DECIMAL),
            new FormField("gamma", "Дисконтирование γ", DECIMAL),
            new FormField("epsilonStart", "Доля случайных шагов в начале", DECIMAL),
            new FormField("epsilonEnd", "Доля случайных шагов в конце", DECIMAL),
            new FormField("epsilonDecay", "Доля случайных шагов убывает", YES_NO),
            new FormField("decayEpisodes", "За сколько попыток убывает", INTEGER),
            new FormField("maxSteps", "Лимит шагов в попытке", INTEGER),
            new FormField("stableEpisodes", "Автостоп: попыток без изменений пути", INTEGER),
            new FormField("seed", "Зерно случайности (seed)", LONG));

    private SettingsForm() {
    }

    /** Значения настроек как тексты для полей формы, в порядке {@link #FIELDS}. */
    public static Map<String, String> toTexts(Settings s) {
        return Forms.texts(FIELDS,
                s.cheeseReward(), s.waterReward(), s.shockReward(), s.stepReward(), s.wallReward(),
                s.alpha(), s.gamma(), s.epsilonStart(), s.epsilonEnd(), s.epsilonDecay(),
                s.decayEpisodes(), s.maxSteps(), s.stableEpisodes(), s.seed());
    }

    public static FormResult<Settings> parse(Map<String, String> texts) {
        return Forms.parse(FIELDS, texts, v -> Settings.builder()
                .cheeseReward((double) v[0]).waterReward((double) v[1]).shockReward((double) v[2])
                .stepReward((double) v[3]).wallReward((double) v[4])
                .alpha((double) v[5]).gamma((double) v[6])
                .epsilonStart((double) v[7]).epsilonEnd((double) v[8]).epsilonDecay((boolean) v[9])
                .decayEpisodes((int) v[10]).maxSteps((int) v[11]).stableEpisodes((int) v[12])
                .seed((long) v[13])
                .build());
    }
}
