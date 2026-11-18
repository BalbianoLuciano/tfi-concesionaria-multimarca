package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaCamioneta;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoMotocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;

/**
 * Datos de prueba reutilizables. Fija "hoy" en el 15/06/2026 para que los ejemplos de la spec
 * den siempre lo mismo.
 */
public final class Fabrica {

    /** Fecha fija de las pruebas. */
    public static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    private Fabrica() {
    }

    /** Fija el reloj del sistema en {@link #HOY}. */
    public static void fijarHoy() {
        Reloj.fijar(HOY);
    }

    /**
     * @param nombre nombre de la marca
     * @return una marca que fabrica los tres tipos
     * @throws DatoInvalidoException no debería pasar
     */
    public static Marca marca(String nombre) throws DatoInvalidoException {
        return new Marca(nombre, "Japón", EnumSet.allOf(TipoVehiculo.class));
    }

    /**
     * @param anio   año
     * @param km     kilómetros
     * @param precio precio de lista
     * @return un automóvil usado
     * @throws DatoInvalidoException si los datos no son válidos
     */
    public static Automovil autoUsado(int anio, int km, String precio) throws DatoInvalidoException {
        fijarHoy();
        return new Automovil(Estado.USADO, marca("Toyota"), "Corolla", anio, Color.GRIS,
                new BigDecimal(precio), km, "AB123CD", CarroceriaAutomovil.SEDAN, 4);
    }

    /**
     * @param precio precio de lista
     * @return un automóvil 0 km sin patente
     * @throws DatoInvalidoException si los datos no son válidos
     */
    public static Automovil autoNuevo(String precio) throws DatoInvalidoException {
        fijarHoy();
        return new Automovil(Estado.NUEVO, marca("Toyota"), "Yaris", 2026, Color.BLANCO,
                new BigDecimal(precio), null, null, CarroceriaAutomovil.HATCHBACK, 5);
    }

    /**
     * @return la Hilux del ejemplo 1 de SPEC §4
     * @throws DatoInvalidoException si los datos no son válidos
     */
    public static Camioneta hiluxEjemplo() throws DatoInvalidoException {
        fijarHoy();
        return new Camioneta(Estado.USADO, marca("Toyota"), "Hilux", 2019, Color.BLANCO,
                new BigDecimal("38000000"), 112_900, "AD456FG",
                CarroceriaCamioneta.PICKUP_DOBLE_CABINA, true, 1000);
    }

    /**
     * @param precio precio de lista
     * @return una moto 0 km
     * @throws DatoInvalidoException si los datos no son válidos
     */
    public static Motocicleta motoNueva(String precio) throws DatoInvalidoException {
        fijarHoy();
        return new Motocicleta(Estado.NUEVO, marca("Honda"), "CB 500F", 2026, Color.ROJO,
                new BigDecimal(precio), null, null, TipoMotocicleta.NAKED, 471);
    }
}
