package maze.model;

/**
 * Что произошло за один шаг мыши — ровно то, что нужно для обучения.
 *
 * @param from      откуда шагнула
 * @param direction куда шагнула
 * @param to        где оказалась (та же клетка, если там стена)
 * @param reward    сколько получила от среды
 * @param terminal  дошла до сыра: у этого шага нет будущего
 */
public record StepResult(Position from, Direction direction, Position to, double reward, boolean terminal) {
}
