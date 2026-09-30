package maze.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Числа в тексте для человека. */
public final class Texts {

    private Texts() {
    }

    /** «N попытку / попытки / попыток» — для фразы «за N ...». */
    public static String attempts(int n) {
        return n + " " + plural(n, "попытку", "попытки", "попыток");
    }

    /** Форма слова для числа n: одна (1, 21), две–четыре (2–4, 22–24), остальные. */
    private static String plural(int n, String one, String few, String many) {
        int lastTwo = Math.abs(n) % 100;
        int last = lastTwo % 10;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return many;
        }
        if (last == 1) {
            return one;
        }
        return last >= 2 && last <= 4 ? few : many;
    }

    /** «N шаг / шага / шагов». */
    public static String steps(int n) {
        return n + " " + plural(n, "шаг", "шага", "шагов");
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
