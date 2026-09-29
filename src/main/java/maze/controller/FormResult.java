package maze.controller;

import maze.model.InvalidSettingsException.Problem;

import java.util.List;

/** Результат разбора формы: готовое значение или список понятных проблем. */
public sealed interface FormResult<T> {

    record Valid<T>(T value) implements FormResult<T> {
    }

    record Invalid<T>(List<Problem> problems) implements FormResult<T> {
        public Invalid {
            problems = List.copyOf(problems);
        }
    }
}
