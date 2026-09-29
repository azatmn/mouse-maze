package maze.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class SettingsTest {

    /** Поля, на которые пожаловалась проверка. */
    private static List<String> problems(UnaryOperator<Settings.Builder> change) {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> change.apply(Settings.builder()).build());
        return e.problems().stream().map(InvalidSettingsException.Problem::field).toList();
    }

    private static void ok(UnaryOperator<Settings.Builder> change) {
        assertDoesNotThrow(() -> change.apply(Settings.builder()).build());
    }

    @Test
    void defaultsAreValidAndMatchTask() {
        Settings s = Settings.defaults();
        // условие: сыр — самая большая награда, вода меньше, ток — штраф
        assertTrue(s.cheeseReward() > s.waterReward());
        assertTrue(s.waterReward() > 0);
        assertTrue(s.shockReward() < 0);
        assertTrue(s.stepReward() <= 0);
        assertTrue(s.wallReward() <= 0);
    }

    @Test
    void toBuilderKeepsEverything() {
        Settings s = Settings.builder()
                .cheeseReward(50).waterReward(5).shockReward(-20).stepReward(-0.5).wallReward(-2)
                .alpha(0.3).gamma(0.8).epsilonStart(0.4).epsilonEnd(0.05).epsilonDecay(false)
                .decayEpisodes(77).maxSteps(123).stableEpisodes(9).seed(-7)
                .build();
        assertEquals(s, s.toBuilder().build());
        assertEquals(50, s.cheeseReward());
        assertEquals(5, s.waterReward());
        assertEquals(-20, s.shockReward());
        assertEquals(-0.5, s.stepReward());
        assertEquals(-2, s.wallReward());
        assertEquals(0.3, s.alpha());
        assertEquals(0.8, s.gamma());
        assertEquals(0.4, s.epsilonStart());
        assertEquals(0.05, s.epsilonEnd());
        assertFalse(s.epsilonDecay());
        assertEquals(77, s.decayEpisodes());
        assertEquals(123, s.maxSteps());
        assertEquals(9, s.stableEpisodes());
        assertEquals(-7, s.seed());
    }

    @Test
    void rewardSigns() {
        assertEquals(List.of("cheeseReward"), problems(b -> b.cheeseReward(0)));
        assertEquals(List.of("waterReward"), problems(b -> b.waterReward(-1)));
        assertEquals(List.of("shockReward"), problems(b -> b.shockReward(1)));
        assertEquals(List.of("stepReward"), problems(b -> b.stepReward(0.1)));
        assertEquals(List.of("wallReward"), problems(b -> b.wallReward(0.1)));
        ok(b -> b.waterReward(0).shockReward(0).stepReward(0).wallReward(0));
    }

    @Test
    void waterMustBeSmallerThanCheese() {
        assertEquals(List.of("waterReward"), problems(b -> b.cheeseReward(10).waterReward(10)));
        ok(b -> b.cheeseReward(10).waterReward(9.99));
    }

    @Test
    void hugeRewardsRejected() {
        assertEquals(List.of("cheeseReward"), problems(b -> b.cheeseReward(Settings.MAX_REWARD + 1)));
        assertEquals(List.of("shockReward"), problems(b -> b.shockReward(-Settings.MAX_REWARD - 1)));
        assertEquals(List.of("stepReward"), problems(b -> b.stepReward(-Settings.MAX_REWARD - 1)));
        assertEquals(List.of("wallReward"), problems(b -> b.wallReward(-Settings.MAX_REWARD - 1)));
        ok(b -> b.cheeseReward(Settings.MAX_REWARD).shockReward(-Settings.MAX_REWARD));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void notNumbersRejectedEverywhere(double bad) {
        assertEquals(List.of("cheeseReward", "waterReward", "shockReward", "stepReward", "wallReward",
                        "alpha", "gamma", "epsilonStart", "epsilonEnd"),
                problems(b -> b.cheeseReward(bad).waterReward(bad).shockReward(bad).stepReward(bad).wallReward(bad)
                        .alpha(bad).gamma(bad).epsilonStart(bad).epsilonEnd(bad)));
    }

    @Test
    void alphaInZeroExclusiveToOne() {
        assertEquals(List.of("alpha"), problems(b -> b.alpha(0)));
        assertEquals(List.of("alpha"), problems(b -> b.alpha(1.01)));
        ok(b -> b.alpha(1));
        ok(b -> b.alpha(0.001));
    }

    @Test
    void gammaAndEpsilonInZeroToOne() {
        assertEquals(List.of("gamma"), problems(b -> b.gamma(-0.01)));
        assertEquals(List.of("gamma"), problems(b -> b.gamma(1.01)));
        assertEquals(List.of("epsilonStart"), problems(b -> b.epsilonStart(1.5).epsilonDecay(false)));
        assertEquals(List.of("epsilonStart"), problems(b -> b.epsilonStart(-0.1).epsilonEnd(0).epsilonDecay(false)));
        assertEquals(List.of("epsilonEnd"), problems(b -> b.epsilonEnd(-0.1)));
        ok(b -> b.gamma(0).gamma(1).epsilonStart(1).epsilonEnd(0));
    }

    @Test
    void decayingEpsilonMustNotGrow() {
        assertEquals(List.of("epsilonEnd"), problems(b -> b.epsilonDecay(true).epsilonStart(0.1).epsilonEnd(0.2)));
        ok(b -> b.epsilonDecay(false).epsilonStart(0.1).epsilonEnd(0.2));
        ok(b -> b.epsilonDecay(true).epsilonStart(0.2).epsilonEnd(0.2));
    }

    @Test
    void freeStepsWithoutDiscountForbidden() {
        // γ = 1 и бесплатные шаги: мыши незачем торопиться, она бегает туда-обратно у воды
        assertEquals(List.of("stepReward"), problems(b -> b.gamma(1).stepReward(0)));
        ok(b -> b.gamma(1).stepReward(-0.01));
        ok(b -> b.gamma(0.99).stepReward(0));
    }

    @Test
    void freeStepsMessageExplainsWhy() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> Settings.builder().gamma(1).stepReward(0).build());
        String msg = e.problems().getFirst().message();
        assertTrue(msg.contains("γ") && msg.contains("торопиться"), msg);
    }

    @Test
    void defaultsHaveNoWarnings() {
        assertEquals(List.of(), Settings.defaults().warnings());
    }

    @Test
    void cheapStepsWithFarSightWarn() {
        // замер: γ ≥ 0.99 и шаг дешевле 0.1 — мышь часто не выучивает маршрут; шаг 0 — при любом γ
        assertEquals(1, Settings.builder().gamma(0.99).stepReward(-0.09).build().warnings().size());
        assertEquals(1, Settings.builder().gamma(1).stepReward(-0.01).build().warnings().size());
        assertEquals(1, Settings.builder().gamma(0.5).stepReward(0).build().warnings().size());
        assertEquals(List.of(), Settings.builder().gamma(0.99).stepReward(-0.1).build().warnings());
        assertEquals(List.of(), Settings.builder().gamma(0.98).stepReward(-0.01).build().warnings());
    }

    @Test
    void warningExplainsInPlainWords() {
        String w = Settings.builder().gamma(1).stepReward(-0.01).build().warnings().getFirst();
        assertTrue(w.contains("может не выучить"), w);
    }

    @Test
    void countsInRange() {
        assertEquals(List.of("decayEpisodes"), problems(b -> b.decayEpisodes(0)));
        assertEquals(List.of("maxSteps"), problems(b -> b.maxSteps(0)));
        assertEquals(List.of("stableEpisodes"), problems(b -> b.stableEpisodes(0)));
        assertEquals(List.of("decayEpisodes"), problems(b -> b.decayEpisodes(Settings.MAX_COUNT + 1)));
        assertEquals(List.of("maxSteps"), problems(b -> b.maxSteps(Settings.MAX_COUNT + 1)));
        assertEquals(List.of("stableEpisodes"), problems(b -> b.stableEpisodes(Settings.MAX_COUNT + 1)));
        ok(b -> b.decayEpisodes(1).maxSteps(1).stableEpisodes(1));
        ok(b -> b.decayEpisodes(Settings.MAX_COUNT).maxSteps(Settings.MAX_COUNT).stableEpisodes(Settings.MAX_COUNT));
    }

    @Test
    void allProblemsReportedAtOnce() {
        List<String> fields = problems(b -> b.cheeseReward(-1).alpha(5).maxSteps(-3));
        assertEquals(List.of("cheeseReward", "alpha", "maxSteps"), fields);
    }

    @Test
    void messagesAreHumanReadable() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> Settings.builder().alpha(5).build());
        String msg = e.problems().getFirst().message();
        assertTrue(msg.contains("5"), msg);
        assertTrue(msg.contains("0") && msg.contains("1"), msg);
    }

    @Test
    void epsilonScheduleConstant() {
        Settings s = Settings.builder().epsilonDecay(false).epsilonStart(0.2).epsilonEnd(0.9).build();
        assertEquals(0.2, s.epsilonAt(0));
        assertEquals(0.2, s.epsilonAt(1000));
    }

    @Test
    void epsilonScheduleLinearThenFlat() {
        Settings s = Settings.builder().epsilonDecay(true).epsilonStart(0.5).epsilonEnd(0.1).decayEpisodes(100).build();
        assertEquals(0.5, s.epsilonAt(0), 1e-12);
        assertEquals(0.3, s.epsilonAt(50), 1e-12);
        assertEquals(0.1, s.epsilonAt(100), 1e-12);
        assertEquals(0.1, s.epsilonAt(5000), 1e-12);
        assertEquals(0.5 - 0.4 / 100, s.epsilonAt(1), 1e-12);
    }
}
