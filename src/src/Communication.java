import com.fazecast.jSerialComm.SerialPort;

public class Communication {
    private SerialPort sp;
    private int commPort;

    public Communication(int commPort) {
        this.commPort = commPort;
        sp = SerialPort.getCommPorts()[this.commPort];
        sp.setBaudRate(115200);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (!sp.openPort()) {
            throw new RuntimeException("No se pudo abrir el puerto COM" + commPort);
        }

        // Dar tiempo al ESP32 para inicializarse
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Puerto COM" + commPort + " (" + sp.getDescriptivePortName() + ") abierto correctamente");
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

    // Para enviar texto (comandos)
    public void write(String value) {
        try {
            // Asegurar que termine con \n si no lo tiene
            String dataToSend = value;
            if (!dataToSend.endsWith("\n")) {
                dataToSend += "\n";
            }

            byte[] bytesToSend = dataToSend.getBytes("UTF-8");

            System.out.println("Enviando comando: \"" + value.replace("\n", "\\n") + "\" (" + bytesToSend.length + " bytes)");

            int written = sp.writeBytes(bytesToSend, bytesToSend.length);
            sp.flushIOBuffers();  // CRÍTICO: Forzar envío

            if (written == bytesToSend.length) {
                System.out.println("Comando enviado correctamente");
            } else {
                System.err.println("Solo se escribieron " + written + " de " + bytesToSend.length + " bytes");
            }

            Thread.sleep(50);  // Pequeño delay para que el ESP32 procese

        } catch (Exception e) {
            System.err.println("Error al enviar comando:");
            e.printStackTrace();
        }
    }

    // Para enviar bytes binarios (imágenes)
    public void writeBytes(byte[] data) {
        try {
            System.out.println("Enviando " + data.length + " bytes binarios...");

            // Enviar en chunks para mayor confiabilidad
            int chunkSize = 128;  // Enviar en bloques de 128 bytes
            int totalSent = 0;

            for (int i = 0; i < data.length; i += chunkSize) {
                int length = Math.min(chunkSize, data.length - i);
                byte[] chunk = new byte[length];
                System.arraycopy(data, i, chunk, 0, length);

                int written = sp.writeBytes(chunk, length);
                totalSent += written;

                // Mostrar progreso cada 200 bytes
                if (totalSent % 200 == 0 || totalSent == data.length) {
                    System.out.println("  Progreso: " + totalSent + "/" + data.length + " bytes");
                }

                // Pequeño delay entre chunks
                Thread.sleep(5);
            }

            sp.flushIOBuffers();  // Asegurar que todo se envíe

            if (totalSent == data.length) {
                System.out.println("Todos los bytes enviados correctamente (" + totalSent + " bytes)");
            } else {
                System.err.println("Advertencia: Solo se escribieron " + totalSent + " de " + data.length + " bytes");
            }

            Thread.sleep(200);  // Dar tiempo al ESP32 para procesar

        } catch (Exception e) {
            System.err.println("Error al enviar bytes:");
            e.printStackTrace();
        }
    }

    public void close() {
        if (sp != null && sp.isOpen()) {
            sp.closePort();
            System.out.println("Puerto cerrado");
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