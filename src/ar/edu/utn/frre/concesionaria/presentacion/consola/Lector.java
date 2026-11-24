package ar.edu.utn.frre.concesionaria.presentacion.consola;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Data-entry con reintentos (SPEC §3).
 *
 * <p>Cada campo tiene 3 intentos. Cada error dice qué estuvo mal, cómo corregirlo y cuántos
 * intentos quedan. Al tercero se cancela la operación entera. {@code :q} cancela en cualquier
 * campo de texto y {@code 0} en cualquier lista.</p>
 *
 * <p>El lector solo convierte texto en datos ({@code Integer.parseInt}, montos): las reglas las
 * aplica el {@link Conversor} que recibe, que en general llama a un setter del modelo. Así la
 * misma regla vale para la consola y para el CSV.</p>
 */
public final class Lector {

    /** Intentos por campo. */
    public static final int INTENTOS = 3;

    /**
     * Convierte y valida un texto.
     *
     * @param <T> tipo del valor que produce
     */
    @FunctionalInterface
    public interface Conversor<T> {
        /**
         * @param texto lo que escribió el usuario, ya recortado
         * @return el valor
         * @throws DatoInvalidoException si no es válido, con el mensaje para el usuario
         */
        T convertir(String texto) throws DatoInvalidoException;
    }

    private final BufferedReader entrada;
    private final Terminal terminal;
    private final boolean interactivo;
    private String queSeCancela = "la operación";
    private boolean avisoClaveVisible;

    /**
     * @param entrada     de dónde leer
     * @param terminal    dónde escribir los prompts y errores
     * @param interactivo {@code true} si hay una terminal real; si no (entrada redirigida desde
     *                    un archivo), se repite lo leído para que la salida se pueda seguir
     */
    public Lector(BufferedReader entrada, Terminal terminal, boolean interactivo) {
        this.entrada = entrada;
        this.terminal = terminal;
        this.interactivo = interactivo;
    }

    /**
     * Fija qué se nombra al cancelar: "Se canceló el registro…", "Se canceló la venta…".
     *
     * @param que "el registro", "la venta"
     */
    public void operacion(String que) {
        this.queSeCancela = que;
    }

    /**
     * Lee una línea cruda.
     *
     * @param prompt el texto del prompt, sin el {@code ›}
     * @return la línea recortada
     * @throws FinDeEntrada si se cerró la entrada
     */
    public String linea(String prompt) {
        terminal.escribir(" " + terminal.estilo().prompt(prompt + " › "));
        try {
            String leida = entrada.readLine();
            if (leida == null) {
                throw new FinDeEntrada();
            }
            if (!interactivo) {
                terminal.linea(leida);
            }
            return Textos.limpiar(leida);
        } catch (IOException e) {
            throw new FinDeEntrada();
        }
    }

    /**
     * Lee un dato obligatorio.
     *
     * @param <T>       tipo del dato
     * @param etiqueta  nombre del campo ("Año")
     * @param conversor cómo convertir y validar
     * @return el valor
     * @throws OperacionCancelada si escribió {@code :q} o no quiso seguir tras agotar los intentos
     */
    public <T> T leer(String etiqueta, Conversor<T> conversor) throws OperacionCancelada {
        return leer(etiqueta, null, conversor);
    }

    /**
     * Lee un dato mostrando el valor actual entre corchetes. Enter lo mantiene.
     *
     * @param <T>       tipo del dato
     * @param etiqueta  nombre del campo
     * @param actual    valor actual para mostrar, o {@code null} si el dato es obligatorio
     * @param conversor cómo convertir y validar
     * @return el valor nuevo, o {@code null} si dejó el actual
     * @throws OperacionCancelada si escribió {@code :q} o no quiso seguir tras agotar los intentos
     */
    public <T> T leer(String etiqueta, String actual, Conversor<T> conversor)
            throws OperacionCancelada {
        String prompt = actual == null ? etiqueta : etiqueta + " [" + actual + "]";
        return leerInterno(prompt, etiqueta, actual != null, conversor);
    }

    /**
     * Lee un dato opcional: Enter lo deja vacío.
     *
     * @param <T>       tipo del dato
     * @param etiqueta  nombre del campo (conviene aclarar "Enter si no tiene")
     * @param conversor cómo convertir y validar
     * @return el valor, o {@code null} si lo dejó vacío
     * @throws OperacionCancelada si escribió {@code :q} o no quiso seguir tras agotar los intentos
     */
    public <T> T leerOpcional(String etiqueta, Conversor<T> conversor) throws OperacionCancelada {
        return leerInterno(etiqueta, etiqueta, true, conversor);
    }

    private <T> T leerInterno(String prompt, String etiqueta, boolean vacioPermitido,
            Conversor<T> conversor) throws OperacionCancelada {
        terminal.separarCampo();
        for (int intento = 1; true; intento++) {
            String texto = linea(prompt);
            if (texto.equalsIgnoreCase(":q")) {
                throw new OperacionCancelada("Se canceló " + queSeCancela + ". No se guardó nada.");
            }
            if (texto.isEmpty() && vacioPermitido) {
                return null;
            }
            try {
                if (texto.isEmpty()) {
                    throw new DatoInvalidoException(etiqueta, texto,
                            "Este dato es obligatorio. Escribí un valor, o :q para cancelar.");
                }
                return conversor.convertir(texto);
            } catch (DatoInvalidoException e) {
                mostrarError(e.getMessage(), intento);
            }
            if (intento == INTENTOS) {
                exigirSeguir(etiqueta);
                intento = 0;
            }
        }
    }

    /**
     * Agotados los intentos de un campo, pregunta si seguir en lugar de descartar lo cargado
     * (SPEC §3). Enter o "s" sigue con otros 3 intentos.
     *
     * @param etiqueta el campo
     * @throws OperacionCancelada si respondió que no
     */
    public void exigirSeguir(String etiqueta) throws OperacionCancelada {
        while (true) {
            String respuesta = Textos.clave(linea("¿Seguir con «" + etiqueta + "»? (s/n)"));
            if (respuesta.isEmpty() || respuesta.equals("s") || respuesta.equals("si")) {
                return;
            }
            if (respuesta.equals("n") || respuesta.equals("no") || respuesta.equals(":q")) {
                throw new OperacionCancelada("Se canceló " + queSeCancela
                        + ": demasiados intentos en «" + etiqueta + "».");
            }
            terminal.error("Respondé s para seguir con «" + etiqueta + "» o n para cancelar.");
        }
    }

    private void mostrarError(String mensaje, int intento) {
        int quedan = INTENTOS - intento;
        terminal.error(mensaje + (quedan == 0 ? ""
                : quedan == 1 ? " Te queda 1 intento." : " Te quedan " + quedan + " intentos."));
    }

    /**
     * Muestra una lista numerada y pide elegir. {@code 0} cancela.
     *
     * @param <T>      tipo de las opciones
     * @param etiqueta nombre del campo
     * @param opciones las opciones
     * @param mostrar  cómo mostrar cada una
     * @return la elegida
     * @throws OperacionCancelada si eligió 0 o agotó los intentos
     */
    public <T> T elegir(String etiqueta, List<T> opciones, Function<T, String> mostrar)
            throws OperacionCancelada {
        return elegir(etiqueta, opciones, mostrar, null);
    }

    /**
     * Como {@link #elegir(String, List, Function)}, pero con valor actual: Enter lo mantiene.
     *
     * @param <T>      tipo de las opciones
     * @param etiqueta nombre del campo
     * @param opciones las opciones
     * @param mostrar  cómo mostrar cada una
     * @param actual   el valor actual, o {@code null}
     * @return la elegida, o el actual si apretó Enter
     * @throws OperacionCancelada si eligió 0 o agotó los intentos
     */
    public <T> T elegir(String etiqueta, List<T> opciones, Function<T, String> mostrar, T actual)
            throws OperacionCancelada {
        int columnas = opciones.size() > 6 ? 3 : 1;
        int anchoOpcion = opciones.stream().mapToInt(o -> mostrar.apply(o).length()).max()
                .orElse(0) + 6;
        StringBuilder fila = new StringBuilder();
        terminal.separarCampo();
        for (int i = 0; i < opciones.size(); i++) {
            String item = String.format("%3d  %s", i + 1, mostrar.apply(opciones.get(i)));
            fila.append(String.format("%-" + anchoOpcion + "s", item));
            if ((i + 1) % columnas == 0 || i == opciones.size() - 1) {
                terminal.linea(" " + fila.toString().stripTrailing());
                fila.setLength(0);
            }
        }
        terminal.introduccion();
        String textoActual = actual == null ? null : mostrar.apply(actual);
        Integer elegido = leer(etiqueta, textoActual, texto -> {
            if (texto.equals("0")) {
                return 0;
            }
            int n;
            try {
                n = Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                n = porNombre(etiqueta, texto, opciones, mostrar);
            }
            if (n < 1 || n > opciones.size()) {
                throw new DatoInvalidoException(etiqueta, texto, "Elegí un número entre 1 y "
                        + opciones.size() + ", o 0 para volver.");
            }
            return n;
        });
        if (elegido == null) {
            return actual;
        }
        if (elegido == 0) {
            throw new OperacionCancelada("Se canceló " + queSeCancela + ". No se guardó nada.");
        }
        return opciones.get(elegido - 1);
    }

    /**
     * Elige una opción por su nombre o su comienzo ("auto" → Automóvil), SPEC §3.
     *
     * @return el número de la opción (desde 1)
     * @throws DatoInvalidoException si no coincide ninguna, o coinciden varias
     */
    private static <T> int porNombre(String etiqueta, String texto, List<T> opciones,
            Function<T, String> mostrar) throws DatoInvalidoException {
        List<String> nombres = opciones.stream().map(mostrar).toList();
        List<Integer> encontradas = Textos.coincidencias(texto, nombres);
        if (encontradas.size() == 1) {
            return encontradas.get(0) + 1;
        }
        if (encontradas.isEmpty()) {
            throw new DatoInvalidoException(etiqueta, texto, "\"" + texto + "\" no es ninguna"
                    + " de las opciones. Escribí el número (1 a " + opciones.size()
                    + ") o el nombre, o 0 para volver.");
        }
        List<String> posibles = encontradas.stream().map(nombres::get).toList();
        String lista = posibles.size() == 2 ? posibles.get(0) + " o " + posibles.get(1)
                : String.join(", ", posibles.subList(0, posibles.size() - 1)) + " o "
                        + posibles.get(posibles.size() - 1);
        throw new DatoInvalidoException(etiqueta, texto, "«" + texto + "» puede ser " + lista
                + ". Escribí más letras o el número.");
    }

    /**
     * Pide una opción de menú. No cancela: devuelve lo que se haya escrito para que el menú
     * decida, y repregunta si no es válido.
     *
     * @param opcionesValidas las opciones aceptadas (en minúsculas)
     * @return la opción elegida, en minúsculas
     */
    public String opcion(List<String> opcionesValidas) {
        while (true) {
            String texto = linea("Elegí una opción").toLowerCase();
            if (opcionesValidas.contains(texto)) {
                return texto;
            }
            String lista = String.join(", ", opcionesValidas).toUpperCase();
            terminal.error(texto.isEmpty() ? "Escribí una de las opciones: " + lista + "."
                    : "\"" + texto + "\" no es una opción. Elegí una de: " + lista + ".");
        }
    }

    /**
     * Confirmación s/n (SPEC §5: proporcional para las altas).
     *
     * @param pregunta la pregunta, sin el "(s/n)"
     * @return {@code true} si respondió que sí
     * @throws OperacionCancelada si agotó los intentos
     */
    public boolean confirmar(String pregunta) throws OperacionCancelada {
        return leer(pregunta + " (s/n)", texto -> {
            String clave = Textos.clave(texto);
            if (clave.equals("s") || clave.equals("si")) {
                return true;
            }
            if (clave.equals("n") || clave.equals("no")) {
                return false;
            }
            throw new DatoInvalidoException("Confirmación", texto, "Respondé s o n.");
        });
    }

    /**
     * Lee una clave sin mostrarla. Si no hay una consola real (IDE, entrada redirigida), avisa
     * una vez y la lee visible.
     *
     * <p>Contenido extra (SPEC §7): {@code System.console().readPassword()}.</p>
     *
     * @param etiqueta el prompt
     * @return la clave (puede estar vacía)
     */
    public String clave(String etiqueta) {
        Console consola = System.console();
        if (interactivo && consola != null) {
            char[] leida = consola.readPassword(" %s", terminal.estilo().prompt(etiqueta + " › "));
            if (leida == null) {
                throw new FinDeEntrada();
            }
            return new String(leida);
        }
        if (!avisoClaveVisible) {
            terminal.info("No hay una terminal real: la clave se va a ver mientras la escribís.");
            avisoClaveVisible = true;
        }
        return linea(etiqueta);
    }

    /** Espera Enter. */
    public void pausa() {
        terminal.blanco();
        terminal.escribir(" " + terminal.estilo().tenue("Enter para continuar ›") + " ");
        try {
            String leida = entrada.readLine();
            if (leida == null) {
                throw new FinDeEntrada();
            }
            if (!interactivo) {
                terminal.linea(leida);
            }
        } catch (IOException e) {
            throw new FinDeEntrada();
        }
    }

    // ---------------------------------------------------------------- conversores comunes

    /**
     * Número entero; acepta puntos de miles ("84.500").
     *
     * @param campo  nombre del campo
     * @param ayuda  cómo escribirlo bien ("Escribí el año con cuatro dígitos, por ejemplo 2019.")
     * @return el conversor
     */
    public static Conversor<Integer> entero(String campo, String ayuda) {
        return texto -> {
            try {
                long n = Formatos.leerEntero(texto);
                if (n > Integer.MAX_VALUE || n < Integer.MIN_VALUE) {
                    throw new NumberFormatException(texto);
                }
                return (int) n;
            } catch (NumberFormatException e) {
                throw new DatoInvalidoException(campo, texto,
                        "\"" + texto + "\" no es un número. " + ayuda, e);
            }
        };
    }

    /**
     * Monto en pesos: "38.000.000", "38000000" o "$ 38.000.000".
     *
     * @param campo nombre del campo
     * @return el conversor
     */
    public static Conversor<BigDecimal> monto(String campo) {
        return texto -> {
            try {
                return Formatos.leerMonto(texto);
            } catch (NumberFormatException e) {
                throw new DatoInvalidoException(campo, texto, "\"" + texto
                        + "\" no es un monto. Escribilo con o sin puntos: 38.000.000.", e);
            }
        };
    }

    /**
     * Fecha dd/mm/aaaa.
     *
     * @param campo nombre del campo
     * @return el conversor
     */
    public static Conversor<LocalDate> fecha(String campo) {
        return texto -> {
            try {
                return Formatos.leerFecha(texto);
            } catch (IllegalArgumentException e) {
                throw new DatoInvalidoException(campo, texto, "\"" + texto
                        + "\" no es una fecha. Escribila como dd/mm/aaaa, por ejemplo 01/11/2026.", e);
            }
        };
    }
}
