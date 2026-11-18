package ar.edu.utn.frre.concesionaria.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Venta registrada (CU-10). Guarda una copia de los datos del vehículo y del precio, no una
 * referencia viva: si después cambian las reglas de precio o se corrige el vehículo, la venta
 * sigue diciendo lo que se cobró.
 */
public class Venta implements Identificable<Integer>, Serializable, Comparable<Venta> {

    private static final long serialVersionUID = 1L;

    private Integer numero;
    private LocalDate fecha;
    private String codigoVehiculo;
    private String descripcionVehiculo;
    private TipoVehiculo tipoVehiculo;
    private String marca;
    private Comprador comprador;
    private String usuarioVendedor;
    private DesglosePrecio desglose;

    /** Constructor vacío de JavaBean. */
    public Venta() {
    }

    /**
     * @param numero              número correlativo
     * @param fecha               fecha de la venta
     * @param codigoVehiculo      código del vehículo vendido
     * @param descripcionVehiculo descripción congelada ("Toyota Hilux 2019")
     * @param tipoVehiculo        tipo de vehículo, para los reportes
     * @param marca               nombre de la marca, para los reportes
     * @param comprador           comprador
     * @param usuarioVendedor     usuario que vendió
     * @param desglose            precio congelado
     */
    public Venta(Integer numero, LocalDate fecha, String codigoVehiculo,
            String descripcionVehiculo, TipoVehiculo tipoVehiculo, String marca,
            Comprador comprador, String usuarioVendedor, DesglosePrecio desglose) {
        this.numero = numero;
        this.fecha = fecha;
        this.codigoVehiculo = codigoVehiculo;
        this.descripcionVehiculo = descripcionVehiculo;
        this.tipoVehiculo = tipoVehiculo;
        this.marca = marca;
        this.comprador = comprador;
        this.usuarioVendedor = usuarioVendedor;
        this.desglose = desglose;
    }

    @Override
    public Integer getId() {
        return numero;
    }

    /** @return el precio final cobrado */
    public BigDecimal getPrecioFinal() {
        return desglose == null ? BigDecimal.ZERO : desglose.getPrecioFinal();
    }

    /** @return el número correlativo */
    public Integer getNumero() {
        return numero;
    }

    /** @param numero el número correlativo */
    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    /** @return la fecha */
    public LocalDate getFecha() {
        return fecha;
    }

    /** @param fecha la fecha */
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    /** @return el código del vehículo */
    public String getCodigoVehiculo() {
        return codigoVehiculo;
    }

    /** @param codigoVehiculo el código del vehículo */
    public void setCodigoVehiculo(String codigoVehiculo) {
        this.codigoVehiculo = codigoVehiculo;
    }

    /** @return la descripción congelada del vehículo */
    public String getDescripcionVehiculo() {
        return descripcionVehiculo;
    }

    /** @param descripcionVehiculo la descripción del vehículo */
    public void setDescripcionVehiculo(String descripcionVehiculo) {
        this.descripcionVehiculo = descripcionVehiculo;
    }

    /** @return el tipo de vehículo */
    public TipoVehiculo getTipoVehiculo() {
        return tipoVehiculo;
    }

    /** @param tipoVehiculo el tipo de vehículo */
    public void setTipoVehiculo(TipoVehiculo tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo;
    }

    /** @return el nombre de la marca */
    public String getMarca() {
        return marca;
    }

    /** @param marca el nombre de la marca */
    public void setMarca(String marca) {
        this.marca = marca;
    }

    /** @return el comprador */
    public Comprador getComprador() {
        return comprador;
    }

    /** @param comprador el comprador */
    public void setComprador(Comprador comprador) {
        this.comprador = comprador;
    }

    /** @return el usuario que vendió */
    public String getUsuarioVendedor() {
        return usuarioVendedor;
    }

    /** @param usuarioVendedor el usuario que vendió */
    public void setUsuarioVendedor(String usuarioVendedor) {
        this.usuarioVendedor = usuarioVendedor;
    }

    /** @return el desglose congelado */
    public DesglosePrecio getDesglose() {
        return desglose;
    }

    /** @param desglose el desglose congelado */
    public void setDesglose(DesglosePrecio desglose) {
        this.desglose = desglose;
    }

    /** Igualdad por número: es correlativo y único. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Venta)) {
            return false;
        }
        Venta otra = (Venta) o;
        return numero != null && numero.equals(otra.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(numero);
    }

    /** Orden cronológico por número. */
    @Override
    public int compareTo(Venta otra) {
        return Integer.compare(numero, otra.numero);
    }

    @Override
    public String toString() {
        return "Venta[" + numero + ", " + fecha + ", " + codigoVehiculo + ", " + comprador
                + ", " + usuarioVendedor + ", " + getPrecioFinal() + "]";
    }
}
