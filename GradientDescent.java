import java.util.ArrayList;
import java.util.List;

public abstract class GradientDescent {
  protected static final double H_VAL = Math.pow(2, -12);
  private static final double CONV_THRESHOLD = 1e-4;
  private static final int MAX_EPOCHS = 100;

  protected FunctionParser.ExpressionParser3D parser;
  protected List<List<Point>> epochs;

  static class Point {
    public double x;
    public double y;
    public double z;

    public double velX;
    public double velY;

    double b11 = 1.0;
    double b12 = 0.0;
    double b21 = 0.0;
    double b22 = 1.0;
    Double prevGradX = null;
    Double prevGradY = null;
    Double prevX = null;
    Double prevY = null;

    public Point(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
  }

  public enum InitialPoint {
    RANDOM,
    GRID
  }

  protected GradientDescent(FunctionParser.ExpressionParser3D parser) {
    this.parser = parser;
    epochs = new ArrayList<>();
  }

  public void initializePoints(InitialPoint method, double minX, double maxX, double minY, double maxY, int numPoints) {
    if (method == InitialPoint.RANDOM) {
      epochs.clear();
      epochs.add(new ArrayList<Point>());

      for (int i = 0; i < numPoints; i++) {
        double x = Math.random() * (maxX - minX) + minX;
        double y = Math.random() * (maxY - minY) + minY;
        double z = parser.evaluate(x, y);

        epochs.get(0).add(new Point(x, y, z));
      }

    } else if (method == InitialPoint.GRID) {
      epochs.clear();
      epochs.add(new ArrayList<Point>());

      double widthX = maxX - minX;
      double widthY = maxY - minY;

      double aspect = widthX / widthY;
      int cols = (int) Math.round(Math.sqrt(numPoints * aspect));
      if (cols < 1) cols = 1;

      int rows = (int) Math.ceil((double) numPoints / cols);
      if (rows < 1) rows = 1;

      double deltaX = widthX / cols;
      double deltaY = widthY / rows;

      for (int k = 0; k < numPoints; k++) {
        int row = k / cols;
        int col = k % cols;

        double x = minX + (col + 0.5) * deltaX;
        double y = minY + (row + 0.5) * deltaY;

        double z = parser.evaluate(x, y);
        epochs.get(0).add(new Point(x, y, z));
      }
    }
  }

  // returns the approximate partial derivative of z with respect to x
  protected double dX(double x, double y) {
    // approximate using (f(x+h, y) - f(x-h, y)) / 2h
    double x1 = parser.evaluate(x - H_VAL, y);
    double x2 = parser.evaluate(x + H_VAL, y);

    return (x2 - x1) / (2.0 * H_VAL); 
  }

  // returns the approximate partial derivative of z with respect to y
  protected double dY(double x, double y) {
    // approximate using (f(x, y+h) - f(x, y-h)) / 2h
    double y1 = parser.evaluate(x, y - H_VAL);
    double y2 = parser.evaluate(x, y + H_VAL);

    return (y2 - y1) / (2.0 * H_VAL);
  }

  public void run() {
    if (epochs.isEmpty()) {
      return;
    }

    if (epochs.size() > 1) {
      List<Point> initial = epochs.get(0);
      epochs.clear();
      epochs.add(initial);
    }

    List<Point> firstEpoch = epochs.get(0);
    List<Boolean> converged = new ArrayList<>();
    for (int i = 0; i < firstEpoch.size(); i++) {
      converged.add(false);
    }

    boolean isFinished = false;

    for (int epoch = 0; !isFinished && epoch < MAX_EPOCHS; epoch++) {
      isFinished = true;

      List<Point> latestEpoch = epochs.get(epochs.size() - 1);
      List<Point> newEpoch = new ArrayList<>();

      for (int i = 0; i < latestEpoch.size(); i++) {
        Point point = latestEpoch.get(i);

        if (converged.get(i) || !Double.isFinite(point.z) || Double.isNaN(point.z)) {
          newEpoch.add(point);
          continue;
        }

        Point nextPoint = computeNextPoint(point);

        newEpoch.add(nextPoint);

        double stepSize = Math.abs(nextPoint.x - point.x) + Math.abs(nextPoint.y - point.y);
        if (stepSize <= CONV_THRESHOLD) {
          converged.set(i, true);
        } else {
          isFinished = false;
        }
      }

      epochs.add(newEpoch);
    }
  }

  public List<List<Point>> getEpochs() {
    return epochs;
  }

  protected abstract Point computeNextPoint(Point point);
}

