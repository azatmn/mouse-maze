package maze.model;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static maze.model.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

class MouseTest {

    private static final Position HERE = new Position(1, 1);
    private static final Position THERE = new Position(2, 1);

    private static Map<Direction, Integer> choices(QTable q, double epsilon, int times) {
        Mouse mouse = new Mouse(q, new Random(5));
        Map<Direction, Integer> n = new EnumMap<>(Direction.class);
        for (int i = 0; i < times; i++) {
            n.merge(mouse.choose(HERE, epsilon), 1, Integer::sum);
        }
        return n;
    }

    @Test
    void greedyTakesBiggestNumber() {
        QTable q = new QTable(3, 3);
        q.set(HERE, DOWN, 3);
        q.set(HERE, LEFT, 2);
        assertEquals(Map.of(DOWN, 1000), choices(q, 0, 1000));
    }

    @Test
    void greedyBreaksTiesRandomly() {
        QTable q = new QTable(3, 3);
        q.set(HERE, UP, 4);
        q.set(HERE, LEFT, 4);
        q.set(HERE, RIGHT, -1);
        Map<Direction, Integer> n = choices(q, 0, 2000);
        assertEquals(2, n.size(), n.toString());
        assertTrue(n.get(UP) > 800 && n.get(LEFT) > 800, n.toString());
    }

    @Test
    void epsilonOneIsUniform() {
        QTable q = new QTable(3, 3);
        q.set(HERE, RIGHT, 100);
        Map<Direction, Integer> n = choices(q, 1, 4000);
        for (Direction d : Direction.values()) {
            assertTrue(n.get(d) > 850 && n.get(d) < 1150, n.toString());
        }
    }

    @Test
    void epsilonShareOfRandomSteps() {
        // при 20% случайных шагов «не лучшее» выпадает в 20% * 3/4 = 15% случаев
        QTable q = new QTable(3, 3);
        q.set(HERE, RIGHT, 100);
        Map<Direction, Integer> n = choices(q, 0.2, 10000);
        int other = 10000 - n.get(RIGHT);
        assertTrue(other > 1350 && other < 1650, "не лучших " + other);
    }

    @Test
    void learnMovesNumberTowardTarget() {
        // 2 + 0.5 * (-1 + 0.9 * 10 - 2) = 5
        QTable q = new QTable(3, 3);
        q.set(HERE, RIGHT, 2);
        q.set(THERE, UP, 10);
        q.set(THERE, DOWN, 6);
        new Mouse(q, new Random(1)).learn(new StepResult(HERE, RIGHT, THERE, -1, false), 0.5, 0.9);
        assertEquals(5, q.get(HERE, RIGHT), 1e-12);
    }

    @Test
    void learnWithAlphaOneReplaces() {
        QTable q = new QTable(3, 3);
        q.set(HERE, RIGHT, 50);
        q.set(THERE, LEFT, 20);
        new Mouse(q, new Random(1)).learn(new StepResult(HERE, RIGHT, THERE, 10, false), 1, 1);
        assertEquals(30, q.get(HERE, RIGHT), 1e-12);
    }

    @Test
    void terminalStepHasNoFuture() {
        QTable q = new QTable(3, 3);
        q.set(THERE, UP, 1000);
        new Mouse(q, new Random(1)).learn(new StepResult(HERE, RIGHT, THERE, 99, true), 1, 0.9);
        assertEquals(99, q.get(HERE, RIGHT), 1e-12);
    }

    @Test
    void learnChangesOnlyThatArrow() {
        QTable q = new QTable(3, 3);
        q.set(THERE, UP, 10);
        new Mouse(q, new Random(1)).learn(new StepResult(HERE, RIGHT, THERE, -1, false), 0.5, 0.9);
        for (Direction d : Direction.values()) {
            if (d != RIGHT) {
                assertEquals(0, q.get(HERE, d));
            }
        }
        assertEquals(10, q.get(THERE, UP));
    }

    @Test
    void wallBumpLearnsFromSameCell() {
        // удар в стену: остался в той же клетке, будущее — лучшее число этой же клетки
        QTable q = new QTable(3, 3);
        q.set(HERE, DOWN, 8);
        new Mouse(q, new Random(1)).learn(new StepResult(HERE, UP, HERE, -5, false), 1, 0.5);
        assertEquals(-5 + 0.5 * 8, q.get(HERE, UP), 1e-12);
    }
}
