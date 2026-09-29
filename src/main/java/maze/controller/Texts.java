package maze.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Числа в тексте для человека. */
public final class Texts {

    private Texts() {
    }

    /** «N попытку / попытки / попыток» — для фразы «за N ...». */
    public static String attempts(int n) {
        int lastTwo = Math.abs(n) % 100;
        int last = lastTwo % 10;
        String word;
        if (lastTwo >= 11 && lastTwo <= 14) {
            word = "попыток";
        } else if (last == 1) {
            word = "попытку";
        } else if (last >= 2 && last <= 4) {
            word = "попытки";
        } else {
            word = "попыток";
        }
        return n + " " + word;
    }

    /** Очки со знаком и не больше чем двумя знаками после точки: «+87», «−0.5», «0». Не число — «—». */
    public static String score(double v) {
        if (!Double.isFinite(v)) {
            return "—";
        }
        String abs = plain(Math.abs(v));
        if (abs.equals("0")) {
            return "0";
        }
        return (v > 0 ? "+" : "−") + abs;
    }

    /** Число без лишних нулей и хвостов вроде 0.30000000000000004, до двух знаков после точки. */
    static String plain(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
