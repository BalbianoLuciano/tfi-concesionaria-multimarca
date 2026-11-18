package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.excepciones.VehiculoNoDisponibleException;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que el negocio necesita para cotizar y vender algo. {@code ServicioVentas} trabaja con
 * esta interfaz, no con {@link Vehiculo}: si mañana se vendieran accesorios, implementarían
 * {@code Vendible} y la venta no cambiaría (transmisión del comportamiento).
 */
public interface Vendible {

    /**
     * @return el código que identifica lo que se vende
     */
    String getCodigo();

    /**
     * @return una descripción de una línea para el comprobante ("Toyota Hilux 2019")
     */
    String getDescripcion();

    /**
     * Calcula el precio final a la fecha dada.
     *
     * @param hoy la fecha de referencia (para la antigüedad)
     * @return el precio final en pesos, redondeado a pesos enteros
     */
    BigDecimal calcularPrecioFinal(LocalDate hoy);

    /**
     * Arma el desglose completo del precio.
     *
     * @param hoy la fecha de referencia
     * @return el desglose
     */
    DesglosePrecio desglosarPrecio(LocalDate hoy);

    /**
     * @return {@code true} si se puede vender ahora
     */
    boolean estaDisponible();

    /**
     * Marca lo vendido.
     *
     * @throws VehiculoNoDisponibleException si no estaba disponible
     */
    void marcarVendido() throws VehiculoNoDisponibleException;

    /**
     * Deshace {@link #marcarVendido()}. Lo usa la venta si falla el guardado, para no dejar
     * algo vendido sin venta registrada.
     */
    void revertirVenta();
}
