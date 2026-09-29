module maze {
    requires javafx.controls;
    exports maze;
    exports maze.model;
    exports maze.controller;
    exports maze.util;
    exports maze.view to javafx.graphics;
    opens maze.view to javafx.graphics;
}
