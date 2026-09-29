package maze.model;

/** Что лежит в клетке. Сыр в лабиринте один, его место хранит {@link Maze}. */
public enum CellType {
    EMPTY,
    WATER,
    SHOCK,
    CHEESE
}
