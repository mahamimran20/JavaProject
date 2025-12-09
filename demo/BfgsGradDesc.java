public class BfgsGradDesc extends GradientDescent {
  private double learningRate;

  public BfgsGradDesc(FunctionParser.ExpressionParser3D parser, double learningRate) {
    super(parser);
    this.learningRate = learningRate;
  }

  @Override
  protected Point computeNextPoint(Point point) {
    double x = point.x;
    double y = point.y;

    double gradX = dX(x, y);
    double gradY = dY(x, y);

    if (point.prevGradX != null) {
      double stepX = x - point.prevX;
      double stepY = y - point.prevY;
      double gradDiffX = gradX - point.prevGradX;
      double gradDiffY = gradY - point.prevGradY;

      double ys = gradDiffX * stepX + gradDiffY * stepY;

      if (Math.abs(ys) > 1e-12) {
        double rho = 1.0 / ys;

        double bYX = point.b11 * gradDiffX + point.b12 * gradDiffY;
        double bYY = point.b21 * gradDiffX + point.b22 * gradDiffY;

        double newB11 = point.b11 - rho * (stepX * (gradDiffX * point.b11 + gradDiffY * point.b21) + bYX * stepX) + rho * (stepX * stepX);
        double newB12 = point.b12 - rho * (stepX * (gradDiffX * point.b12 + gradDiffY * point.b22) + bYX * stepY) + rho * (stepX * stepY);
        double newB21 = point.b21 - rho * (stepY * (gradDiffX * point.b11 + gradDiffY * point.b21) + bYY * stepX) + rho * (stepY * stepX);
        double newB22 = point.b22 - rho * (stepY * (gradDiffX * point.b12 + gradDiffY * point.b22) + bYY * stepY) + rho * (stepY * stepY);

        point.b11 = newB11;
        point.b12 = newB12;
        point.b21 = newB21;
        point.b22 = newB22;
      }
    }

    double dirX = -(point.b11 * gradX + point.b12 * gradY);
    double dirY = -(point.b21 * gradX + point.b22 * gradY);

    double nextX = x + learningRate * dirX;
    double nextY = y + learningRate * dirY;
    double nextZ = parser.evaluate(nextX, nextY);

    Point next = new Point(nextX, nextY, nextZ);
    next.prevX = x;
    next.prevY = y;
    next.prevGradX = gradX;
    next.prevGradY = gradY;
    return next;
  }
}
