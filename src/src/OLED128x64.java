import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;

public class OLED128x64 {

    // Comandos SSD1306
    private static final int SSD1306_I2C_ADDRESS = 0x3C;
    private static final int SSD1306_SETCONTRAST = 0x81;
    private static final int SSD1306_DISPLAYALLON_RESUME = 0xA4;
    private static final int SSD1306_DISPLAYALLON = 0xA5;
    private static final int SSD1306_NORMALDISPLAY = 0xA6;
    private static final int SSD1306_INVERTDISPLAY = 0xA7;
    private static final int SSD1306_DISPLAYOFF = 0xAE;
    private static final int SSD1306_DISPLAYON = 0xAF;
    private static final int SSD1306_SETDISPLAYOFFSET = 0xD3;
    private static final int SSD1306_SETCOMPINS = 0xDA;
    private static final int SSD1306_SETVCOMDETECT = 0xDB;
    private static final int SSD1306_SETDISPLAYCLOCKDIV = 0xD5;
    private static final int SSD1306_SETPRECHARGE = 0xD9;
    private static final int SSD1306_SETMULTIPLEX = 0xA8;
    private static final int SSD1306_SETLOWCOLUMN = 0x00;
    private static final int SSD1306_SETHIGHCOLUMN = 0x10;
    private static final int SSD1306_SETSTARTLINE = 0x40;
    private static final int SSD1306_MEMORYMODE = 0x20;
    private static final int SSD1306_COLUMNADDR = 0x21;
    private static final int SSD1306_PAGEADDR = 0x22;
    private static final int SSD1306_COMSCANINC = 0xC0;
    private static final int SSD1306_COMSCANDEC = 0xC8;
    private static final int SSD1306_SEGREMAP = 0xA0;
    private static final int SSD1306_CHARGEPUMP = 0x8D;

    private static final int WIDTH = 128;
    private static final int HEIGHT = 64;
    private static final int PAGES = HEIGHT / 8;

    private I2C i2c;
    private byte[] buffer;

    public OLED128x64() throws Exception {
        // Inicializar Pi4J
        Context pi4j = Pi4J.newAutoContext();
        I2CProvider i2cProvider = pi4j.provider("linuxfs-i2c");
        I2CConfig config = I2C.newConfigBuilder(pi4j)
                .id("SSD1306")
                .bus(1)
                .device(SSD1306_I2C_ADDRESS)
                .build();

        i2c = i2cProvider.create(config);
        buffer = new byte[WIDTH * PAGES];

        initDisplay();
    }

    private void initDisplay() throws Exception {
        // Secuencia de inicialización SSD1306
        command(SSD1306_DISPLAYOFF);
        command(SSD1306_SETDISPLAYCLOCKDIV);
        command(0x80);
        command(SSD1306_SETMULTIPLEX);
        command(0x3F);
        command(SSD1306_SETDISPLAYOFFSET);
        command(0x0);
        command(SSD1306_SETSTARTLINE | 0x0);
        command(SSD1306_CHARGEPUMP);
        command(0x14);
        command(SSD1306_MEMORYMODE);
        command(0x00);
        command(SSD1306_SEGREMAP | 0x1);
        command(SSD1306_COMSCANDEC);
        command(SSD1306_SETCOMPINS);
        command(0x12);
        command(SSD1306_SETCONTRAST);
        command(0xCF);
        command(SSD1306_SETPRECHARGE);
        command(0xF1);
        command(SSD1306_SETVCOMDETECT);
        command(0x40);
        command(SSD1306_DISPLAYALLON_RESUME);
        command(SSD1306_NORMALDISPLAY);
        command(SSD1306_DISPLAYON);

        clear();
        display();
    }

    private void command(int cmd) throws Exception {
        i2c.write((byte) 0x00, (byte)cmd);
    }

    public void clear() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
    }

    public void setPixel(int x, int y, boolean on) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;

        int page = y / 8;
        int bit = y % 8;
        int index = x + (page * WIDTH);

        if (on) {
            buffer[index] |= (1 << bit);
        } else {
            buffer[index] &= ~(1 << bit);
        }
    }

    public void drawText(String text, int x, int y) {
        // Fuente simple 5x7 para cada carácter
        for (int i = 0; i < text.length(); i++) {
            drawChar(text.charAt(i), x + (i * 6), y);
        }
    }

    private void drawChar(char c, int x, int y) {
        // Fuente básica 5x7 - aquí solo algunos caracteres de ejemplo
        byte[][] font = getCharFont(c);

        for (int col = 0; col < 5; col++) {
            for (int row = 0; row < 7; row++) {
                if ((font[col][row / 8] & (1 << (row % 8))) != 0) {
                    setPixel(x + col, y + row, true);
                }
            }
        }
    }

    private byte[][] getCharFont(char c) {
        // Fuente simplificada para caracteres básicos
        // Cada carácter es 5 columnas de ancho
        byte[][] defaultFont = {{0x00}, {0x00}, {0x00}, {0x00}, {0x00}};

        switch(c) {
            case 'H': return new byte[][]{{0x7F}, {0x08}, {0x08}, {0x08}, {0x7F}};
            case 'o': return new byte[][]{{0x38}, {0x44}, {0x44}, {0x44}, {0x38}};
            case 'l': return new byte[][]{{0x00}, {0x41}, {0x7F}, {0x40}, {0x00}};
            case 'a': return new byte[][]{{0x20}, {0x54}, {0x54}, {0x54}, {0x78}};
            case ' ': return new byte[][]{{0x00}, {0x00}, {0x00}, {0x00}, {0x00}};
            case 'J': return new byte[][]{{0x20}, {0x40}, {0x41}, {0x3F}, {0x01}};
            case 'v': return new byte[][]{{0x1C}, {0x20}, {0x40}, {0x20}, {0x1C}};
            case '!': return new byte[][]{{0x00}, {0x00}, {0x5F}, {0x00}, {0x00}};
            default: return defaultFont;
        }
    }

    public void drawRect(int x, int y, int width, int height, boolean filled) {
        if (filled) {
            for (int i = x; i < x + width; i++) {
                for (int j = y; j < y + height; j++) {
                    setPixel(i, j, true);
                }
            }
        } else {
            for (int i = x; i < x + width; i++) {
                setPixel(i, y, true);
                setPixel(i, y + height - 1, true);
            }
            for (int j = y; j < y + height; j++) {
                setPixel(x, j, true);
                setPixel(x + width - 1, j, true);
            }
        }
    }

    public void display() throws Exception {
        command(SSD1306_COLUMNADDR);
        command(0);
        command(WIDTH - 1);
        command(SSD1306_PAGEADDR);
        command(0);
        command(PAGES - 1);

        // Enviar buffer completo
        byte[] data = new byte[buffer.length + 1];
        data[0] = 0x40; // Co = 0, D/C = 1
        System.arraycopy(buffer, 0, data, 1, buffer.length);
        i2c.write(data);
    }

    public static void main(String[] args) {
        try {
            OLED128x64 oled = new OLED128x64();

            // Limpiar pantalla
            oled.clear();

            // Dibujar texto
            oled.drawText("Hola Java!", 10, 10);

            // Dibujar un rectángulo
            oled.drawRect(5, 25, 118, 30, false);

            // Dibujar un rectángulo relleno
            oled.drawRect(50, 30, 20, 20, true);

            // Mostrar en pantalla
            oled.display();

            System.out.println("Contenido mostrado en la pantalla OLED");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}