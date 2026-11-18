package ar.edu.utn.frre.concesionaria.presentacion.consola;

/**
 * Color con significado, nunca de adorno (SPEC §5): cian para los títulos, amarillo para el
 * prompt, verde para el éxito y los 0 km, rojo para los errores y tenue para lo secundario.
 *
 * <p>Si el color está apagado ({@code NO_COLOR}, {@code --sin-color} o la salida no es una
 * terminal), todos los métodos devuelven el texto tal cual. El color nunca es la única señal:
 * los errores llevan además {@code ✗} y los éxitos {@code ✓}.</p>
 *
 * <p>Contenido extra (SPEC §7): códigos de escape ANSI, sin librerías.</p>
 */
public final class Estilo {

    private static final String RESET = "\u001B[0m";
    private static final String NEGRITA = "\u001B[1m";
    private static final String TENUE = "\u001B[2m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CIAN = "\u001B[36m";

    /** Estilo sin color, para exportar reportes a texto. */
    public static final Estilo SIN_COLOR = new Estilo(false);

    private final boolean color;

    /**
     * @param color si se usan códigos ANSI
     */
    public Estilo(boolean color) {
        this.color = color;
    }

    /** @return {@code true} si se usan códigos ANSI */
    public boolean conColor() {
        return color;
    }

    private String aplicar(String codigo, String texto) {
        return color ? codigo + texto + RESET : texto;
    }

    /**
     * @param texto el texto
     * @return el texto como título (cian y negrita)
     */
    public String titulo(String texto) {
        return aplicar(CIAN + NEGRITA, texto);
    }

    /**
     * @param texto el texto
     * @return el texto como prompt (amarillo)
     */
    public String prompt(String texto) {
        return aplicar(AMARILLO, texto);
    }

    /**
     * @param texto el texto
     * @return el texto como éxito o 0 km (verde)
     */
    public String verde(String texto) {
        return aplicar(VERDE, texto);
    }

    /**
     * @param texto el texto
     * @return el texto como error (rojo)
     */
    public String rojo(String texto) {
        return aplicar(ROJO, texto);
    }

    /**
     * @param texto el texto
     * @return el texto como información secundaria (tenue)
     */
    public String tenue(String texto) {
        return aplicar(TENUE, texto);
    }

    /**
     * @param texto el texto
     * @return el texto en negrita
     */
    public String negrita(String texto) {
        return aplicar(NEGRITA, texto);
    }

    /**
     * Largo visible de un texto, sin contar los códigos ANSI. Lo usan las tablas para alinear.
     *
     * @param texto el texto (con o sin códigos)
     * @return la cantidad de caracteres que se ven
     */
    public static int largoVisible(String texto) {
        return texto.replaceAll("\u001B\\[[0-9;]*m", "").length();
    }
}
