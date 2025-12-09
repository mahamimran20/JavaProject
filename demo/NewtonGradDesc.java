// Worked on by Abdala Aljewarane

public class NewtonGradDesc extends GradientDescent {
  private double stepScale;

  public NewtonGradDesc(FunctionParser.ExpressionParser3D parser, double stepScale) {
    super(parser);
    this.stepScale = stepScale;
  }

  @Override
  protected Point computeNextPoint(Point point) {
    double x = point.x;
    double y = point.y;

    double gradX = dX(x, y);
    double gradY = dY(x, y);

    double h = H_VAL;
    double fCenter = point.z;

    double fXPlus  = parser.evaluate(x + h, y);
    double fXMinus = parser.evaluate(x - h, y);
    double fYPlus  = parser.evaluate(x, y + h);
    double fYMinus = parser.evaluate(x, y - h);

    double fXYPlusPlus   = parser.evaluate(x + h, y + h);
    double fXYPlusMinus  = parser.evaluate(x + h, y - h);
    double fXYMinusPlus  = parser.evaluate(x - h, y + h);
    double fXYMinusMinus = parser.evaluate(x - h, y - h);

    double fXX = (fXPlus - 2.0 * fCenter + fXMinus) / (h * h);
    double fYY = (fYPlus - 2.0 * fCenter + fYMinus) / (h * h);
    double fXY = (fXYPlusPlus - fXYPlusMinus - fXYMinusPlus + fXYMinusMinus) / (4.0 * h * h);

    double determinant = fXX * fYY - fXY * fXY;

    if (Math.abs(determinant) < 1e-9) {
      determinant = Math.copySign(1e-9, determinant);
    }

    double stepX = -stepScale * ((fYY * gradX - fXY * gradY) / determinant);
    double stepY = -stepScale * ((-fXY * gradX + fXX * gradY) / determinant);

    double nextX = x + stepX;
    double nextY = y + stepY;
    double nextZ = parser.evaluate(nextX, nextY);

    return new Point(nextX, nextY, nextZ);
  }
}
