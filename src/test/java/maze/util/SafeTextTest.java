package maze.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeTextTest {

    @Test
    void shortTextIsShownAsIs() {
        assertEquals("abc", SafeText.preview("abc", 20));
        assertEquals("", SafeText.preview("", 20));
    }

    @Test
    void textOfExactlyLimitLengthIsNotMarkedAsCut() {
        assertEquals("x".repeat(20), SafeText.preview("x".repeat(20), 20));
    }

    @Test
    void longerTextIsCutAndMarked() {
        assertEquals("x".repeat(20) + "...", SafeText.preview("x".repeat(21), 20));
        assertEquals("abc...", SafeText.preview("abcdef", 3));
    }

    @Test
    void controlCharactersBecomeQuestionMarks() {
        assertEquals("?[2J", SafeText.preview("\u001b[2J", 20));
        assertEquals("a?b?c", SafeText.preview("a\u0000b\tc", 20));
    }

    @Test
    void emojiCountsAsOneCharacterAndIsNotBroken() {
        String emoji = "🙂";
        assertEquals(emoji.repeat(3), SafeText.preview(emoji.repeat(3), 3));
        assertEquals(emoji.repeat(2) + "...", SafeText.preview(emoji.repeat(3), 2));
    }
}
