package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Formatos de patente argentinos (SPEC §3). Las patentes llegan ya normalizadas: mayúsculas,
 * sin espacios ni guiones.
 */
public final class Patentes {

    /** Auto o camioneta, formato 1995–2016: ABC123. */
    private static final String AUTO_ANTERIOR = "[A-Z]{3}\\d{3}";
    /** Auto o camioneta, formato Mercosur: AB123CD. */
    private static final String AUTO_MERCOSUR = "[A-Z]{2}\\d{3}[A-Z]{2}";
    /** Moto, formato anterior: 123ABC. */
    private static final String MOTO_ANTERIOR = "\\d{3}[A-Z]{3}";
    /** Moto, formato Mercosur: A123BCD. */
    private static final String MOTO_MERCOSUR = "[A-Z]\\d{3}[A-Z]{3}";

    private Patentes() {
    }

    /**
     * @param patente patente normalizada
     * @return {@code true} si es una patente válida de auto o camioneta
     */
    public static boolean esDeAuto(String patente) {
        return patente.matches(AUTO_ANTERIOR) || patente.matches(AUTO_MERCOSUR);
    }

    /**
     * @param patente patente normalizada
     * @return {@code true} si es una patente válida de moto
     */
    public static boolean esDeMoto(String patente) {
        return patente.matches(MOTO_ANTERIOR) || patente.matches(MOTO_MERCOSUR);
    }

    /**
     * @param escrita lo que escribió el usuario
     * @return mensaje de error para auto o camioneta
     */
    public static String mensajeAuto(String escrita) {
        return "\"" + escrita.trim() + "\" no es una patente válida. Formatos: ABC123 o AB123CD.";
    }

    /**
     * @param escrita lo que escribió el usuario
     * @return mensaje de error para moto
     */
    public static String mensajeMoto(String escrita) {
        return "\"" + escrita.trim() + "\" no es una patente de moto. Formatos: 123ABC o A123BCD.";
    }
}
