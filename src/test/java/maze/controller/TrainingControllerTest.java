package maze.controller;

import maze.model.Direction;
import maze.model.Maze;
import maze.model.MazeGenerator;
import maze.model.MazeSpec;
import maze.model.Position;
import maze.model.Settings;
import org.junit.jupiter.api.Test;

import static maze.controller.TrainingController.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainingControllerTest {

    private static final long SECOND = 1_000_000_000L;

    private static final Settings FAST = Settings.builder()
            .gamma(1).alpha(0.5).epsilonStart(0.5).epsilonEnd(0).decayEpisodes(100).stableEpisodes(5).maxSteps(200).seed(1)
            .build();

    private static Maze small() {
        return new Maze(3, 3);
    }

    private static TrainingController controller() {
        return new TrainingController(small(), FAST);
    }

    /** Коридор, который мышь выучивает за несколько десятков попыток. */
    private static TrainingController corridor() {
        return new TrainingController(new Maze(1, 3), FAST);
    }

    @Test
    void startsPausedAtFirstAttempt() {
        TrainingController c = controller();
        assertFalse(c.isRunning());
        assertEquals(1, c.trainer().episode());
        assertEquals(0, c.trainer().attempt().steps());
        assertEquals(DEFAULT_SPEED, c.getSpeed());
        assertTrue(c.isAutoStop());
        assertEquals("", c.statusMessage());
    }

    @Test
    void startPauseToggle() {
        TrainingController c = controller();
        c.start();
        c.start();
        assertTrue(c.isRunning());
        c.pause();
        c.pause();
        assertFalse(c.isRunning());
        c.toggleRunning();
        assertTrue(c.isRunning());
        c.toggleRunning();
        assertFalse(c.isRunning());
    }

    @Test
    void stepOncePausesAndMakesOneStep() {
        TrainingController c = controller();
        c.start();
        c.stepOnce();
        assertFalse(c.isRunning());
        assertEquals(1, c.trainer().attempt().steps());
    }

    @Test
    void tickMakesStepsBySpeed() {
        TrainingController c = new TrainingController(new Maze(20, 20), FAST);
        c.setSpeed(10);
        c.start();
        c.tick(0);                       // первый кадр только запоминает время
        assertEquals(0, totalSteps(c));
        c.tick(SECOND / 2);              // полсекунды при 10 шагах/с = 5 шагов
        assertEquals(5, totalSteps(c));
        c.tick(SECOND / 2 + SECOND / 20); // ещё 0.05 с = полшага — пока не шагаем
        assertEquals(5, totalSteps(c));
        c.tick(SECOND / 2 + SECOND / 10); // ещё полшага — набрался целый
        assertEquals(6, totalSteps(c));
    }

    @Test
    void noStepsWhilePaused() {
        TrainingController c = controller();
        c.tick(0);
        c.tick(10 * SECOND);
        assertEquals(0, totalSteps(c));
    }

    @Test
    void pauseTimeDoesNotTurnIntoSteps() {
        TrainingController c = new TrainingController(new Maze(20, 20), FAST);
        c.setSpeed(10);
        c.start();
        c.tick(0);
        c.pause();
        c.tick(100 * SECOND);
        c.start();
        c.tick(200 * SECOND);
        assertEquals(0, totalSteps(c));
        c.tick(200 * SECOND + SECOND / 10);
        assertEquals(1, totalSteps(c));
    }

    @Test
    void stepsPerTickLimited() {
        TrainingController c = new TrainingController(new Maze(50, 50), FAST.toBuilder().maxSteps(1_000_000).build());
        c.setSpeed(MAX_SPEED);
        c.start();
        c.tick(0);
        c.tick(SECOND);
        assertEquals(MAX_STEPS_PER_TICK, totalSteps(c));
        c.tick(SECOND + SECOND / 1000);   // долг не копится: 1 мс при 5000/с = 5 шагов
        assertEquals(MAX_STEPS_PER_TICK + 5, totalSteps(c));
    }

    @Test
    void timeGoingBackwardsIsIgnored() {
        TrainingController c = new TrainingController(new Maze(20, 20), FAST);
        c.setSpeed(10);
        c.start();
        c.tick(10 * SECOND);
        // часы ушли назад на 5.45 с: кадр только запоминает новое время.
        // Если бы -54.5 шага попали в долг, дробная половина съела бы следующий шаг.
        c.tick(4 * SECOND + SECOND * 55 / 100);
        assertEquals(0, totalSteps(c));
        c.tick(4 * SECOND + SECOND * 65 / 100);
        assertEquals(1, totalSteps(c), "отрицательное время не должно превращаться в долг");
    }

    @Test
    void hugeTimeJumpCountsAsOneSecond() {
        // 3e15 нс * 5000 шагов/с переполнило бы long и дало отрицательный долг
        TrainingController c = new TrainingController(new Maze(50, 50), FAST.toBuilder().maxSteps(1_000_000).build());
        c.setSpeed(MAX_SPEED);
        c.start();
        c.tick(0);
        c.tick(3_000_000_000_000_000L);
        assertEquals(MAX_STEPS_PER_TICK, totalSteps(c));
    }

    @Test
    void stepsPerTickLimitExactBoundary() {
        TrainingController c = new TrainingController(new Maze(50, 50), FAST.toBuilder().maxSteps(1_000_000).build());
        c.setSpeed(MAX_STEPS_PER_TICK + 1);
        c.start();
        c.tick(0);
        c.tick(SECOND);
        assertEquals(MAX_STEPS_PER_TICK, totalSteps(c));
    }

    @Test
    void secondStartDoesNotLoseTime() {
        TrainingController c = new TrainingController(new Maze(20, 20), FAST);
        c.setSpeed(10);
        c.start();
        c.tick(0);
        c.tick(SECOND / 2);
        c.start();
        c.tick(SECOND);
        assertEquals(10, totalSteps(c));
    }

    @Test
    void speedClamped() {
        TrainingController c = controller();
        c.setSpeed(0);
        assertEquals(MIN_SPEED, c.getSpeed());
        c.setSpeed(Integer.MAX_VALUE);
        assertEquals(MAX_SPEED, c.getSpeed());
    }

    @Test
    void autoStopPausesOnceWhenLearned() {
        TrainingController c = corridor();
        c.setSpeed(MAX_SPEED);
        c.start();
        long now = 0;
        for (int i = 0; i < 1000 && c.isRunning(); i++) {
            c.tick(now);
            now += SECOND / 60;
        }
        assertFalse(c.isRunning(), "мышь так и не выучила коридор");
        assertTrue(c.trainer().isLearned());
        assertEquals("Мышь выучила маршрут за " + Texts.attempts(c.trainer().learnedAt()), c.statusMessage());

        // продолжили — второй раз автостоп не срабатывает
        int learnedAt = c.trainer().learnedAt();
        c.start();
        for (int i = 0; i < 100; i++) {
            c.tick(now);
            now += SECOND / 60;
        }
        assertTrue(c.isRunning());
        assertTrue(c.trainer().finishedEpisodes() > learnedAt + 5);
    }

    @Test
    void withoutAutoStopKeepsRunning() {
        TrainingController c = corridor();
        c.setAutoStop(false);
        assertFalse(c.isAutoStop());
        c.setSpeed(MAX_SPEED);
        c.start();
        long now = 0;
        for (int i = 0; i < 300; i++) {
            c.tick(now);
            now += SECOND / 60;
        }
        assertTrue(c.isRunning());
        assertTrue(c.trainer().isLearned());
    }

    @Test
    void autoStopDuringStepsInsideOneTickStopsRightThere() {
        TrainingController c = corridor();
        c.setSpeed(MAX_SPEED);
        c.start();
        c.tick(0);
        long now = 0;
        while (c.isRunning()) {
            now += SECOND;
            c.tick(now);
        }
        // остановились сразу после попытки, на которой путь признан выученным
        assertEquals(c.trainer().learnedAt(), c.trainer().finishedEpisodes());
    }

    @Test
    void trainEpisodesRunsExactlyAndPauses() {
        TrainingController c = controller();
        c.start();
        assertEquals(40, c.trainEpisodes(40));
        assertFalse(c.isRunning());
        assertEquals(40, c.trainer().finishedEpisodes());
        assertEquals(0, c.trainEpisodes(0));
        assertEquals(0, c.trainEpisodes(-5));
        assertEquals(40, c.trainer().finishedEpisodes());
    }

    @Test
    void trainEpisodesStopsAtStepBudget() {
        // сыр недостижим: каждая попытка — 1 000 000 шагов; бюджета хватает на STEP_BUDGET шагов
        Maze closed = new Maze(1, 3);
        closed.setWall(new Position(0, 1), Direction.UP, true);
        TrainingController c = new TrainingController(closed, FAST.toBuilder().maxSteps(1_000_000).build());
        int done = c.trainEpisodes(1000);
        assertEquals(STEP_BUDGET / 1_000_000, done);
        assertEquals(done, c.trainer().finishedEpisodes());
        assertEquals(0, c.trainer().attempt().steps(), "обрывается между попытками, а не посреди");
    }

    @Test
    void trainEpisodesCapped() {
        TrainingController c = new TrainingController(new Maze(1, 2), FAST);
        assertEquals(MAX_EPISODES_AT_ONCE, c.trainEpisodes(Integer.MAX_VALUE));
    }

    @Test
    void resetForgetsLearningKeepsMaze() {
        TrainingController c = controller();
        Maze maze = c.maze();
        c.trainEpisodes(20);
        c.start();
        c.reset();
        assertFalse(c.isRunning());
        assertEquals(0, c.trainer().finishedEpisodes());
        assertSame(maze, c.maze());
        assertEquals(0, c.trainer().table().get(maze.start(), Direction.UP));
    }

    @Test
    void applySettingsRestartsTraining() {
        TrainingController c = controller();
        c.trainEpisodes(10);
        Settings other = FAST.toBuilder().seed(99).build();
        c.applySettings(other);
        assertSame(other, c.settings());
        assertSame(other, c.trainer().settings());
        assertEquals(0, c.trainer().finishedEpisodes());
    }

    @Test
    void setMazeRestartsTrainingOnNewMaze() {
        TrainingController c = controller();
        c.trainEpisodes(10);
        c.start();
        Maze m = MazeGenerator.generate(new MazeSpec(7, 5, 1, 1, 10, 3L));
        c.setMaze(m);
        assertFalse(c.isRunning());
        assertSame(m, c.maze());
        assertSame(m, c.trainer().maze());
        assertEquals(0, c.trainer().finishedEpisodes());
    }

    @Test
    void mazeEditedRestartsTraining() {
        TrainingController c = controller();
        c.trainEpisodes(10);
        c.start();
        c.mazeEdited();
        assertFalse(c.isRunning());
        assertEquals(0, c.trainer().finishedEpisodes());
    }

    @Test
    void unreachableCheeseReported() {
        Maze closed = new Maze(1, 3);
        closed.setWall(new Position(0, 1), Direction.UP, true);
        TrainingController c = new TrainingController(closed, FAST);
        assertEquals("Сыр недостижим: мышь не сможет его найти", c.statusMessage());
    }

    // ---------- «Пройти выученный путь» ----------

    /** Коридор, который уже выучен: путь к сыру известен. */
    private static TrainingController learnedCorridor() {
        TrainingController c = new TrainingController(new Maze(1, 4), FAST);
        c.trainEpisodes(200);
        assertTrue(c.trainer().bestPath().reachesCheese());
        return c;
    }

    @Test
    void replayRefusedWhenNothingLearned() {
        TrainingController c = controller();
        assertFalse(c.startReplay());
        assertFalse(c.isReplaying());
        assertNull(c.replay());
    }

    @Test
    void replayStartsAtStartAndPausesTraining() {
        TrainingController c = learnedCorridor();
        c.start();
        assertTrue(c.startReplay());
        assertTrue(c.isReplaying());
        assertFalse(c.isRunning());
        assertEquals(c.maze().start(), c.replay().position());
        assertEquals(0, c.replay().steps());
    }

    @Test
    void replayWalksBestPathBySpeedWithoutLearning() {
        TrainingController c = learnedCorridor();
        long trainedSteps = c.trainer().totalSteps();
        int episodes = c.trainer().finishedEpisodes();
        double q = c.trainer().table().get(c.maze().start(), Direction.UP);
        c.setSpeed(2);
        c.startReplay();
        c.tick(0);
        c.tick(SECOND / 2);                      // 0.5 с при 2 шагах/с — один шаг
        assertEquals(1, c.replay().steps());
        assertEquals(c.trainer().bestPath().cells().get(1), c.replay().position());
        c.tick(SECOND * 5);                      // остальные два шага и конец
        assertTrue(c.replay().reachedCheese());
        assertFalse(c.isReplaying(), "дошла — показ окончен");
        assertEquals(3, c.replay().steps());
        assertEquals(trainedSteps, c.trainer().totalSteps(), "мышь не обучалась");
        assertEquals(episodes, c.trainer().finishedEpisodes());
        assertEquals(q, c.trainer().table().get(c.maze().start(), Direction.UP));
    }

    @Test
    void replayStartsItsOwnClock() {
        // до показа шло обучение и часы уже тикали; время до нажатия кнопки не превращается в шаги показа
        TrainingController c = learnedCorridor();
        c.setSpeed(2);
        c.start();
        c.tick(0);
        c.startReplay();
        c.tick(10 * SECOND);
        assertEquals(0, c.replay().steps());
    }

    @Test
    void replayIsDeterministic() {
        TrainingController c = learnedCorridor();
        c.setSpeed(MAX_SPEED);
        c.startReplay();
        c.tick(0);
        c.tick(SECOND);
        double first = c.replay().score();
        c.startReplay();
        c.tick(2 * SECOND);
        c.tick(3 * SECOND);
        assertEquals(first, c.replay().score());
        assertEquals(97, first, 1e-9, "три шага по коридору: −1 −1 +99");
    }

    @Test
    void replayEndsWhenTrainingOrMazeChanges() {
        TrainingController c = learnedCorridor();
        c.startReplay();
        c.start();
        assertFalse(c.isReplaying());
        assertNull(c.replay());

        c.startReplay();
        c.stepOnce();
        assertNull(c.replay());
        c.startReplay();
        c.trainEpisodes(1);
        assertNull(c.replay());
        c.startReplay();
        c.reset();
        assertNull(c.replay());
    }

    @Test
    void stopReplayClears() {
        TrainingController c = learnedCorridor();
        c.startReplay();
        c.stopReplay();
        assertFalse(c.isReplaying());
        assertNull(c.replay());
    }

    @Test
    void replayMessages() {
        TrainingController c = learnedCorridor();
        c.setSpeed(MAX_SPEED);
        c.startReplay();
        assertEquals("Мышь идёт по выученному пути: шаг 0", c.statusMessage());
        c.tick(0);
        c.tick(SECOND);
        assertEquals("Мышь прошла выученный путь: 3 шага, очки +97", c.statusMessage());
    }

        private static long totalSteps(TrainingController c) {
        // шаги законченных попыток не хранятся, но в этих тестах попытки не заканчиваются
        assertEquals(0, c.trainer().finishedEpisodes());
        return c.trainer().attempt().steps();
    }
}
