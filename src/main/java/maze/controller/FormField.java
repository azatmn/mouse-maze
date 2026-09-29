package maze.controller;

import maze.util.SafeText;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Поле формы: ключ (имя параметра), подпись для человека и вид значения.
 * Умеет прочитать текст из поля ввода и превратить значение обратно в текст.
 */
public record FormField(String key, String label, Kind kind) {

    public enum Kind {
        INTEGER,
        LONG,
        DECIMAL,
        /** Флажок: окно пишет "true" или "false". */
        YES_NO
    }

    /** Только ASCII-цифры, необязательный знак: «５» или «٣» числом не считаются. */
    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    /** Дробь через точку или запятую: «0.5», «0,5», «.5». Экспоненты («1e3») и NaN не принимаются. */
    private static final Pattern DECIMAL = Pattern.compile("[+-]?([0-9]+([.,][0-9]+)?|[.,][0-9]+)");
    private static final int ECHO_LIMIT = 20;

    /** Прочитанное значение или текст ошибки. */
    public sealed interface Read {
        record Value(Object value) implements Read {
        }

        record Error(String message) implements Read {
        }
    }

    public Read read(String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty()) {
            return error("не заполнено");
        }
        String echo = "\"" + SafeText.preview(text, ECHO_LIMIT) + "\"";
        return switch (kind) {
            case INTEGER, LONG -> {
                if (!INTEGER.matcher(text).matches()) {
                    yield error("нужно целое число, а указано " + echo);
                }
                try {
                    yield new Read.Value(kind == Kind.LONG ? (Object) Long.parseLong(text) : Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    yield error("слишком большое число " + echo);
                }
            }
            case DECIMAL -> {
                if (!DECIMAL.matcher(text).matches()) {
                    yield error("нужно число, а указано " + echo);
                }
                double value = Double.parseDouble(text.replace(',', '.'));
                yield Double.isFinite(value) ? new Read.Value(value) : error("слишком большое число " + echo);
            }
            case YES_NO -> switch (text) {
                case "true" -> new Read.Value(true);
                case "false" -> new Read.Value(false);
                default -> error("нужно да или нет, а указано " + echo);
            };
        };
    }

    /** Значение как текст для поля ввода: дроби без лишних нулей («0.5», «100»). */
    public static String text(Object value) {
        if (value instanceof Double d) {
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private Read error(String what) {
        return new Read.Error(label + ": " + what);
    }
}
