import javafx.scene.control.Button;
import javafx.scene.image.Image;

public class MacroBoton {
    private Image icono;
    private String rutaIcono;
    private Button boton;
    private String macro;
    private String label;
    private int row;
    private int col;

    public MacroBoton(int row, int col, Button boton) {
        this.row = row;
        this.col = col;
        this.boton = boton;
        this.macro = "";
        this.label = "";
        this.rutaIcono = "";
    }

    public Image getIcono() {
        return icono;
    }

    public void setIcono(Image icono) {
        this.icono = icono;
    }

    public String getRutaIcono() {
        return rutaIcono;
    }

    public void setRutaIcono(String rutaIcono) {
        this.rutaIcono = rutaIcono;
        if(!rutaIcono.isEmpty()){
            try{
                this.icono = new Image("file:" + rutaIcono);
            } catch (Exception e){
                System.err.println("Error al cargar la imagen: " + rutaIcono);
            }
        }
    }

    public Button getBoton() {
        return boton;
    }

    public void setBoton(Button boton) {
        this.boton = boton;
    }

    public String getMacro() {
        return macro;
    }

    public void setMacro(String macro) {
        this.macro = macro;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public boolean hasConfiguration() {
        return !macro.isEmpty() || !label.isEmpty() || !rutaIcono.isEmpty();
    }
}
