// Worked on by Abdala Aljewarane

public class MomentumGradDesc extends GradientDescent {
  private double learningRate;
  private double beta;

  public MomentumGradDesc(FunctionParser.ExpressionParser3D parser, double learningRate, double beta) {
    super(parser);
    this.learningRate = learningRate;
    this.beta = beta;
  }

  @Override
  protected Point computeNextPoint(Point point) {
    double gradX = dX(point.x, point.y);
    double gradY = dY(point.x, point.y);

    double velX = beta * point.velX - learningRate * gradX;
    double velY = beta * point.velY - learningRate * gradY;

    double nextX = point.x + velX;
    double nextY = point.y + velY;
    double nextZ = parser.evaluate(nextX, nextY);

    Point next = new Point(nextX, nextY, nextZ);
    next.velX = velX;
    next.velY = velY;
    return next;
  }
}
