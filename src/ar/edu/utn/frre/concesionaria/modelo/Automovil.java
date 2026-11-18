package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.math.BigDecimal;

/**
 * Automóvil: carrocería y cantidad de puertas. Prefijo de código A.
 *
 * <p>Reglas de precio (SPEC §4; valores de ejemplo, no una tabla oficial):</p>
 * <ul>
 *   <li>usado: 6 % por año de antigüedad, tope 45 %; 1 % cada 10.000 km, tope 15 %;</li>
 *   <li>sin recargos;</li>
 *   <li>0 km: patentamiento del 3 %.</li>
 * </ul>
 */
public class Automovil extends Vehiculo {

    private static final long serialVersionUID = 1L;

    /** Depreciación por año de antigüedad: 6 %. */
    public static final BigDecimal DEPRECIACION_POR_ANIO = new BigDecimal("0.06");
    /** Tope de depreciación por antigüedad: 45 %. */
    public static final BigDecimal TOPE_DEPRECIACION_ANIO = new BigDecimal("0.45");
    /** Tramo de kilometraje: 10.000 km. */
    public static final int KM_POR_TRAMO = 10_000;
    /** Depreciación por tramo de km: 1 %. */
    public static final BigDecimal DEPRECIACION_POR_TRAMO = new BigDecimal("0.01");
    /** Tope de depreciación por km: 15 %. */
    public static final BigDecimal TOPE_DEPRECIACION_KM = new BigDecimal("0.15");
    /** Patentamiento de un 0 km: 3 %. */
    public static final BigDecimal PATENTAMIENTO = new BigDecimal("0.03");

    private CarroceriaAutomovil carroceria;
    private Integer puertas;

    /** Constructor vacío de JavaBean. */
    public Automovil() {
    }

    /**
     * @param estado      nuevo o usado
     * @param marca       marca del catálogo
     * @param modelo      modelo
     * @param anio        año
     * @param color       color
     * @param precioLista precio de lista
     * @param kilometraje kilómetros (si es usado)
     * @param patente     patente
     * @param carroceria  carrocería
     * @param puertas     cantidad de puertas (2 a 5)
     * @throws DatoInvalidoException si algún dato no cumple su regla
     */
    public Automovil(Estado estado, Marca marca, String modelo, Integer anio, Color color,
            BigDecimal precioLista, Integer kilometraje, String patente,
            CarroceriaAutomovil carroceria, Integer puertas) throws DatoInvalidoException {
        super(estado, marca, modelo, anio, color, precioLista, kilometraje, patente);
        setCarroceria(carroceria);
        setPuertas(puertas);
    }

    /**
     * Constructor de copia.
     *
     * @param otro el automóvil a copiar
     */
    protected Automovil(Automovil otro) {
        super(otro);
        this.carroceria = otro.carroceria;
        this.puertas = otro.puertas;
    }

    @Override
    public TipoVehiculo getTipo() {
        return TipoVehiculo.AUTOMOVIL;
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

    @Override
    protected BigDecimal recargos() {
        return BigDecimal.ZERO;
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
        return (carroceria == null ? "?" : carroceria.getEtiqueta()) + " · " + puertas
                + " puertas";
    }

    @Override
    public Vehiculo copia() {
        return new Automovil(this);
    }

    @Override
    public void validarCompleto() throws DatoInvalidoException {
        super.validarCompleto();
        exigir("Carrocería", carroceria);
        exigir("Puertas", puertas);
    }

    /** @return la carrocería */
    public CarroceriaAutomovil getCarroceria() {
        return carroceria;
    }

    /**
     * @param carroceria la carrocería
     * @throws DatoInvalidoException si es {@code null}
     */
    public void setCarroceria(CarroceriaAutomovil carroceria) throws DatoInvalidoException {
        exigir("Carrocería", carroceria);
        this.carroceria = carroceria;
    }

    /** @return la cantidad de puertas */
    public Integer getPuertas() {
        return puertas;
    }

    /**
     * @param puertas de 2 a 5
     * @throws DatoInvalidoException si está fuera de rango
     */
    public void setPuertas(Integer puertas) throws DatoInvalidoException {
        if (puertas == null || puertas < 2 || puertas > 5) {
            throw new DatoInvalidoException("Puertas", String.valueOf(puertas),
                    "Un automóvil tiene entre 2 y 5 puertas.");
        }
        this.puertas = puertas;
    }
}
