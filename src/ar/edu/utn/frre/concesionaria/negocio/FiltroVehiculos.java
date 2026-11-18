package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Situacion;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Filtros combinables del listado de stock (CU-04). Cada campo en {@code null} significa "sin
 * filtro". Por defecto muestra solo los disponibles.
 *
 * <p>Se traduce a un {@link Predicate} encadenando con {@code and}: cada filtro activo agrega
 * una condición.</p>
 */
public class FiltroVehiculos {

    private TipoVehiculo tipo;
    private Estado estado;
    private Marca marca;
    private Integer anioDesde;
    private Integer anioHasta;
    private BigDecimal precioDesde;
    private BigDecimal precioHasta;
    private Situacion situacion = Situacion.DISPONIBLE;

    /**
     * @param hoy fecha para calcular el precio final
     * @return la condición que cumplen los vehículos que pasan el filtro
     */
    public Predicate<Vehiculo> comoPredicado(LocalDate hoy) {
        Predicate<Vehiculo> p = v -> true;
        if (situacion != null) {
            p = p.and(v -> v.getSituacion() == situacion);
        }
        if (tipo != null) {
            p = p.and(v -> v.getTipo() == tipo);
        }
        if (estado != null) {
            p = p.and(v -> v.getEstado() == estado);
        }
        if (marca != null) {
            p = p.and(v -> marca.equals(v.getMarca()));
        }
        if (anioDesde != null) {
            p = p.and(v -> v.getAnio() >= anioDesde);
        }
        if (anioHasta != null) {
            p = p.and(v -> v.getAnio() <= anioHasta);
        }
        if (precioDesde != null) {
            p = p.and(v -> v.calcularPrecioFinal(hoy).compareTo(precioDesde) >= 0);
        }
        if (precioHasta != null) {
            p = p.and(v -> v.calcularPrecioFinal(hoy).compareTo(precioHasta) <= 0);
        }
        return p;
    }

    /**
     * @return los filtros activos en texto ("Camioneta · Usado · 2018–2022"), o "Sin filtros"
     */
    public String describir() {
        List<String> partes = new ArrayList<>();
        partes.add(situacion == null ? "Todas las situaciones" : situacion.getEtiqueta() + "s");
        if (tipo != null) {
            partes.add(tipo.getEtiqueta());
        }
        if (estado != null) {
            partes.add(estado.getEtiqueta());
        }
        if (marca != null) {
            partes.add(marca.getNombre());
        }
        if (anioDesde != null || anioHasta != null) {
            partes.add("año " + (anioDesde == null ? "…" : anioDesde) + "–"
                    + (anioHasta == null ? "…" : anioHasta));
        }
        if (precioDesde != null || precioHasta != null) {
            partes.add((precioDesde == null ? "…" : Formatos.moneda(precioDesde)) + " a "
                    + (precioHasta == null ? "…" : Formatos.moneda(precioHasta)));
        }
        return String.join(" · ", partes);
    }

    /** Vuelve a los valores por defecto: solo disponibles, sin otros filtros. */
    public void limpiar() {
        tipo = null;
        estado = null;
        marca = null;
        anioDesde = null;
        anioHasta = null;
        precioDesde = null;
        precioHasta = null;
        situacion = Situacion.DISPONIBLE;
    }

    /** @return el tipo, o {@code null} */
    public TipoVehiculo getTipo() {
        return tipo;
    }

    /** @param tipo el tipo, o {@code null} para todos */
    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    /** @return el estado, o {@code null} */
    public Estado getEstado() {
        return estado;
    }

    /** @param estado el estado, o {@code null} para todos */
    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    /** @return la marca, o {@code null} */
    public Marca getMarca() {
        return marca;
    }

    /** @param marca la marca, o {@code null} para todas */
    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    /** @return el año mínimo, o {@code null} */
    public Integer getAnioDesde() {
        return anioDesde;
    }

    /** @param anioDesde el año mínimo, o {@code null} */
    public void setAnioDesde(Integer anioDesde) {
        this.anioDesde = anioDesde;
    }

    /** @return el año máximo, o {@code null} */
    public Integer getAnioHasta() {
        return anioHasta;
    }

    /** @param anioHasta el año máximo, o {@code null} */
    public void setAnioHasta(Integer anioHasta) {
        this.anioHasta = anioHasta;
    }

    /** @return el precio final mínimo, o {@code null} */
    public BigDecimal getPrecioDesde() {
        return precioDesde;
    }

    /** @param precioDesde el precio final mínimo, o {@code null} */
    public void setPrecioDesde(BigDecimal precioDesde) {
        this.precioDesde = precioDesde;
    }

    /** @return el precio final máximo, o {@code null} */
    public BigDecimal getPrecioHasta() {
        return precioHasta;
    }

    /** @param precioHasta el precio final máximo, o {@code null} */
    public void setPrecioHasta(BigDecimal precioHasta) {
        this.precioHasta = precioHasta;
    }

    /** @return la situación, o {@code null} para todas */
    public Situacion getSituacion() {
        return situacion;
    }

    /** @param situacion la situación, o {@code null} para todas */
    public void setSituacion(Situacion situacion) {
        this.situacion = situacion;
    }
}
