package maze.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Умолчания подобраны перебором (720 + 216 наборов на 30 лабиринтах, seed 1000+).
 * Здесь проверка на лабиринтах, которые в подборе не участвовали (seed 9000+).
 */
class DefaultsTest {

    @ParameterizedTest
    @ValueSource(ints = {10, 15, 20, 30})
    void defaultsLearnMazesOfDifferentSizes(int size) {
        int cells = size * size;
        for (long seed = 9000; seed < 9003; seed++) {
            Maze m = MazeGenerator.generate(new MazeSpec(size, size, cells / 15, cells / 12, 15, seed));
            Trainer t = new Trainer(m, Settings.defaults());
            // 30x30 seed 9000 (путь 77 клеток, 75 молний) замером выучивается за 1000–1075 попыток
            t.runEpisodes(size >= 30 ? 2000 : 1000);
            assertTrue(t.bestPath().reachesCheese(), size + "x" + size + " seed " + seed);
        }
    }

    @Test
    void demoMazeShowsDetourAroundShock() {
        // лабиринт при запуске программы: кратчайший путь идёт по молнии, а выученный её обходит
        Maze m = MazeGenerator.generate(MazeSpec.demo());
        List<Position> shortest = m.shortestPath();
        assertTrue(shortest.stream().anyMatch(p -> m.cellAt(p) == CellType.SHOCK), "кратчайший путь без молний");
        for (long mouseSeed = 1; mouseSeed <= 5; mouseSeed++) {
            Trainer t = new Trainer(m, Settings.defaults().toBuilder().seed(mouseSeed).build());
            t.runEpisodes(500);
            GreedyPath path = t.bestPath();
            assertTrue(path.reachesCheese(), "seed мыши " + mouseSeed);
            assertTrue(path.cells().stream().noneMatch(p -> m.cellAt(p) == CellType.SHOCK), "seed мыши " + mouseSeed);
            assertTrue(path.cells().size() > shortest.size(), "обход длиннее кратчайшего пути");
        }
    }

    @Test
    void defaultsProduceNoWarnings() {
        assertEquals(List.of(), Settings.defaults().warnings());
    }
}
