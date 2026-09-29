package maze.view;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * График «сумма выигрыша за попытку». Бледная линия — каждая попытка,
 * яркая — среднее за последние {@value #WINDOW} попыток: по ней видно, что мышь учится.
 */
public class ScoreChart extends LineChart<Number, Number> {

    /** Больше точек на линию не рисуем: график остаётся быстрым при любой длине истории. */
    private static final int MAX_POINTS = 300;
    static final int WINDOW = 20;

    private final XYChart.Series<Number, Number> raw = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> average = new XYChart.Series<>();

    public ScoreChart() {
        super(new NumberAxis(), new NumberAxis());
        setAnimated(false);
        setCreateSymbols(false);
        setLegendVisible(true);
        getXAxis().setLabel("Попытка");
        NumberAxis attempts = (NumberAxis) getXAxis();
        attempts.setForceZeroInRange(false);
        // Номер попытки — целое число: «10», а не «10.00»
        attempts.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return Long.toString(Math.round(value.doubleValue()));
            }

            @Override
            public Number fromString(String text) {
                return Long.parseLong(text);
            }
        });
        raw.setName("за попытку");
        average.setName("среднее за " + WINDOW);
        getData().addAll(List.of(raw, average));
    }

    /** history — суммы законченных попыток по порядку (попытка i+1 — элемент i). */
    public void update(List<Double> history) {
        int n = history.size();
        double[] prefix = new double[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + history.get(i);
        }
        List<XYChart.Data<Number, Number>> r = new ArrayList<>();
        List<XYChart.Data<Number, Number>> a = new ArrayList<>();
        int stride = Math.max(1, (int) Math.ceil(n / (double) MAX_POINTS));
        for (int i = 0; i < n; i++) {
            // каждая stride-я точка, но последняя — всегда
            if (i % stride != 0 && i != n - 1) {
                continue;
            }
            int from = Math.max(0, i + 1 - WINDOW);
            r.add(new XYChart.Data<>(i + 1, history.get(i)));
            a.add(new XYChart.Data<>(i + 1, (prefix[i + 1] - prefix[from]) / (i + 1 - from)));
        }
        raw.getData().setAll(r);
        average.getData().setAll(a);
    }
}
