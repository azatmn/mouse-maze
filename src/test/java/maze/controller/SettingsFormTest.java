package maze.controller;

import maze.model.InvalidSettingsException;
import maze.model.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsFormTest {

    private static Map<String, String> defaultsWith(String key, String text) {
        Map<String, String> texts = new HashMap<>(SettingsForm.toTexts(Settings.defaults()));
        texts.put(key, text);
        return texts;
    }

    private static List<InvalidSettingsException.Problem> problems(Map<String, String> texts) {
        FormResult<Settings> result = SettingsForm.parse(texts);
        if (result instanceof FormResult.Invalid<Settings> invalid) {
            return invalid.problems();
        }
        return fail("форма должна быть отвергнута: " + result);
    }

    private static Settings valid(Map<String, String> texts) {
        FormResult<Settings> result = SettingsForm.parse(texts);
        if (result instanceof FormResult.Valid<Settings> v) {
            return v.value();
        }
        return fail("форма должна приниматься: " + result);
    }

    private static String onlyProblem(Map<String, String> texts, String expectedField) {
        List<InvalidSettingsException.Problem> list = problems(texts);
        assertEquals(1, list.size(), list.toString());
        assertEquals(expectedField, list.getFirst().field());
        return list.getFirst().message();
    }

    @Test
    void formHasFieldForEverySettingInSameOrder() {
        List<String> components = Arrays.stream(Settings.class.getRecordComponents()).map(RecordComponent::getName).toList();
        List<String> keys = SettingsForm.FIELDS.stream().map(FormField::key).toList();
        assertEquals(components, keys, "добавили параметр в Settings — добавьте поле в форму");
    }

    @Test
    void everyFieldHasHumanLabel() {
        for (FormField field : SettingsForm.FIELDS) {
            assertFalse(field.label().isBlank(), field.key());
            assertNotEquals(field.key(), field.label());
        }
    }

    @Test
    void decayIsTheOnlyYesNoField() {
        assertEquals(List.of("epsilonDecay"), SettingsForm.FIELDS.stream()
                .filter(f -> f.kind() == FormField.Kind.YES_NO).map(FormField::key).toList());
    }

    @Test
    void textsShowNumbersWithoutNoise() {
        Map<String, String> t = SettingsForm.toTexts(Settings.builder()
                .cheeseReward(100).alpha(0.5).epsilonEnd(0.01).stepReward(-1).epsilonDecay(true).seed(-7).build());
        assertEquals(SettingsForm.FIELDS.stream().map(FormField::key).toList(), List.copyOf(t.keySet()));
        assertEquals("100", t.get("cheeseReward"));
        assertEquals("0.5", t.get("alpha"));
        assertEquals("0.01", t.get("epsilonEnd"));
        assertEquals("-1", t.get("stepReward"));
        assertEquals("true", t.get("epsilonDecay"));
        assertEquals("-7", t.get("seed"));
    }

    @Test
    void defaultsSurviveRoundTrip() {
        assertEquals(Settings.defaults(), valid(SettingsForm.toTexts(Settings.defaults())));
    }

    @Test
    void everyFieldSurvivesRoundTrip() {
        Settings original = Settings.builder()
                .cheeseReward(123.25).waterReward(0.1).shockReward(-77.5).stepReward(-0.3).wallReward(-2)
                .alpha(0.07).gamma(0.999).epsilonStart(0.45).epsilonEnd(0.001).epsilonDecay(false)
                .decayEpisodes(11).maxSteps(12).stableEpisodes(13).seed(Long.MIN_VALUE)
                .build();
        assertEquals(original, valid(SettingsForm.toTexts(original)));
    }

    @Test
    void decimalCommaAndSpacesAccepted() {
        assertEquals(0.25, valid(defaultsWith("alpha", " 0,25 ")).alpha());
        assertEquals(0.5, valid(defaultsWith("alpha", ".5")).alpha());
        assertEquals(-3.5, valid(defaultsWith("shockReward", "-3.5")).shockReward());
        assertEquals(7, valid(defaultsWith("waterReward", "+7")).waterReward());
    }

    @ParameterizedTest(name = "«{0}» — не число")
    @ValueSource(strings = {"abc", "1e3", "NaN", "Infinity", "５", "٣", "1.2.3", "1,,2", "12 34", "--5", "0x10", "+", "-", ".", "пять"})
    void notANumberIsReported(String text) {
        assertTrue(onlyProblem(defaultsWith("alpha", text), "alpha").contains("нужно число"));
    }

    @ParameterizedTest(name = "«{0}» — не целое")
    @ValueSource(strings = {"2.5", "2,5", "abc", ""})
    void countsMustBeIntegers(String text) {
        String msg = onlyProblem(defaultsWith("maxSteps", text), "maxSteps");
        assertTrue(msg.contains("целое число") || msg.contains("не заполнено"), msg);
    }

    @Test
    void hugeNumbersReported() {
        assertTrue(onlyProblem(defaultsWith("maxSteps", "2147483648"), "maxSteps").contains("слишком большое"));
        assertTrue(onlyProblem(defaultsWith("seed", "9223372036854775808"), "seed").contains("слишком большое"));
        assertTrue(onlyProblem(defaultsWith("cheeseReward", "9".repeat(400)), "cheeseReward").contains("слишком большое"));
    }

    @Test
    void numberInRangeOfTypeButNotOfSettingsCheckedBySettings() {
        String msg = onlyProblem(defaultsWith("alpha", "5"), "alpha");
        assertTrue(msg.contains("допустимо"), msg);
    }

    @Test
    void yesNoField() {
        assertFalse(valid(defaultsWith("epsilonDecay", "false")).epsilonDecay());
        assertTrue(valid(defaultsWith("epsilonDecay", "true")).epsilonDecay());
        assertEquals("epsilonDecay", problems(defaultsWith("epsilonDecay", "да")).getFirst().field());
    }

    @Test
    void blankAndMissingReported() {
        assertTrue(onlyProblem(defaultsWith("gamma", "  "), "gamma").contains("не заполнено"));
        Map<String, String> texts = new HashMap<>(SettingsForm.toTexts(Settings.defaults()));
        texts.remove("wallReward");
        assertTrue(onlyProblem(texts, "wallReward").contains("не заполнено"));
    }

    @Test
    void messagesUseLabelAndSafeEcho() {
        String msg = onlyProblem(defaultsWith("alpha", "\u001b[2J" + "x".repeat(5000)), "alpha");
        assertTrue(msg.startsWith(SettingsForm.FIELDS.get(5).label()), msg);
        assertTrue(msg.chars().noneMatch(Character::isISOControl), msg);
        assertTrue(msg.length() < 200, "длина " + msg.length());
    }

    @Test
    void unreadableFieldsFirstThenSettingsRules() {
        Map<String, String> texts = defaultsWith("alpha", "abc");
        texts.put("gamma", "5");
        texts.put("seed", "x");
        assertEquals(List.of("alpha", "seed"), problems(texts).stream().map(InvalidSettingsException.Problem::field).toList());
        Map<String, String> ok = defaultsWith("gamma", "5");
        ok.put("cheeseReward", "-1");
        assertEquals(List.of("cheeseReward", "gamma"), problems(ok).stream().map(InvalidSettingsException.Problem::field).toList());
    }

    @Test
    void parserNeverThrows() {
        String[] nasty = {"\uD800", "🙂", "%s", "\\", "9".repeat(10_000), "-".repeat(50), "\u0000", null, "1" + ".".repeat(1000)};
        for (String key : List.of("alpha", "maxSteps", "seed", "epsilonDecay")) {
            for (String text : nasty) {
                Map<String, String> texts = defaultsWith(key, text);
                assertDoesNotThrow(() -> SettingsForm.parse(texts), key + " " + text);
            }
        }
    }
}
