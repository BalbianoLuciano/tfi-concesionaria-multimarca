package ar.edu.utn.frre.concesionaria.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Formatos argentinos para mostrar y para leer números: separador de miles con punto y
 * decimales con coma ({@code $ 22.850.920}, {@code 112.900 km}, {@code 18/11/2026}).
 */
public final class Formatos {

    /** Configuración regional de Argentina. */
    public static final Locale ARGENTINA = Locale.of("es", "AR");

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_ENCABEZADO =
            DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", ARGENTINA);

    private Formatos() {
    }

    private static DecimalFormat formato(String patron) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(ARGENTINA);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        return new DecimalFormat(patron, simbolos);
    }

    /**
     * Número entero con separador de miles: 112900 → "112.900".
     *
     * @param numero el número
     * @return el texto
     */
    public static String miles(long numero) {
        return formato("#,##0").format(numero);
    }

    /**
     * Monto en pesos redondeado a pesos enteros: "$ 22.850.920". Los negativos llevan el signo
     * delante del símbolo: "-$ 3.344.000".
     *
     * @param monto el monto (si es {@code null} se muestra "—")
     * @return el texto
     */
    public static String moneda(BigDecimal monto) {
        if (monto == null) {
            return "—";
        }
        BigDecimal redondeado = monto.setScale(0, RoundingMode.HALF_UP);
        String signo = redondeado.signum() < 0 ? "-" : "";
        return signo + "$ " + formato("#,##0").format(redondeado.abs());
    }

    /**
     * Kilometraje: "112.900 km".
     *
     * @param km los kilómetros (si es {@code null} se muestra "—")
     * @return el texto
     */
    public static String km(Integer km) {
        return km == null ? "—" : miles(km) + " km";
    }

    /**
     * Porcentaje a partir de una fracción: 0.088 → "8,8 %".
     *
     * @param fraccion la fracción (0,35 = 35 %)
     * @return el texto
     */
    public static String porcentaje(BigDecimal fraccion) {
        return formato("0.#").format(fraccion.multiply(BigDecimal.valueOf(100))) + " %";
    }

    /**
     * Fecha corta: "18/11/2026".
     *
     * @param fecha la fecha (si es {@code null} se muestra "—")
     * @return el texto
     */
    public static String fecha(LocalDate fecha) {
        return fecha == null ? "—" : FECHA.format(fecha);
    }

    /**
     * Fecha con día de la semana para el encabezado: "mié 18/11/2026".
     *
     * @param fecha la fecha
     * @return el texto
     */
    public static String fechaEncabezado(LocalDate fecha) {
        return FECHA_ENCABEZADO.format(fecha).replace(".", "");
    }

    /**
     * Mes y año: "nov 2026".
     *
     * @param mes el mes
     * @return el texto
     */
    public static String mes(YearMonth mes) {
        return mes.getMonth().getDisplayName(TextStyle.SHORT, ARGENTINA).replace(".", "")
                + " " + mes.getYear();
    }

    /**
     * Lee una fecha en formato dd/mm/aaaa.
     *
     * @param texto el texto
     * @return la fecha
     * @throws IllegalArgumentException si no tiene ese formato o no es una fecha real
     */
    public static LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(Textos.limpiar(texto), DateTimeFormatter.ofPattern("d/M/uuuu"));
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("fecha inválida: " + texto, e);
        }
    }

    /**
     * Lee un número entero que puede venir con puntos de miles: "84.500" → 84500.
     * También acepta "84500" y "84 500".
     *
     * @param texto el texto escrito por el usuario
     * @return el número
     * @throws NumberFormatException si no es un entero
     */
    public static long leerEntero(String texto) {
        String limpio = Textos.limpiar(texto).replace(" ", "");
        if (!limpio.matches("-?\\d{1,3}(\\.\\d{3})+|-?\\d+")) {
            throw new NumberFormatException(texto);
        }
        return Long.parseLong(limpio.replace(".", ""));
    }

    /**
     * Lee un monto en pesos: "38.000.000", "38000000", "$ 38.000.000" o "1.234,50".
     *
     * @param texto el texto escrito por el usuario
     * @return el monto
     * @throws NumberFormatException si no es un monto
     */
    public static BigDecimal leerMonto(String texto) {
        String limpio = Textos.limpiar(texto).replace("$", "").replace(" ", "");
        if (!limpio.matches("-?(\\d{1,3}(\\.\\d{3})+|\\d+)(,\\d{1,2})?")) {
            throw new NumberFormatException(texto);
        }
        return new BigDecimal(limpio.replace(".", "").replace(',', '.'));
    }
}
