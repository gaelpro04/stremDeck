import com.fazecast.jSerialComm.SerialPort;

public class Communication {
    private SerialPort sp;
    private int commPort;

    public Communication(int commPort) {
        this.commPort = commPort;
        sp = SerialPort.getCommPorts()[this.commPort];
        sp.openPort();
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
        }  catch (Exception e) {
            e.printStackTrace(); }

        sp.closePort();

        return null;
    }

    public void write(String value) {
        byte[] bytesToSend = value.getBytes();
        sp.writeBytes(bytesToSend, bytesToSend.length);

    }


    public int getCommPort() {
        return commPort;
    }

    public void setCommPort(int commPort) {
        this.commPort = commPort;
        sp = SerialPort.getCommPorts()[this.commPort];
    }
}
