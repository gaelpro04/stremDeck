import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;

public class macroInterfaz extends Application {
    @Override
    public void start(Stage primaryStage) {

        // StackPane principal para centrar todo
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #1e1e1e;");

        // StackPane para la carcasa y el teclado
        StackPane keyboardContainer = new StackPane();
        Label labelTitulo = new Label("Configuracion de macros");
        labelTitulo.setFont(Font.font("Arial", FontWeight.NORMAL, 17));
        labelTitulo.setStyle("-fx-background-color: rgba(226,213,226,0.11);" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 10;");

        // Carcasa (rectángulo que representa la caja del teclado)
        Rectangle carcasa = new Rectangle(360, 360);
        carcasa.setFill(Color.rgb(40, 40, 40));
        carcasa.setStroke(Color.rgb(60, 60, 60));
        carcasa.setStrokeWidth(3);
        carcasa.setArcWidth(20);
        carcasa.setArcHeight(20);

        // GridPane para los botones
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.setAlignment(Pos.CENTER);

        int rows = 4;
        int cols = 4;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Button btn = new Button("");
                btn.setPrefSize(60, 60);
                btn.setStyle(
                        "-fx-background-color: rgba(226,213,226,0.11);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 10;"
                );

                btn.setOnAction(e -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
                    st.setFromX(1);
                    st.setFromY(1);
                    st.setToX(1.2);
                    st.setToY(1.2);
                    st.setAutoReverse(true);
                    st.setCycleCount(2);
                    st.play();

                    // Cambio de color rápido
                    btn.setStyle(
                            "-fx-background-color: #7d5ba6;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-background-radius: 10;"
                    );

                    // Volver al color original después de la animación
                    st.setOnFinished(ev -> btn.setStyle(
                            "-fx-background-color: rgba(226,213,226,0.11);" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-background-radius: 10;"
                    ));
                    System.out.println("Botón presionado: " + btn.getText());
                });

                grid.add(btn, j, i);
            }
        }

        // Agregar carcasa y grid al contenedor
        keyboardContainer.getChildren().addAll(carcasa, grid);

        // Agregar el contenedor al root
        root.getChildren().add(keyboardContainer);
        root.setAlignment(Pos.CENTER);
        root.getChildren().add(labelTitulo);
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Configuracion Stream Deck");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

