package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.modelo.ConEtiqueta;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Órdenes del listado de stock (CU-04). El orden natural está en {@code Vehiculo.compareTo};
 * los demás se arman con {@code Comparator.comparing(...).thenComparing(...)}.
 */
public enum OrdenVehiculos implements ConEtiqueta {

    /** Tipo y marca (orden natural). */
    TIPO_Y_MARCA("Tipo y marca"),
    /** Precio final, de menor a mayor. */
    PRECIO_ASC("Precio ↑"),
    /** Precio final, de mayor a menor. */
    PRECIO_DESC("Precio ↓"),
    /** Año, del más viejo al más nuevo. */
    ANIO_ASC("Año ↑"),
    /** Año, del más nuevo al más viejo. */
    ANIO_DESC("Año ↓"),
    /** Kilometraje, de menor a mayor. */
    KILOMETRAJE("Kilometraje");

    private final String etiqueta;

    OrdenVehiculos(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @param precioFinal cómo obtener el precio final de cada vehículo. El servicio pasa
     *                    precios ya calculados, para no recalcularlos en cada comparación
     * @return el comparador; desempata siempre por el orden natural
     */
    public Comparator<Vehiculo> comparador(Function<Vehiculo, BigDecimal> precioFinal) {
        Comparator<Vehiculo> porPrecio = Comparator.comparing(precioFinal);
        Comparator<Vehiculo> natural = Comparator.naturalOrder();
        switch (this) {
            case PRECIO_ASC:
                return porPrecio.thenComparing(natural);
            case PRECIO_DESC:
                return porPrecio.reversed().thenComparing(natural);
            case ANIO_ASC:
                return Comparator.comparing(Vehiculo::getAnio).thenComparing(natural);
            case ANIO_DESC:
                return Comparator.comparing(Vehiculo::getAnio).reversed().thenComparing(natural);
            case KILOMETRAJE:
                return Comparator.comparing(Vehiculo::getKilometraje).thenComparing(natural);
            default:
                return natural;
        }
    }
}
