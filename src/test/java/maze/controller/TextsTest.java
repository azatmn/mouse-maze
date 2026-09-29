package maze.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TextsTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0 попыток", "1, 1 попытку", "2, 2 попытки", "4, 4 попытки", "5, 5 попыток",
            "11, 11 попыток", "12, 12 попыток", "14, 14 попыток", "21, 21 попытку", "22, 22 попытки",
            "25, 25 попыток", "101, 101 попытку", "111, 111 попыток", "112, 112 попыток", "1004, 1004 попытки"
    })
    void attemptsAgreeWithNumber(int n, String expected) {
        // «за N ...»: винительный падеж
        assertEquals(expected, Texts.attempts(n));
    }

    @Test
    void scoreHasSignAndNoNoise() {
        assertEquals("+87", Texts.score(87));
        assertEquals("−12", Texts.score(-12));
        assertEquals("0", Texts.score(0));
        assertEquals("+0.5", Texts.score(0.5));
        assertEquals("−0.25", Texts.score(-0.25));
        assertEquals("+0.3", Texts.score(0.1 + 0.2), "без хвоста 0.30000000000000004");
        assertEquals("—", Texts.score(Double.NaN));
    }
}
