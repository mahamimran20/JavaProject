import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application {
  private static final String css = """
    .root {
      -fx-background-color: #1e293b;
    }

    .function-menu {
      -fx-padding: 8;
      -fx-spacing: 8;
      -fx-alignment: CENTER_LEFT;
    }

    .function-label {
      -fx-text-fill: white;
    }

    .function-input {
      -fx-pref-width: 0;
    }

    .function-button {
      -fx-background-radius: 999px;
      -fx-background-color: #3b82f6;
      -fx-text-fill: white;
      -fx-padding: 6 16;
      -fx-effect: dropshadow(gaussian, rgba(37,99,235,0.8), 4, 0.1, 0, 1);
      -fx-cursor: hand;
    }

    .function-button:hover {
      -fx-background-color: #2563eb;
      -fx-effect: dropshadow(gaussian, rgba(30,64,175,0.8), 4, 0.1, 0, 1);
    }

    .red-button {
      -fx-background-radius: 999px;
      -fx-background-color: #f63c3c;
      -fx-text-fill: white;
      -fx-padding: 6 16;
      -fx-effect: dropshadow(gaussian, rgba(235,36,36,0.8), 4, 0.1, 0, 1);
      -fx-cursor: hand;
    }

    .red-button:hover {
      -fx-background-color: #eb2424;
      -fx-effect: dropshadow(gaussian, rgba(174,30,30,0.8), 4, 0.1, 0, 1);
    }
  """;

  @Override
  public void start(Stage stage) {
    FunctionMenu functionMenu = new FunctionMenu();
    Viewport viewport = new Viewport();
    ViewportMenu viewportMenu = new ViewportMenu(viewport);
    GradDescMenu gradDescMenu = new GradDescMenu(viewport);

    VBox leftColumn = new VBox();
    leftColumn.getChildren().addAll(functionMenu, viewport, viewportMenu);

    leftColumn.setMinWidth(0);
    leftColumn.setMinHeight(0);

    HBox.setHgrow(leftColumn, Priority.ALWAYS);
    HBox.setHgrow(gradDescMenu, Priority.NEVER);
    VBox.setVgrow(viewport, Priority.ALWAYS);
    leftColumn.setFillWidth(true);

    HBox root = new HBox();
    root.getChildren().addAll(leftColumn, gradDescMenu);
    root.setMinHeight(0);

    Scene scene = new Scene(root, 1200, 600);
    scene.getStylesheets().add("data:text/css," + css.replace("\n", "%0A"));

    functionMenu.getInput().setOnAction(e -> {
      String expr = functionMenu.getFunctionText();
      try {
        if (viewport.updateFunction(expr)) {
          gradDescMenu.clearConfigs();
        }
      } catch (RuntimeException ex){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid function");
        alert.setHeaderText(null);
        alert.setContentText("The function is invalid:\n" + ex.getMessage());
        alert.showAndWait();
      }
    });

    functionMenu.getFunctionButton().setOnAction(e -> {
      String expr = functionMenu.getFunctionText();
      try {
        if (viewport.updateFunction(expr)) {
          gradDescMenu.clearConfigs();
        }
      } catch (RuntimeException ex){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid function");
        alert.setHeaderText(null);
        alert.setContentText("The function is invalid:\n" + ex.getMessage());
        alert.showAndWait();
      }
    });

    stage.setScene(scene);
    stage.setTitle("Gradient Descent Visualization");
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
