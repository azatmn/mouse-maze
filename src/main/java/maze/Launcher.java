package maze;

import javafx.application.Application;
import maze.view.MazeApp;

/**
 * Точка входа.
 * Отдельный класс, а не сам MazeApp: если главный класс наследует Application,
 * запуск без модулей JavaFX падает с ошибкой «JavaFX runtime components are missing».
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MazeApp.class, args);
    }
}
