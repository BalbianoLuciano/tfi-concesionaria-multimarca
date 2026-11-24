package ar.edu.utn.frre.concesionaria.presentacion.consola;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import java.io.PrintStream;
import java.util.List;

/**
 * Salida a la terminal con la estructura fija de cada pantalla (SPEC §5): encabezado con el
 * sistema, el usuario, el rol y la fecha; título y ruta; contexto; contenido; pie.
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
     * Encabezado fijo (SPEC §5): sistema, usuario y fecha; una línea; el título de la pantalla a
     * la izquierda y la ruta a la derecha.
     *
     * @param derecha texto de la derecha ("lbalbiano · Vendedor · mié 18/11/2026")
     * @param titulo  título de la pantalla ("Vehículos")
     * @param ruta    la ruta ("Inicio › Vehículos")
     */
    public void encabezado(String derecha, String titulo, String ruta) {
        out.println(" " + estilo.titulo(SISTEMA) + relleno(SISTEMA, derecha)
                + estilo.tenue(derecha));
        regla();
        String mayusculas = titulo.toUpperCase();
        out.println(" " + estilo.titulo(mayusculas)
                + (ruta.isEmpty() ? "" : relleno(mayusculas, ruta) + estilo.tenue(ruta)));
    }

    /** Espacios para que {@code derecha} quede pegada al margen derecho. */
    private String relleno(String izquierda, String derecha) {
        return " ".repeat(Math.max(2, ancho - 1 - izquierda.length() - derecha.length()));
    }

    /** Línea separadora tenue de todo el ancho. */
    public void regla() {
        out.println(" " + estilo.tenue("─".repeat(ancho - 1)));
    }

    /**
     * Línea de contexto debajo del título: datos de la sección, en tenue.
     *
     * @param texto por ejemplo "10 disponibles · 0 km: 4 · usados: 6"
     */
    public void contexto(String texto) {
        out.println(" " + estilo.tenue(texto));
    }

    /**
     * Título de un grupo de opciones de menú.
     *
     * @param nombre por ejemplo "Consultar"
     */
    public void seccion(String nombre) {
        out.println("   " + estilo.negrita(nombre));
    }

    /**
     * Una opción de menú: tecla en negrita, etiqueta y descripción tenue, siempre en la misma
     * columna (también {@code 0} y {@code C}).
     *
     * @param tecla         la tecla ("1", "C", "0")
     * @param etiqueta      la etiqueta
     * @param detalle       descripción corta, o vacía
     * @param anchoEtiqueta ancho de la columna de etiquetas
     */
    public void opcion(String tecla, String etiqueta, String detalle, int anchoEtiqueta) {
        String texto = "     " + estilo.negrita(String.format("%2s", tecla)) + "  ";
        if (detalle == null || detalle.isEmpty()) {
            out.println(texto + etiqueta);
        } else {
            out.println(texto + String.format("%-" + anchoEtiqueta + "s", etiqueta) + "  "
                    + estilo.tenue(detalle));
        }
    }

    /**
     * Pie fijo: una línea y, debajo, las teclas de la pantalla.
     *
     * @param teclas por ejemplo "S siguiente · 0 volver", o vacío si sigue un prompt
     */
    public void pie(String teclas) {
        out.println();
        regla();
        if (teclas != null && !teclas.isEmpty()) {
            out.println(" " + estilo.tenue(teclas));
        }
    }

    /**
     * Encabezado de un paso de un formulario: "Paso 2 de 4 · Datos generales".
     *
     * @param numero número de paso
     * @param total  cantidad de pasos
     * @param nombre qué se pide en este paso
     */
    public void paso(int numero, int total, String nombre) {
        out.println();
        out.println(" " + estilo.tenue("Paso " + numero + " de " + total + " ·") + " "
                + estilo.titulo(nombre));
    }

    /**
     * Ayuda de un campo, antes de pedirlo: tenue y con sangría.
     *
     * @param texto la ayuda ("Entre 1990 y 2027.")
     */
    public void ayuda(String texto) {
        out.println("   " + estilo.tenue(texto));
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
     * Mensaje de éxito, separado por una línea en blanco (SPEC §5).
     *
     * @param mensaje mensaje de éxito, con {@code ✓}
     */
    public void exito(String mensaje) {
        out.println();
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
        for (String parte : partir("! " + mensaje, ancho - 2)) {
            out.println(" " + estilo.amarillo(parte));
        }
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
