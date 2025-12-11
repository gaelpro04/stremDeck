import com.fazecast.jSerialComm.*;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.util.List;

public class macroInterfaz extends Application {
    private boolean menuAbierto = false;
    private Communication esp32;
    private HashMap<String, MacroBoton> botones = new HashMap<>();
    private MacroBoton botonSeleccionado = null;
    private static final String CONFIG_FILE = "C:\\Users\\sgsg_\\IdeaProjects\\stremDeck\\macro_config.dat";
    private ImageView previewImagen;
    private Label labelBotonSeleccionado;
    private Image image;
    private int modoLED;
    private Map<String, Integer> keyMap = new HashMap<>();
    private volatile boolean running = true;
    private volatile boolean stateBefore = true;

    //Atributo para las rutas que se estaran guardando
    private String ruta;

    @Override
    public void start(Stage primaryStage) throws InterruptedException {

        modoLED = 1;
        inicializarTeclas();

        //StackPane principal para centrar todo
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #1e1e1e;");

        //StackPane para la carcasa y el teclado
        StackPane keyboardContainer = new StackPane();
        Label labelTitulo = new Label("Configuracion de macros");
        labelTitulo.setFont(Font.font("Arial", FontWeight.NORMAL, 25));
        labelTitulo.setStyle("-fx-background-color: #1e1e1e;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;");

        //Carcasa (rectangulo que representa la caja del teclado)
        Rectangle carcasa = new Rectangle(360, 360);
        carcasa.setFill(Color.rgb(40, 40, 40));
        carcasa.setStroke(Color.rgb(60, 60, 60));
        carcasa.setStrokeWidth(3);
        carcasa.setArcWidth(20);
        carcasa.setArcHeight(20);

        //INformacion macro
        VBox tarjetaInfo = new VBox(5);
        tarjetaInfo.setPrefSize(483, 420);
        tarjetaInfo.setStyle(
                "-fx-background-color: #3a3a3a;" +
                        "-fx-background-radius: 25;" +
                        "-fx-padding: 15;" +
                        "-fx-border-radius: 25;" +
                        "-fx-border-color: #7d5ba6;"
        );
        tarjetaInfo.setMaxWidth(483);
        tarjetaInfo.setMinWidth(320);


        Label infoTitulo = new Label("Macro");
        infoTitulo.setTextFill(Color.WHITE);
        infoTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Label infoContenido = new Label("");
        infoContenido.setTextFill(Color.WHITE);
        infoContenido.setWrapText(true);
        infoContenido.setFont(Font.font(14));

        tarjetaInfo.getChildren().addAll(infoTitulo, infoContenido);

        //Posición inicial oculta (abajo)
        tarjetaInfo.setTranslateY(600);

        StackPane.setAlignment(tarjetaInfo, Pos.BOTTOM_LEFT);
        root.getChildren().add(tarjetaInfo);


        //GridPane para los botones
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
            cerrarMenu(menu, keyboardContainer, tarjetaInfo);
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


        cargarImagenBtn.setOnAction(e -> {
            if (botonSeleccionado == null) {
                System.err.println("Error: Debes seleccionar un botón primero");
                return;
            }

            System.out.println("\nAbriendo selector de archivos...");

            FileChooser imagenIcono = new FileChooser();
            imagenIcono.setTitle("Seleccione una imagen");
            imagenIcono.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
            );

            File archivo = imagenIcono.showOpenDialog(primaryStage);

            if (archivo != null) {
                try {
                    String ruta = archivo.getAbsolutePath();

                    System.out.println("Cargando imagen...");
                    System.out.println("   Archivo: " + archivo.getName());
                    System.out.println("   Ruta: " + ruta);

                    // Cargar la imagen
                    Image nuevaImagen = new Image(archivo.toURI().toString());

                    if (nuevaImagen.isError()) {
                        System.err.println("Error al cargar la imagen");
                        System.err.println("La imagen puede estar corrupta o en formato no soportado");
                        return;
                    }

                    image = nuevaImagen;

                    botonSeleccionado.setIcono(image);
                    botonSeleccionado.setRutaIcono(ruta);

                    // Mostrar preview
                    previewImagen.setImage(image);
                    previewImagen.setVisible(true);

                    System.out.println("Imagen cargada correctamente");
                    System.out.println("   Para botón [" +
                            botonSeleccionado.getRow() + "," +
                            botonSeleccionado.getCol() + "]");
                    System.out.println("   Dimensiones: " +
                            (int)image.getWidth() + "x" + (int)image.getHeight());

                } catch (Exception ex) {
                    System.err.println("Error al procesar la imagen:");
                    ex.printStackTrace();
                }
            } else {
                System.out.println("Selección de imagen cancelada");
            }
        });

        //Conexion y lectura macros inicial
        conectar();
        Thread.sleep(3000);
        extracted();

        Button btnNuevaMacro = new Button("Nueva Macro");
        btnNuevaMacro.setPrefWidth(160);
        btnNuevaMacro.setStyle(
                "-fx-background-color: #666;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 10;"
        );

        btnNuevaMacro.setOnAction(e -> {
            try {
                abrirVentanaNuevaMacro();
            } catch (AWTException ex) {
                throw new RuntimeException(ex);
            }
        });

        seccionSuperior.getChildren().add(btnNuevaMacro);
        seccionSuperior.getChildren().addAll(cargarImagenBtn, previewImagen);

        VBox seccionMedia = new VBox(15);
        seccionMedia.setAlignment(Pos.TOP_CENTER);

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
        borrar.setOnAction(e -> {
            if (botonSeleccionado == null) {
                System.err.println("Error: No hay botón seleccionado");
                return;
            }

            System.out.println("\nBorrando configuración del botón [" +
                    botonSeleccionado.getRow() + "," +
                    botonSeleccionado.getCol() + "]");

            // Limpiar datos del botón
            botonSeleccionado.setRutaIcono("");
            botonSeleccionado.setMacro("");
            botonSeleccionado.setLabel("");
            botonSeleccionado.setIcono(null);
            botonSeleccionado.setRuta("");

            // Limpiar interfaz
            previewImagen.setImage(null);
            previewImagen.setVisible(false);

            image = null;

            //Guardar cambios
            guardarConfiguracion();
            actualizarLabelBoton(botonSeleccionado);

            System.out.println("Configuración borrada");
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

        configurar.setOnAction(e -> {
            // VALIDACIONES PRIMERO
            if (esp32 == null) {
                System.err.println("Error: Puerto serial no configurado");
                System.err.println("   Por favor selecciona un puerto COM primero");
                return;
            }

            if (botonSeleccionado == null) {
                System.err.println("Error: No hay botón seleccionado");
                return;
            }

            if (image == null) {
                System.out.println("No se ha cargado ninguna imagen");
            }

            System.out.println("\n════════════════════════════════════");
            System.out.println("CONFIGURANDO BOTÓN [" +
                    botonSeleccionado.getRow() + "," +
                    botonSeleccionado.getCol() + "]");
            System.out.println("════════════════════════════════════");

            String macroSeleccionada = botonSeleccionado.getMacro();
            if (macroSeleccionada == null || macroSeleccionada.isEmpty()) {
                System.out.println("Advertencia: No se seleccionó ningún macro");
            } else {
                botonSeleccionado.setMacro(macroSeleccionada);
                botonSeleccionado.setLabel(macroSeleccionada);

                if (ruta != null) {
                    botonSeleccionado.setRuta(ruta);
                }
                System.out.println("Macro configurado: " + macroSeleccionada);
            }

            if (image != null) {
                System.out.println("\nProcesando imagen...");
                byte[] imageBytes = convertirAArduinoBytes(image);

                if (imageBytes == null || imageBytes.length != 900) {
                    System.err.println("Error: Conversión de imagen falló");
                    System.err.println("   Bytes generados: " + (imageBytes != null ? imageBytes.length : 0));
                    return;
                }

                System.out.println("✓ Imagen convertida correctamente (900 bytes)");

                System.out.println("\nEnviando imagen al ESP32...");

                try {
                    esp32.sendImage(imageBytes);
                    System.out.println("Imagen enviada al ESP32 exitosamente");

                } catch (Exception ex) {
                    System.err.println("Error al enviar imagen:");
                    ex.printStackTrace();
                    return;
                }
            }

            System.out.println("\nGuardando configuración...");
            guardarConfiguracion();
            actualizarLabelBoton(botonSeleccionado);

            System.out.println("====================================");
            System.out.println("CONFIGURACIÓN COMPLETADA");
            System.out.println("====================================\n");
        });



        Region espaciadorExtra = new Region();
        espaciadorExtra.setPrefHeight(5);

        seccionInferior.getChildren().addAll(configurar, espaciadorExtra, borrar);

        javafx.scene.layout.Region espaciador1 = new javafx.scene.layout.Region();
        espaciador1.setPrefHeight(5);

        javafx.scene.layout.Region espaciador2 = new javafx.scene.layout.Region();
        espaciador2.setPrefHeight(30);

        javafx.scene.layout.Region espaciador3 = new javafx.scene.layout.Region();
        espaciador2.setPrefHeight(30);

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
                String key = i + "," + j;
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
                    mostrarInfoMacro(tarjetaInfo, infoContenido, macroB);


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

        //Agregar carcasa y grid al contenedor
        keyboardContainer.getChildren().addAll(carcasa, grid);

        //Agregar el contenedor al root
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
            System.out.println("\nCerrando aplicación...");
            running = false;

            if (esp32 != null) {
                esp32.close();
                Platform.exit();
                System.exit(0);
            }

            System.out.println("Aplicación cerrada\n");
        });
    }

    /**
     * Metodo que inicia la lectura y escritura de las macros
     */
    private void extracted() {
        try {
            esp32.setButtonPressListener((fila, col) -> {
                System.out.println("   BOTÓN FÍSICO PRESIONADO: [" + fila + "," + col + "]");

                String key = fila + "," + col;
                MacroBoton btn = botones.get(key);

                if (btn != null) {
                    String ruta = btn.getRuta();
                    String macro = btn.getMacro();
                    Image imagen = btn.getIcono();

                    System.out.println("Configuración del botón:");
                    System.out.println("   Macro: " + (macro != null && !macro.isEmpty() ? macro : "Sin macro"));
                    System.out.println("   Ruta: " + (ruta != null && !ruta.isEmpty() ? ruta : "Sin ruta"));
                    System.out.println("   Imagen: " + (imagen != null ? "Sí" : "No"));

                    if (ruta != null && !ruta.isEmpty()) {
                        System.out.println("\nEjecutando macro...");
                        ejecutarMacro(ruta);
                        System.out.println("Macro ejecutado");
                    } else {
                        System.out.println("\nEste botón no tiene macro configurado");
                    }

                    if (imagen != null && esp32 != null) {
                        System.out.println("\nMostrando imagen en display...");
                        byte[] imageBytes = convertirAArduinoBytes(imagen);
                        if (imageBytes != null && imageBytes.length == 900) {
                            try {
                                esp32.sendImage(imageBytes);
                                System.out.println("Imagen mostrada en display");
                            } catch (Exception ex) {
                                System.err.println("Error al mostrar imagen: " + ex.getMessage());
                            }
                        }
                    }
                } else {
                    System.out.println("Botón no encontrado en la configuración");
                }
            });
        } catch (NullPointerException exception) {
            System.err.println("ERROR PINCHE PENDEJO");
        }
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

        System.out.println("Imagen de prueba: fondo negro + marco blanco + X blanca");
        return testImage;
    }

    private void abrirMenu(VBox menu, StackPane teclado) {
        StackPane menuContainer = (StackPane) menu.getParent();
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), menuContainer);
        tt.setToX(300);  //entra a la pantalla

        TranslateTransition keys = new TranslateTransition(Duration.millis(300), teclado);
        keys.setToX(-100);

        tt.play();
        keys.play();
    }

    /**
     * Metodo de la interfaz que cierra el menu derecho y la terjeta con la info de la macro
     * @param menu
     * @param teclado
     * @param tarjeta
     */
    private void cerrarMenu(VBox menu, StackPane teclado, VBox tarjeta) {

        //Para la info del macro
        TranslateTransition subir = new TranslateTransition(Duration.millis(300), tarjeta);
        subir.setFromY(491);
        subir.setToY(600);
        subir.play();

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
        if (img == null) {
            System.out.println("Error: Imagen es null");
            return null;
        }

        int width = 120;
        int height = 60;
        int umbral = 220;

        System.out.println("🖼Dimensiones originales: " +
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

        System.out.println("Escalado: " + scaledWidth + "x" + scaledHeight +
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
            System.err.println("Error: Se generaron " + byteIndex + " bytes en lugar de 900");
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
        System.out.println("Ruta: '" + botonSeleccionado.getRuta() + "'");

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
                    oos.writeObject(btn.getRuta());
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
                String ruta = (String) ois.readObject();

                System.out.println("Cargando botón: " + key);
                System.out.println("  Macro: " + command);
                System.out.println("Ruta: " + ruta);

                MacroBoton btn = botones.get(key);
                if (btn != null) {
                    btn.setLabel(label);
                    btn.setMacro(command);
                    btn.setRuta(ruta);
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

    private void actualizarMenuConBoton(MacroBoton macroB) {
        if (macroB == null) {
            System.err.println("actualizarMenuConBoton: botón es null");
            return;
        }

        System.out.println("\nActualizando menú para botón [" +
                macroB.getRow() + "," + macroB.getCol() + "]");

        // Actualizar el ComboBox con el macro guardado
        String macro = macroB.getMacro();
        if (macro != null && !macro.isEmpty()) {
            System.out.println("  Macro: " + macro);
        } else {
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
            System.err.println("actualizarLabelBoton: botón es null");
            return;
        }

        if (macroBoton.getBoton() == null) {
            System.err.println("actualizarLabelBoton: botón visual es null");
            return;
        }

        Button botonVisual = macroBoton.getBoton();
        String label = macroBoton.getLabel();

        // Actualizar texto del botón
        if (label != null && !label.isEmpty()) {
            botonVisual.setText(label);
            System.out.println("Label actualizado: " + label);
        } else {
            botonVisual.setText("");
            System.out.println("Label limpiado");
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

        // Actualizar la referencia global
        image = icono;
    }

    /**
     * Metodo que ejecuta los macros
     * @param ruta
     */
    private void ejecutarMacro(String ruta) {

        //Cada macro tiene una etiqueta por lo tanto, identificando la etiqueta se puede saber
        //que tipo de macro es y por lo tanto se ejecuta
        try {
            if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
                Desktop.getDesktop().browse(new URI(ruta));
                return;
            }

            if (ruta.equals("LEDs")) {
                if (modoLED > 15) {
                    modoLED = 1;
                    String comandoLed = "LED:" + modoLED;
                    System.out.println("SE ENVIARA POR SERIAL");
                    esp32.write(comandoLed);
                    ++modoLED;

                } else {
                    String comandoLed = "LED:" + modoLED;
                    System.out.println("SE ENVIARA POR SERIAL");
                    esp32.write(comandoLed);
                    ++modoLED;

                }
                return;
            }

            if (ruta.startsWith("TXT:")) {
                String contenido = ruta.substring(4);
                escribirTexto(contenido);
                return;
            }

            if (ruta.startsWith("CTRL") || ruta.startsWith("SHIFT") || ruta.startsWith("ALT") || ruta.startsWith("WIN")
                    || ruta.startsWith("ENTER") || ruta.startsWith("TAB") || ruta.startsWith("ESC") ||
                    ruta.startsWith("BACKSPACE") || ruta.startsWith("SPACE")) {

                Robot robot = new Robot();
                String[] teclas = ruta.toUpperCase().split("\\+");

                //Presionado de tecla
                for (String tecla : teclas) {
                    Integer keyCode = keyMap.get(tecla);
                    if (keyCode != null) {
                        robot.keyPress(keyCode);
                    } else {
                        System.out.println("Tecla no reconocida");
                    }
                }

                robot.delay(50);

                //Aqui se suelta la tecla
                for (int i = teclas.length - 1; i >= 0; i--) {
                    Integer keyCode = keyMap.get(teclas[i]);
                    if (keyCode != null) {
                        robot.keyRelease(keyCode);
                    } else {
                        System.out.println("Tecla no reconocida");
                    }
                }
                return;
            }

            String comando = "cmd /c start \"\" \"" + ruta + "\"";

            Runtime.getRuntime().exec(comando);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Metodo que permite crear nuevos macros
     * @throws AWTException
     */
    private void abrirVentanaNuevaMacro() throws AWTException {
        if (botonSeleccionado == null) {
            System.err.println("Selecciona un botón primero");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Asignar Macro");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2a2a2a; -fx-padding: 20;");

        Label titulo = new Label("Nueva macro");
        titulo.setTextFill(Color.WHITE);
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Button btnGuardar = new Button("Guardar");
        btnGuardar.setStyle(
                "-fx-background-color: #7d5ba6;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 10;"
        );

        ComboBox<String> tipoMacro = new ComboBox<>();
        tipoMacro.getItems().addAll("Pagina web", "Ejecutable", "Texto", "LEDs", "Comando");
        tipoMacro.setOnAction(escogido -> {

            Label nombreLabel = new Label("Ingresa el nombre del macro");
            nombreLabel.setTextFill(Color.WHITE);
            nombreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

            TextField capNombre = new TextField();
            capNombre.setPrefWidth(130);

            switch (tipoMacro.getSelectionModel().getSelectedItem()) {
                case "Pagina web":
                    root.getChildren().clear();
                    root.getChildren().addAll(titulo, tipoMacro,  nombreLabel, capNombre, btnGuardar);

                    TextField capRuta = new TextField();
                    capRuta.setPrefWidth(150);

                    Label mensajeCap = new Label("Ingresa la ruta");
                    mensajeCap.setTextFill(Color.WHITE);
                    mensajeCap.setFont(Font.font("Arial", FontWeight.BOLD, 16));

                    btnGuardar.setOnAction(captura -> {

                        if (capRuta.getText()!= null) {
                            String ruta = capRuta.getText();

                            Label pagina = new Label("Pagina capturada");
                            pagina.setTextFill(Color.WHITE);
                            pagina.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                            root.getChildren().add(4, pagina);

                            if (botonSeleccionado != null) {
                                botonSeleccionado.setRuta(ruta);
                                botonSeleccionado.setMacro(capNombre.getText());
                            }
                        }

                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                javafx.application.Platform.runLater(() -> stage.close());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });

                    root.getChildren().add(2, mensajeCap);
                    root.getChildren().add(3, capRuta);
                    break;
                case "Ejecutable":
                    root.getChildren().clear();
                    root.getChildren().addAll(titulo, tipoMacro, nombreLabel, capNombre, btnGuardar);

                    FileChooser imagenIcono = new FileChooser();
                    imagenIcono.setTitle("Seleccione una imagen");
                    imagenIcono.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("Ejecutables", "*.exe")
                    );

                    File archivo = imagenIcono.showOpenDialog(stage);

                    if (archivo != null) {
                        Label aplicacion = new Label(archivo.getName());
                        aplicacion.setTextFill(Color.WHITE);
                        aplicacion.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                        root.getChildren().add(2, aplicacion);


                        btnGuardar.setOnAction(evento -> {
                            if (botonSeleccionado != null) {
                                botonSeleccionado.setRuta(archivo.getAbsolutePath());
                                botonSeleccionado.setMacro(capNombre.getText());
                            }

                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                    javafx.application.Platform.runLater(() -> stage.close());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        });
                    }



                    break;
                case "Texto":
                    root.getChildren().clear();
                    root.getChildren().addAll(titulo, tipoMacro, nombreLabel, capNombre, btnGuardar);

                    Label texto = new Label("Ingrese el texto");
                    texto.setTextFill(Color.WHITE);
                    texto.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    root.getChildren().add(2, texto);

                    TextField textoLugar = new TextField();
                    textoLugar.setPrefWidth(150);
                    root.getChildren().add(3, textoLugar);

                    btnGuardar.setOnAction(evento -> {
                        String contenido = "TXT:" + textoLugar.getText();

                        if (botonSeleccionado != null) {
                            botonSeleccionado.setMacro(capNombre.getText());
                            botonSeleccionado.setRuta(contenido);
                        }

                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                javafx.application.Platform.runLater(() -> stage.close());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });
                    break;
                case "LEDs":
                    root.getChildren().clear();
                    root.getChildren().addAll(titulo, tipoMacro, nombreLabel, capNombre, btnGuardar);


                    btnGuardar.setOnAction(evento -> {
                        Label configurado = new Label("Configurado");

                        configurado.setTextFill(Color.WHITE);
                        configurado.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                        root.getChildren().add(2, configurado);

                        if (botonSeleccionado != null) {
                            botonSeleccionado.setRuta("LEDs");
                            botonSeleccionado.setMacro(capNombre.getText());
                        }

                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                javafx.application.Platform.runLater(() -> stage.close());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });
                    break;
                case "Comando":
                    root.getChildren().clear();
                    root.getChildren().addAll(titulo, tipoMacro, nombreLabel, capNombre, btnGuardar);
                    ArrayList<String> hotKeys = new ArrayList<>(List.of(new String[]{"CTRL", "SHIFT", "ALT", "WIN", "ENTER", "TAB", "ESC", "" +
                            "BACKSPACE", "SPACE"}));

                    ArrayList<Character> teclas = new ArrayList<>();
                    for (char c = 'A'; c <= 'Z'; c++) {
                        teclas.add(c);
                    }
                    for (char c = '0'; c <= '9'; c++) {
                        teclas.add(c);
                    }

                    ArrayList<String> letrasYnumeros = new ArrayList<>();

                    //HotKeys
                    ComboBox<String> comandos = new ComboBox<>();
                    comandos.getItems().addAll(hotKeys);

                    //Teclas
                    ComboBox<Character> comandoLetras = new ComboBox<>();
                    comandoLetras.getItems().addAll(teclas);

                    HBox filaCombos = new HBox(10);
                    filaCombos.setAlignment(Pos.CENTER);

                    filaCombos.getChildren().addAll(comandos, comandoLetras);
                    root.getChildren().add(2, filaCombos);


                    btnGuardar.setOnAction(evento -> {
                        String comandoS = comandos.getSelectionModel().getSelectedItem();
                        Character tecla = comandoLetras.getSelectionModel().getSelectedItem();

                        if (botonSeleccionado != null) {
                            String comandoSeleccionado = comandoS + "+" + tecla;

                            botonSeleccionado.setRuta(comandoSeleccionado);
                            botonSeleccionado.setMacro(capNombre.getText());
                        }

                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                javafx.application.Platform.runLater(() -> stage.close());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });
                    break;

            }
        });

        root.getChildren().addAll(titulo, tipoMacro, btnGuardar);

        Scene scene = new Scene(root, 500, 400);
        stage.setScene(scene);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.showAndWait();
    }

    /**
     * Metodo para mostrar la informacion de cada macro al momento de ser seleccionada
     * dentro de la interfaz
     * @param tarjeta
     * @param texto
     * @param macro
     */
    private void mostrarInfoMacro(VBox tarjeta, Label texto, MacroBoton macro) {
        if (macro == null) return;

        String info =
                "Macro: " + (macro.getMacro() != null ? macro.getMacro() : "Sin macro") + "\n" +
                        "Ruta: " + (macro.getRuta() != null ? macro.getRuta() : "Sin ruta") + "\n" +
                        "Icono: " + (macro.getIcono() != null ? "Sí" : "No");

        texto.setText(info);

        TranslateTransition subir = new TranslateTransition(Duration.millis(300), tarjeta);
        subir.setFromY(600);
        subir.setToY(491);
        subir.play();

    }

    /**
     * Funcion para asignar dinamicamente comandos
     */
    private void inicializarTeclas() {
        keyMap.put("CTRL", KeyEvent.VK_CONTROL);
        keyMap.put("SHIFT", KeyEvent.VK_SHIFT);
        keyMap.put("ALT", KeyEvent.VK_ALT);
        keyMap.put("WIN", KeyEvent.VK_WINDOWS);
        keyMap.put("ENTER", KeyEvent.VK_ENTER);
        keyMap.put("TAB", KeyEvent.VK_TAB);
        keyMap.put("ESC", KeyEvent.VK_ESCAPE);
        keyMap.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        keyMap.put("SPACE", KeyEvent.VK_SPACE);

        for (char c = 'A'; c <= 'Z'; ++c) {
            keyMap.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        }

        for (char c = '0'; c <= '9'; c++) {
            keyMap.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        }
    }

    /**
     * Metodo que permite escribir texto en pantalla del macro seleccionado
     * @param texto
     */
    private void escribirTexto(String texto) {
        try {
            Robot robot = new Robot();

            for (char c : texto.toCharArray()) {
                int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);

                if (KeyEvent.CHAR_UNDEFINED == keyCode) continue;

                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);

                if (Character.isUpperCase(c)) {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                }

                Thread.sleep(15);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Metodo que lee constantemente lee si se abrio o se cerro el puerto para volver
     * a conectarlo
     */
    private void conectar() {
        String portESP = "COM21";

        Thread t = new Thread(() -> {
            while (running) {



                //Si no hay objeto de comunicacion quiere decir que no esta conectado el puerto
                if (esp32 == null || esp32.getSp() == null || !esp32.getSp().isOpen()) {
                    System.out.println("Intentando abrir COM21...");

                    try {
                        esp32 = new Communication(portESP);

                        //Tiene incorporado una mini maquina de estados de un solo estado donde
                        //al momento que se conecta por primera vez cambia el estado anterior a false
                        //queriendo decir que ya es necesario desde un inicio iniciar la escritura de macros
                        if (esp32.getSp().isOpen()) {
                            System.out.println("COM21 CONECTADO");
                            stateBefore = false;
                        } else {
                            System.out.println("Falló al abrir COM21.");
                        }

                        //Inicio de la escritura de macros
                        if (!stateBefore) {
                            extracted();
                        }
                    } catch (RuntimeException e) {
                        System.out.println("No hay puerto conectado");
                    }


                }

                if (esp32 != null && esp32.getSp().isOpen()) {
                    if (!estadoESP32(esp32.getSp())) {
                        System.out.println("ESP32 se desconectó.");
                        esp32.close();
                        esp32 = null;
                    }
                }
            }

            //Tiempo para que conecte el COM
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}

        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Metodo que permite verificar si el micro esta conectado
     * @param puerto
     * @return
     */
    private boolean estadoESP32(SerialPort puerto) {
        return puerto.bytesAvailable() != -1;
    }



}