import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class FunctionMenu extends HBox {
  private final TextField input;
  private final Button functionButton;

  FunctionMenu() {
    getStyleClass().add("function-menu");
    setAlignment(Pos.CENTER);

    Label label = new Label("f(x, y) =");
    label.getStyleClass().add("function-label");

    input = new TextField();
    input.getStyleClass().add("function-input");
    HBox.setHgrow(input, Priority.ALWAYS);
    input.setMaxWidth(Double.MAX_VALUE);

    functionButton = new Button("Save");
    functionButton.getStyleClass().add("function-button");

    getChildren().addAll(label, input, functionButton);
  }

  public String getFunctionText() {
    return input.getText();
  }

  public TextField getInput() {
    return input;
  }

  public Button getFunctionButton() {
    return functionButton;
  }
}
