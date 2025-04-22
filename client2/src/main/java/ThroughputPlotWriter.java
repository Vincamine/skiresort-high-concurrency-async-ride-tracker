import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import models.ThroughputData;

import javax.swing.*;

/**
 * ThroughputPlotWriter will plot the throughput over time.
 */
public class ThroughputPlotWriter {

    // A list of throughput data points to be plotted
    private final List<ThroughputData> throughputDataList = new ArrayList<>();

    /**
     * Adds a new data point to the throughput data list.
     *
     * @param timeInSeconds The time in seconds (X-axis value).
     * @param throughput The throughput (requests/second) at that time (Y-axis value).
     */
    public void addThroughputData(double timeInSeconds, double throughput) {
        throughputDataList.add(new ThroughputData(timeInSeconds, throughput));
    }

    /**
     * Creates a JFrame to display the throughput plot and adds a custom JPanel for drawing the plot.
     */
    public void plotThroughput() {
        JFrame frame = new JFrame("Throughput Over Time");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel panel = new ThroughputPlotPanel(throughputDataList);

        frame.add(panel);
        frame.setVisible(true);
    }

    /**
     * A custom JPanel used for drawing the throughput plot.
     */
    private static class ThroughputPlotPanel extends JPanel {
        private final List<ThroughputData> throughputDataList;

        public ThroughputPlotPanel(List<ThroughputData> throughputDataList) {
            this.throughputDataList = throughputDataList;
        }

        /**
         * This method is automatically called by the Swing framework when the panel needs to be drawn.
         * It calls drawThroughputPlot() to render the throughput graph.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawThroughputPlot(g);
        }

        /**
         * Draws the throughput plot on the given Graphics object.
         *
         * @param g The Graphics object to draw on.
         */
        private void drawThroughputPlot(Graphics g) {
            if (throughputDataList.isEmpty()) {
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            int padding = 50;
            int labelPadding = 20;

            double maxTime = throughputDataList.get(throughputDataList.size() - 1).getTimeInSeconds();
            double maxThroughput = throughputDataList.stream().mapToDouble(ThroughputData::getThroughput).max().orElse(1);

            double xScale = (width - 2 * padding - labelPadding) / maxTime;
            double yScale = (height - 2 * padding - labelPadding) / maxThroughput;

            g2d.drawLine(padding, height - padding, padding, padding); // Y 轴
            g2d.drawLine(padding, height - padding, width - padding, height - padding); // X 轴

            int numYTicks = 10;
            for (int i = 0; i <= numYTicks; i++) {
                int y = height - padding - (int) (i * (height - 2 * padding) / numYTicks);
                g2d.drawLine(padding - 5, y, padding + 5, y);
                String yLabel = String.format("%.1f", (i * maxThroughput / numYTicks));
                g2d.drawString(yLabel, padding - labelPadding - 10, y + 5);
            }

            int numXTicks = 10;
            for (int i = 0; i <= numXTicks; i++) {
                int x = padding + (int) (i * (width - 2 * padding) / numXTicks);
                g2d.drawLine(x, height - padding - 5, x, height - padding + 5);
                String xLabel = String.format("%.1f", (i * maxTime / numXTicks));
                g2d.drawString(xLabel, x - 10, height - padding + labelPadding + 10);
            }

            g2d.setColor(Color.BLUE);
            for (int i = 1; i < throughputDataList.size(); i++) {
                int x1 = (int) (padding + (throughputDataList.get(i - 1).getTimeInSeconds() * xScale));
                int y1 = (int) (height - padding - (throughputDataList.get(i - 1).getThroughput() * yScale));
                int x2 = (int) (padding + (throughputDataList.get(i).getTimeInSeconds() * xScale));
                int y2 = (int) (height - padding - (throughputDataList.get(i).getThroughput() * yScale));
                g2d.drawLine(x1, y1, x2, y2);
            }

            g2d.setColor(Color.BLACK);
            g2d.drawString("Time (seconds)", width / 2, height - padding + labelPadding + 25);
            g2d.drawString("Throughput (requests/second)", 20, 20);
        }
    }
}
