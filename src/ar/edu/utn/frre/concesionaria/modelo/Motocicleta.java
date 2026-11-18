package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.math.BigDecimal;

/**
 * Motocicleta: tipo y cilindrada. Prefijo de código M. Tiene su propio formato de patente.
 *
 * <p>Reglas de precio (SPEC §4; valores de ejemplo, no una tabla oficial):</p>
 * <ul>
 *   <li>usada: 8 % por año de antigüedad, tope 50 %; 1 % cada 5.000 km, tope 20 %;</li>
 *   <li>sin recargos;</li>
 *   <li>0 km: patentamiento del 2 %, con un mínimo de $ 150.000.</li>
 * </ul>
 */
public class Motocicleta extends Vehiculo {

    private static final long serialVersionUID = 1L;

    /** Depreciación por año de antigüedad: 8 %. */
    public static final BigDecimal DEPRECIACION_POR_ANIO = new BigDecimal("0.08");
    /** Tope de depreciación por antigüedad: 50 %. */
    public static final BigDecimal TOPE_DEPRECIACION_ANIO = new BigDecimal("0.50");
    /** Tramo de kilometraje: 5.000 km. */
    public static final int KM_POR_TRAMO = 5_000;
    /** Depreciación por tramo de km: 1 %. */
    public static final BigDecimal DEPRECIACION_POR_TRAMO = new BigDecimal("0.01");
    /** Tope de depreciación por km: 20 %. */
    public static final BigDecimal TOPE_DEPRECIACION_KM = new BigDecimal("0.20");
    /** Patentamiento de un 0 km: 2 %. */
    public static final BigDecimal PATENTAMIENTO = new BigDecimal("0.02");
    /** Patentamiento mínimo: $ 150.000. */
    public static final BigDecimal PATENTAMIENTO_MINIMO = new BigDecimal("150000");
    /** Cilindrada mínima en cc. */
    public static final int CILINDRADA_MINIMA = 50;
    /** Cilindrada máxima en cc. */
    public static final int CILINDRADA_MAXIMA = 2_500;

    private TipoMotocicleta tipoMoto;
    private Integer cilindradaCc;

    /** Constructor vacío de JavaBean. */
    public Motocicleta() {
    }

    /**
     * @param estado       nuevo o usado
     * @param marca        marca del catálogo
     * @param modelo       modelo
     * @param anio         año
     * @param color        color
     * @param precioLista  precio de lista
     * @param kilometraje  kilómetros (si es usada)
     * @param patente      patente
     * @param tipoMoto     tipo de moto
     * @param cilindradaCc cilindrada en cc (50 a 2.500)
     * @throws DatoInvalidoException si algún dato no cumple su regla
     */
    public Motocicleta(Estado estado, Marca marca, String modelo, Integer anio, Color color,
            BigDecimal precioLista, Integer kilometraje, String patente,
            TipoMotocicleta tipoMoto, Integer cilindradaCc) throws DatoInvalidoException {
        super(estado, marca, modelo, anio, color, precioLista, kilometraje, patente);
        setTipoMoto(tipoMoto);
        setCilindradaCc(cilindradaCc);
    }

    /**
     * Constructor de copia.
     *
     * @param otra la moto a copiar
     */
    protected Motocicleta(Motocicleta otra) {
        super(otra);
        this.tipoMoto = otra.tipoMoto;
        this.cilindradaCc = otra.cilindradaCc;
    }

    @Override
    public TipoVehiculo getTipo() {
        return TipoVehiculo.MOTOCICLETA;
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

    /** El 2 % del precio, pero nunca menos de $ 150.000. */
    @Override
    protected BigDecimal patentamiento(BigDecimal base) {
        return base.multiply(PATENTAMIENTO).max(PATENTAMIENTO_MINIMO);
    }

    @Override
    protected boolean patenteConFormatoValido(String patente) {
        return Patentes.esDeMoto(patente);
    }

    @Override
    protected String mensajePatenteInvalida(String patente) {
        return Patentes.mensajeMoto(patente);
    }

    @Override
    public String getDetalleTipo() {
        return (tipoMoto == null ? "?" : tipoMoto.getEtiqueta()) + " · "
                + (cilindradaCc == null ? "?" : Formatos.miles(cilindradaCc)) + " cc";
    }

    @Override
    public Vehiculo copia() {
        return new Motocicleta(this);
    }

    @Override
    public void validarCompleto() throws DatoInvalidoException {
        super.validarCompleto();
        exigir("Tipo de moto", tipoMoto);
        exigir("Cilindrada", cilindradaCc);
    }

    /** @return el tipo de moto */
    public TipoMotocicleta getTipoMoto() {
        return tipoMoto;
    }

    /**
     * @param tipoMoto el tipo de moto
     * @throws DatoInvalidoException si es {@code null}
     */
    public void setTipoMoto(TipoMotocicleta tipoMoto) throws DatoInvalidoException {
        exigir("Tipo de moto", tipoMoto);
        this.tipoMoto = tipoMoto;
    }

    /** @return la cilindrada en cc */
    public Integer getCilindradaCc() {
        return cilindradaCc;
    }

    /**
     * @param cilindradaCc de 50 a 2.500 cc
     * @throws DatoInvalidoException si está fuera de rango
     */
    public void setCilindradaCc(Integer cilindradaCc) throws DatoInvalidoException {
        if (cilindradaCc == null || cilindradaCc < CILINDRADA_MINIMA
                || cilindradaCc > CILINDRADA_MAXIMA) {
            throw new DatoInvalidoException("Cilindrada", String.valueOf(cilindradaCc),
                    "La cilindrada va de 50 a 2.500 cc.");
        }
        this.cilindradaCc = cilindradaCc;
    }
}
