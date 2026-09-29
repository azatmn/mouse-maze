package maze.model;

/**
 * Заказ на случайный лабиринт.
 *
 * @param water       сколько клеток с водой
 * @param shock       сколько клеток с током
 * @param loopPercent какую долю стен, оставшихся после построения «идеального» лабиринта, убрать (0..100):
 *                    0 — к любой клетке ровно один путь, 100 — стен внутри нет
 * @param seed        зерно генератора: одинаковый seed — одинаковый лабиринт
 */
public record MazeSpec(int width, int height, int water, int shock, int loopPercent, long seed) {

    public MazeSpec {
        Maze.checkSize(width, height);
        if (water < 0 || shock < 0) {
            throw new IllegalArgumentException("количество воды и тока не может быть отрицательным");
        }
        long free = (long) width * height - 2;  // без старта и сыра
        if ((long) water + shock > free) {
            throw new IllegalArgumentException(
                    "воды и тока " + ((long) water + shock) + ", а свободных клеток только " + free);
        }
        if (loopPercent < 0 || loopPercent > 100) {
            throw new IllegalArgumentException("процент лишних проходов должен быть от 0 до 100");
        }
    }
}
