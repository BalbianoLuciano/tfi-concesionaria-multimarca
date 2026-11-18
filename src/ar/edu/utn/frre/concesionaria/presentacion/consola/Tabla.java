package ar.edu.utn.frre.concesionaria.presentacion.consola;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla de texto con columnas calculadas según el contenido, números alineados a la derecha y
 * una sola línea de borde bajo el encabezado (SPEC §5). Si no entra en el ancho, achica las
 * columnas de texto marcadas como recortables y corta con "…".
 */
public final class Tabla {

    /** Alineación de una columna. */
    public enum Alineacion {
        /** Texto. */
        IZQUIERDA,
        /** Números. */
        DERECHA
    }

    private static final String SEPARACION = "  ";

    private final List<String> titulos = new ArrayList<>();
    private final List<Alineacion> alineaciones = new ArrayList<>();
    private final List<Boolean> recortables = new ArrayList<>();
    private final List<String[]> filas = new ArrayList<>();

    /**
     * Agrega una columna de texto que se puede recortar si falta espacio.
     *
     * @param titulo el encabezado
     * @return la misma tabla, para encadenar
     */
    public Tabla texto(String titulo) {
        return columna(titulo, Alineacion.IZQUIERDA, true);
    }

    /**
     * Agrega una columna de texto que no se recorta (códigos, fechas).
     *
     * @param titulo el encabezado
     * @return la misma tabla
     */
    public Tabla fija(String titulo) {
        return columna(titulo, Alineacion.IZQUIERDA, false);
    }

    /**
     * Agrega una columna numérica, alineada a la derecha.
     *
     * @param titulo el encabezado
     * @return la misma tabla
     */
    public Tabla numero(String titulo) {
        return columna(titulo, Alineacion.DERECHA, false);
    }

    private Tabla columna(String titulo, Alineacion alineacion, boolean recortable) {
        titulos.add(titulo);
        alineaciones.add(alineacion);
        recortables.add(recortable);
        return this;
    }

    /**
     * Agrega una fila. Las celdas pueden traer códigos de color.
     *
     * @param celdas una celda por columna
     */
    public void fila(String... celdas) {
        filas.add(celdas);
    }

    /** @return {@code true} si no tiene filas */
    public boolean estaVacia() {
        return filas.isEmpty();
    }

    /**
     * Arma las líneas de la tabla.
     *
     * @param ancho  ancho disponible
     * @param estilo para el encabezado y el borde
     * @return las líneas, sin salto final
     */
    public List<String> renglones(int ancho, Estilo estilo) {
        int n = titulos.size();
        int[] anchos = new int[n];
        for (int c = 0; c < n; c++) {
            anchos[c] = titulos.get(c).length();
            for (String[] f : filas) {
                anchos[c] = Math.max(anchos[c], Estilo.largoVisible(celda(f, c)));
            }
        }
        ajustar(anchos, ancho - 1);

        List<String> salida = new ArrayList<>();
        StringBuilder enc = new StringBuilder(" ");
        for (int c = 0; c < n; c++) {
            enc.append(c > 0 ? SEPARACION : "")
                    .append(alinear(titulos.get(c), anchos[c], alineaciones.get(c)));
        }
        salida.add(estilo.negrita(enc.toString()));
        salida.add(" " + estilo.tenue("─".repeat(Math.max(0, total(anchos)))));
        for (String[] f : filas) {
            StringBuilder linea = new StringBuilder(" ");
            for (int c = 0; c < n; c++) {
                String valor = celda(f, c);
                if (Estilo.largoVisible(valor) > anchos[c]) {
                    // Se recorta el texto sin color: un código ANSI cortado al medio rompe la
                    // terminal.
                    valor = Textos.recortar(valor.replaceAll("\u001B\\[[0-9;]*m", ""), anchos[c]);
                }
                linea.append(c > 0 ? SEPARACION : "").append(alinear(valor, anchos[c],
                        alineaciones.get(c)));
            }
            salida.add(linea.toString().stripTrailing());
        }
        return salida;
    }

    private static String celda(String[] fila, int c) {
        return c < fila.length && fila[c] != null ? fila[c] : "";
    }

    private int total(int[] anchos) {
        int suma = SEPARACION.length() * (anchos.length - 1);
        for (int a : anchos) {
            suma += a;
        }
        return suma;
    }

    /** Achica de a uno la columna recortable más ancha hasta entrar o llegar a 6 caracteres. */
    private void ajustar(int[] anchos, int disponible) {
        while (total(anchos) > disponible) {
            int mayor = -1;
            for (int c = 0; c < anchos.length; c++) {
                if (recortables.get(c) && anchos[c] > 6 && (mayor < 0 || anchos[c] > anchos[mayor])) {
                    mayor = c;
                }
            }
            if (mayor < 0) {
                return;
            }
            anchos[mayor]--;
        }
    }

    private static String alinear(String texto, int ancho, Alineacion alineacion) {
        int relleno = Math.max(0, ancho - Estilo.largoVisible(texto));
        return alineacion == Alineacion.DERECHA
                ? " ".repeat(relleno) + texto
                : texto + " ".repeat(relleno);
    }
}
