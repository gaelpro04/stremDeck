import com.fazecast.jSerialComm.SerialPort;

public class Communication {
    private SerialPort sp;
    private int commPort;
    private SerialListener listener;

    private volatile boolean running = true;
    private Thread lectorThread = null;


    public Communication(int commPort) {
        this.commPort = commPort;
        sp = SerialPort.getCommPorts()[this.commPort];
        sp.setBaudRate(115200);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 50, 0);

        if (!sp.openPort()) {
            throw new RuntimeException("No se pudo abrir el puerto COM" + commPort);
        }

        try {
            Thread.sleep(2000);  // Más tiempo para init
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Limpiar buffers iniciales
        clearBuffers();

        System.out.println("✓ Puerto COM" + commPort + " (" + sp.getDescriptivePortName() + ") abierto");

        // Iniciar hilo para leer respuestas del ESP32
        iniciarLectorSerial();
    }

    // NUEVO: Hilo para leer constantemente del ESP32
    private void iniciarLectorSerial() {
        running = true;
        Thread lector = new Thread(() -> {
            while (sp.isOpen() && running) {
                try {
                    if (sp.bytesAvailable() > 0) {
                        byte[] buffer = new byte[sp.bytesAvailable()];
                        sp.readBytes(buffer, buffer.length);
                        String respuesta = new String(buffer);

                        // Mostrar respuestas del ESP32 en consola
                        if (!respuesta.trim().isEmpty()) {
                            System.out.println("📥 ESP32: " + respuesta.trim());

                            if (listener != null) {
                                listener.datoSeriales(respuesta.trim());
                            }

                            manejarBoton(respuesta);
                        }
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // mostrar y seguir intentando (no matar hilo)
                    e.printStackTrace();
                    try { Thread.sleep(200); } catch (InterruptedException ex) { break; }
                }
            }
        });
        lector.setDaemon(true);
        lector.start();
    }

    private ButtonPressListener buttonListener = null;

    public void setButtonPressListener(ButtonPressListener listener) {
        this.buttonListener = listener;
    }

    private void manejarBoton(String comando) {
        try {

            String[] partes = comando.trim().split(",");

            if (partes.length == 2) {
                int fila = Integer.parseInt(partes[0]);
                int col = Integer.parseInt(partes[1]);

                if (buttonListener != null) {
                    buttonListener.onButtonPressed(fila, col);
                }
            }
        } catch (Exception e) {
            System.out.println("NO ES BOTON: " + comando);
        }

    }


    // NUEVO: Método para limpiar buffers
    private void clearBuffers() {
        try {
            Thread.sleep(100);
            while (sp.bytesAvailable() > 0) {
                byte[] trash = new byte[sp.bytesAvailable()];
                sp.readBytes(trash, trash.length);
                Thread.sleep(10);
            }
            sp.flushIOBuffers();
        } catch (Exception e) {
            e.printStackTrace();
        }
}

        public void setSerialListener(SerialListener listener) {
        this.listener = listener;
    }

    public String read() {
        try {
            while (true) {
                while (sp.bytesAvailable() == 0) {
                    Thread.sleep(20);
                }

                byte[] readBuffer = new byte[sp.bytesAvailable()];
                int numRead = sp.readBytes(readBuffer, readBuffer.length);

                return new String(readBuffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void write(String value) {
        try {
            String dataToSend = value;
            if (!dataToSend.endsWith("\n")) {
                dataToSend += "\n";
            }

            byte[] bytesToSend = dataToSend.getBytes("UTF-8");

            System.out.println("Enviando: \"" + value.replace("\n", "\\n") + "\"");

            int written = sp.writeBytes(bytesToSend, bytesToSend.length);
            sp.flushIOBuffers();

            if (written == bytesToSend.length) {
                System.out.println("✓ Comando enviado");
            } else {
                System.err.println("✗ Solo " + written + "/" + bytesToSend.length + " bytes");
            }

            Thread.sleep(100);  // Más tiempo para procesar

        } catch (Exception e) {
            System.err.println("Error al enviar:");
            e.printStackTrace();
        }
    }

    public void writeBytes(byte[] data) {
        try {
            System.out.println("\n=== Enviando imagen (" + data.length + " bytes) ===");

            int chunkSize = 64;  // Chunks más pequeños = más confiable
            int totalSent = 0;

            for (int i = 0; i < data.length; i += chunkSize) {
                int length = Math.min(chunkSize, data.length - i);
                byte[] chunk = new byte[length];
                System.arraycopy(data, i, chunk, 0, length);

                int written = sp.writeBytes(chunk, length);
                totalSent += written;

                if (totalSent % 200 == 0 || totalSent == data.length) {
                    System.out.println("Progreso: " + totalSent + "/" + data.length + " bytes");
                }

                Thread.sleep(10);  // Más delay entre chunks
            }

            sp.flushIOBuffers();

            if (totalSent == data.length) {
                System.out.println("✓ Imagen enviada completa");
            } else {
                System.err.println("✗ Solo " + totalSent + "/" + data.length + " bytes");
            }

            Thread.sleep(300);  // Esperar a que ESP32 procese

            // CRÍTICO: Limpiar buffers después de enviar
            clearBuffers();

        } catch (Exception e) {
            System.err.println("Error al enviar imagen:");
            e.printStackTrace();
        }
    }

    // NUEVO: Método para enviar una imagen completa (comando + datos)
    public void sendImage(byte[] imageData) {
        if (imageData == null) {
            System.err.println("Error: No hay datos de imagen");
            return;
        }

        if (imageData.length != 900) {
            System.err.println("Error: La imagen debe ser exactamente 900 bytes (recibido: " + imageData.length + ")");
            return;
        }

        System.out.println("\n>>> Enviando nueva imagen <<<");

        // 1. Limpiar pantalla primero
        write("CLEAR");

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. Limpiar buffers previos
        clearBuffers();

        // 3. Enviar comando IMG: (SIN \n porque write() ya lo agrega)
        write("IMG:");

        // 4. Esperar confirmación
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 5. Enviar datos binarios
//        writeBytes(imageData);

        System.out.println(">>> Imagen enviada y mostrada <<<\n");
    }

    public void close() {
        System.out.println("🔻 Cerrando comunicación SERIAL de forma segura...");

        try {
            // 1. Detener hilo lector
            running = false;

            if (lectorThread != null && lectorThread.isAlive()) {
                lectorThread.interrupt();
                try { lectorThread.join(300); } catch (InterruptedException ignored) {}
            }

            // 2. Vaciar buffers
            if (sp != null) {
                try { sp.flushIOBuffers(); } catch (Exception ignored) {}

                // 3. CERRAR PUERTO LIMPIO (ESTO ES LO QUE EVITA EL FREEZE)
                if (sp.isOpen()) {
                    sp.closePort();
                }
            }

            System.out.println("✅ Puerto cerrado sin congelar el ESP32");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public int getCommPort() {
        return commPort;
    }

    public void setCommPort(int commPort) {
        if (sp != null && sp.isOpen()) {
            sp.closePort();
        }
        this.commPort = commPort;
        sp = SerialPort.getCommPorts()[this.commPort];
        sp.setBaudRate(115200);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        sp.openPort();
    }
}