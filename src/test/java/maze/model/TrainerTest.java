package maze.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static maze.model.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainerTest {

    private static Position p(int x, int y) {
        return new Position(x, y);
    }

    /** Настройки для точных проверок: γ = 1, поэтому ценность пути — просто сумма наград. */
    private static final Settings EXACT = Settings.builder()
            .cheeseReward(100).waterReward(10).shockReward(-50).stepReward(-1).wallReward(-5)
            .alpha(0.5).gamma(1).epsilonDecay(true).epsilonStart(0.5).epsilonEnd(0).decayEpisodes(300)
            .maxSteps(200).stableEpisodes(20).seed(3)
            .build();

    /** Коридор 1 x n: старт снизу, сыр сверху. */
    private static Maze corridor(int n) {
        return new Maze(1, n);
    }

    @Nested
    class Bookkeeping {

        @Test
        void freshTrainer() {
            Trainer t = new Trainer(corridor(3), EXACT);
            assertEquals(1, t.episode());
            assertEquals(0, t.finishedEpisodes());
            assertTrue(t.history().isEmpty());
            assertEquals(p(0, 2), t.attempt().position());
            assertFalse(t.isLearned());
            assertEquals(-1, t.learnedAt());
            assertEquals(0.5, t.epsilon());
        }

        @Test
        void stepMovesCurrentAttempt() {
            Trainer t = new Trainer(new Maze(3, 3), EXACT);
            StepResult r = t.step();
            assertEquals(p(0, 2), r.from());
            assertEquals(1, t.attempt().steps());
            assertEquals(r.to(), t.attempt().position());
        }

        @Test
        void finishedAttemptGoesToHistoryAndNewOneStarts() {
            // коридор из двух клеток: любой шаг вверх — сыр
            Trainer t = new Trainer(corridor(2), EXACT.toBuilder().epsilonStart(0).build());
            t.table().set(p(0, 1), UP, 1);
            StepResult r = t.step();
            assertTrue(r.terminal());
            assertEquals(List.of(99.0), t.history());
            assertEquals(2, t.episode());
            assertEquals(1, t.finishedEpisodes());
            assertEquals(p(0, 1), t.attempt().position(), "новая попытка — снова со старта");
            assertEquals(0, t.attempt().steps());
            assertEquals(99, t.lastScore());
        }

        @Test
        void runEpisodeFinishesExactlyOne() {
            Trainer t = new Trainer(new Maze(4, 4), EXACT);
            t.step();
            t.runEpisode();
            assertEquals(1, t.history().size());
            t.runEpisode();
            assertEquals(2, t.history().size());
        }

        @Test
        void runEpisodesAddsN() {
            Trainer t = new Trainer(new Maze(4, 4), EXACT);
            t.runEpisodes(25);
            assertEquals(25, t.history().size());
            assertEquals(26, t.episode());
            assertThrows(IllegalArgumentException.class, () -> t.runEpisodes(-1));
            t.runEpisodes(0);
            assertEquals(25, t.history().size());
        }

        @Test
        void historyIsReadOnly() {
            Trainer t = new Trainer(new Maze(2, 2), EXACT);
            t.runEpisode();
            assertThrows(UnsupportedOperationException.class, () -> t.history().add(1.0));
        }

        @Test
        void lastScoreIsNaNBeforeFirstAttempt() {
            assertTrue(Double.isNaN(new Trainer(new Maze(2, 2), EXACT).lastScore()));
        }

        @Test
        void stepLimitFinishesAttempt() {
            Maze closed = corridor(3);
            closed.setWall(p(0, 1), UP, true);
            Trainer t = new Trainer(closed, EXACT.toBuilder().maxSteps(7).build());
            t.runEpisode();
            assertEquals(1, t.history().size());
            assertEquals(2, t.episode());
        }

        @Test
        void epsilonFollowsSchedule() {
            Trainer t = new Trainer(new Maze(3, 3), EXACT);
            t.runEpisodes(150);
            assertEquals(EXACT.epsilonAt(150), t.epsilon(), 1e-12);
            assertEquals(0.25, t.epsilon(), 1e-12);
        }

        @Test
        void sameSeedSameStoryDifferentSeedDifferent() {
            Maze m = MazeGenerator.generate(new MazeSpec(8, 8, 3, 3, 20, 1L));
            Trainer a = new Trainer(m, EXACT);
            Trainer b = new Trainer(m, EXACT);
            Trainer c = new Trainer(m, EXACT.toBuilder().seed(4).build());
            a.runEpisodes(30);
            b.runEpisodes(30);
            c.runEpisodes(30);
            assertEquals(a.history(), b.history());
            assertNotEquals(a.history(), c.history());
        }

        @Test
        void mazeUntouched() {
            Maze m = MazeGenerator.generate(new MazeSpec(6, 6, 3, 3, 20, 2L));
            Maze before = m.copy();
            new Trainer(m, EXACT).runEpisodes(50);
            assertEquals(before, m);
        }
    }

    @Nested
    class Learning {

        @Test
        void corridorLearnedExactly() {
            // коридор 1x4: три шага вверх. С γ = 1 число у стрелки = сумма до конца: -1 -1 +99 = 97
            Trainer t = new Trainer(corridor(4), EXACT);
            t.runEpisodes(400);
            GreedyPath path = t.bestPath();
            assertTrue(path.reachesCheese());
            assertEquals(List.of(p(0, 3), p(0, 2), p(0, 1), p(0, 0)), path.cells());
            assertEquals(97, t.table().get(p(0, 3), UP), 1e-6);
            assertEquals(99, t.table().get(p(0, 1), UP), 1e-6);
        }

        @Test
        void avoidsShockWhenDetourIsCheaper() {
            // 5x2: старт (0,1), сыр (4,1), ток (2,1). Прямо: -4 - 50 + 100 = 46. В обход по верху: -6 + 100 = 94
            Maze m = new Maze(5, 2);
            m.setStart(p(0, 1));
            m.setCheese(p(4, 1));
            m.setItem(p(2, 1), CellType.SHOCK);
            Trainer t = new Trainer(m, EXACT);
            t.runEpisodes(500);
            GreedyPath path = t.bestPath();
            assertTrue(path.reachesCheese());
            assertFalse(path.cells().contains(p(2, 1)), path.cells().toString());
            assertEquals(7, path.cells().size());
        }

        @Test
        void takesShockWhenItIsCheap() {
            // тот же лабиринт, но ток -0.5: прямо -4 - 0.5 + 100 = 95.5 > 94 в обход
            Maze m = new Maze(5, 2);
            m.setStart(p(0, 1));
            m.setCheese(p(4, 1));
            m.setItem(p(2, 1), CellType.SHOCK);
            Trainer t = new Trainer(m, EXACT.toBuilder().shockReward(-0.5).build());
            t.runEpisodes(500);
            assertEquals(List.of(p(0, 1), p(1, 1), p(2, 1), p(3, 1), p(4, 1)), t.bestPath().cells());
        }

        @Test
        void detoursForWater() {
            // вода (2,0) на обходе: прямо -4 + 100 = 96, через воду -6 + 10 + 100 = 104
            Maze m = new Maze(5, 2);
            m.setStart(p(0, 1));
            m.setCheese(p(4, 1));
            m.setItem(p(2, 0), CellType.WATER);
            Trainer t = new Trainer(m, EXACT);
            t.runEpisodes(500);
            GreedyPath path = t.bestPath();
            assertTrue(path.reachesCheese());
            assertTrue(path.cells().contains(p(2, 0)), path.cells().toString());
        }

        @ParameterizedTest
        @ValueSource(longs = {1, 2, 3, 4, 5})
        void findsShortestPathInRandomMaze(long seed) {
            // без воды и тока лучший путь — кратчайший; мышь карты не видит, но должна прийти к той же длине
            Maze m = MazeGenerator.generate(new MazeSpec(10, 10, 0, 0, 20, seed));
            Settings s = EXACT.toBuilder().gamma(0.99).decayEpisodes(400).maxSteps(1000).seed(seed).build();
            Trainer t = new Trainer(m, s);
            t.runEpisodes(600);
            GreedyPath path = t.bestPath();
            assertTrue(path.reachesCheese(), "seed " + seed);
            assertEquals(m.shortestPath().size(), path.cells().size(), "seed " + seed);
        }

        @Test
        void scoresGrowWithLearning() {
            Maze m = MazeGenerator.generate(new MazeSpec(10, 10, 3, 5, 20, 8L));
            Trainer t = new Trainer(m, EXACT.toBuilder().gamma(0.99).maxSteps(1000).build());
            t.runEpisodes(600);
            List<Double> h = t.history();
            double first = h.subList(0, 20).stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            double last = h.subList(580, 600).stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            assertTrue(last > first + 100, "первые " + first + ", последние " + last);
        }
    }

    @Nested
    class AutoStop {

        @Test
        void learnedAfterStablePath() {
            Trainer t = new Trainer(corridor(4), EXACT.toBuilder().stableEpisodes(10).build());
            t.runEpisodes(400);
            assertTrue(t.isLearned());
            assertTrue(t.learnedAt() >= 10, "learnedAt " + t.learnedAt());
            assertTrue(t.learnedAt() <= 400);
        }

        @Test
        void notLearnedBeforeEnoughStableAttempts() {
            Trainer t = new Trainer(corridor(2), EXACT.toBuilder().stableEpisodes(5).epsilonStart(0).build());
            t.runEpisodes(4);
            assertFalse(t.isLearned(), "путь стабилен только 4 попытки из 5");
            t.runEpisode();
            assertTrue(t.isLearned());
            assertEquals(5, t.learnedAt());
        }

        @Test
        void learnedAtIsRememberedOnce() {
            Trainer t = new Trainer(corridor(2), EXACT.toBuilder().stableEpisodes(3).epsilonStart(0).build());
            t.runEpisodes(20);
            assertEquals(3, t.learnedAt());
        }

        @Test
        void brokenPathForgetsLearned() {
            Trainer t = new Trainer(corridor(2), EXACT.toBuilder().stableEpisodes(3).epsilonStart(0).build());
            t.runEpisodes(5);
            assertTrue(t.isLearned());
            // портим таблицу: лучшая стрелка со старта — в стену, путь теперь петля
            t.table().set(p(0, 1), UP, -1e9);
            t.table().set(p(0, 1), DOWN, 1e9);
            t.runEpisode();
            assertFalse(t.bestPath().reachesCheese());
            assertFalse(t.isLearned());
            assertEquals(3, t.learnedAt(), "первое выучивание не забывается");
        }

        @Test
        void changedPathStartsCountingAgain() {
            // 3x2: старт (0,1), сыр (2,0). Путь A — низом, путь B — верхом. α крошечная, чтобы обучение не мешало
            Maze m = new Maze(3, 2);
            Trainer t = new Trainer(m, EXACT.toBuilder().alpha(0.001).epsilonStart(0).stableEpisodes(3).build());
            t.table().set(p(0, 1), RIGHT, 1000);
            t.table().set(p(1, 1), RIGHT, 1000);
            t.table().set(p(2, 1), UP, 1000);
            t.runEpisodes(2);
            assertEquals(List.of(p(0, 1), p(1, 1), p(2, 1), p(2, 0)), t.bestPath().cells());
            assertFalse(t.isLearned(), "путь A стабилен 2 попытки из 3");

            t.table().set(p(0, 1), UP, 2000);
            t.table().set(p(0, 0), RIGHT, 2000);
            t.table().set(p(1, 0), RIGHT, 2000);
            t.runEpisode();
            assertEquals(List.of(p(0, 1), p(0, 0), p(1, 0), p(2, 0)), t.bestPath().cells());
            assertFalse(t.isLearned(), "путь сменился — счёт начинается заново");
            t.runEpisodes(2);
            assertTrue(t.isLearned());
        }

        @Test
        void neverLearnedWhenCheeseUnreachable() {
            Maze closed = corridor(3);
            closed.setWall(p(0, 1), UP, true);
            Trainer t = new Trainer(closed, EXACT.toBuilder().maxSteps(30).stableEpisodes(2).build());
            t.runEpisodes(50);
            assertFalse(t.isLearned());
            assertEquals(-1, t.learnedAt());
            assertFalse(t.bestPath().reachesCheese());
        }
    }
}
