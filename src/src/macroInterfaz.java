import com.fazecast.jSerialComm.SerialPort;
import javafx.animation.TranslateTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class macroInterfaz extends Application {
    private boolean menuAbierto = false;
    private Communication esp32;

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

        //menu
        VBox menu = new VBox(15);
        menu.setPrefWidth(200);
        menu.setMaxWidth(200);
        menu.setStyle("-fx-background-color: #2a2a2a;");
        menu.setAlignment(Pos.TOP_CENTER);

        Button cargarImagenBtn = new Button("Subir imagen");
        cargarImagenBtn.setPrefWidth(160);
        cargarImagenBtn.setStyle(
                "-fx-background-color: #444;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 10;"
        );

        menu.getChildren().add(cargarImagenBtn);

        Button configurar = new Button("Configurar");
        configurar.setPrefWidth(160);
        configurar.setStyle(
                "-fx-background-color: #555;"+
                "-fx-text-fill: white;"+
                "-fx-background-radius: 10;"+
                "-fx-padding: 10;"
        );

        ComboBox<String> combo = new ComboBox<>();
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            combo.getItems().add(port.getSystemPortName());
        }

        combo.setOnAction(e -> {
            int index = combo.getSelectionModel().getSelectedIndex();
            esp32 = new Communication(index);
        });

        menu.getChildren().add(combo);

        configurar.setOnAction(e ->{
            if (esp32 == null) {
                System.out.println("No hay puerto seleccionado");
                return;
            }
            esp32.write("Hola mundo\n");
        });

        menu.getChildren().add(configurar);

        cargarImagenBtn.setOnAction(e -> {
            FileChooser imagenIcono = new FileChooser();
            imagenIcono.setTitle("Seleccione una imagen");
            imagenIcono.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
            );

            File archivo = imagenIcono.showOpenDialog(primaryStage);

            if (archivo != null) {
                Image image = new Image(archivo.toURI().toString());
                ImageView iv = new ImageView(image);
                iv.setFitWidth(60);
                iv.setFitHeight(60);
                iv.setPreserveRatio(true);

                // Convertir a bytes binarios
                byte[] imageBytes = convertirAArduinoBytes(image);

                System.out.println("=== Enviando Imagen ===");
                System.out.println("Bytes totales: " + imageBytes.length);

                if(esp32 != null) {
                    // Primero enviar el comando "IMG:\n"
                    esp32.write("IMG:");

                    // Luego enviar los bytes binarios
                    esp32.writeBytes(imageBytes);

                    System.out.println("✓ Imagen enviada al ESP32");
                } else {
                    System.err.println("⚠ Error: Puerto serial no configurado");
                }

                menu.getChildren().removeIf(nodo -> nodo instanceof ImageView);
                menu.getChildren().add(iv);
            }
        });

        StackPane menuContainer = new StackPane();
        menuContainer.getChildren().add(menu);
        menuContainer.setMaxWidth(200);
        menuContainer.setTranslateX(500);

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

                    if(!menuAbierto){
                        abrirMenu(menu, keyboardContainer);
                    } else{
                        cerrarMenu(menu, keyboardContainer);
                    }
                    menuAbierto = !menuAbierto;

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
        root.getChildren().addAll(keyboardContainer, menuContainer);
        root.setAlignment(Pos.CENTER);
        root.getChildren().add(labelTitulo);
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Configuracion Stream Deck");
        primaryStage.show();
    }

    private void abrirMenu(VBox menu, StackPane teclado) {
        StackPane menuContainer = (StackPane) menu.getParent();
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), menuContainer);
        tt.setToX(300);  //entra a la pantalla

        TranslateTransition keys = new TranslateTransition(Duration.millis(300), teclado);
        keys.setToX(-100); //se mueve el teclado

        tt.play();
        keys.play();
    }

    private void cerrarMenu(VBox menu, StackPane teclado) {
        StackPane menuContainer = (StackPane) menu.getParent();
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), menuContainer);
        tt.setToX(500); //se esconde fuera

        TranslateTransition keys = new TranslateTransition(Duration.millis(300), teclado);
        keys.setToX(0); //regresa el teclado

        tt.play();
        keys.play();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static byte[] convertirAArduinoBytes(Image img) {
        int width = 120;
        int height = 60;
        int umbral = 220;

        // Convertir Image de JavaFX a BufferedImage
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffered.createGraphics();

        // Fondo BLANCO
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Calcular escalado para preservar relación de aspecto
        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();
        double scale = Math.min((double)width / imgWidth, (double)height / imgHeight);

        int scaledWidth = (int)(imgWidth * scale);
        int scaledHeight = (int)(imgHeight * scale);

        // Calcular posición para centrar
        int x = (width - scaledWidth) / 2;
        int y = (height - scaledHeight) / 2;

        // Dibujar imagen escalada con calidad
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(SwingFXUtils.fromFXImage(img, null), x, y, scaledWidth, scaledHeight, null);
        g.dispose();

        // Crear array de bytes
        int bytesPerRow = (width + 7) / 8; // 15 bytes por fila
        int totalBytes = bytesPerRow * height; // 15 * 60 = 900 bytes
        byte[] imageData = new byte[totalBytes];
        int byteIndex = 0;

        // Procesar por filas (Y)
        for (int yPos = 0; yPos < height; yPos++) {
            for (int byteCol = 0; byteCol < bytesPerRow; byteCol++) {
                int byteValue = 0;

                // Procesar 8 píxeles horizontales
                for (int bit = 0; bit < 8; bit++) {
                    int xPos = byteCol * 8 + bit;
                    if (xPos < width) {
                        int pixel = buffered.getRGB(xPos, yPos);
                        int r = (pixel >> 16) & 0xff;
                        int gC = (pixel >> 8) & 0xff;
                        int b = pixel & 0xff;
                        int gray = (int)(r * 0.299 + gC * 0.587 + b * 0.114);

                        // 1 = NEGRO, 0 = BLANCO - MSB first
                        if (gray < umbral) {
                            byteValue |= (1 << (7 - bit));
                        }
                    }
                }

                imageData[byteIndex] = (byte) byteValue;
                byteIndex++;
            }
        }

        System.out.println("Bytes generados: " + byteIndex);
        return imageData;
    }
}

