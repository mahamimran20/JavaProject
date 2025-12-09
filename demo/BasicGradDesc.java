public class BasicGradDesc extends GradientDescent {
  private double learningRate;

  public BasicGradDesc(FunctionParser.ExpressionParser3D parser, double learningRate) {
    super(parser);
    this.learningRate = learningRate;
  }

  @Override
  protected Point computeNextPoint(Point point) {
    double nextX = point.x - learningRate * dX(point.x, point.y);
    double nextY = point.y - learningRate * dY(point.x, point.y);
    double nextZ = parser.evaluate(nextX, nextY);

    return new Point(nextX, nextY, nextZ);
  }
}
