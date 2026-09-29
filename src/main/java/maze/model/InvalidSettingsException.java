package maze.model;

import java.util.List;

/**
 * Настройки некорректны. Содержит сразу все найденные проблемы,
 * чтобы пользователь исправил их за один раз, а не по одной.
 */
public class InvalidSettingsException extends IllegalArgumentException {

    /** Одна проблема: какое поле и что с ним не так (понятным языком). */
    public record Problem(String field, String message) {
    }

    private final List<Problem> problems;

    public InvalidSettingsException(List<Problem> problems) {
        super("Некорректные настройки: " + String.join("; ", problems.stream().map(Problem::message).toList()));
        this.problems = List.copyOf(problems);
    }

    public List<Problem> problems() {
        return problems;
    }
}
