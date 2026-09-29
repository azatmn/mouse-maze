package maze.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Заказ на случайный лабиринт. Проверяется при создании; обо всех проблемах сообщает сразу.
 *
 * @param water       сколько клеток с водой
 * @param shock       сколько клеток с током
 * @param loopPercent какую долю стен, оставшихся после построения «идеального» лабиринта, убрать (0..100):
 *                    0 — к любой клетке ровно один путь, 100 — стен внутри нет
 * @param seed        зерно генератора: одинаковый seed — одинаковый лабиринт
 */
public record MazeSpec(int width, int height, int water, int shock, int loopPercent, long seed) {

    /**
     * Лабиринт при запуске программы: подобран так, чтобы кратчайший путь к сыру шёл через молнию,
     * а выученный — обходил её и заходил за водой (см. DefaultsTest).
     */
    public static MazeSpec demo() {
        return new MazeSpec(12, 9, 4, 6, 20, 9L);
    }

    public MazeSpec {
        List<InvalidSettingsException.Problem> problems = new ArrayList<>();
        boolean widthOk = width >= 1 && width <= Maze.MAX_SIZE;
        boolean heightOk = height >= 1 && height <= Maze.MAX_SIZE;
        if (!widthOk) {
            problems.add(new InvalidSettingsException.Problem("width",
                    "Ширина лабиринта: допустимо от 1 до " + Maze.MAX_SIZE + ", указано " + width));
        }
        if (!heightOk) {
            problems.add(new InvalidSettingsException.Problem("height",
                    "Высота лабиринта: допустимо от 1 до " + Maze.MAX_SIZE + ", указано " + height));
        }
        if (widthOk && heightOk && width * height < 2) {
            problems.add(new InvalidSettingsException.Problem("width",
                    "Нужно хотя бы 2 клетки: для старта и для сыра"));
        }
        if (water < 0) {
            problems.add(new InvalidSettingsException.Problem("water",
                    "Клеток с водой: не может быть меньше 0, указано " + water));
        }
        if (shock < 0) {
            problems.add(new InvalidSettingsException.Problem("shock",
                    "Клеток с током: не может быть меньше 0, указано " + shock));
        }
        if (widthOk && heightOk && width * height >= 2 && water >= 0 && shock >= 0) {
            long free = (long) width * height - 2;  // без старта и сыра
            long items = (long) water + shock;
            if (items > free) {
                problems.add(new InvalidSettingsException.Problem("shock",
                        "Воды и тока " + items + ", а свободных клеток только " + free));
            }
        }
        if (loopPercent < 0 || loopPercent > 100) {
            problems.add(new InvalidSettingsException.Problem("loopPercent",
                    "Процент лишних проходов: допустимо от 0 до 100, указано " + loopPercent));
        }
        if (!problems.isEmpty()) {
            throw new InvalidSettingsException(problems);
        }
    }
}
