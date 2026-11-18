package ar.edu.utn.frre.concesionaria.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Desglose del precio de un vehículo (SPEC §4): lo que se le muestra al cliente y lo que queda
 * congelado en la venta.
 *
 * <p>Es un POJO inmutable: todos los campos son {@code final} y no tiene setters. Una vez
 * calculado no cambia, aunque después cambien las reglas de precio.</p>
 *
 * <p>Las depreciaciones se guardan como montos positivos que se restan; los porcentajes
 * aplicados se guardan aparte para poder mostrarlos.</p>
 */
public final class DesglosePrecio implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BigDecimal precioLista;
    private final BigDecimal porcentajeAntiguedad;
    private final BigDecimal depreciacionAntiguedad;
    private final BigDecimal porcentajeKm;
    private final BigDecimal depreciacionKm;
    private final BigDecimal porcentajeRecargos;
    private final BigDecimal recargos;
    private final BigDecimal patentamiento;
    private final BigDecimal precioFinal;

    /**
     * @param precioLista            precio de lista
     * @param porcentajeAntiguedad   fracción aplicada por antigüedad (0,35 = 35 %)
     * @param depreciacionAntiguedad monto descontado por antigüedad
     * @param porcentajeKm           fracción aplicada por kilometraje
     * @param depreciacionKm         monto descontado por kilometraje
     * @param porcentajeRecargos     fracción de recargos (0,07 = 7 %)
     * @param recargos               monto de recargos
     * @param patentamiento          monto de patentamiento (solo 0 km)
     * @param precioFinal            precio final
     */
    public DesglosePrecio(BigDecimal precioLista, BigDecimal porcentajeAntiguedad,
            BigDecimal depreciacionAntiguedad, BigDecimal porcentajeKm, BigDecimal depreciacionKm,
            BigDecimal porcentajeRecargos, BigDecimal recargos, BigDecimal patentamiento,
            BigDecimal precioFinal) {
        this.precioLista = precioLista;
        this.porcentajeAntiguedad = porcentajeAntiguedad;
        this.depreciacionAntiguedad = depreciacionAntiguedad;
        this.porcentajeKm = porcentajeKm;
        this.depreciacionKm = depreciacionKm;
        this.porcentajeRecargos = porcentajeRecargos;
        this.recargos = recargos;
        this.patentamiento = patentamiento;
        this.precioFinal = precioFinal;
    }

    /** @return el precio de lista */
    public BigDecimal getPrecioLista() {
        return precioLista;
    }

    /** @return la fracción descontada por antigüedad */
    public BigDecimal getPorcentajeAntiguedad() {
        return porcentajeAntiguedad;
    }

    /** @return el monto descontado por antigüedad */
    public BigDecimal getDepreciacionAntiguedad() {
        return depreciacionAntiguedad;
    }

    /** @return la fracción descontada por kilometraje */
    public BigDecimal getPorcentajeKm() {
        return porcentajeKm;
    }

    /** @return el monto descontado por kilometraje */
    public BigDecimal getDepreciacionKm() {
        return depreciacionKm;
    }

    /** @return la fracción de recargos */
    public BigDecimal getPorcentajeRecargos() {
        return porcentajeRecargos;
    }

    /** @return el monto de recargos */
    public BigDecimal getRecargos() {
        return recargos;
    }

    /** @return el monto de patentamiento */
    public BigDecimal getPatentamiento() {
        return patentamiento;
    }

    /** @return el precio final */
    public BigDecimal getPrecioFinal() {
        return precioFinal;
    }

    @Override
    public String toString() {
        return "DesglosePrecio[lista=" + precioLista + ", antig=-" + depreciacionAntiguedad
                + ", km=-" + depreciacionKm + ", recargos=+" + recargos + ", patent=+"
                + patentamiento + ", final=" + precioFinal + "]";
    }
}
