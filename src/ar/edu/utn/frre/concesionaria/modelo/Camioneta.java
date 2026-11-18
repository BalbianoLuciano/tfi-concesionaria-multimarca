package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.math.BigDecimal;

/**
 * Camioneta: carrocería, tracción 4x4 y carga útil. Prefijo de código C.
 *
 * <p>Reglas de precio (SPEC §4; valores de ejemplo, no una tabla oficial):</p>
 * <ul>
 *   <li>usada: 5 % por año de antigüedad, tope 40 %; 0,8 % cada 10.000 km, tope 12 %;</li>
 *   <li>recargo 4x4: +7 %;</li>
 *   <li>0 km: patentamiento del 3 %.</li>
 * </ul>
 */
public class Camioneta extends Vehiculo {

    private static final long serialVersionUID = 1L;

    /** Depreciación por año de antigüedad: 5 %. */
    public static final BigDecimal DEPRECIACION_POR_ANIO = new BigDecimal("0.05");
    /** Tope de depreciación por antigüedad: 40 %. */
    public static final BigDecimal TOPE_DEPRECIACION_ANIO = new BigDecimal("0.40");
    /** Tramo de kilometraje: 10.000 km. */
    public static final int KM_POR_TRAMO = 10_000;
    /** Depreciación por tramo de km: 0,8 %. */
    public static final BigDecimal DEPRECIACION_POR_TRAMO = new BigDecimal("0.008");
    /** Tope de depreciación por km: 12 %. */
    public static final BigDecimal TOPE_DEPRECIACION_KM = new BigDecimal("0.12");
    /** Recargo por tracción 4x4: 7 %. */
    public static final BigDecimal RECARGO_4X4 = new BigDecimal("0.07");
    /** Patentamiento de un 0 km: 3 %. */
    public static final BigDecimal PATENTAMIENTO = new BigDecimal("0.03");
    /** Carga útil mínima en kg. */
    public static final int CARGA_MINIMA = 300;
    /** Carga útil máxima en kg. */
    public static final int CARGA_MAXIMA = 3_500;

    private CarroceriaCamioneta carroceria;
    private Boolean traccion4x4;
    private Integer cargaUtilKg;

    /** Constructor vacío de JavaBean. */
    public Camioneta() {
    }

    /**
     * @param estado      nuevo o usado
     * @param marca       marca del catálogo
     * @param modelo      modelo
     * @param anio        año
     * @param color       color
     * @param precioLista precio de lista
     * @param kilometraje kilómetros (si es usada)
     * @param patente     patente
     * @param carroceria  carrocería
     * @param traccion4x4 si tiene tracción 4x4
     * @param cargaUtilKg carga útil en kg (300 a 3.500)
     * @throws DatoInvalidoException si algún dato no cumple su regla
     */
    public Camioneta(Estado estado, Marca marca, String modelo, Integer anio, Color color,
            BigDecimal precioLista, Integer kilometraje, String patente,
            CarroceriaCamioneta carroceria, Boolean traccion4x4, Integer cargaUtilKg)
            throws DatoInvalidoException {
        super(estado, marca, modelo, anio, color, precioLista, kilometraje, patente);
        setCarroceria(carroceria);
        setTraccion4x4(traccion4x4);
        setCargaUtilKg(cargaUtilKg);
    }

    /**
     * Constructor de copia.
     *
     * @param otra la camioneta a copiar
     */
    protected Camioneta(Camioneta otra) {
        super(otra);
        this.carroceria = otra.carroceria;
        this.traccion4x4 = otra.traccion4x4;
        this.cargaUtilKg = otra.cargaUtilKg;
    }

    @Override
    public TipoVehiculo getTipo() {
        return TipoVehiculo.CAMIONETA;
    }

    @Override
    protected BigDecimal depreciacionPorAnio() {
        return DEPRECIACION_POR_ANIO;
    }

    @Override
    protected BigDecimal topeDepreciacionAnio() {
        return TOPE_DEPRECIACION_ANIO;
    }

    @Override
    protected int kmPorTramo() {
        return KM_POR_TRAMO;
    }

    @Override
    protected BigDecimal depreciacionPorTramoKm() {
        return DEPRECIACION_POR_TRAMO;
    }

    @Override
    protected BigDecimal topeDepreciacionKm() {
        return TOPE_DEPRECIACION_KM;
    }

    /** La 4x4 suma un 7 %. */
    @Override
    protected BigDecimal recargos() {
        return Boolean.TRUE.equals(traccion4x4) ? RECARGO_4X4 : BigDecimal.ZERO;
    }

    @Override
    protected BigDecimal patentamiento(BigDecimal base) {
        return base.multiply(PATENTAMIENTO);
    }

    @Override
    protected boolean patenteConFormatoValido(String patente) {
        return Patentes.esDeAuto(patente);
    }

    @Override
    protected String mensajePatenteInvalida(String patente) {
        return Patentes.mensajeAuto(patente);
    }

    @Override
    public String getDetalleTipo() {
        return (carroceria == null ? "?" : carroceria.getEtiqueta()) + " · "
                + (Boolean.TRUE.equals(traccion4x4) ? "4x4" : "4x2") + " · "
                + (cargaUtilKg == null ? "?" : Formatos.miles(cargaUtilKg)) + " kg";
    }

    @Override
    public Vehiculo copia() {
        return new Camioneta(this);
    }

    @Override
    public void validarCompleto() throws DatoInvalidoException {
        super.validarCompleto();
        exigir("Carrocería", carroceria);
        exigir("Tracción 4x4", traccion4x4);
        exigir("Carga útil", cargaUtilKg);
    }

    /** @return la carrocería */
    public CarroceriaCamioneta getCarroceria() {
        return carroceria;
    }

    /**
     * @param carroceria la carrocería
     * @throws DatoInvalidoException si es {@code null}
     */
    public void setCarroceria(CarroceriaCamioneta carroceria) throws DatoInvalidoException {
        exigir("Carrocería", carroceria);
        this.carroceria = carroceria;
    }

    /** @return si tiene tracción 4x4 */
    public Boolean getTraccion4x4() {
        return traccion4x4;
    }

    /**
     * @param traccion4x4 si tiene tracción 4x4
     * @throws DatoInvalidoException si es {@code null}
     */
    public void setTraccion4x4(Boolean traccion4x4) throws DatoInvalidoException {
        exigir("Tracción 4x4", traccion4x4);
        this.traccion4x4 = traccion4x4;
    }

    /** @return la carga útil en kg */
    public Integer getCargaUtilKg() {
        return cargaUtilKg;
    }

    /**
     * @param cargaUtilKg de 300 a 3.500 kg
     * @throws DatoInvalidoException si está fuera de rango
     */
    public void setCargaUtilKg(Integer cargaUtilKg) throws DatoInvalidoException {
        if (cargaUtilKg == null || cargaUtilKg < CARGA_MINIMA || cargaUtilKg > CARGA_MAXIMA) {
            throw new DatoInvalidoException("Carga útil", String.valueOf(cargaUtilKg),
                    "La carga útil va de 300 a 3.500 kg.");
        }
        this.cargaUtilKg = cargaUtilKg;
    }
}
