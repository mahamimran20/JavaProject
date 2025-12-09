import java.util.*;
import javafx.animation.AnimationTimer;
import javafx.scene.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;

public class Viewport extends StackPane {
  private final SubScene subScene;
  private final Group root3D;
  private final Group worldGroup;
  private final PerspectiveCamera camera;
  private final MeshView meshView;
  private final Group cameraGroup;
  private final Group pointsGroup = new Group();
  private final List<List<Sphere>> pointSpheresSets = new ArrayList<>();
  private final List<Color> pointColors = new ArrayList<>();

  private static final double MESH_REBUILD_THRESHOLD = 0.25;
  private final AnimationTimer meshTimer;
  private double meshCenterX;
  private double meshCenterY;
  private boolean meshNeedsUpdate = false;
  private boolean forceFullMeshRebuild = false;
  
  private double centerX = 0.0;
  private double centerY = 0.0;
  private double centerZ = 0.0;
  private double alpha = -60.0;
  private double theta = 0.0;
  private double radius = 15.0;
  private double deltaX = 10.0;
  private double deltaY = 10.0;

  private double mouseLastX;
  private double mouseLastY;

  private ViewportMenu viewportMenu;
  private FunctionParser.ExpressionParser3D parser;

  Viewport() {
    setMinWidth(0);
    setMinHeight(0);

    meshCenterX = centerX;
    meshCenterY = centerY;

    root3D = new Group();
    cameraGroup = new Group();
    worldGroup = new Group();

    camera = new PerspectiveCamera(true);
    camera.setNearClip(0.1);
    camera.setFarClip(10000.0);
    camera.setFieldOfView(60);

    cameraGroup.getChildren().add(camera);

    meshView = new MeshView();
    PhongMaterial material = new PhongMaterial();
    material.setDiffuseColor(Color.LIGHTBLUE);
    material.setSpecularColor(Color.WHITE);
    meshView.setMaterial(material);
    meshView.setDrawMode(DrawMode.LINE);
    meshView.setCullFace(CullFace.NONE);

    worldGroup.getChildren().addAll(meshView, pointsGroup);

    root3D.getChildren().addAll(worldGroup, cameraGroup);

    subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
    subScene.setFill(Color.web("#0f172a"));
    subScene.setCamera(camera);

    subScene.widthProperty().bind(widthProperty());
    subScene.heightProperty().bind(heightProperty());

    getChildren().add(subScene);

    meshTimer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        if (parser == null) {
          return;
        }

        double dx = Math.abs(centerX - meshCenterX);
        double dy = Math.abs(centerY - meshCenterY);

        if (forceFullMeshRebuild || (meshNeedsUpdate && (dx > MESH_REBUILD_THRESHOLD || dy > MESH_REBUILD_THRESHOLD))) {
          rebuildMesh();
          meshNeedsUpdate = false;
          forceFullMeshRebuild = false;
        }
      }
    };
    meshTimer.start();

    setupMouseControls();
    updateCameraPosition();
  }

  private void setupMouseControls() {
    subScene.setOnMousePressed((MouseEvent e) -> {
      mouseLastX = e.getSceneX();
      mouseLastY = e.getSceneY();
    });

    subScene.setOnMouseDragged((MouseEvent e) -> {
      double deltaX = e.getSceneX() - mouseLastX;
      double deltaY = e.getSceneY() - mouseLastY;
      mouseLastX = e.getSceneX();
      mouseLastY = e.getSceneY();

      if (e.isPrimaryButtonDown()) {
        theta += deltaX * 0.5;
        alpha -= deltaY * 0.5;

        alpha = Math.max(-89.9, Math.min(89.9, alpha));

        if (theta >= 360.0 || theta < 0.0) {
          theta %= 360.0;
          if (theta < 0) theta += 360.0;
        }

        updateCameraPosition();
        if (viewportMenu != null) {
          viewportMenu.syncFromViewport();
        }
      } else if (e.isSecondaryButtonDown()) {
        double sensitivity = radius * 0.002;

        double alphaRad = Math.toRadians(alpha);
        double thetaRad = Math.toRadians(theta);

        double rightX = Math.cos(thetaRad);
        double rightY = 0;
        double rightZ = -Math.sin(thetaRad);

        double upX = -Math.sin(thetaRad) * Math.sin(alphaRad);
        double upY = Math.cos(alphaRad);
        double upZ = -Math.cos(thetaRad) * Math.sin(alphaRad);

        centerX -= (rightX * deltaX - upX * deltaY) * sensitivity;
        centerY -= (rightZ * deltaX - upZ * deltaY) * sensitivity;
        centerZ -= (rightY * deltaX - upY * deltaY) * sensitivity;

        meshNeedsUpdate = true;

        updateCameraPosition();
        if (viewportMenu != null) {
          viewportMenu.syncFromViewport();
        }
      }
    });

    subScene.setOnScroll((ScrollEvent e) -> {
      double delta = e.getDeltaY();
      double factor = delta > 0 ? 0.9 : 1.1;
      radius *= factor;

      radius = Math.max(0.5, radius);

      updateCameraPosition();
      if (viewportMenu != null) {
        viewportMenu.syncFromViewport();
      }
    });
  }

  private void updateCameraPosition() {
    worldGroup.getTransforms().setAll(new Translate(-centerX, centerZ, -centerY));

    cameraGroup.getTransforms().setAll(
      new Rotate(theta, Rotate.Y_AXIS),
      new Rotate(alpha, Rotate.X_AXIS)
    );

    camera.getTransforms().setAll(new Translate(0, 0, -radius));
  }

  boolean updateFunction(String expr) {
    if (expr == null || expr.trim().isEmpty()) {
      return false;
    }

    parser = new FunctionParser.ExpressionParser3D(expr);

    try {
      double z = parser.evaluate(0.0, 0.0);
      if (Double.isFinite(z)) {
        centerZ = z;
      } else {
        centerZ = 0.0;
      }
    } catch (Exception e) {
      centerZ = 0.0;
    }

    forceFullMeshRebuild = true;
    meshNeedsUpdate = true;

    clearPoints();
    updateCameraPosition();
    if (viewportMenu != null) {
      viewportMenu.syncFromViewport();
    }

    return true;
  }

  void showPoints(List<List<GradientDescent.Point>> allPoints, List<Color> colors) {
    clearPoints();
    if (allPoints == null || allPoints.isEmpty()) {
      return;
    }

    for (int i = 0; i < allPoints.size(); i++) {
      List<GradientDescent.Point> points = allPoints.get(i);
      if (points == null || points.isEmpty()) {
        continue;
      }

      Color c = Color.RED;
      if (colors != null && i < colors.size() && colors.get(i) != null) {
        c = colors.get(i);
      }
      pointColors.add(c);

      List<Sphere> spheresForAlg = new ArrayList<>();

      for (GradientDescent.Point p : points) {
        if (p == null || !Double.isFinite(p.x) || !Double.isFinite(p.y) || !Double.isFinite(p.z)) {
          continue;
        }

        Sphere sphere = new Sphere(0.12);
        PhongMaterial mat = new PhongMaterial(c);
        sphere.setMaterial(mat);

        sphere.setTranslateX(p.x);
        sphere.setTranslateY(-p.z);
        sphere.setTranslateZ(p.y);

        pointsGroup.getChildren().add(sphere);
        spheresForAlg.add(sphere);
      }

      pointSpheresSets.add(spheresForAlg);
    }
  }

  void updatePoints(List<List<GradientDescent.Point>> allPoints) {
    if (allPoints == null) return;

    int algCount = Math.min(pointSpheresSets.size(), allPoints.size());

    for (int i = 0; i < algCount; i++) {
      List<Sphere> spheresForAlg = pointSpheresSets.get(i);
      List<GradientDescent.Point> points = allPoints.get(i);

      if (points == null) {
        for (Sphere s : spheresForAlg) {
          s.setVisible(false);
        }
        continue;
      }

      int n = Math.min(spheresForAlg.size(), points.size());
      for (int j = 0; j < n; j++) {
        GradientDescent.Point p = points.get(j);
        Sphere sphere = spheresForAlg.get(j);

        if (p == null || !Double.isFinite(p.x) || !Double.isFinite(p.y) || !Double.isFinite(p.z)) {
          sphere.setVisible(false);
        } else {
          sphere.setVisible(true);
          sphere.setTranslateX(p.x);
          sphere.setTranslateY(-p.z);
          sphere.setTranslateZ(p.y);
        }
      }

      for (int j = n; j < spheresForAlg.size(); j++) {
        spheresForAlg.get(j).setVisible(false);
      }
    }

    for (int i = algCount; i < pointSpheresSets.size(); i++) {
      for (Sphere s : pointSpheresSets.get(i)) {
        s.setVisible(false);
      }
    }
  }

  void clearPoints() {
    pointsGroup.getChildren().clear();
    pointSpheresSets.clear();
    pointColors.clear();
  }

  private void rebuildMesh() {
    if (parser == null) return;

    meshCenterX = centerX;
    meshCenterY = centerY;

    double xMin = meshCenterX - (deltaX / 2.0);
    double xMax = meshCenterX + (deltaX / 2.0);
    double yMin = meshCenterY - (deltaY / 2.0);
    double yMax = meshCenterY + (deltaY / 2.0);

    int xSteps = (int) Math.round((xMax - xMin) * 10.0);
    int ySteps = (int) Math.round((yMax - yMin) * 10.0);

    TriangleMesh mesh = parser.buildTriangleMesh(
      xMin, xMax,
      yMin, yMax,
      xSteps, ySteps
    );

    meshView.setMesh(mesh);
  }

  void setViewportMenu(ViewportMenu menu) {
    this.viewportMenu = menu;
  }

  void setView(double x, double y, double z, double alpha, double theta, double r, double deltaX, double deltaY) {
    boolean rebuild = (this.deltaX != deltaX || this.deltaY != deltaY);
    this.centerX = x;
    this.centerY = y;
    this.centerZ = z;
    this.alpha = alpha;
    this.theta = theta;
    this.radius = r;
    this.deltaX = deltaX;
    this.deltaY = deltaY;

    meshNeedsUpdate = true;
    if (rebuild) {
      forceFullMeshRebuild = true;
    }

    updateCameraPosition();
  }

  double getCenterX() { return centerX; }
  double getCenterY() { return centerY; }
  double getCenterZ() { return centerZ; }
  double getAlpha() { return alpha; }
  double getTheta() { return theta; }
  double getRadius() { return radius; }
  double getDeltaX() { return deltaX; }
  double getDeltaY() { return deltaY; }
  FunctionParser.ExpressionParser3D getParser() { return parser; }
  double getPointsMinX() { return centerX - (deltaX / 2.0); }
  double getPointsMaxX() { return centerX + (deltaX / 2.0); }
  double getPointsMinY() { return centerY - (deltaY / 2.0); }
  double getPointsMaxY() { return centerY + (deltaY / 2.0); }
}
