// Worked on by Abdala Aljewarane

public class NesterovGradDesc extends GradientDescent {
  private double learningRate;
  private double beta;

  public NesterovGradDesc(FunctionParser.ExpressionParser3D parser, double learningRate, double beta) {
    super(parser);
    this.learningRate = learningRate;
    this.beta = beta;
  }

  @Override
  protected Point computeNextPoint(Point point) {
    double lookX = point.x + beta * point.velX;
    double lookY = point.y + beta * point.velY;

    double gradX = dX(lookX, lookY);
    double gradY = dY(lookX, lookY);

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
