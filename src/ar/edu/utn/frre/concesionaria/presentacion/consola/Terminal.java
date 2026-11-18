package ar.edu.utn.frre.concesionaria.presentacion.consola;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import java.io.PrintStream;
import java.util.List;

/**
 * Salida a la terminal con la estructura fija de cada pantalla (SPEC §5): encabezado con el
 * sistema, el usuario, el rol y la fecha; ruta de navegación; contenido; pie con las teclas.
 */
public final class Terminal {

    /** Ancho mínimo. */
    public static final int ANCHO_MINIMO = 80;
    /** Ancho máximo: más allá, las líneas largas se leen peor. */
    public static final int ANCHO_MAXIMO = 120;

    private static final String SISTEMA = "CONCESIONARIA MULTIMARCA";

    private final PrintStream out;
    private final Estilo estilo;
    private final int ancho;

    /**
     * @param out    dónde escribir
     * @param estilo con o sin color
     * @param ancho  ancho de la terminal (se ajusta entre 80 y 120)
     */
    public Terminal(PrintStream out, Estilo estilo, int ancho) {
        this.out = out;
        this.estilo = estilo;
        this.ancho = Math.max(ANCHO_MINIMO, Math.min(ANCHO_MAXIMO, ancho));
    }

    /**
     * Ancho de la variable {@code COLUMNS}, o el mínimo si no está o no es un número.
     *
     * @return el ancho detectado
     */
    public static int anchoDelEntorno() {
        try {
            return Integer.parseInt(System.getenv().getOrDefault("COLUMNS", "").trim());
        } catch (NumberFormatException e) {
            return ANCHO_MINIMO;
        }
    }

    /** @return el estilo */
    public Estilo estilo() {
        return estilo;
    }

    /** @return el ancho útil */
    public int ancho() {
        return ancho;
    }

    /**
     * Limpia la pantalla. Sin color (archivo, terminal sin ANSI) imprime un separador, para que
     * igual se distinga dónde empieza la pantalla nueva.
     */
    public void limpiar() {
        if (estilo.conColor()) {
            out.print("\u001B[H\u001B[2J");
            out.flush();
        } else {
            out.println();
            out.println("=".repeat(ancho));
        }
    }

    /**
     * Encabezado fijo y ruta de navegación.
     *
     * @param derecha texto de la derecha ("lbalbiano · Vendedor · mié 18/11/2026"), o vacío
     * @param ruta    la ruta ("Inicio › Vehículos › Registrar")
     */
    public void encabezado(String derecha, String ruta) {
        int espacio = Math.max(1, ancho - 1 - SISTEMA.length() - derecha.length());
        out.println(" " + estilo.titulo(SISTEMA) + " ".repeat(espacio) + estilo.tenue(derecha));
        out.println(" " + estilo.tenue(ruta));
        out.println(" " + estilo.tenue("─".repeat(ancho - 1)));
    }

    /**
     * Pie con las teclas disponibles.
     *
     * @param teclas por ejemplo "0 Volver · Enter confirma"
     */
    public void pie(String teclas) {
        out.println();
        out.println(" " + estilo.tenue(teclas));
    }

    /**
     * @param texto una línea
     */
    public void linea(String texto) {
        out.println(texto);
    }

    /** Línea en blanco. */
    public void blanco() {
        out.println();
    }

    /**
     * @param lineas varias líneas
     */
    public void lineas(List<String> lineas) {
        lineas.forEach(out::println);
    }

    /**
     * @param texto título de sección
     */
    public void titulo(String texto) {
        out.println(" " + estilo.titulo(texto));
    }

    /**
     * @param mensaje mensaje de éxito, con {@code ✓}
     */
    public void exito(String mensaje) {
        out.println(" " + estilo.verde("✓ " + mensaje));
    }

    /**
     * @param mensaje mensaje de error, con {@code ✗}
     */
    public void error(String mensaje) {
        List<String> partes = partir(mensaje, ancho - 4);
        for (int i = 0; i < partes.size(); i++) {
            out.println(" " + estilo.rojo((i == 0 ? "✗ " : "  ") + partes.get(i)));
        }
    }

    /**
     * @param mensaje aviso importante que no es error, con {@code !}
     */
    public void aviso(String mensaje) {
        out.println(" " + estilo.negrita("! " + mensaje));
    }

    /**
     * @param mensaje información secundaria
     */
    public void info(String mensaje) {
        out.println(" " + estilo.tenue(mensaje));
    }

    /**
     * Escribe sin salto de línea (para el prompt).
     *
     * @param texto el texto
     */
    public void escribir(String texto) {
        out.print(texto);
        out.flush();
    }

    /**
     * Parte un texto largo en renglones de a lo sumo {@code ancho} caracteres, cortando en
     * espacios.
     *
     * @param texto el texto
     * @param ancho el máximo por renglón
     * @return los renglones
     */
    public static List<String> partir(String texto, int ancho) {
        List<String> renglones = new java.util.ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : Textos.limpiar(texto).split(" ")) {
            if (actual.length() > 0 && actual.length() + 1 + palabra.length() > ancho) {
                renglones.add(actual.toString());
                actual.setLength(0);
            }
            if (actual.length() > 0) {
                actual.append(' ');
            }
            actual.append(palabra);
        }
        renglones.add(actual.toString());
        return renglones;
    }
}
