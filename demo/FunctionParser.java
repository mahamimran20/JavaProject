/* CS4361.001 (Spring 2025) Team 13
 *
 * This class parses a mathematical function of x and y as an input string, 
 * tokenizing that string and generating a syntax tree, which can be evaluated
 * at any (x, y) with evaluate(x, y), or a JavaFX TriangleMesh can be generated
 * with buildTriangleMesh(xMin, xMax, yMin, yMax, xSteps, ySteps).
 * Tree folding is also done to make evaluation of the function more efficient.
 *
 * @author Luke Nelson
 */

import java.util.*;
import javafx.scene.shape.TriangleMesh;

public class FunctionParser {
  enum TokenType {
    // terminals
    CONST, VAR_X, VAR_Y, L_PAREN, R_PAREN,
    // operators
    ADD_OP, SUB_OP, MULT_OP, DIV_OP, EXP_OP,
    // functions
    ABS_FUNC, LN_FUNC, LOG_FUNC, SIN_FUNC, COS_FUNC, TAN_FUNC,
    SEC_FUNC, CSC_FUNC, COT_FUNC, SQRT_FUNC,
    ASIN_FUNC, ACOS_FUNC, ATAN_FUNC
  }

  // This class represents tokens which are used while tokenizing input string
  static final class Token {
    final TokenType type;
    final double value;

    Token(TokenType type) {
      this.type = type;
      this.value = Double.NaN;
    }

    Token(TokenType type, double value) {
      this.type = type;
      this.value = value;
    }

    boolean isFunc() {
      switch (type) {
        case CONST:
        case VAR_X:
        case VAR_Y:
        case L_PAREN:
        case R_PAREN:
        case ADD_OP:
        case SUB_OP:
        case MULT_OP:
        case DIV_OP:
        case EXP_OP:
          return false;
        default:
          return true;
      }
    }

    boolean endsPrimary() {
      return type == TokenType.CONST || type == TokenType.VAR_X ||
          type == TokenType.VAR_Y || type == TokenType.R_PAREN;
    }
  }

  // This class does the parses the input string and converts it into Tokens
  static final class Lexer {
    private final String s;
    private int i = 0;
    private final List<Token> out = new ArrayList<>();

    Lexer(String input) {
      this.s = input.replace(" ", "").trim();
    }

    // iterates through the input string checking each character to identify tokens
    List<Token> lex() {
      int parenDepth = 0;
      boolean absOpen = false;

      while (i < s.length()) {
        char c = s.charAt(i);

        if (isNumberStart(c)) {
          possibleImplicitMult();
          readNumber(false);
          continue;
        }

        switch (c) {
          case '+':
            if (atStartOrAfterOp()) {
              i++;
            } else {
              pushBinary(TokenType.ADD_OP);
              i++;
            }
            break;
          case '-':
            if (atStartOrAfterOp()) {
              int j = i + 1;
              if (j < s.length() && (isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                possibleImplicitMult();
                readNumber(true);
                continue;
              } else {
                possibleImplicitMult();
                out.add(new Token(TokenType.CONST, -1.0));
                out.add(new Token(TokenType.MULT_OP));
                i++;
              }
            } else {
              pushBinary(TokenType.SUB_OP);
              i++;
            }
            break;
          case '*':
            requirePrevPrimary();
            out.add(new Token(TokenType.MULT_OP));
            i++;
            break;
          case '/':
            requirePrevPrimary();
            out.add(new Token(TokenType.DIV_OP));
            i++;
            break;
          case '^':
            requirePrevPrimary();
            out.add(new Token(TokenType.EXP_OP));
            i++;
            break;
          case '(':
            possibleImplicitMult();
            out.add(new Token(TokenType.L_PAREN));
            i++;
            parenDepth++;
            break;
          case ')':
            if (parenDepth == 0) {
              throw new RuntimeException("Unmatched ')'");
            }

            out.add(new Token(TokenType.R_PAREN));
            i++;
            parenDepth--;
            break;
          case '|':
            if (!absOpen) {
              possibleImplicitMult();
              out.add(new Token(TokenType.ABS_FUNC));
              out.add(new Token(TokenType.L_PAREN));
              absOpen = true;
              parenDepth++;
              i++;
            } else {
              out.add(new Token(TokenType.R_PAREN));
              absOpen = false;
              parenDepth--;
              i++;
            }
            break;
          case 'e':
            possibleImplicitMult();
            out.add(new Token(TokenType.CONST, Math.E));
            i++;
            break;
          case 'p':
            if (peek("pi")) {
              possibleImplicitMult();
              out.add(new Token(TokenType.CONST, Math.PI));
              i += 2;
            } else {
              throw new RuntimeException("Unknown literal starting with 'p'");
            }
            break;
          case 'x':
            possibleImplicitMult();
            out.add(new Token(TokenType.VAR_X));
            i++;
            break;
          case 'y':
            possibleImplicitMult();
            out.add(new Token(TokenType.VAR_Y));
            i++;
            break;
          default:
            Token func = tryFunction();
            if (func != null) {
              possibleImplicitMult();
              out.add(func);
            } else {
              throw new RuntimeException("Unexpected char: '" + c + "'");
            }
        }
      }

      if (parenDepth != 0) {
        throw new RuntimeException("Unmatched '('");
      }
      return out;
    }

    private boolean atStartOrAfterOp() {
      if (out.isEmpty()) {
        return true;
      }

      Token t = out.get(out.size() - 1);
      if (t.type == TokenType.L_PAREN) {
        return true;
      }
      switch (t.type) {
        case ADD_OP:
        case SUB_OP:
        case MULT_OP:
        case DIV_OP:
        case EXP_OP:
          return true;
        default:
          return t.isFunc();
      }
    }

    private void pushBinary(TokenType t) {
      requirePrevPrimary();
      out.add(new Token(t));
    }

    private void requirePrevPrimary() {
      if (out.isEmpty() || !out.get(out.size() - 1).endsPrimary()) {
        throw new RuntimeException("Binary operator missing left operand at " + i);
      }
    }

    // adds a multiplication token if implicit multiplication is present
    // e.g. 5x = 5 * x
    private void possibleImplicitMult() {
      if (!out.isEmpty()) {
        Token prev = out.get(out.size() - 1);
        if (prev.endsPrimary()) {
          out.add(new Token(TokenType.MULT_OP));
        }
      }
    }

    private boolean isNumberStart(char c) {
      if (isDigit(c) || c == '.') {
        return true;
      }

      return false;
    }

    private static boolean isDigit(char c) {
      return c >= '0' && c <= '9';
    }

    private void readNumber(boolean negate) {
      int j = negate ? (i + 1) : i;
      boolean seenDecimal = false;
      if (j < s.length() && s.charAt(j) == '.') {
        seenDecimal = true;
        j++;
      }

      while (j < s.length()) {
        char d = s.charAt(j);

        if (!isDigit(d) && (d != '.' || seenDecimal))
          break;

        if (d == '.' && !seenDecimal)
          seenDecimal = true;

        j++;
      }

      String num = s.substring(i, j);
      double val = Double.parseDouble(num);
      out.add(new Token(TokenType.CONST, val));
      i = j;
    }

    private boolean peek(String str) {
      return s.regionMatches(i, str, 0, str.length());
    }

    // TODO: possibly rewrite this
    private Token tryFunction() {
      String[] names = {
          "sqrt", "asin", "acos", "atan", "sec", "csc", "cot", "sin", "cos", "tan", "log", "ln"
      };

      Map<String, TokenType> map = Map.ofEntries(
          Map.entry("abs", TokenType.ABS_FUNC),
          Map.entry("ln", TokenType.LN_FUNC),
          Map.entry("log", TokenType.LOG_FUNC),
          Map.entry("sin", TokenType.SIN_FUNC),
          Map.entry("cos", TokenType.COS_FUNC),
          Map.entry("tan", TokenType.TAN_FUNC),
          Map.entry("sec", TokenType.SEC_FUNC),
          Map.entry("csc", TokenType.CSC_FUNC),
          Map.entry("cot", TokenType.COT_FUNC),
          Map.entry("sqrt", TokenType.SQRT_FUNC),
          Map.entry("asin", TokenType.ASIN_FUNC),
          Map.entry("acos", TokenType.ACOS_FUNC),
          Map.entry("atan", TokenType.ATAN_FUNC));

      for (String name : names) {
        if (peek(name)) {
          i += name.length();
          return new Token(map.get(name));
        }
      }

      return null;
    }
  }

  static final class Node {
    final TokenType type;
    final double value;

    Node left, right;

    boolean isConstant;
    double constValue;

    Node(TokenType type) {
      this.type = type;
      this.value = Double.NaN;
    }

    Node(TokenType type, double value) {
      this.type = type;
      this.value = value;
    }
  }

  static final class Parser {
    private final List<Token> tokens;
    private int p = 0;

    Parser(List<Token> tokens) {
      this.tokens = tokens;
    }

    Node parse() {
      Node node = parseExpr();

      if (p != tokens.size()) {
        throw new RuntimeException("Extra tokens at end");
      }

      foldConstants(node);
      return node;
    }

    private Node parseExpr() {
      Node node = parseTerm();

      while (p < tokens.size()) {
        TokenType type = tokens.get(p).type;
        if (type == TokenType.ADD_OP || type == TokenType.SUB_OP) {
          p++;
          Node right = parseTerm();
          Node parent = new Node(type);
          parent.left = node;
          parent.right = right;
          node = parent;
        } else {
          break;
        }
      }

      return node;
    }

    private Node parseTerm() {
      Node node = parseFactor();

      while (p < tokens.size()) {
        TokenType type = tokens.get(p).type;
        if (type == TokenType.MULT_OP || type == TokenType.DIV_OP) {
          p++;
          Node right = parseFactor();
          Node parent = new Node(type);
          parent.left = node;
          parent.right = right;
          node = parent;
        } else {
          break;
        }
      }

      return node;
    }

    private Node parseFactor() {
      Node node = parseFunc();

      while (p < tokens.size()) {
        TokenType type = tokens.get(p).type;
        if (type == TokenType.EXP_OP) {
          p++;
          Node right = parseFunc();
          Node parent = new Node(type);
          parent.left = node;
          parent.right = right;
          node = parent;
        } else {
          break;
        }
      }

      return node;
    }

    private Node parseFunc() {
      if (p >= tokens.size()) {
        throw new RuntimeException("Unexpected end");
      }

      Token token = tokens.get(p);

      if (token.type == TokenType.L_PAREN) {
        p++;
        Node inner = parseExpr();
        expect(TokenType.R_PAREN);
        return inner;
      }

      if (token.isFunc()) {
        p++;
        Node arg = parseFuncOrPrimary();
        Node f = new Node(token.type);
        f.left = arg;
        return f;
      }

      return parsePrimary();
    }

    private Node parseFuncOrPrimary() {
      if (p < tokens.size() && tokens.get(p).type == TokenType.L_PAREN) {
        p++;
        Node inner = parseExpr();
        expect(TokenType.R_PAREN);
        return inner;
      }

      if (p < tokens.size() && tokens.get(p).isFunc()) {
        Token f = tokens.get(p++);
        Node arg = parseFuncOrPrimary();
        Node fn = new Node(f.type);
        fn.left = arg;
        return fn;
      }

      return parsePrimary();
    }

    private Node parsePrimary() {
      if (p >= tokens.size()) {
        throw new RuntimeException("Missing primary");
      }

      Token token = tokens.get(p);
      p++;

      if (token.type == TokenType.VAR_X || token.type == TokenType.VAR_Y) {
        return new Node(token.type);
      }

      if (token.type == TokenType.CONST) {
        return new Node(TokenType.CONST, token.value);
      }

      throw new RuntimeException("Bad primary");
    }

    private void expect(TokenType type) {
      if (p >= tokens.size() || tokens.get(p).type != type) {
        throw new RuntimeException("Expected " + type);
      }

      p++;
    }

    // recursively evaluates subtrees that don't contain variables to fold the tree
    private void foldConstants(Node node) {
      if (node == null) {
        return;
      }

      foldConstants(node.left);
      foldConstants(node.right);

      switch (node.type) {
        case CONST:
          node.isConstant = true;
          node.constValue = node.value;
          break;
        case VAR_X:
        case VAR_Y:
          node.isConstant = false;
          break;
        default:
          boolean leftConstant = node.left != null && node.left.isConstant;
          boolean rightConstant = node.right == null || node.right.isConstant;

          node.isConstant = leftConstant && rightConstant;

          if (node.isConstant) {
            node.constValue = evalNode(node, Double.NaN, Double.NaN);
            node.left = null;
            node.right = null;
          }
      }
    }
  }

  static double evaluate(Node node, double x, double y) {
    if (node.isConstant) {
      return node.constValue;
    }

    return evalNode(node, x, y);
  }

  // recursively evaluate the tree at (x, y)
  private static double evalNode(Node node, double x, double y) {
    switch (node.type) {
      case CONST:
        return node.value;
      case VAR_X:
        return x;
      case VAR_Y:
        return y;
      case ADD_OP:
        return evaluate(node.left, x, y) + evaluate(node.right, x, y);
      case SUB_OP:
        return evaluate(node.left, x, y) - evaluate(node.right, x, y);
      case MULT_OP:
        return evaluate(node.left, x, y) * evaluate(node.right, x, y);
      case DIV_OP:
        return evaluate(node.left, x, y) / evaluate(node.right, x, y);
      case EXP_OP:
        return Math.pow(evaluate(node.left, x, y), evaluate(node.right, x, y));
      case ABS_FUNC:
        return Math.abs(evaluate(node.left, x, y));
      case LN_FUNC:
        return Math.log(evaluate(node.left, x, y));
      case LOG_FUNC:
        return Math.log10(evaluate(node.left, x, y));
      case SIN_FUNC:
        return Math.sin(evaluate(node.left, x, y));
      case COS_FUNC:
        return Math.cos(evaluate(node.left, x, y));
      case TAN_FUNC:
        return Math.tan(evaluate(node.left, x, y));
      case SEC_FUNC:
        return 1.0 / Math.cos(evaluate(node.left, x, y));
      case CSC_FUNC:
        return 1.0 / Math.sin(evaluate(node.left, x, y));
      case COT_FUNC:
        return 1.0 / Math.tan(evaluate(node.left, x, y));
      case SQRT_FUNC:
        return Math.sqrt(evaluate(node.left, x, y));
      case ASIN_FUNC:
        return Math.asin(evaluate(node.left, x, y));
      case ACOS_FUNC:
        return Math.acos(evaluate(node.left, x, y));
      case ATAN_FUNC:
        return Math.atan(evaluate(node.left, x, y));
      default:
        throw new RuntimeException("Unknown node: " + node.type);
    }
  }

  public static final class ExpressionParser3D {
    private static final Random RNG = new Random();
    private final Node root;

    public ExpressionParser3D(String expression) {
      List<Token> tokens = new Lexer(expression).lex();
      this.root = new Parser(tokens).parse();
    }

    // Evaluate z = f (x, y)
    public double evaluate(double x, double y) {
      return FunctionParser.evaluate(root, x, y);
    }

    public TriangleMesh buildTriangleMesh(
      double xMin,
      double xMax,
      double yMin,
      double yMax,
      int xSteps,
      int ySteps
    ) {
      if (xSteps < 1 || ySteps < 1)
        throw new IllegalArgumentException("steps must be >= 1");

      int nx = xSteps + 1;
      int ny = ySteps + 1;
      double dx = (xMax - xMin) / xSteps;
      double dy = (yMax - yMin) / ySteps;

      float[] points = new float[nx * ny * 3];
      boolean[] valid = new boolean[nx * ny];

      float[] tex = new float[] { 0f, 0f };
      int[] faces = new int[xSteps * ySteps * 12];
      int facePtr = 0;

      double[] zValues = new double[nx * ny];
      List<Double> finiteZ = new ArrayList<>();
      double minZ = Double.POSITIVE_INFINITY;
      double maxZ = Double.NEGATIVE_INFINITY;

      double[] xCache = new double[nx];
      for (int i = 0; i < nx; i++) {
        xCache[i] = xMin + i * dx;
      }

      int flatIndex = 0;
      for (int j = 0; j < ny; j++) {
        double y = yMin + j * dy;
        for (int i = 0; i < nx; i++) {
          double x = xCache[i];
          double z = evaluate(x, y);
          zValues[flatIndex] = z;

          if (Double.isFinite(z)) {
            finiteZ.add(z);
            if (z < minZ) {
              minZ = z;
            }
            if (z > maxZ) {
              maxZ = z;
            }
          }
          flatIndex++;
        }
      }

      if (finiteZ.isEmpty()) {
        TriangleMesh emptyMesh = new TriangleMesh();
        emptyMesh.getPoints().addAll(points);
        emptyMesh.getTexCoords().addAll(tex);
        return emptyMesh;
      }

      double[] finiteArray = new double[finiteZ.size()];
      for (int k = 0; k < finiteArray.length; k++) {
        finiteArray[k] = finiteZ.get(k);
      }

      double medianZ = median(finiteArray);
      double madZ = medianAbsoluteDeviation(finiteArray, medianZ);

      double clampLow;
      double clampHigh;

      if (madZ == 0.0) {
        clampLow = minZ;
        clampHigh = maxZ;
      } else {
        double k = 4.0;
        clampLow = medianZ - k * madZ;
        clampHigh = medianZ + k * madZ;
      }

      // generate points
      int index = 0;
      flatIndex = 0;
      for (int j = 0; j < ny; j++) {
        double y = yMin + j * dy;

        for (int i = 0; i < nx; i++) {
          double x = xCache[i];
          double z = zValues[flatIndex];

          boolean ok = Double.isFinite(z) && z >= clampLow && z <= clampHigh;
          valid[flatIndex] = ok;

          points[index++] = (float) x;
          points[index++] = (float) -z;
          points[index++] = (float) y;

          flatIndex++;
        }
      }

      // add faces
      for (int j = 0; j < ySteps; j++) {
        for (int i = 0; i < xSteps; i++) {
          int p00 = j * nx + i;
          int p10 = j * nx + (i + 1);
          int p01 = (j + 1) * nx + i;
          int p11 = (j + 1) * nx + (i + 1);

          if (valid[p00] && valid[p01] && valid[p10] && valid[p11]) {
            facePtr = addFace(faces, facePtr, p00, 0, p10, 0, p11, 0);
            facePtr = addFace(faces, facePtr, p00, 0, p11, 0, p01, 0);
          }
        }
      }

      if (facePtr != faces.length) {
        faces = Arrays.copyOf(faces, facePtr);
      }

      TriangleMesh mesh = new TriangleMesh();

      mesh.getPoints().addAll(points);
      mesh.getTexCoords().addAll(tex);
      if (facePtr > 0) {
        mesh.getFaces().addAll(faces);
      }
      return mesh;
    }

    private static double median(double[] values) {
      int n = values.length;
      if (n == 0) return Double.NaN;

      double[] a = Arrays.copyOf(values, n);

      int mid = n / 2;
      if ((n & 1) == 1) {
        return quickSelect(a, 0, n - 1, mid);
      } else {
        double lowerMid = quickSelect(a, 0, n - 1, mid - 1);
        double upperMid = quickSelect(a, 0, n - 1, mid);
        return 0.5 * (lowerMid + upperMid);
      }
    }

    private static double quickSelect(double[] a, int left, int right, int k) {
      while (true) {
        if (left == right) return a[left];

        int p = left + RNG.nextInt(right - left + 1);
        double tmp = a[p];
        a[p] = a[right];
        a[right] = tmp;

        double pivot = a[right];

        int store = left;
        for (int i = left; i < right; i++) {
          if (a[i] < pivot) {
            double t = a[store];
            a[store] = a[i];
            a[i] = t;
            store++;
          }
        }

        double t2 = a[store];
        a[store] = a[right];
        a[right] = t2;

        if (k == store) {
          return a[k];
        } else if (k < store) {
          right = store - 1;
        } else {
          left = store + 1;
        }
      }
    }

    private static double medianAbsoluteDeviation(double[] values, double median) {
      if (values.length == 0) return 0.0;

      double[] deviations = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        deviations[i] = Math.abs(values[i] - median);
      }

      return median(deviations);
    }

    private static int addFace(
        int[] faces,
        int ptr,
        int pointA,
        int texA,
        int pointB,
        int texB,
        int pointC,
        int texC) {
      faces[ptr++] = pointA;
      faces[ptr++] = texA;
      faces[ptr++] = pointB;
      faces[ptr++] = texB;
      faces[ptr++] = pointC;
      faces[ptr++] = texC;
      return ptr;
    }
  }
}
