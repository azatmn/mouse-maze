package maze.view;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import maze.controller.FormField;
import maze.controller.FormResult;
import maze.model.InvalidSettingsException.Problem;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Окно с формой. Проверка введённого — в контроллере (SettingsForm, MazeForm);
 * здесь только поля, подсветка ошибок и кнопки. С ошибками окно не закрывается.
 */
class FormDialog<T> extends Dialog<T> {

    private final Map<String, Control> inputs = new LinkedHashMap<>();
    private final Label errors = new Label();
    private final Function<Map<String, String>, FormResult<T>> parser;
    private T accepted;

    /**
     * @param sections подзаголовки: ключ поля → заголовок группы, которая с него начинается
     * @param defaults тексты для кнопки «По умолчанию»
     */
    FormDialog(String title, String header, String applyText,
               List<FormField> fields, Map<String, String> sections,
               Map<String, String> current, Map<String, String> defaults,
               Function<Map<String, String>, FormResult<T>> parser) {
        this.parser = parser;
        setTitle(title);
        setHeaderText(header);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 4, 4, 4));
        // Подписи никогда не обрезаются многоточием: столбец не уже самой длинной подписи
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        grid.getColumnConstraints().addAll(labelColumn, new ColumnConstraints(), new ColumnConstraints());
        int row = 0;
        for (FormField field : fields) {
            String section = sections.get(field.key());
            if (section != null) {
                Label heading = new Label(section);
                heading.getStyleClass().add("section");
                grid.add(heading, 0, row++, 3, 1);
            }
            Control input;
            if (field.kind() == FormField.Kind.YES_NO) {
                CheckBox box = new CheckBox();
                box.setSelected(Boolean.parseBoolean(current.get(field.key())));
                input = box;
            } else {
                TextField text = new TextField(current.get(field.key()));
                text.setPrefColumnCount(10);
                input = text;
            }
            input.setId("field-" + field.key());
            inputs.put(field.key(), input);
            grid.add(new Label(field.label()), 0, row);
            grid.add(input, 1, row);
            if (field.key().equals("seed")) {
                Button random = new Button("Случайный");
                random.setMinWidth(Region.USE_PREF_SIZE);
                random.setOnAction(e -> ((TextField) input).setText(Long.toString(new Random().nextLong())));
                grid.add(random, 2, row);
            }
            row++;
        }

        Button reset = new Button("По умолчанию");
        reset.setOnAction(e -> fill(defaults));

        errors.getStyleClass().add("error-text");
        errors.setWrapText(true);
        errors.setMaxWidth(520);
        // Блок ошибок растёт по высоте под все строки, а не обрезается
        errors.setMinHeight(Region.USE_PREF_SIZE);

        VBox content = new VBox(10, grid, new HBox(reset), errors);
        getDialogPane().setContent(content);
        ButtonType apply = new ButtonType(applyText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(apply, cancel);
        getDialogPane().getStylesheets().add(MazeApp.stylesheet());

        Button applyButton = (Button) getDialogPane().lookupButton(apply);
        applyButton.setId("apply");
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!tryAccept()) {
                event.consume(); // есть ошибки — окно остаётся открытым
            }
        });
        setResultConverter(button -> button == apply ? accepted : null);
    }

    private void fill(Map<String, String> texts) {
        texts.forEach((key, text) -> {
            switch (inputs.get(key)) {
                case CheckBox box -> box.setSelected(Boolean.parseBoolean(text));
                case TextField field -> field.setText(text);
                case null, default -> {
                }
            }
        });
    }

    private boolean tryAccept() {
        Map<String, String> texts = new HashMap<>();
        inputs.forEach((key, input) -> texts.put(key, switch (input) {
            case CheckBox box -> Boolean.toString(box.isSelected());
            case TextField field -> field.getText();
            default -> "";
        }));
        inputs.values().forEach(input -> input.getStyleClass().remove("invalid"));

        switch (parser.apply(texts)) {
            case FormResult.Valid<T>(T value) -> {
                accepted = value;
                errors.setText("");
                return true;
            }
            case FormResult.Invalid<T>(List<Problem> problems) -> {
                showProblems(problems);
                return false;
            }
        }
    }

    private void showProblems(List<Problem> problems) {
        StringBuilder text = new StringBuilder();
        for (Problem problem : problems) {
            text.append("• ").append(problem.message()).append('\n');
            Control input = inputs.get(problem.field());
            if (input != null && !input.getStyleClass().contains("invalid")) {
                input.getStyleClass().add("invalid");
            }
        }
        errors.setText(text.toString().strip());
    }
}
