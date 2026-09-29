package maze.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MazeSpecTest {

    @Test
    void validSpecKeepsValues() {
        MazeSpec s = new MazeSpec(12, 9, 3, 4, 10, 42L);
        assertEquals(12, s.width());
        assertEquals(9, s.height());
        assertEquals(3, s.water());
        assertEquals(4, s.shock());
        assertEquals(10, s.loopPercent());
        assertEquals(42L, s.seed());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 5, 0, 0, 0",     // размер
            "101, 5, 0, 0, 0",
            "1, 1, 0, 0, 0",
            "3, 3, -1, 0, 0",    // отрицательное количество
            "3, 3, 0, -1, 0",
            "3, 3, 4, 4, 0",     // 8 предметов, а свободных клеток 3*3-2 = 7
            "3, 3, 0, 0, -1",    // процент вне 0..100
            "3, 3, 0, 0, 101"
    })
    void badSpecRejected(int w, int h, int water, int shock, int loop) {
        assertThrows(IllegalArgumentException.class, () -> new MazeSpec(w, h, water, shock, loop, 1L));
    }

    @ParameterizedTest
    @CsvSource({"3, 3, 7, 0, 0", "3, 3, 3, 4, 100", "1, 2, 0, 0, 0"})
    void boundaryValuesAllowed(int w, int h, int water, int shock, int loop) {
        assertDoesNotThrow(() -> new MazeSpec(w, h, water, shock, loop, 1L));
    }

    private static java.util.List<String> fields(int w, int h, int water, int shock, int loop) {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> new MazeSpec(w, h, water, shock, loop, 1L));
        return e.problems().stream().map(InvalidSettingsException.Problem::field).toList();
    }

    @Test
    void problemsNameTheirFields() {
        assertEquals(java.util.List.of("width"), fields(0, 5, 0, 0, 0));
        assertEquals(java.util.List.of("height"), fields(5, 101, 0, 0, 0));
        assertEquals(java.util.List.of("width"), fields(1, 1, 0, 0, 0));
        assertEquals(java.util.List.of("water"), fields(3, 3, -1, 0, 0));
        assertEquals(java.util.List.of("shock"), fields(3, 3, 0, -1, 0));
        assertEquals(java.util.List.of("shock"), fields(3, 3, 4, 4, 0));
        assertEquals(java.util.List.of("loopPercent"), fields(3, 3, 0, 0, 101));
    }

    @Test
    void allProblemsAtOnce() {
        assertEquals(java.util.List.of("width", "water", "shock", "loopPercent"), fields(0, 3, -1, -2, -3));
    }

    @Test
    void tooManyItemsMessageShowsNumbers() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> new MazeSpec(3, 3, 4, 4, 0, 1L));
        String msg = e.problems().getFirst().message();
        assertTrue(msg.contains("8") && msg.contains("7"), msg);
    }

    @Test
    void hugeCountsDoNotOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> new MazeSpec(3, 3, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 1L));
    }
}
