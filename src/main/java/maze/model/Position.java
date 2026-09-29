package maze.model;

/** Клетка лабиринта: x — столбец, y — строка (сверху вниз). */
public record Position(int x, int y) {

    /** Соседняя клетка в направлении d (без проверки стен и границ). */
    public Position step(Direction d) {
        return new Position(x + d.dx(), y + d.dy());
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
