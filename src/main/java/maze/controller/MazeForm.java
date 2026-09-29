package maze.controller;

import maze.model.MazeSpec;

import java.util.List;
import java.util.Map;

import static maze.controller.FormField.Kind.*;

/** Форма «Новый лабиринт» без JavaFX: тексты полей → {@link MazeSpec} или список проблем. */
public final class MazeForm {

    /** Поля в том же порядке, что и части {@link MazeSpec}. */
    public static final List<FormField> FIELDS = List.of(
            new FormField("width", "Ширина", INTEGER),
            new FormField("height", "Высота", INTEGER),
            new FormField("water", "Клеток с водой", INTEGER),
            new FormField("shock", "Клеток с током", INTEGER),
            new FormField("loopPercent", "Лишних проходов, %", INTEGER),
            new FormField("seed", "Зерно случайности (seed)", LONG));

    private MazeForm() {
    }

    public static Map<String, String> toTexts(MazeSpec s) {
        return Forms.texts(FIELDS, s.width(), s.height(), s.water(), s.shock(), s.loopPercent(), s.seed());
    }

    public static FormResult<MazeSpec> parse(Map<String, String> texts) {
        return Forms.parse(FIELDS, texts,
                v -> new MazeSpec((int) v[0], (int) v[1], (int) v[2], (int) v[3], (int) v[4], (long) v[5]));
    }
}
