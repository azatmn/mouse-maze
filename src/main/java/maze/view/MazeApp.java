package maze.view;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import maze.controller.FormField;
import maze.controller.MazeEditor;
import maze.controller.MazeForm;
import maze.controller.SettingsForm;
import maze.controller.SpeedScale;
import maze.controller.Texts;
import maze.controller.TrainingController;
import maze.model.Attempt;
import maze.model.GreedyPath;
import maze.model.MazeGenerator;
import maze.model.MazeSpec;
import maze.model.Settings;
import maze.model.Trainer;

import java.util.List;
import java.util.Map;

/**
 * Главное окно: лабиринт в центре, управление и график справа, редактор над лабиринтом.
 * Цикл обучения — таймер JavaFX (AnimationTimer): на каждом кадре контроллер решает,
 * сколько шагов сделать, а окно перерисовывает то, что изменилось.
 */
public class MazeApp extends Application {

    /** Лабиринт при запуске программы. */
    static final MazeSpec START_SPEC = new MazeSpec(12, 9, 3, 4, 15, 42L);
    /** График перерисовываем не чаще 4 раз в секунду — он дороже лабиринта. */
    private static final long CHART_INTERVAL_NANOS = 250_000_000L;
    private static final FormField EPISODES = new FormField("episodes", "Число попыток", FormField.Kind.INTEGER);

    private TrainingController controller;
    private MazeEditor editor;
    private AnimationTimer timer;
    private MazeSpec spec = START_SPEC;
    private boolean edited;

    private final MazeCanvas canvas = new MazeCanvas();
    private final ScoreChart chart = new ScoreChart();
    private final Label mazeInfo = new Label();
    private final Button startPause = new Button();
    private final Button step = new Button("Шаг");
    private final TextField episodes = new TextField("500");
    private final Button train = new Button("Обучить");
    private final Button reset = new Button("Сбросить обучение");
    private final Slider speed = new Slider(SpeedScale.SLIDER_MIN, SpeedScale.SLIDER_MAX, 0);
    private final Label speedLabel = new Label();
    private final CheckBox showPath = new CheckBox("выученный путь");
    private final CheckBox showArrows = new CheckBox("стрелки направлений");
    private final CheckBox autoStop = new CheckBox("стоп, когда выучила");
    private final Label attemptValue = new Label();
    private final Label scoreValue = new Label();
    private final Label stepsValue = new Label();
    private final Label randomValue = new Label();
    private final Label lastValue = new Label();
    private final Label status = new Label();
    private final Label message = new Label();
    private final ToggleButton editorToggle = new ToggleButton("Редактор");
    private final ToggleGroup tools = new ToggleGroup();
    private final Label legendWater = new Label();
    private final Label legendShock = new Label();
    private final Label legendCheese = new Label();
    private HBox editorBar;

    private long lastDrawnSteps = -1;
    private long lastChartUpdate;

    static String stylesheet() {
        return MazeApp.class.getResource("app.css").toExternalForm();
    }

    @Override
    public void start(Stage stage) {
        controller = new TrainingController(MazeGenerator.generate(spec), Settings.defaults());
        editor = new MazeEditor(controller);

        editorBar = buildEditorBar();
        VBox center = new VBox(8, editorBar, canvas, buildLegend());
        VBox.setVgrow(canvas, Priority.ALWAYS);
        center.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(center);
        root.setRight(buildPanel());

        Scene scene = new Scene(root, 1180, 780);
        scene.getStylesheets().add(stylesheet());
        stage.setTitle("Мышь в лабиринте");
        stage.setMinWidth(860);
        stage.setMinHeight(600);
        stage.setScene(scene);

        canvas.setOnMouseClicked(e -> {
            if (editorToggle.isSelected() && tools.getSelectedToggle() != null) {
                MazeEditor.Tool tool = (MazeEditor.Tool) tools.getSelectedToggle().getUserData();
                MazeEditor.Result result = editor.apply(tool,
                        canvas.toMazeX(e.getX()), canvas.toMazeY(e.getY()), canvas.cellSize());
                switch (result) {
                    case MazeEditor.Result.Changed() -> {
                        edited = true;
                        message.setText("");
                    }
                    case MazeEditor.Result.Rejected(String why) -> message.setText(why);
                    case MazeEditor.Result.Unchanged() -> {
                    }
                }
                refresh(true, System.nanoTime());
            }
        });

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                controller.tick(now);
                refresh(false, now);
            }
        };
        refresh(true, System.nanoTime());
        stage.show();
        timer.start();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private HBox buildToolbar() {
        Label title = new Label("Мышь в лабиринте");
        title.getStyleClass().add("title");
        mazeInfo.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newMaze = new Button("Новый лабиринт…");
        newMaze.setId("newMaze");
        newMaze.setOnAction(e -> openNewMaze());
        editorToggle.setId("editor");
        editorToggle.setOnAction(e -> {
            controller.pause();
            message.setText("");
            refresh(true, System.nanoTime());
        });
        Button parameters = new Button("Параметры…");
        parameters.setId("parameters");
        parameters.setOnAction(e -> openParameters());

        HBox bar = new HBox(10, title, mazeInfo, spacer, newMaze, editorToggle, parameters);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar-row");
        return bar;
    }

    private HBox buildEditorBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("editor-bar");
        String[][] names = {{"WALL", "Стена"}, {"WATER", "Вода"}, {"SHOCK", "Ток"},
                {"START", "Старт"}, {"CHEESE", "Сыр"}, {"ERASE", "Ластик"}};
        for (String[] n : names) {
            ToggleButton b = new ToggleButton(n[1]);
            b.setId("tool-" + n[0]);
            b.setUserData(MazeEditor.Tool.valueOf(n[0]));
            b.setToggleGroup(tools);
            b.setMinWidth(Region.USE_PREF_SIZE);
            bar.getChildren().add(b);
        }
        tools.getToggles().getFirst().setSelected(true);
        // инструмент нельзя «отжать» совсем: повторный клик по выбранному оставляет его выбранным
        tools.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                old.setSelected(true);
            }
        });
        Label hint = new Label("Стена — клик по границе клеток; остальное — клик в клетку. Изменения сбрасывают обучение.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        hint.setMinWidth(0);
        HBox.setHgrow(hint, Priority.ALWAYS);
        bar.getChildren().add(hint);
        bar.visibleProperty().bind(editorToggle.selectedProperty());
        bar.managedProperty().bind(editorToggle.selectedProperty());
        return bar;
    }

    private HBox buildLegend() {
        HBox legend = new HBox(16,
                legendItem(MazeCanvas.WATER_TILE, MazeCanvas.WATER, legendWater),
                legendItem(MazeCanvas.SHOCK_TILE, MazeCanvas.SHOCK, legendShock),
                legendItem(MazeCanvas.CHEESE_TILE, MazeCanvas.CHEESE, legendCheese),
                legendItem(MazeCanvas.PATH, MazeCanvas.PATH, new Label("выученный путь")));
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getStyleClass().add("legend");
        return legend;
    }

    private static HBox legendItem(Color fill, Color stroke, Label label) {
        Rectangle square = new Rectangle(12, 12, fill);
        square.setStroke(stroke);
        HBox row = new HBox(6, square, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildPanel() {
        startPause.setId("startPause");
        step.setId("step");
        train.setId("train");
        episodes.setId("episodes");
        reset.setId("reset");
        speed.setId("speed");
        showPath.setId("showPath");
        showArrows.setId("showArrows");
        autoStop.setId("autoStop");

        startPause.setMaxWidth(Double.MAX_VALUE);
        step.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(startPause, Priority.ALWAYS);
        HBox.setHgrow(step, Priority.ALWAYS);
        HBox run = new HBox(6, startPause, step);

        episodes.setPrefColumnCount(6);
        train.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(train, Priority.ALWAYS);
        HBox trainRow = new HBox(6, episodes, train);
        reset.setMaxWidth(Double.MAX_VALUE);

        startPause.setOnAction(e -> command(controller::toggleRunning));
        step.setOnAction(e -> command(controller::stepOnce));
        train.setOnAction(e -> trainEpisodes());
        episodes.setOnAction(e -> trainEpisodes());
        reset.setOnAction(e -> command(controller::reset));

        speed.setValue(SpeedScale.toSlider(controller.getSpeed()));
        speed.valueProperty().addListener((obs, old, now) -> {
            controller.setSpeed(SpeedScale.toSpeed(now.doubleValue()));
            updateSpeedLabel();
        });
        updateSpeedLabel();

        showPath.setSelected(true);
        autoStop.setSelected(controller.isAutoStop());
        autoStop.setOnAction(e -> controller.setAutoStop(autoStop.isSelected()));
        showPath.setOnAction(e -> refresh(true, System.nanoTime()));
        showArrows.setOnAction(e -> refresh(true, System.nanoTime()));

        GridPane stats = new GridPane();
        stats.setHgap(6);
        stats.setVgap(6);
        stats.add(stat("Попытка", attemptValue), 0, 0);
        stats.add(stat("Очки", scoreValue), 1, 0);
        stats.add(stat("Шагов", stepsValue), 0, 1);
        stats.add(stat("Наугад", randomValue), 1, 1);
        lastValue.getStyleClass().add("muted");

        status.getStyleClass().add("status");
        status.setWrapText(true);
        message.getStyleClass().add("message");
        message.setWrapText(true);

        Label chartTitle = new Label("Сумма выигрыша за попытку");
        chartTitle.getStyleClass().add("muted");
        chart.setMinHeight(180);
        VBox.setVgrow(chart, Priority.ALWAYS);

        VBox box = new VBox(8,
                run, trainRow, reset,
                new Separator(), speedLabel, speed, showPath, showArrows, autoStop,
                new Separator(), stats, lastValue, status, message,
                new Separator(), chartTitle, chart);
        box.getStyleClass().add("panel");
        box.setPrefWidth(290);
        box.setMinWidth(290);
        return box;
    }

    private static VBox stat(String name, Label value) {
        Label label = new Label(name);
        label.getStyleClass().add("stat-name");
        value.getStyleClass().add("stat-value");
        VBox card = new VBox(0, label, value);
        card.getStyleClass().add("stat");
        card.setPrefWidth(130);
        return card;
    }

    /** Нажатие кнопки: выполнить действие и сразу показать результат. */
    private void command(Runnable action) {
        message.setText("");
        action.run();
        refresh(true, System.nanoTime());
    }

    private void trainEpisodes() {
        switch (EPISODES.read(episodes.getText())) {
            case FormField.Read.Error(String why) -> message.setText(why);
            case FormField.Read.Value(Object value) -> {
                int n = (int) value;
                if (n < 1 || n > TrainingController.MAX_EPISODES_AT_ONCE) {
                    message.setText("Число попыток: от 1 до " + TrainingController.MAX_EPISODES_AT_ONCE);
                    break;
                }
                int done = controller.trainEpisodes(n);
                message.setText(done == n
                        ? "Мышь пробежала ещё " + Texts.attempts(done)
                        : "Мышь пробежала " + Texts.attempts(done) + " из " + n + ": остальное заняло бы слишком долго");
            }
        }
        refresh(true, System.nanoTime());
    }

    private void openParameters() {
        controller.pause();
        Map<String, String> sections = Map.of(
                "cheeseReward", "Награды (среда сообщает мыши)",
                "alpha", "Обучение (Q-learning)",
                "maxSteps", "Попытки");
        new FormDialog<>("Параметры обучения", "После применения мышь учится заново.", "Применить",
                SettingsForm.FIELDS, sections,
                SettingsForm.toTexts(controller.settings()), SettingsForm.toTexts(Settings.defaults()),
                SettingsForm::parse, Settings::warnings)
                .showAndWait().ifPresent(controller::applySettings);
        message.setText("");
        refresh(true, System.nanoTime());
    }

    private void openNewMaze() {
        controller.pause();
        new FormDialog<>("Новый лабиринт", "Лабиринт строится случайно по seed; потом его можно поправить в редакторе.",
                "Построить", MazeForm.FIELDS, Map.of(),
                MazeForm.toTexts(spec), MazeForm.toTexts(START_SPEC), MazeForm::parse, newSpec -> List.of())
                .showAndWait().ifPresent(newSpec -> {
                    spec = newSpec;
                    edited = false;
                    controller.setMaze(MazeGenerator.generate(newSpec));
                });
        message.setText("");
        refresh(true, System.nanoTime());
    }

    private void updateSpeedLabel() {
        speedLabel.setText("Скорость: " + controller.getSpeed() + " шаг./с");
    }

    private void refresh(boolean force, long now) {
        Trainer trainer = controller.trainer();
        Attempt attempt = trainer.attempt();
        boolean changed = trainer.totalSteps() != lastDrawnSteps;
        if (force || changed) {
            // обрывок пути (мышь ещё не знает дороги) выглядел бы как выученный путь — его не рисуем
            GreedyPath path = showPath.isSelected() ? trainer.bestPath() : null;
            canvas.draw(controller.maze(), attempt, trainer.table(),
                    path != null && path.reachesCheese() ? path : null, showArrows.isSelected());
            attemptValue.setText(Integer.toString(trainer.episode()));
            scoreValue.setText(Texts.score(attempt.score()));
            stepsValue.setText(Integer.toString(attempt.steps()));
            randomValue.setText(Math.round(trainer.epsilon() * 100) + "%");
            lastValue.setText("Прошлая попытка: " + Texts.score(trainer.lastScore()));
            status.setText(controller.statusMessage());
            lastDrawnSteps = trainer.totalSteps();
        }
        if (force || (changed && now - lastChartUpdate >= CHART_INTERVAL_NANOS)) {
            chart.update(trainer.history());
            lastChartUpdate = now;
        }
        Settings s = controller.settings();
        legendWater.setText("вода " + Texts.score(s.waterReward()));
        legendShock.setText("ток " + Texts.score(s.shockReward()));
        legendCheese.setText("сыр " + Texts.score(s.cheeseReward()));
        mazeInfo.setText(controller.maze().width() + " × " + controller.maze().height()
                + " · seed " + spec.seed() + (edited ? " · изменён" : ""));
        startPause.setText(controller.isRunning() ? "Пауза" : "Старт");
    }
}
