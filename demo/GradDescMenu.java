import java.util.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class GradDescMenu extends VBox {
  private static class GradDescConfig {
    String name;
    Color color;
    String method;
    String initialPosition;
    GradientDescent.InitialPoint initialPoint;
    int numPoints;
    double learningRate;
    double beta;
    GradientDescent gradDesc;
  }

  private static final double DEFAULT_MIN_X = -5.0;
  private static final double DEFAULT_MAX_X = 5.0;
  private static final double DEFAULT_MIN_Y = -5.0;
  private static final double DEFAULT_MAX_Y = 5.0;

  private final List<GradDescConfig> items = new ArrayList<>();
  private final VBox listContainer;
  private final Viewport viewport;
  private boolean animationDialogOpen = false;

  GradDescMenu(Viewport viewport) {
    this.viewport = viewport;
    setPadding(new Insets(16));
    setSpacing(12);
    setMinWidth(240);
    setPrefWidth(240);
    setMinHeight(0);

    Label title = new Label("Gradient Descents");
    title.getStyleClass().add("function-label");
    title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    Button addButton = new Button("Add");
    addButton.getStyleClass().add("function-button");
    addButton.setMaxWidth(Double.MAX_VALUE);

    ScrollPane scrollPane = new ScrollPane();
    scrollPane.setFitToWidth(true);
    scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setStyle("-fx-background-color: #0f172a; -fx-background: #0f172a;");

    listContainer = new VBox(6);
    listContainer.setPadding(new Insets(6));
    listContainer.setFillWidth(true);
    scrollPane.setContent(listContainer);
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    Button runButton = new Button("Run");
    runButton.getStyleClass().add("function-button");
    runButton.setMaxWidth(Double.MAX_VALUE);

    getChildren().addAll(title, addButton, scrollPane, runButton);

    addButton.setOnAction(e -> {
      if (!ensureFunctionPresent()) {
        return;
      }
      openEditor(null);
    });

    runButton.setOnAction(e -> {
      if (!ensureFunctionPresent()) {
        return;
      }

      if (items.isEmpty()) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No gradient descents");
        alert.setHeaderText(null);
        alert.setContentText("At least one gradient descent configuration is required to run. Click the \"Add\" button to add one.");
        alert.showAndWait();
        return;
      }

      if (animationDialogOpen) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Animation already open");
        alert.setHeaderText(null);
        alert.setContentText("The animation controls are already open. Close them before running again.");
        alert.showAndWait();
        return;
      }

      runAndShowAnimation();
    });
  }

  private void refreshList() {
    listContainer.getChildren().clear();
    for (GradDescConfig cfg : items) {
      listContainer.getChildren().add(createItemView(cfg));
    }
  }

  public void clearConfigs() {
    items.clear();
    refreshList();
    if (viewport != null) {
      viewport.clearPoints();
    }
  }

  private Node createItemView(GradDescConfig cfg) {
    VBox itemView = new VBox(8);
    itemView.setAlignment(Pos.CENTER_LEFT);
    itemView.setPadding(new Insets(8));
    itemView.setStyle("-fx-background-color: #020617; -fx-background-radius: 8;");

    Region colorSwatch = new Region();
    colorSwatch.setMinSize(18, 18);
    colorSwatch.setMaxSize(18, 18);
    colorSwatch.setStyle(
        "-fx-background-color: " + toWebColor(cfg.color) + ";" +
        "-fx-background-radius: 999;" +
        "-fx-border-radius: 4;"
    );

    Label nameLabel = new Label(cfg.name != null ? cfg.name : "(unnamed)");
    nameLabel.getStyleClass().add("function-label");

    HBox row = new HBox(8);
    row.getChildren().addAll(colorSwatch, nameLabel);

    Label methodLabel = new Label(cfg.method);
    methodLabel.getStyleClass().add("function-label");
    methodLabel.setStyle("-fx-opacity: 0.8;");

    Label positionLabel = new Label(cfg.initialPosition + " (n=" + cfg.numPoints + ")");
    positionLabel.getStyleClass().add("function-label");
    positionLabel.setStyle("-fx-opacity: 0.8;");

    Button editButton = new Button("Edit");
    editButton.getStyleClass().add("function-button");
    editButton.setOnAction(e -> openEditor(cfg));

    Button deleteButton = new Button("Delete");
    deleteButton.getStyleClass().add("red-button");
    deleteButton.setOnAction(e -> deleteItem(cfg));

    HBox buttonRow = new HBox();
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    buttonRow.getChildren().addAll(editButton, spacer, deleteButton);

    itemView.getChildren().addAll(row, methodLabel, positionLabel, buttonRow);
    return itemView;
  }

  private void openEditor(GradDescConfig existing) {
    boolean isNew = (existing == null);

    GradDescConfig working = new GradDescConfig();
    if (isNew) {
      working.name            = "Untitled";
      working.color           = Color.ORANGE;
      working.method          = "Basic Gradient Descent";
      working.initialPoint    = GradientDescent.InitialPoint.GRID;
      working.initialPosition = "Grid";
      working.numPoints       = 10;
      working.learningRate    = 0.1;
      working.beta            = 0.9;
    } else {
      working.name            = existing.name;
      working.color           = existing.color;
      working.method          = existing.method;
      working.initialPoint    = existing.initialPoint;
      working.initialPosition = existing.initialPosition;
      working.numPoints       = existing.numPoints > 0 ? existing.numPoints : 10;
      working.learningRate    = existing.learningRate > 0 ? existing.learningRate : 0.1;
      working.beta            = existing.beta != 0.0 ? existing.beta : 0.9;
    }

    if (isNew) {
      working.gradDesc = createGradDesc(working.method, working.learningRate, working.beta);
      initializePoints(working);
    } else if (existing.gradDesc != null) {
      working.gradDesc = cloneGradDesc(existing.gradDesc, working.learningRate, working.beta);
    } else {
      working.gradDesc = createGradDesc(working.method, working.learningRate, working.beta);
    }

    previewInitialPoints(working);

    Stage dialog = new Stage();
    if (getScene() != null && getScene().getWindow() != null) {
      dialog.initOwner(getScene().getWindow());
    }
    dialog.setTitle(isNew ? "Add Gradient Descent" : "Edit Gradient Descent");

    VBox root = new VBox(10);
    root.setPadding(new Insets(12));

    Label nameLabel = new Label("Name:");
    TextField nameField = new TextField();
    if (working.name != null) {
      nameField.setText(working.name);
    }

    Label colorLabel = new Label("Color:");
    ColorPicker colorPicker = new ColorPicker(working.color != null ? working.color : Color.ORANGE);
    colorPicker.setMaxWidth(Double.MAX_VALUE);

    Label methodLabel = new Label("Method:");
    ComboBox<String> methodBox = new ComboBox<>();
    methodBox.setMaxWidth(Double.MAX_VALUE);
    methodBox.getItems().addAll(
      "Basic Gradient Descent",
      "Momentum",
      "Nesterov Momentum",
      "Newton's Method",
      "BFGS"
    );
    if (working.method != null) {
      methodBox.setValue(working.method);
    } else {
      methodBox.getSelectionModel().selectFirst();
      working.method = methodBox.getValue();
    }

    Label initLabel = new Label("Initial Points:");
    ComboBox<String> initBox = new ComboBox<>();
    initBox.setMaxWidth(Double.MAX_VALUE);
    initBox.getItems().addAll("Grid", "Random");

    if (working.initialPoint != null) {
      initBox.setValue(
        working.initialPoint == GradientDescent.InitialPoint.GRID ? "Grid" : "Random"
      );
    } else {
      initBox.setValue("Grid");
      working.initialPoint = GradientDescent.InitialPoint.GRID;
      working.initialPosition = "Grid";
    }

    Label nLabel = new Label("n:");
    TextField nField = new TextField();
    nField.setText(Integer.toString(working.numPoints > 0 ? working.numPoints : 10));

    Label lrLabel = new Label("Learning Rate:");
    TextField lrField = new TextField(
      Double.toString(working.learningRate > 0 ? working.learningRate : 0.1)
    );

    Label betaLabel = new Label("β:");
    TextField betaField = new TextField(
      Double.toString(working.beta != 0.0 ? working.beta : 0.9)
    );

    // SVG source (https://lucide.dev/icons/refresh-ccw)
    Button refreshButton = new Button();
    SVGPath p1 = new SVGPath();
    p1.setContent("M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8");
    SVGPath p2 = new SVGPath();
    p2.setContent("M3 3v5h5");
    SVGPath p3 = new SVGPath();
    p3.setContent("M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16");
    SVGPath p4 = new SVGPath();
    p4.setContent("M16 16h5v5");
    p1.setStroke(Color.BLACK);
    p2.setStroke(Color.BLACK);
    p3.setStroke(Color.BLACK);
    p4.setStroke(Color.BLACK);
    p1.setFill(null);
    p2.setFill(null);
    p3.setFill(null);
    p4.setFill(null);
    Group g = new Group(p1, p2, p3, p4);
    g.setScaleX(0.75);
    g.setScaleY(0.75);
    refreshButton.setGraphic(g);

    refreshButton.setPadding(new Insets(3));
    refreshButton.setMaxWidth(Double.MAX_VALUE);
    updateRefreshState(refreshButton, working.initialPoint);

    Button saveButton = new Button("Save");
    saveButton.setMaxWidth(Double.MAX_VALUE);

    updateBetaVisibility(methodBox.getValue(), betaLabel, betaField);

    methodBox.setOnAction(e -> {
      working.method = methodBox.getValue();
      updateBetaVisibility(working.method, betaLabel, betaField);

      if ("Newton's Method".equals(working.method)) {
        lrLabel.setText("Step Size:");
      } else {
        lrLabel.setText("Learning Rate:");
      }

      working.learningRate = parseDoubleField(lrField, working.learningRate);
      working.beta = parseDoubleField(betaField, working.beta);

      working.gradDesc = createGradDesc(working.method, working.learningRate, working.beta);
      initializePoints(working);
      previewInitialPoints(working);
    });

    colorPicker.setOnAction(e -> {
      working.color = colorPicker.getValue();
      previewInitialPoints(working);
    });

    initBox.setOnAction(e -> {
      working.initialPoint = getInitialPoint(initBox.getValue());
      working.initialPosition = initBox.getValue();
      updateRefreshState(refreshButton, working.initialPoint);
      working.numPoints = parseNumPoints(nField, working.numPoints);
      initializePoints(working);
      previewInitialPoints(working);
    });

    nField.textProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue == null || newValue.trim().isEmpty()) {
        return;
      }

      int parsed = parseNumPointsText(newValue, working.numPoints);
      if (parsed != working.numPoints) {
        working.numPoints = parsed;
        initializePoints(working);
        previewInitialPoints(working);
      }
    });

    lrField.textProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue == null || newValue.trim().isEmpty()) {
        return;
      }
      working.learningRate = parseDoubleText(newValue, working.learningRate);
    });

    betaField.textProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue == null || newValue.trim().isEmpty()) {
        return;
      }
      working.beta = parseDoubleText(newValue, working.beta);
    });


    refreshButton.setOnAction(e -> {
      working.numPoints = parseNumPoints(nField, working.numPoints);
      if (working.initialPoint != GradientDescent.InitialPoint.GRID) {
        initializePoints(working);
        previewInitialPoints(working);
      }
    });

    saveButton.setOnAction(e -> {
      String name = nameField.getText().trim();
      working.name = name.isEmpty() ? "Untitled" : name;
      working.color = colorPicker.getValue();
      working.method = methodBox.getValue();
      working.initialPosition = initBox.getValue();
      working.initialPoint = getInitialPoint(initBox.getValue());
      working.numPoints = parseNumPoints(nField, working.numPoints);
      working.learningRate = parseDoubleField(lrField, working.learningRate);
      working.beta = parseDoubleField(betaField, working.beta);

      if (isNew) {
        items.add(working);
      } else {
        existing.name            = working.name;
        existing.color           = working.color;
        existing.method          = working.method;
        existing.initialPoint    = working.initialPoint;
        existing.initialPosition = working.initialPosition;
        existing.numPoints       = working.numPoints;
        existing.learningRate    = working.learningRate;
        existing.beta            = working.beta;
        existing.gradDesc        = working.gradDesc;
      }

      refreshList();
      if (viewport != null) {
        viewport.clearPoints();
      }
      dialog.close();
    });

    dialog.setOnCloseRequest(e -> {
      if (viewport != null) {
        viewport.clearPoints();
      }
    });

    HBox nameBox = new HBox(12);
    nameBox.getChildren().addAll(nameLabel, nameField);
    HBox colorBox = new HBox(12);
    colorBox.getChildren().addAll(colorLabel, colorPicker);
    HBox methodBoxOuter = new HBox(12);
    methodBoxOuter.getChildren().addAll(methodLabel, methodBox);
    HBox initBoxOuter = new HBox(12);
    initBoxOuter.getChildren().addAll(initLabel, initBox, refreshButton);
    HBox nBox = new HBox(12);
    nBox.getChildren().addAll(nLabel, nField);
    HBox lrBox = new HBox(12);
    lrBox.getChildren().addAll(lrLabel, lrField);
    HBox betaBox = new HBox(12);
    betaBox.getChildren().addAll(betaLabel, betaField);

    nameLabel.setMinWidth(Region.USE_PREF_SIZE);
    colorLabel.setMinWidth(Region.USE_PREF_SIZE);
    methodLabel.setMinWidth(Region.USE_PREF_SIZE);
    initLabel.setMinWidth(Region.USE_PREF_SIZE);
    nLabel.setMinWidth(Region.USE_PREF_SIZE);
    lrLabel.setMinWidth(Region.USE_PREF_SIZE);
    betaLabel.setMinWidth(Region.USE_PREF_SIZE);

    nameBox.setAlignment(Pos.CENTER_LEFT);
    colorBox.setAlignment(Pos.CENTER_LEFT);
    methodBoxOuter.setAlignment(Pos.CENTER_LEFT);
    initBoxOuter.setAlignment(Pos.CENTER_LEFT);
    nBox.setAlignment(Pos.CENTER_LEFT);
    lrBox.setAlignment(Pos.CENTER_LEFT);
    betaBox.setAlignment(Pos.CENTER_LEFT);

    HBox.setHgrow(nameField, Priority.ALWAYS);
    HBox.setHgrow(colorPicker, Priority.ALWAYS);
    HBox.setHgrow(methodBox, Priority.ALWAYS);
    HBox.setHgrow(initBox, Priority.ALWAYS);
    HBox.setHgrow(nField, Priority.ALWAYS);
    HBox.setHgrow(lrField, Priority.ALWAYS);
    HBox.setHgrow(betaField, Priority.ALWAYS);

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    root.getChildren().addAll(
      nameBox,
      colorBox,
      methodBoxOuter,
      initBoxOuter,
      nBox,
      lrBox,
      betaBox,
      spacer,
      saveButton
    );

    Scene scene = new Scene(root);
    dialog.setScene(scene);
    dialog.setWidth(300);
    dialog.setMinWidth(300);
    dialog.setHeight(350);
    dialog.setMinHeight(350);
    dialog.show();
  }

  private void deleteItem(GradDescConfig cfg) {
    items.remove(cfg);
    refreshList();
  }

  private static String toWebColor(Color c) {
    if (c == null) return "#ffffff";
    int r = (int) Math.round(c.getRed() * 255);
    int g = (int) Math.round(c.getGreen() * 255);
    int b = (int) Math.round(c.getBlue() * 255);
    return String.format("#%02x%02x%02x", r, g, b);
  }

  private GradientDescent createGradDesc(String method, double learningRate, double beta) {
    FunctionParser.ExpressionParser3D parser = (viewport != null) ? viewport.getParser() : null;

    if ("Momentum".equals(method)) {
      return new MomentumGradDesc(parser, learningRate, beta);
    } else if ("Nesterov Momentum".equals(method)) {
      return new NesterovGradDesc(parser, learningRate, beta);
    } else if ("Newton's Method".equals(method)) {
      return new NewtonGradDesc(parser, learningRate);
    } else if ("BFGS".equals(method)) {
      return new BfgsGradDesc(parser, learningRate);
    } 
    return new BasicGradDesc(parser, learningRate);
  }

  private static GradientDescent.InitialPoint getInitialPoint(String value) {
    if ("Random".equalsIgnoreCase(value)) {
      return GradientDescent.InitialPoint.RANDOM;
    }
    return GradientDescent.InitialPoint.GRID;
  }

  private static int parseNumPoints(TextField field, int fallback) {
    try {
      int n = Integer.parseInt(field.getText().trim());
      if (n > 0) return n;
    } catch (NumberFormatException e) {}

    field.setText(Integer.toString(fallback > 0 ? fallback : 10));
    return fallback > 0 ? fallback : 10;
  }

  private static int parseNumPointsText(String text, int fallback) {
    if (text == null) return fallback;
    String trimmed = text.trim();
    if (trimmed.isEmpty()) return fallback;
    try {
      int n = Integer.parseInt(trimmed);
      return (n > 0) ? n : fallback;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static double parseDoubleField(TextField field, double fallback) {
    try {
      double val = Double.parseDouble(field.getText().trim());
      if (Double.isFinite(val)) return val;
    } catch (NumberFormatException e) {}

    field.setText(Double.toString(fallback));
    return fallback;
  }

  private static double parseDoubleText(String text, double fallback) {
    if (text == null) return fallback;
    String trimmed = text.trim();
    if (trimmed.isEmpty()) return fallback;
    try {
      double val = Double.parseDouble(trimmed);
      return Double.isFinite(val) ? val : fallback;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private void updateRefreshState(Button refreshButton, GradientDescent.InitialPoint initialPoint) {
    boolean disable = initialPoint == GradientDescent.InitialPoint.GRID;
    refreshButton.setDisable(disable);
    refreshButton.setOpacity(disable ? 0.5 : 1.0);
  }

  private void initializePoints(GradDescConfig cfg) {
    if (cfg.gradDesc == null || cfg.initialPoint == null || cfg.numPoints <= 0) return;

    double minX = viewport.getPointsMinX();
    double maxX = viewport.getPointsMaxX();
    double minY = viewport.getPointsMinY();
    double maxY = viewport.getPointsMaxY();

    cfg.gradDesc.initializePoints(cfg.initialPoint, minX, maxX, minY, maxY, cfg.numPoints);
  }

  private void previewInitialPoints(GradDescConfig cfg) {
    if (viewport == null || cfg == null || cfg.gradDesc == null) {
      if (viewport != null) {
        viewport.clearPoints();
      }
      return;
    }

    List<List<GradientDescent.Point>> epochs = cfg.gradDesc.getEpochs();
    if (epochs == null || epochs.isEmpty() || epochs.get(0) == null) {
      viewport.clearPoints();
      return;
    }

    List<GradientDescent.Point> initial = epochs.get(0);

    List<List<GradientDescent.Point>> allPoints = new ArrayList<>();
    allPoints.add(initial);

    List<Color> colors = new ArrayList<>();
    colors.add(cfg.color != null ? cfg.color : Color.RED);

    viewport.showPoints(allPoints, colors);
  }

  private GradientDescent cloneGradDesc(GradientDescent source, double learningRate, double beta) {
    if (source == null) return null;

    FunctionParser.ExpressionParser3D parser = source.parser;

    double lr = (learningRate > 0.0) ? learningRate : 0.1;
    double b = (beta != 0.0) ? beta : 0.9;

    GradientDescent clone;
    if (source instanceof MomentumGradDesc) {
      clone = new MomentumGradDesc(parser, lr, b);
    } else if (source instanceof NesterovGradDesc) {
      clone = new NesterovGradDesc(parser, lr, b);
    } else if (source instanceof NewtonGradDesc) {
      clone = new NewtonGradDesc(parser, lr);
    } else if (source instanceof BfgsGradDesc) {
      clone = new BfgsGradDesc(parser, lr);
    } else {
      clone = new BasicGradDesc(parser, lr);
    }

    List<List<GradientDescent.Point>> srcEpochs = source.getEpochs();
    List<List<GradientDescent.Point>> dstEpochs = clone.getEpochs();

    for (List<GradientDescent.Point> srcEpoch : srcEpochs) {
      if (srcEpoch == null) {
        dstEpochs.add(null);
        continue;
      }

      List<GradientDescent.Point> dstEpoch = new ArrayList<>();
      for (GradientDescent.Point p : srcEpoch) {
        if (p == null) continue;
        dstEpoch.add(new GradientDescent.Point(p.x, p.y, p.z));
      }
      dstEpochs.add(dstEpoch);
    }

    return clone;
  }

  private boolean ensureFunctionPresent() {
    if (viewport == null || viewport.getParser() == null) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("No function defined");
      alert.setHeaderText(null);
      alert.setContentText("A function is required to add or run gradient descents");
      alert.showAndWait();
      return false;
    }
    return true;
  }

  private static void updateBetaVisibility(String method, Label betaLabel, TextField betaField) {
    boolean needsBeta = "Momentum".equals(method) || "Nesterov Momentum".equals(method);

    betaLabel.setVisible(needsBeta);
    betaLabel.setManaged(needsBeta);
    betaField.setVisible(needsBeta);
    betaField.setManaged(needsBeta);
  }
  
  private void runAndShowAnimation() {
    if (viewport == null) {
      return;
    }

    List<List<List<GradientDescent.Point>>> allEpochs = new ArrayList<>();
    List<Color> colors = new ArrayList<>();
    int maxEpochs = 0;

    for (GradDescConfig cfg : items) {
      if (cfg == null || cfg.gradDesc == null) continue;

      cfg.gradDesc.run();

      List<List<GradientDescent.Point>> epochs = cfg.gradDesc.getEpochs();
      if (epochs == null || epochs.isEmpty()) {
        continue;
      }

      allEpochs.add(epochs);
      colors.add(cfg.color != null ? cfg.color : Color.RED);
      if (epochs.size() > maxEpochs) {
        maxEpochs = epochs.size();
      }
    }

    if (allEpochs.isEmpty() || maxEpochs == 0) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("No epochs to display");
      alert.setHeaderText(null);
      alert.setContentText("No gradient descent epochs were produced.");
      alert.showAndWait();
      return;
    }

    openAnimationDialog(allEpochs, colors, maxEpochs);
  }

  private void updateViewportEpoch(int epochIndex, List<List<List<GradientDescent.Point>>> allEpochs, List<Color> colors) {
    if (viewport == null) return;

    List<List<GradientDescent.Point>> pointsForEpoch = new ArrayList<>();
    List<Color> colorsForEpoch = new ArrayList<>();

    for (int i = 0; i < allEpochs.size(); i++) {
      List<List<GradientDescent.Point>> epochs = allEpochs.get(i);
      if (epochs == null || epochs.isEmpty()) continue;

      int idx = Math.min(epochIndex, epochs.size() - 1);
      List<GradientDescent.Point> pts = epochs.get(idx);
      if (pts == null || pts.isEmpty()) continue;

      pointsForEpoch.add(pts);
      colorsForEpoch.add(colors.get(i));
    }

    viewport.showPoints(pointsForEpoch, colorsForEpoch);
  }

  private void openAnimationDialog(List<List<List<GradientDescent.Point>>> allEpochs, List<Color> colors, int maxEpochs) {
    animationDialogOpen = true;

    final int[] currentEpoch = {0};
    final boolean[] playing = {false};

    Stage dialog = new Stage();
    if (getScene() != null && getScene().getWindow() != null) {
      dialog.initOwner(getScene().getWindow());
    }
    dialog.setTitle("Gradient Descent Animation");

    VBox root = new VBox(8);
    root.setPadding(new Insets(12));

    Label epochLabel = new Label();

    Slider epochSlider = new Slider();
    epochSlider.setMin(0);
    epochSlider.setMax(maxEpochs);
    epochSlider.setValue(0);
    epochSlider.setMajorTickUnit(1);
    epochSlider.setMinorTickCount(0);
    epochSlider.setSnapToTicks(true);
    epochSlider.setShowTickMarks(false);
    epochSlider.setShowTickLabels(false);
    epochSlider.getStylesheets().add("data:text/css,.slider .track { -fx-background-color: black; }");

    final boolean[] sliderChanging = new boolean[]{false};

    // SVG source (https://lucide.dev/icons/play)
    SVGPath playIcon = new SVGPath();
    playIcon.setContent("M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z");
    playIcon.setFill(null);
    playIcon.setStroke(Color.BLACK);
    Button playPauseButton = new Button();
    playPauseButton.setGraphic(playIcon);

    // SVG source (https://lucide.dev/icons/pause)
    Rectangle r1 = new Rectangle(14, 3, 5, 18);
    r1.setFill(null);
    r1.setStroke(Color.BLACK);
    r1.setArcWidth(2);
    r1.setArcHeight(2);
    Rectangle r2 = new Rectangle(5, 3, 5, 18);
    r2.setFill(null);
    r2.setStroke(Color.BLACK);
    r2.setArcWidth(2);
    r2.setArcHeight(2);
    Group pauseIcon = new Group(r1, r2);

    // SVG source (https://lucide.dev/icons/step-forward)
    SVGPath p1 = new SVGPath();
    p1.setContent("M10.029 4.285A2 2 0 0 0 7 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z");
    p1.setFill(null);
    p1.setStroke(Color.BLACK);
    SVGPath p2 = new SVGPath();
    p2.setContent("M3 4v16");
    p2.setFill(null);
    p2.setStroke(Color.BLACK);
    Group stepForwardIcon = new Group(p1, p2);
    Button stepForwardButton = new Button();
    stepForwardButton.setGraphic(stepForwardIcon);

    // SVG source (https://lucide.dev/icons/step-back)
    SVGPath p3 = new SVGPath();
    p3.setContent("M13.971 4.285A2 2 0 0 1 17 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z");
    p3.setFill(null);
    p3.setStroke(Color.BLACK);
    SVGPath p4 = new SVGPath();
    p4.setContent("M21 20V4");
    p4.setFill(null);
    p4.setStroke(Color.BLACK);
    Group stepBackwardIcon = new Group(p3, p4);
    Button stepBackwardButton = new Button();
    stepBackwardButton.setGraphic(stepBackwardIcon);

    // SVG source (https://lucide.dev/icons/skip-back)
    SVGPath p5 = new SVGPath();
    p5.setContent("M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z");
    p5.setFill(null);
    p5.setStroke(Color.BLACK);
    SVGPath p6 = new SVGPath();
    p6.setContent("M3 20V4");
    p6.setFill(null);
    p6.setStroke(Color.BLACK);
    Group startIcon = new Group(p5, p6);
    Button startButton = new Button();
    startButton.setGraphic(startIcon);

    // SVG source (https://lucide.dev/icons/skip-forward)
    SVGPath p7 = new SVGPath();
    p7.setContent("M21 4v16");
    p7.setFill(null);
    p7.setStroke(Color.BLACK);
    SVGPath p8 = new SVGPath();
    p8.setContent("M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z");
    p8.setFill(null);
    p8.setStroke(Color.BLACK);
    Group endIcon = new Group(p7, p8);
    Button endButton = new Button();
    endButton.setGraphic(endIcon);

    HBox controls = new HBox(6, startButton, stepBackwardButton, playPauseButton, stepForwardButton, endButton);
    controls.setAlignment(Pos.CENTER);

    Label resultLabel = new Label(computeResultSummary());
    resultLabel.setWrapText(true);
    resultLabel.setMaxWidth(Double.MAX_VALUE);
    resultLabel.setAlignment(Pos.CENTER);
    resultLabel.setTextAlignment(TextAlignment.CENTER);
    HBox.setHgrow(resultLabel, Priority.ALWAYS);

    root.getChildren().addAll(epochLabel, epochSlider, controls, resultLabel);

    Scene scene = new Scene(root);
    dialog.setScene(scene);
    dialog.setMinWidth(410);
    dialog.setWidth(410);
    dialog.setMinHeight(175);
    dialog.setHeight(175);


    updateViewportEpoch(currentEpoch[0], allEpochs, colors);
    epochLabel.setText("Epoch " + currentEpoch[0] + " / " + (maxEpochs-1));

    AnimationTimer timer = new AnimationTimer() {
      private long lastUpdate = 0L;
      private static final long STEP_NANOS = 100_000_000L;

      @Override
      public void handle(long now) {
        if (!playing[0]) {
          return;
        }
        if (lastUpdate == 0L) {
          lastUpdate = now;
          return;
        }
        if (now - lastUpdate >= STEP_NANOS) {
          if (currentEpoch[0] < maxEpochs - 1) {
            currentEpoch[0]++;
            updateViewportEpoch(currentEpoch[0], allEpochs, colors);
            epochLabel.setText("Epoch " + currentEpoch[0] + " / " + (maxEpochs-1));

            sliderChanging[0] = true;
            epochSlider.setValue(currentEpoch[0]);
            sliderChanging[0] = false;
          } else {
            playing[0] = false;
            playPauseButton.setGraphic(playIcon);
          }
          lastUpdate = now;
        }
      }
    };
    timer.start();

    epochSlider.valueProperty().addListener((obs, oldV, newV) -> {
      if (sliderChanging[0]) return;

      int newEpoch = newV.intValue();
      currentEpoch[0] = newEpoch;
      epochLabel.setText("Epoch " + newEpoch + " / " + (maxEpochs-1));

      updateViewportEpoch(currentEpoch[0], allEpochs, colors);
    });

    playPauseButton.setOnAction(e -> {
      playing[0] = !playing[0];
      playPauseButton.setGraphic(playing[0] ? pauseIcon : playIcon);
    });

    stepForwardButton.setOnAction(e -> {
      if (currentEpoch[0] < maxEpochs - 1) {
        currentEpoch[0]++;
        updateViewportEpoch(currentEpoch[0], allEpochs, colors);
        epochLabel.setText("Epoch " + currentEpoch[0] + " / " + (maxEpochs-1));

        sliderChanging[0] = true;
        epochSlider.setValue(currentEpoch[0]);
        sliderChanging[0] = false;
      }
    });

    stepBackwardButton.setOnAction(e -> {
      if (currentEpoch[0] > 0) {
        currentEpoch[0]--;
        updateViewportEpoch(currentEpoch[0], allEpochs, colors);
        epochLabel.setText("Epoch " + currentEpoch[0] + " / " + (maxEpochs-1));

        sliderChanging[0] = true;
        epochSlider.setValue(currentEpoch[0]);
        sliderChanging[0] = false;
      }
    });

    startButton.setOnAction(e -> {
      currentEpoch[0] = 0;
      updateViewportEpoch(currentEpoch[0], allEpochs, colors);
      epochLabel.setText("Epoch " + currentEpoch[0] + " / " + (maxEpochs-1));

      sliderChanging[0] = true;
      epochSlider.setValue(currentEpoch[0]);
      sliderChanging[0] = false;
    });

    endButton.setOnAction(e -> {
      currentEpoch[0] = maxEpochs - 1;
      updateViewportEpoch(currentEpoch[0], allEpochs, colors);
      epochLabel.setText("Epoch " + currentEpoch[0] + " / " + (maxEpochs-1));

      sliderChanging[0] = true;
      epochSlider.setValue(currentEpoch[0]);
      sliderChanging[0] = false;
    });

    dialog.setOnCloseRequest(e -> {
      animationDialogOpen = false;
      playing[0] = false;
      timer.stop();
      if (viewport != null) {
        viewport.clearPoints();
      }
    });

    dialog.show();
  }

  private String computeResultSummary() {
    double bestZ = Double.POSITIVE_INFINITY;
    GradientDescent.Point bestPoint = null;
    String bestConfigName = null;
    String bestConfigMethod = null;

    boolean anyFiniteInLastEpoch = false;

    for (GradDescConfig cfg : items) {
      if (cfg == null || cfg.gradDesc == null) continue;

      List<List<GradientDescent.Point>> epochs = cfg.gradDesc.getEpochs();
      if (epochs == null || epochs.isEmpty()) continue;

      for (List<GradientDescent.Point> epoch : epochs) {
        if (epoch == null) continue;

        for (GradientDescent.Point p : epoch) {
          if (p == null) continue;
          double z = p.z;
          if (!Double.isFinite(z)) continue;

          if (z < bestZ) {
            bestZ = z;
            bestPoint = p;
            bestConfigName = (cfg.name != null && !cfg.name.isEmpty()) ? cfg.name : "(unnamed)";
            bestConfigMethod = cfg.method;
          }
        }
      }

      List<GradientDescent.Point> lastEpoch = epochs.get(epochs.size() - 1);
      if (lastEpoch != null) {
        for (GradientDescent.Point p : lastEpoch) {
          if (p == null) continue;
          if (Double.isFinite(p.z)) {
            anyFiniteInLastEpoch = true;
            break;
          }
        }
      }
    }

    if (bestPoint == null) {
      return "Result: all points diverged";
    }

    String base = String.format("Best minimum found at (x = %.4f, y = %.4f, z = %.4f) by \"%s\" (%s)", bestPoint.x, bestPoint.y, bestPoint.z, bestConfigName, bestConfigMethod);

    if (!anyFiniteInLastEpoch) {
      return "Result: all points diverged. " + base + " (found before divergence)";
    }

    return "Result: " + base;
  }
}
