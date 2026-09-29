package maze.controller;

import maze.model.InvalidSettingsException;
import maze.model.InvalidSettingsException.Problem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Общая часть форм: сначала читаются все поля, потом проверяются правила самого объекта. */
final class Forms {

    private Forms() {
    }

    /**
     * Если хоть одно поле не прочиталось — возвращаются ошибки чтения (по порядку полей).
     * Иначе значения отдаются build, а его {@link InvalidSettingsException} превращается в список проблем.
     */
    static <T> FormResult<T> parse(List<FormField> fields, Map<String, String> texts, Function<Object[], T> build) {
        List<Problem> problems = new ArrayList<>();
        Object[] values = new Object[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            FormField field = fields.get(i);
            switch (field.read(texts.get(field.key()))) {
                case FormField.Read.Value(Object value) -> values[i] = value;
                case FormField.Read.Error(String message) -> problems.add(new Problem(field.key(), message));
            }
        }
        if (!problems.isEmpty()) {
            return new FormResult.Invalid<>(problems);
        }
        try {
            return new FormResult.Valid<>(build.apply(values));
        } catch (InvalidSettingsException e) {
            return new FormResult.Invalid<>(e.problems());
        }
    }

    static Map<String, String> texts(List<FormField> fields, Object... values) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            texts.put(fields.get(i).key(), FormField.text(values[i]));
        }
        return texts;
    }
}
