package maze.controller;

import maze.model.InvalidSettingsException;
import maze.model.MazeSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MazeFormTest {

    private static final MazeSpec SPEC = new MazeSpec(12, 9, 3, 4, 10, 42L);

    private static Map<String, String> with(String key, String text) {
        Map<String, String> texts = new HashMap<>(MazeForm.toTexts(SPEC));
        texts.put(key, text);
        return texts;
    }

    private static List<String> fields(Map<String, String> texts) {
        if (MazeForm.parse(texts) instanceof FormResult.Invalid<MazeSpec> invalid) {
            return invalid.problems().stream().map(InvalidSettingsException.Problem::field).toList();
        }
        return fail("форма должна быть отвергнута");
    }

    @Test
    void formHasFieldForEveryPartOfSpec() {
        List<String> components = Arrays.stream(MazeSpec.class.getRecordComponents()).map(RecordComponent::getName).toList();
        assertEquals(components, MazeForm.FIELDS.stream().map(FormField::key).toList());
        for (FormField f : MazeForm.FIELDS) {
            assertNotEquals(f.key(), f.label());
            assertNotEquals(FormField.Kind.DECIMAL, f.kind(), "в лабиринте всё целое");
        }
    }

    @Test
    void roundTrip() {
        assertEquals(new FormResult.Valid<>(SPEC), MazeForm.parse(MazeForm.toTexts(SPEC)));
        MazeSpec odd = new MazeSpec(1, 100, 0, 98, 100, Long.MAX_VALUE);
        assertEquals(new FormResult.Valid<>(odd), MazeForm.parse(MazeForm.toTexts(odd)));
    }

    @Test
    void notIntegersReported() {
        assertEquals(List.of("width"), fields(with("width", "12.5")));
        assertEquals(List.of("water"), fields(with("water", "")));
        assertEquals(List.of("seed"), fields(with("seed", "abc")));
        assertEquals(List.of("loopPercent"), fields(with("loopPercent", "10%")));
    }

    @Test
    void specRulesReported() {
        assertEquals(List.of("width"), fields(with("width", "0")));
        assertEquals(List.of("height"), fields(with("height", "1000")));
        Map<String, String> t = with("water", "60");
        t.put("shock", "60");
        assertEquals(List.of("shock"), fields(t));
    }

    @Test
    void neverThrows() {
        for (FormField f : MazeForm.FIELDS) {
            for (String text : new String[]{null, "\uD800", "9".repeat(5000), "-", "\u0000"}) {
                Map<String, String> texts = with(f.key(), text);
                assertDoesNotThrow(() -> MazeForm.parse(texts));
            }
        }
    }
}
