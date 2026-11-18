package ar.edu.utn.frre.concesionaria.comun;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Fuente única de "hoy" para todo el sistema.
 *
 * <p>Contenido extra (SPEC §7): se usa {@link Clock} en lugar de {@code LocalDate.now()}
 * suelto para que las pruebas puedan fijar la fecha (por ejemplo, 2026) y los ejemplos de
 * precio de la spec den siempre lo mismo, sin importar cuándo se corran.</p>
 */
public final class Reloj {

    private static Clock reloj = Clock.systemDefaultZone();

    private Reloj() {
    }

    /**
     * @return la fecha de hoy según el reloj vigente
     */
    public static LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /**
     * @return el año actual según el reloj vigente
     */
    public static int anioActual() {
        return hoy().getYear();
    }

    /**
     * Fija el reloj. Solo lo usan las pruebas y las mediciones.
     *
     * @param nuevo el reloj a usar
     */
    public static void fijar(Clock nuevo) {
        reloj = nuevo;
    }

    /**
     * Fija "hoy" en una fecha dada. Atajo para las pruebas.
     *
     * @param fecha la fecha que pasa a ser hoy
     */
    public static void fijar(LocalDate fecha) {
        reloj = Clock.fixed(fecha.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                java.time.ZoneOffset.UTC);
    }

    /** Vuelve al reloj del sistema. */
    public static void restablecer() {
        reloj = Clock.systemDefaultZone();
    }
}
