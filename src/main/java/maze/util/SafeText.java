package maze.util;

/** Безопасный показ введённого пользователем текста внутри сообщений. */
public final class SafeText {

    private SafeText() {
    }

    /**
     * Текст для сообщения об ошибке: не длиннее limit символов (дальше «...»),
     * управляющие символы заменены на «?», чтобы они не испортили экран.
     */
    public static String preview(String text, int limit) {
        StringBuilder sb = new StringBuilder();
        text.codePoints()
                .limit(limit)
                .forEach(cp -> sb.appendCodePoint(Character.isISOControl(cp) ? '?' : cp));
        if (text.codePointCount(0, text.length()) > limit) {
            sb.append("...");
        }
        return sb.toString();
    }
}
