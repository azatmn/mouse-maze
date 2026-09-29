package maze.model;

import java.util.List;

/**
 * Путь «по лучшим стрелкам» от старта.
 *
 * @param cells         пройденные клетки начиная со старта
 * @param reachesCheese дошёл ли путь до сыра (иначе оборвался на петле или стене)
 */
public record GreedyPath(List<Position> cells, boolean reachesCheese) {

    public GreedyPath {
        cells = List.copyOf(cells);
    }
}
