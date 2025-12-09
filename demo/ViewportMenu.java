import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ViewportMenu extends HBox {
  private final Viewport viewport;
  private final TextField xField;
  private final TextField yField;
  private final TextField zField;
  private final TextField alphaField;
  private final TextField thetaField;
  private final TextField rField;
  private final TextField deltaXField;
  private final TextField deltaYField;
  private final Button updateButton;

  ViewportMenu(Viewport viewport) {
    this.viewport = viewport;

    setAlignment(Pos.CENTER);
    setPadding(new Insets(8));
    setSpacing(8);

    Label xLabel = new Label("x =");
    xLabel.getStyleClass().add("function-label");
    Label yLabel = new Label("y =");
    yLabel.getStyleClass().add("function-label");
    Label zLabel = new Label("z =");
    zLabel.getStyleClass().add("function-label");
    Label alphaLabel = new Label("α =");
    alphaLabel.getStyleClass().add("function-label");
    Label thetaLabel = new Label("θ =");
    thetaLabel.getStyleClass().add("function-label");
    Label rLabel = new Label("r =");
    rLabel.getStyleClass().add("function-label");
    Label dxLabel = new Label("Δx =");
    dxLabel.getStyleClass().add("function-label");
    Label dyLabel = new Label("Δy =");
    dyLabel.getStyleClass().add("function-label");

    xLabel.setMinWidth(Region.USE_PREF_SIZE);
    yLabel.setMinWidth(Region.USE_PREF_SIZE);
    zLabel.setMinWidth(Region.USE_PREF_SIZE);
    alphaLabel.setMinWidth(Region.USE_PREF_SIZE);
    thetaLabel.setMinWidth(Region.USE_PREF_SIZE);
    rLabel.setMinWidth(Region.USE_PREF_SIZE);
    dxLabel.setMinWidth(Region.USE_PREF_SIZE);
    dyLabel.setMinWidth(Region.USE_PREF_SIZE);

    xField = new TextField();
    yField = new TextField();
    zField = new TextField();
    alphaField = new TextField();
    thetaField = new TextField();
    rField = new TextField();
    deltaXField = new TextField("10.0");
    deltaYField = new TextField("10.0");

    HBox.setHgrow(xField, Priority.ALWAYS);
    HBox.setHgrow(yField, Priority.ALWAYS);
    HBox.setHgrow(zField, Priority.ALWAYS);
    HBox.setHgrow(alphaField, Priority.ALWAYS);
    HBox.setHgrow(thetaField, Priority.ALWAYS);
    HBox.setHgrow(rField, Priority.ALWAYS);

    updateButton = new Button("Update");
    updateButton.getStyleClass().add("function-button");
    updateButton.setMinWidth(75);

    getChildren().addAll(
      xLabel,
      xField,
      yLabel,
      yField,
      zLabel,
      zField,
      alphaLabel,
      alphaField,
      thetaLabel,
      thetaField,
      rLabel,
      rField,
      dxLabel,
      deltaXField,
      dyLabel,
      deltaYField,
      updateButton
    );

    xField.setOnAction(e -> applyToViewport());
    yField.setOnAction(e -> applyToViewport());
    zField.setOnAction(e -> applyToViewport());
    alphaField.setOnAction(e -> applyToViewport());
    thetaField.setOnAction(e -> applyToViewport());
    rField.setOnAction(e -> applyToViewport());
    deltaXField.setOnAction(e -> applyToViewport());
    deltaYField.setOnAction(e -> applyToViewport());
    updateButton.setOnAction(e -> applyToViewport());

    viewport.setViewportMenu(this);
    syncFromViewport();
  }

  void syncFromViewport() {
    xField.setText(format(viewport.getCenterX()));
    yField.setText(format(viewport.getCenterY()));
    zField.setText(format(viewport.getCenterZ()));
    alphaField.setText(format(viewport.getAlpha()));
    thetaField.setText(format(viewport.getTheta()));
    rField.setText(format(viewport.getRadius()));
    deltaXField.setText(format(viewport.getDeltaX()));
    deltaYField.setText(format(viewport.getDeltaY()));
  }

  private void applyToViewport() {
    double x = parseOrFallback(xField, viewport.getCenterX());
    double y = parseOrFallback(yField, viewport.getCenterY());
    double z = parseOrFallback(zField, viewport.getCenterZ());
    double alpha = parseOrFallback(alphaField, viewport.getAlpha());
    double theta = parseOrFallback(thetaField, viewport.getTheta());
    double r = parseOrFallback(rField, viewport.getRadius());
    double dx = parseOrFallback(deltaXField, viewport.getDeltaX());
    double dy = parseOrFallback(deltaYField, viewport.getDeltaY());

    viewport.setView(x, y, z, alpha, theta, r, dx, dy);
  }

  private static double parseOrFallback(TextField field, double fallback) {
    try {
      return Double.parseDouble(field.getText().trim());
    } catch (NumberFormatException ex) {
      field.setText(format(fallback));
      return fallback;
    }
  }

  private static String format(double v) {
    return String.format("%.3f", v);
  }
}
