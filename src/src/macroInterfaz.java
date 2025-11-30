import com.fazecast.jSerialComm.SerialPort;
import javafx.animation.TranslateTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
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
import java.io.*;
import java.util.HashMap;

public class macroInterfaz extends Application {
    private boolean menuAbierto = false;
    private Communication esp32;
    private HashMap<String, MacroBoton> botones = new HashMap<>();
    private MacroBoton botonSeleccionado = null;
    private static final String CONFIG_FILE = "C:\\Users\\sgsg_\\IdeaProjects\\stremDeck\\macro_config.dat";
    private ComboBox<String> menuMacros;
    private ImageView previewImagen;
    private Label labelBotonSeleccionado;
    private Image image;

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
        VBox menu = new VBox(25);
        menu.setPrefWidth(200);
        menu.setMaxWidth(200);
        menu.setStyle("-fx-background-color: #2a2a2a;");
        menu.setAlignment(Pos.TOP_CENTER);

        VBox seccionCerrarMenu = new VBox(5);
        seccionCerrarMenu.setAlignment(Pos.BASELINE_LEFT);

        Button cerrarMenu = new Button("×");
        cerrarMenu.setPrefWidth(5);
        cerrarMenu.setStyle( "-fx-background-color: #555;"+
                "-fx-text-fill: white;"+
                "-fx-background-radius: 5;"+
                "-fx-padding: 5;"
        );
        cerrarMenu.setOnAction(e -> {
            cerrarMenu(menu, keyboardContainer);
        });

        seccionCerrarMenu.getChildren().add(cerrarMenu);

        VBox seccionSuperior = new VBox(15);
        seccionSuperior.setAlignment(Pos.TOP_CENTER);

        //Boton para agregar la imagen
        Button cargarImagenBtn = new Button("Subir imagen");
        cargarImagenBtn.setPrefWidth(160);
        cargarImagenBtn.setStyle(
                "-fx-background-color: #444;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 10;"
        );

        //donde va ir la imagen que ingrese el usuario
        previewImagen = new ImageView();
        previewImagen.setFitWidth(60);
        previewImagen.setFitHeight(60);
        previewImagen.setPreserveRatio(true);
        previewImagen.setVisible(false);

        // REEMPLAZA COMPLETAMENTE el setOnAction del botón cargarImagenBtn

        cargarImagenBtn.setOnAction(e -> {
            // VALIDAR que haya un botón seleccionado
            if (botonSeleccionado == null) {
                System.err.println("❌ Error: Debes seleccionar un botón primero");
                return;
            }

            System.out.println("\n📂 Abriendo selector de archivos...");

            FileChooser imagenIcono = new FileChooser();
            imagenIcono.setTitle("Seleccione una imagen");
            imagenIcono.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
            );

            File archivo = imagenIcono.showOpenDialog(primaryStage);

            if (archivo != null) {
                try {
                    String ruta = archivo.getAbsolutePath();

                    System.out.println("📸 Cargando imagen...");
                    System.out.println("   Archivo: " + archivo.getName());
                    System.out.println("   Ruta: " + ruta);

                    // Cargar la imagen
                    Image nuevaImagen = new Image(archivo.toURI().toString());

                    // CRÍTICO: Verificar que la imagen se cargó correctamente
                    if (nuevaImagen.isError()) {
                        System.err.println("❌ Error al cargar la imagen");
                        System.err.println("   La imagen puede estar corrupta o en formato no soportado");
                        return;
                    }

                    // Actualizar la imagen global
                    image = nuevaImagen;

                    // Guardar en el botón seleccionado
                    botonSeleccionado.setIcono(image);
                    botonSeleccionado.setRutaIcono(ruta);

                    // Mostrar preview
                    previewImagen.setImage(image);
                    previewImagen.setVisible(true);

                    System.out.println("✅ Imagen cargada correctamente");
                    System.out.println("   Para botón [" +
                            botonSeleccionado.getRow() + "," +
                            botonSeleccionado.getCol() + "]");
                    System.out.println("   Dimensiones: " +
                            (int)image.getWidth() + "x" + (int)image.getHeight());

                } catch (Exception ex) {
                    System.err.println("❌ Error al procesar la imagen:");
                    ex.printStackTrace();
                }
            } else {
                System.out.println("⚠️  Selección de imagen cancelada");
            }
        });

        ComboBox<String> combo = new ComboBox<>();
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            combo.getItems().add(port.getSystemPortName());
        }

        combo.setOnAction(e -> {
            int index = combo.getSelectionModel().getSelectedIndex();
            esp32 = new Communication(index);
        });

        seccionSuperior.getChildren().addAll(cargarImagenBtn, previewImagen, combo);

        VBox seccionMedia = new VBox(15);
        seccionMedia.setAlignment(Pos.TOP_CENTER);

        Label mensajeMacros = new Label("Macros");
        mensajeMacros.setStyle("-fx-text-fill: #F5F5DC; -fx-font-size: 16px; -fx-font-weight: bold;");

        //opciones de macros, poner los que son
        menuMacros = new ComboBox<>();
        menuMacros.getItems().addAll("", "Macro 1", "Macro 2", "Macro 3", "Macro 4");
        menuMacros.setValue("");

        seccionMedia.getChildren().addAll(mensajeMacros, menuMacros);

        VBox seccionInferior = new VBox();
        seccionInferior.setAlignment(Pos.BOTTOM_CENTER);

        Button borrar = new Button("Borrar");
        borrar.setPrefWidth(160);
        borrar.setStyle(
                "-fx-background-color: #555;"+
                        "-fx-text-fill: white;"+
                        "-fx-background-radius: 10;"+
                        "-fx-padding: 10;"
        );

        //reiniciamos los valores para borrar todo y lo guardamos
        // REEMPLAZA el setOnAction del botón borrar

        borrar.setOnAction(e -> {
            if (botonSeleccionado == null) {
                System.err.println("❌ Error: No hay botón seleccionado");
                return;
            }

            System.out.println("\n🗑️ Borrando configuración del botón [" +
                    botonSeleccionado.getRow() + "," +
                    botonSeleccionado.getCol() + "]");

            // Limpiar datos del botón
            botonSeleccionado.setRutaIcono("");
            botonSeleccionado.setMacro("");
            botonSeleccionado.setLabel("");
            botonSeleccionado.setIcono(null);

            // Limpiar interfaz
            menuMacros.setValue("");
            previewImagen.setImage(null);
            previewImagen.setVisible(false);

            // CRÍTICO: Limpiar la referencia global
            image = null;

            // Guardar cambios
            guardarConfiguracion();
            actualizarLabelBoton(botonSeleccionado);

            System.out.println("✅ Configuración borrada");
        });

        Button testBtn = new Button("🧪 TEST ESP32");
        testBtn.setPrefWidth(160);
        testBtn.setStyle(
                "-fx-background-color: #ff6b6b;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 10;"
        );

        // Agrega este botón en tu interfaz para resetear todo
        Button resetConfig = new Button("🗑️ Reset Config");
        resetConfig.setOnAction(e -> {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                configFile.delete();
                System.out.println("✓ Configuración eliminada");
                System.out.println("  Reinicia la aplicación para empezar de cero");
            }
        });

        Region espaciadorExtra1 = new Region();
        espaciadorExtra1.setPrefHeight(5);



        //Boton para que se guarde la configuracion final
        Button configurar = new Button("Configurar");
        configurar.setPrefWidth(160);
        configurar.setStyle(
                "-fx-background-color: #555;"+
                        "-fx-text-fill: white;"+
                        "-fx-background-radius: 10;"+
                        "-fx-padding: 10;"
        );

        // REEMPLAZA SOLO ESTA SECCIÓN EN TU CÓDIGO
// Busca donde está el botón "configurar" y reemplaza su setOnAction por esto:

        configurar.setOnAction(e -> {
            // VALIDACIONES PRIMERO
            if (esp32 == null) {
                System.err.println("❌ Error: Puerto serial no configurado");
                System.err.println("   Por favor selecciona un puerto COM primero");
                return;
            }

            if (botonSeleccionado == null) {
                System.err.println("❌ Error: No hay botón seleccionado");
                return;
            }

            if (image == null) {
                System.err.println("❌ Error: No se ha cargado ninguna imagen");
                System.err.println("   Por favor carga una imagen primero");
                return;
            }

            System.out.println("\n════════════════════════════════════");
            System.out.println("⚙️  CONFIGURANDO BOTÓN [" +
                    botonSeleccionado.getRow() + "," +
                    botonSeleccionado.getCol() + "]");
            System.out.println("════════════════════════════════════");

            // 1. Configurar macro y label
            String macroSeleccionada = menuMacros.getValue();
            if (macroSeleccionada == null || macroSeleccionada.isEmpty()) {
                System.out.println("⚠️  Advertencia: No se seleccionó ningún macro");
            } else {
                botonSeleccionado.setMacro(macroSeleccionada);
                botonSeleccionado.setLabel(macroSeleccionada);
                System.out.println("✓ Macro configurado: " + macroSeleccionada);
            }

            // 2. Convertir imagen a bytes
            System.out.println("\n📸 Procesando imagen...");
            byte[] imageBytes = convertirAArduinoBytes(image);

            if (imageBytes == null || imageBytes.length != 900) {
                System.err.println("❌ Error: Conversión de imagen falló");
                System.err.println("   Bytes generados: " + (imageBytes != null ? imageBytes.length : 0));
                return;
            }

            System.out.println("✓ Imagen convertida correctamente (900 bytes)");

            // 3. Enviar imagen al ESP32
            System.out.println("\n📡 Enviando imagen al ESP32...");

            try {
                // IMPORTANTE: Ya NO envíes "IMG:\n" aquí
                // sendImage() ya lo hace internamente
                esp32.sendImage(imageBytes);
                System.out.println("✓ Imagen enviada al ESP32 exitosamente");

            } catch (Exception ex) {
                System.err.println("❌ Error al enviar imagen:");
                ex.printStackTrace();
                return;
            }

            // 4. Guardar configuración
            System.out.println("\n💾 Guardando configuración...");
            guardarConfiguracion();
            actualizarLabelBoton(botonSeleccionado);

            System.out.println("════════════════════════════════════");
            System.out.println("✅ CONFIGURACIÓN COMPLETADA");
            System.out.println("════════════════════════════════════\n");
        });

        Region espaciadorExtra = new Region();
        espaciadorExtra.setPrefHeight(5);

        seccionInferior.getChildren().addAll(configurar, espaciadorExtra, borrar);
        seccionInferior.getChildren().add(resetConfig);

        javafx.scene.layout.Region espaciador1 = new javafx.scene.layout.Region();
        espaciador1.setPrefHeight(5); // Espacio entre sección cerrar menu y superior

        javafx.scene.layout.Region espaciador2 = new javafx.scene.layout.Region();
        espaciador2.setPrefHeight(30); // Espacio entre sección media e inferior

        javafx.scene.layout.Region espaciador3 = new javafx.scene.layout.Region();
        espaciador2.setPrefHeight(30); // Espacio entre sección media e inferior

        menu.getChildren().addAll(seccionCerrarMenu, espaciador1, seccionSuperior, espaciador2, seccionMedia, espaciador3, seccionInferior);

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

                MacroBoton macroB = new MacroBoton(i,j,btn);
                String key = i + "-" + j;
                botones.put(key, macroB);

                btn.setOnAction(e -> {
                    botonSeleccionado = macroB;
                    actualizarMenuConBoton(macroB);
                    ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
                    st.setFromX(1);
                    st.setFromY(1);
                    st.setToX(1.2);
                    st.setToY(1.2);
                    st.setAutoReverse(true);
                    st.setCycleCount(2);
                    st.play();

                    abrirMenu(menu, keyboardContainer);

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
                    //System.out.println(botonSeleccionado.getRow() + ", " + botonSeleccionado.getCol());
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

        cargarConfiguracion();

        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("\n🚪 Cerrando aplicación...");

            if (esp32 != null) {
                try {
                    // Enviar RESET en lugar de CLEAR
                    System.out.println("📡 Enviando comando RESET al ESP32...");
                    esp32.write("RESET");

                    Thread.sleep(500);  // Dar tiempo a procesar

                    // Cerrar puerto
                    esp32.close();

                    System.out.println("✅ ESP32 reseteado correctamente");

                } catch (Exception e) {
                    System.err.println("⚠️ Error al cerrar: " + e.getMessage());
                }
            }

            System.out.println("👋 Aplicación cerrada\n");
        });
    }

    private byte[] crearImagenDePrueba() {
        // Crear imagen de 120x60 = 900 bytes
        byte[] testImage = new byte[900];

        // LLENAR TODO DE NEGRO primero (0xFF = todos los bits en 1)
        for (int i = 0; i < 900; i++) {
            testImage[i] = (byte) 0xFF; // Todo negro
        }

        // Ahora hacer un marco blanco y una X blanca
        int bytesPerRow = 15; // 120 / 8 = 15

        // Marco blanco (filas 0-5 y 55-59, columnas 0-10 y 110-119)
        for (int y = 0; y < 60; y++) {
            for (int x = 0; x < 120; x++) {
                boolean esBorde = (y < 5 || y > 54 || x < 10 || x > 109);

                if (esBorde) {
                    int byteIndex = y * bytesPerRow + (x / 8);
                    int bitIndex = 7 - (x % 8);
                    testImage[byteIndex] &= ~(1 << bitIndex); // Poner en 0 (blanco)
                }
            }
        }

        // Dibujar X blanca en el centro
        for (int i = 10; i < 50; i++) {
            // Diagonal principal
            int x1 = 30 + i;
            int y1 = 10 + i;
            int byteIndex1 = y1 * bytesPerRow + (x1 / 8);
            int bitIndex1 = 7 - (x1 % 8);
            testImage[byteIndex1] &= ~(1 << bitIndex1);

            // Diagonal inversa
            int x2 = 30 + i;
            int y2 = 50 - i;
            int byteIndex2 = y2 * bytesPerRow + (x2 / 8);
            int bitIndex2 = 7 - (x2 % 8);
            testImage[byteIndex2] &= ~(1 << bitIndex2);
        }

        System.out.println("✓ Imagen de prueba: fondo negro + marco blanco + X blanca");
        return testImage;
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

    // REEMPLAZA el método convertirAArduinoBytes en tu clase

    public static byte[] convertirAArduinoBytes(Image img) {
        if (img == null) {
            System.err.println("❌ Error: Imagen es null");
            return null;
        }

        int width = 120;
        int height = 60;
        int umbral = 220;

        System.out.println("🖼️  Dimensiones originales: " +
                (int)img.getWidth() + "x" + (int)img.getHeight());

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

        System.out.println("📐 Escalado: " + scaledWidth + "x" + scaledHeight +
                " (centrado en " + x + "," + y + ")");

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

        System.out.println("✓ Bytes generados: " + byteIndex + "/900");

        if (byteIndex != 900) {
            System.err.println("❌ Error: Se generaron " + byteIndex + " bytes en lugar de 900");
            return null;
        }

        return imageData;
    }

    public void guardarConfiguracion(){
        System.out.println("\n=== GUARDANDO CONFIGURACIÓN ===");
        System.out.println("Botón: [" + botonSeleccionado.getRow() + "," + botonSeleccionado.getCol() + "]");
        System.out.println("Macro: '" + botonSeleccionado.getMacro() + "'");
        System.out.println("Label: '" + botonSeleccionado.getLabel() + "'");
        System.out.println("Imagen: '" + botonSeleccionado.getRutaIcono() + "'");

        guardarConfiguracionArchivo();
    }

    public void guardarConfiguracionArchivo(){
        System.out.println("\n=== GUARDANDO EN ARCHIVO ===");

        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE))){
            int contadorGuardados = 0;

            for(String key: botones.keySet()){
                MacroBoton btn = botones.get(key);

                if (btn.hasConfiguration()) {
                    System.out.println("Guardando botón: " + key + " - Macro: " + btn.getMacro());
                    oos.writeObject(key);
                    oos.writeObject(btn.getLabel());
                    oos.writeObject(btn.getMacro());
                    oos.writeObject(btn.getRutaIcono());
                    contadorGuardados++;
                }
            }

            oos.writeObject("FIN");

            String rutaCompleta = new File(CONFIG_FILE).getAbsolutePath();
            System.out.println("\n Archivo guardado: " + rutaCompleta);
            System.out.println("Total de botones guardados: " + contadorGuardados);

        } catch (Exception e) {
            System.err.println("ERROR al guardar:");
            e.printStackTrace();
        }
    }

    public void cargarConfiguracion(){
        File configFile = new File(CONFIG_FILE);
        if(!configFile.exists()){
            System.out.println("No existe archivo de configuración previo");
            return;
        }

        System.out.println("\n=== CARGANDO CONFIGURACIÓN ===");

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CONFIG_FILE))) {
            int contador = 0;
            while (true) {
                String key = (String) ois.readObject();
                if (key.equals("FIN")) break;

                String label = (String) ois.readObject();
                String command = (String) ois.readObject();
                String imagePath = (String) ois.readObject();

                System.out.println("Cargando botón: " + key);
                System.out.println("  Macro: " + command);

                MacroBoton btn = botones.get(key);
                if (btn != null) {
                    btn.setLabel(label);
                    btn.setMacro(command);
                    if (!imagePath.isEmpty()) {
                        btn.setRutaIcono(imagePath);
                        File imagenFile = new File(imagePath);
                        if (imagenFile.exists()) {
                            Image img = new Image(imagenFile.toURI().toString());
                            btn.setIcono(img);
                        }
                    }

                    // Actualizar visualmente el botón
                    if (!label.isEmpty() && btn.getBoton() != null) {
                        btn.getBoton().setText(label);
                    }

                    contador++;
                }
            }
            System.out.println("Configuración cargada: " + contador + " botones\n");

        } catch (FileNotFoundException e) {
            System.out.println("No hay configuración previa");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error cargando configuración:");
            e.printStackTrace();
        }
    }

    // REEMPLAZA estos dos métodos en tu clase

    private void actualizarMenuConBoton(MacroBoton macroB) {
        if (macroB == null) {
            System.err.println("⚠️ actualizarMenuConBoton: botón es null");
            return;
        }

        System.out.println("\n🔄 Actualizando menú para botón [" +
                macroB.getRow() + "," + macroB.getCol() + "]");

        // Actualizar el ComboBox con el macro guardado
        String macro = macroB.getMacro();
        if (macro != null && !macro.isEmpty()) {
            menuMacros.setValue(macro);
            System.out.println("  Macro: " + macro);
        } else {
            menuMacros.setValue("");
            System.out.println("  Sin macro configurado");
        }

        // Mostrar imagen si existe
        Image icono = macroB.getIcono();
        if (icono != null) {
            image = icono;  // CRÍTICO: Actualizar la referencia global
            previewImagen.setImage(icono);
            previewImagen.setVisible(true);
            System.out.println("  ✓ Imagen existente cargada en preview");
        } else {
            image = null;  // CRÍTICO: Limpiar la referencia global
            previewImagen.setImage(null);
            previewImagen.setVisible(false);
            System.out.println("  Sin imagen - Puedes cargar una nueva");
        }
    }

    private void actualizarLabelBoton(MacroBoton macroBoton) {
        if (macroBoton == null) {
            System.err.println("⚠️ actualizarLabelBoton: botón es null");
            return;
        }

        if (macroBoton.getBoton() == null) {
            System.err.println("⚠️ actualizarLabelBoton: botón visual es null");
            return;
        }

        Button botonVisual = macroBoton.getBoton();
        String label = macroBoton.getLabel();

        // Actualizar texto del botón
        if (label != null && !label.isEmpty()) {
            botonVisual.setText(label);
            System.out.println("✓ Label actualizado: " + label);
        } else {
            botonVisual.setText("");
            System.out.println("✓ Label limpiado");
        }

        // Actualizar preview de imagen
        Image icono = macroBoton.getIcono();
        if (icono != null) {
            previewImagen.setImage(icono);
            previewImagen.setVisible(true);
        } else {
            previewImagen.setImage(null);
            previewImagen.setVisible(false);
        }

        // Actualizar ComboBox
        String macro = macroBoton.getMacro();
        if (macro != null && !macro.isEmpty()) {
            menuMacros.setValue(macro);
        } else {
            menuMacros.setValue("");
        }

        // Actualizar la referencia global
        image = icono;
    }




}