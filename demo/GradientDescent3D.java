import org.math.plot.*;
import javax.swing.*;
import java.util.ArrayList;

public class GradientDescent3D {
    public static void main(String[] args) {
        // Function: f(x, y) = x² + y²
        double learningRate = 0.1;
        int steps = 30;
        double x = 2.5, y = 2.5;

        ArrayList<Double> xList = new ArrayList<>();
        ArrayList<Double> yList = new ArrayList<>();
        ArrayList<Double> zList = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            xList.add(x);
            yList.add(y);
            zList.add(f(x, y));
            double[] grad = gradF(x, y);
            x -= learningRate * grad[0];
            y -= learningRate * grad[1];
        }

        // Convert to arrays
        double[] xArr = xList.stream().mapToDouble(Double::doubleValue).toArray();
        double[] yArr = yList.stream().mapToDouble(Double::doubleValue).toArray();
        double[] zArr = zList.stream().mapToDouble(Double::doubleValue).toArray();

        // Plotting
        Plot3DPanel plot = new Plot3DPanel();
        plot.addLinePlot("Gradient Descent Path", xArr, yArr, zArr);
        plot.setAxisLabels("X", "Y", "f(x, y)");

        JFrame frame = new JFrame("3D Gradient Descent Demo");
        frame.setSize(800, 600);
        frame.setContentPane(plot);
        frame.setVisible(true);
    }

    public static double f(double x, double y) {
        return x * x + y * y;
    }

    public static double[] gradF(double x, double y) {
        return new double[]{2 * x, 2 * y};
    }
}
