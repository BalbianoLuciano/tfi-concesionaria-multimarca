package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.VehiculoNoDisponibleException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

/**
 * Vehículo del stock. Clase abstracta: no existe "un vehículo" a secas, existe un automóvil,
 * una camioneta o una motocicleta.
 *
 * <p><b>JavaBean:</b> constructor vacío, atributos privados con getters y setters, y
 * {@link Serializable}. Los setters validan y lanzan {@link DatoInvalidoException}, así un
 * objeto nunca queda en un estado inválido, venga el dato de la consola, del CSV o de una
 * prueba.</p>
 *
 * <p><b>Wrappers:</b> año, kilometraje y los datos numéricos son {@link Integer} y no
 * {@code int} porque {@code null} significa "todavía no cargado"; un {@code int} valdría 0, que
 * es un kilometraje válido.</p>
 *
 * <p><b>Template method:</b> {@link #desglosarPrecio(LocalDate)} es {@code final} y arma la
 * fórmula común de SPEC §4. Los porcentajes los resuelve cada subclase con los métodos
 * abstractos.</p>
 *
 * <p>Contenido extra (SPEC §7): los montos son {@link BigDecimal}; las fechas,
 * {@link LocalDate}.</p>
 */
public abstract class Vehiculo
        implements Identificable<String>, Vendible, Comparable<Vehiculo>, Serializable {

    private static final long serialVersionUID = 1L;

    /** Primer año de modelo que acepta el sistema. */
    public static final int ANIO_MINIMO = 1990;
    /** Kilometraje máximo de un usado. */
    public static final int KM_MAXIMO = 999_999;
    /** Precio de lista máximo. */
    public static final BigDecimal PRECIO_MAXIMO = new BigDecimal("2000000000");

    /**
     * Orden natural: tipo, marca, modelo y código. Se arma con {@link Comparator} para no
     * repetir los {@code if} de cada criterio.
     */
    private static final Comparator<Vehiculo> ORDEN_NATURAL = Comparator
            .comparing(Vehiculo::getTipo)
            .thenComparing(v -> v.getMarca() == null ? "" : v.getMarca().getClave())
            .thenComparing(v -> Textos.clave(v.getModelo()))
            .thenComparing(v -> v.getCodigo() == null ? "" : v.getCodigo());

    private String codigo;
    private String patente;
    private Marca marca;
    private String modelo;
    private Integer anio;
    private Color color;
    private Estado estado;
    private Integer kilometraje;
    private BigDecimal precioLista;
    private Situacion situacion = Situacion.DISPONIBLE;
    private LocalDate fechaIngreso;
    private LocalDate fechaBaja;
    private String motivoBaja;

    /** Constructor vacío de JavaBean. */
    protected Vehiculo() {
    }

    /**
     * Crea un vehículo con los datos comunes. El estado se fija primero porque las reglas de
     * año, kilometraje y patente dependen de él.
     *
     * @param estado      nuevo o usado
     * @param marca       marca del catálogo
     * @param modelo      modelo ("Hilux")
     * @param anio        año del modelo
     * @param color       color
     * @param precioLista precio de lista en pesos
     * @param kilometraje kilómetros (se ignora si es 0 km)
     * @param patente     patente (opcional si es 0 km)
     * @throws DatoInvalidoException si algún dato no cumple su regla
     */
    protected Vehiculo(Estado estado, Marca marca, String modelo, Integer anio, Color color,
            BigDecimal precioLista, Integer kilometraje, String patente)
            throws DatoInvalidoException {
        setEstado(estado);
        setMarca(marca);
        setModelo(modelo);
        setAnio(anio);
        setColor(color);
        setPrecioLista(precioLista);
        if (estado == Estado.USADO) {
            setKilometraje(kilometraje);
        }
        setPatente(patente);
    }

    /**
     * Constructor de copia: lo usan las subclases para {@link #copia()}. La marca se comparte
     * (es la misma entidad del catálogo), el resto se copia.
     *
     * @param otro el vehículo a copiar
     */
    protected Vehiculo(Vehiculo otro) {
        this.codigo = otro.codigo;
        this.patente = otro.patente;
        this.marca = otro.marca;
        this.modelo = otro.modelo;
        this.anio = otro.anio;
        this.color = otro.color;
        this.estado = otro.estado;
        this.kilometraje = otro.kilometraje;
        this.precioLista = otro.precioLista;
        this.situacion = otro.situacion;
        this.fechaIngreso = otro.fechaIngreso;
        this.fechaBaja = otro.fechaBaja;
        this.motivoBaja = otro.motivoBaja;
    }

    // ---------------------------------------------------------------- lo que define cada tipo

    /** @return el tipo de vehículo */
    public abstract TipoVehiculo getTipo();

    /** @return fracción de depreciación por cada año de antigüedad (0,06 = 6 %) */
    protected abstract BigDecimal depreciacionPorAnio();

    /** @return tope de la depreciación por antigüedad */
    protected abstract BigDecimal topeDepreciacionAnio();

    /** @return cada cuántos kilómetros se aplica un tramo de depreciación */
    protected abstract int kmPorTramo();

    /** @return fracción de depreciación por cada tramo completo de kilómetros */
    protected abstract BigDecimal depreciacionPorTramoKm();

    /** @return tope de la depreciación por kilometraje */
    protected abstract BigDecimal topeDepreciacionKm();

    /** @return fracción de recargos propios del tipo (0 si no tiene) */
    protected abstract BigDecimal recargos();

    /**
     * @param base el precio sobre el que se calcula
     * @return el monto de patentamiento de un 0 km
     */
    protected abstract BigDecimal patentamiento(BigDecimal base);

    /**
     * @param patente patente ya normalizada (mayúsculas, sin espacios ni guiones)
     * @return {@code true} si tiene un formato válido para este tipo
     */
    protected abstract boolean patenteConFormatoValido(String patente);

    /**
     * @param patente lo que escribió el usuario
     * @return el mensaje de error con los formatos válidos para este tipo
     */
    protected abstract String mensajePatenteInvalida(String patente);

    /**
     * Detalle polimórfico de una línea: "Doble cabina · 4x4 · 1.000 kg", "Sedán · 4 puertas".
     * La tabla del stock lo usa sin saber qué tipo de vehículo está mostrando.
     *
     * @return el detalle
     */
    public abstract String getDetalleTipo();

    /**
     * @return una copia independiente, para modificar sin tocar el original hasta confirmar
     */
    public abstract Vehiculo copia();

    // ---------------------------------------------------------------- precio (template method)

    /**
     * Arma el desglose del precio con la fórmula de SPEC §4. Es {@code final}: la fórmula es
     * una sola y las subclases solo aportan los porcentajes.
     *
     * <pre>
     * final = lista × (1 − dep. antigüedad − dep. km)   ← solo usados
     *               × (1 + recargos)
     *               + patentamiento                    ← solo 0 km
     * </pre>
     *
     * @param hoy fecha de referencia para la antigüedad
     * @return el desglose, con montos redondeados a pesos enteros (HALF_UP)
     */
    @Override
    public final DesglosePrecio desglosarPrecio(LocalDate hoy) {
        BigDecimal lista = precioLista == null ? BigDecimal.ZERO : precioLista;
        BigDecimal porcAntiguedad = BigDecimal.ZERO;
        BigDecimal porcKm = BigDecimal.ZERO;

        if (estado == Estado.USADO) {
            int antiguedad = Math.max(0, hoy.getYear() - (anio == null ? hoy.getYear() : anio));
            porcAntiguedad = depreciacionPorAnio().multiply(BigDecimal.valueOf(antiguedad))
                    .min(topeDepreciacionAnio());
            int tramos = (kilometraje == null ? 0 : kilometraje) / kmPorTramo();
            porcKm = depreciacionPorTramoKm().multiply(BigDecimal.valueOf(tramos))
                    .min(topeDepreciacionKm());
        }

        BigDecimal depAntiguedad = pesos(lista.multiply(porcAntiguedad));
        BigDecimal depKm = pesos(lista.multiply(porcKm));
        BigDecimal base = lista.subtract(depAntiguedad).subtract(depKm);

        BigDecimal porcRecargos = recargos();
        BigDecimal montoRecargos = pesos(base.multiply(porcRecargos));
        BigDecimal subtotal = base.add(montoRecargos);

        BigDecimal montoPatentamiento = estado == Estado.NUEVO
                ? pesos(patentamiento(subtotal))
                : BigDecimal.ZERO;

        BigDecimal precioFinal = pesos(subtotal.add(montoPatentamiento));
        return new DesglosePrecio(pesos(lista), porcAntiguedad, depAntiguedad, porcKm, depKm,
                porcRecargos, montoRecargos, montoPatentamiento, precioFinal);
    }

    @Override
    public final BigDecimal calcularPrecioFinal(LocalDate hoy) {
        return desglosarPrecio(hoy).getPrecioFinal();
    }

    /**
     * Precio final a la fecha de hoy. Atajo para listados y reportes.
     *
     * @return el precio final
     */
    public BigDecimal getPrecioFinal() {
        return calcularPrecioFinal(Reloj.hoy());
    }

    /**
     * Redondea a pesos enteros, como pide SPEC §4.
     *
     * @param monto el monto
     * @return el monto sin decimales
     */
    protected static BigDecimal pesos(BigDecimal monto) {
        return monto.setScale(0, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- situación (MODELO §3)

    @Override
    public boolean estaDisponible() {
        return situacion == Situacion.DISPONIBLE;
    }

    /**
     * Verifica que el vehículo se pueda modificar, vender o dar de baja.
     *
     * @param accion lo que se quiere hacer ("vender"), para el mensaje
     * @throws VehiculoNoDisponibleException si no está disponible
     */
    public void exigirDisponible(String accion) throws VehiculoNoDisponibleException {
        if (!estaDisponible()) {
            throw new VehiculoNoDisponibleException("No se puede " + accion + " " + codigo
                    + ": está " + situacion.getEtiqueta().toLowerCase()
                    + (situacion == Situacion.BAJA ? " desde el " + fechaBaja : "") + ".");
        }
    }

    @Override
    public void marcarVendido() throws VehiculoNoDisponibleException {
        exigirDisponible("vender");
        situacion = Situacion.VENDIDO;
    }

    @Override
    public void revertirVenta() {
        if (situacion == Situacion.VENDIDO) {
            situacion = Situacion.DISPONIBLE;
        }
    }

    /**
     * Baja lógica (CU-08): no se borra, para no perder el historial.
     *
     * @param motivo motivo corto (1 a 80 caracteres)
     * @param fecha  fecha de la baja
     * @throws VehiculoNoDisponibleException si no está disponible
     * @throws DatoInvalidoException         si el motivo está vacío o es largo
     */
    public void darDeBaja(String motivo, LocalDate fecha)
            throws VehiculoNoDisponibleException, DatoInvalidoException {
        exigirDisponible("dar de baja");
        String limpio = Textos.limpiar(motivo);
        if (limpio.isEmpty() || limpio.length() > 80) {
            throw new DatoInvalidoException("Motivo", motivo,
                    "Escribí un motivo corto, de 1 a 80 caracteres.");
        }
        this.motivoBaja = limpio;
        this.fechaBaja = fecha;
        this.situacion = Situacion.BAJA;
    }

    /**
     * Verifica que estén cargados todos los datos obligatorios. Lo llama el servicio antes de
     * registrar, y el importador CSV para cada fila. Las subclases agregan los suyos.
     *
     * @throws DatoInvalidoException con el primer dato que falta
     */
    public void validarCompleto() throws DatoInvalidoException {
        exigir("Estado", estado);
        exigir("Marca", marca);
        exigir("Modelo", modelo);
        exigir("Año", anio);
        exigir("Color", color);
        exigir("Precio de lista", precioLista);
        if (estado == Estado.USADO) {
            exigir("Kilometraje", kilometraje);
            if (patente == null) {
                throw new DatoInvalidoException("Patente", null,
                        "Un usado tiene que tener patente.");
            }
        }
        if (!marca.fabrica(getTipo())) {
            throw new DatoInvalidoException("Marca", marca.getNombre(), marca.getNombre()
                    + " no fabrica " + getTipo().getPlural() + ".");
        }
    }

    /**
     * @param campo nombre del campo
     * @param valor valor a verificar
     * @throws DatoInvalidoException si el valor es {@code null}
     */
    protected static void exigir(String campo, Object valor) throws DatoInvalidoException {
        if (valor == null) {
            throw new DatoInvalidoException(campo, null, "Falta cargar el campo " + campo + ".");
        }
    }

    // ---------------------------------------------------------------- getters y setters

    @Override
    public String getId() {
        return codigo;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    /**
     * Lo asigna el sistema al registrar (C-0004). No cambia nunca.
     *
     * @param codigo el código
     */
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    /** @return la patente normalizada, o {@code null} si no tiene */
    public String getPatente() {
        return patente;
    }

    /**
     * Normaliza (mayúsculas, sin espacios ni guiones) y valida el formato según el tipo.
     * Vacía o {@code null} deja el vehículo sin patente; si es usado, {@link #validarCompleto()}
     * lo rechaza. La unicidad la controla el repositorio.
     *
     * @param patente la patente ("ab 123 cd", "AB-123-CD")
     * @throws DatoInvalidoException si el formato no corresponde al tipo
     */
    public void setPatente(String patente) throws DatoInvalidoException {
        String normalizada = normalizarPatente(patente);
        if (normalizada.isEmpty()) {
            if (estado == Estado.USADO) {
                throw new DatoInvalidoException("Patente", patente,
                        "Un usado tiene que tener patente.");
            }
            this.patente = null;
            return;
        }
        if (!patenteConFormatoValido(normalizada)) {
            throw new DatoInvalidoException("Patente", patente, mensajePatenteInvalida(patente));
        }
        this.patente = normalizada;
    }

    /**
     * @param patente texto escrito por el usuario
     * @return la patente en mayúsculas, sin espacios ni guiones
     */
    public static String normalizarPatente(String patente) {
        return Textos.limpiar(patente).replace(" ", "").replace("-", "").toUpperCase();
    }

    /** @return la marca */
    public Marca getMarca() {
        return marca;
    }

    /**
     * @param marca marca del catálogo; tiene que estar activa al registrar (lo controla el
     *              servicio) y fabricar este tipo
     * @throws DatoInvalidoException si es {@code null} o no fabrica este tipo
     */
    public void setMarca(Marca marca) throws DatoInvalidoException {
        if (marca == null) {
            throw new DatoInvalidoException("Marca", null, "Elegí una marca del catálogo.");
        }
        if (!marca.fabrica(getTipo())) {
            throw new DatoInvalidoException("Marca", marca.getNombre(), marca.getNombre()
                    + " no fabrica " + getTipo().getPlural() + ".");
        }
        this.marca = marca;
    }

    /** @return el modelo */
    public String getModelo() {
        return modelo;
    }

    /**
     * @param modelo 1 a 40 caracteres: letras, números, espacio, guion y punto
     * @throws DatoInvalidoException si no cumple
     */
    public void setModelo(String modelo) throws DatoInvalidoException {
        String limpio = Textos.limpiar(modelo);
        if (!limpio.matches("[\\p{L}\\d .\\-]{1,40}")) {
            throw new DatoInvalidoException("Modelo", modelo, limpio.length() > 40
                    ? "El modelo puede tener hasta 40 caracteres."
                    : "El modelo admite letras, números, espacios, guiones y puntos.");
        }
        this.modelo = limpio;
    }

    /** @return el año del modelo */
    public Integer getAnio() {
        return anio;
    }

    /**
     * Entre 1990 y el año próximo. Un 0 km no puede ser anterior al año pasado.
     *
     * @param anio el año
     * @throws DatoInvalidoException si está fuera de rango
     */
    public void setAnio(Integer anio) throws DatoInvalidoException {
        int actual = Reloj.anioActual();
        String texto = String.valueOf(anio);
        if (anio == null || anio < ANIO_MINIMO || anio > actual + 1) {
            throw new DatoInvalidoException("Año", texto, "El año tiene que estar entre "
                    + ANIO_MINIMO + " y " + (actual + 1) + ".");
        }
        if (estado == Estado.NUEVO && anio < actual - 1) {
            throw new DatoInvalidoException("Año", texto,
                    "Un 0 km no puede ser anterior a " + (actual - 1) + ".");
        }
        this.anio = anio;
    }

    /** @return el color */
    public Color getColor() {
        return color;
    }

    /**
     * @param color el color
     * @throws DatoInvalidoException si es {@code null}
     */
    public void setColor(Color color) throws DatoInvalidoException {
        exigir("Color", color);
        this.color = color;
    }

    /** @return nuevo o usado */
    public Estado getEstado() {
        return estado;
    }

    /**
     * Fija el estado. Un 0 km queda con 0 km recorridos; un usado arranca sin kilometraje
     * cargado hasta que se le asigne.
     *
     * @param estado nuevo o usado
     * @throws DatoInvalidoException si es {@code null}, o si el año cargado no es válido para
     *                               un 0 km
     */
    public void setEstado(Estado estado) throws DatoInvalidoException {
        exigir("Estado", estado);
        int actual = Reloj.anioActual();
        if (estado == Estado.NUEVO && anio != null && anio < actual - 1) {
            throw new DatoInvalidoException("Estado", estado.getEtiqueta(),
                    "Un 0 km no puede ser anterior a " + (actual - 1) + ".");
        }
        this.estado = estado;
        if (estado == Estado.NUEVO) {
            this.kilometraje = 0;
        } else if (Integer.valueOf(0).equals(kilometraje)) {
            this.kilometraje = null;
        }
    }

    /** @return los kilómetros recorridos */
    public Integer getKilometraje() {
        return kilometraje;
    }

    /**
     * Regla "0 km ⇔ kilometraje 0" (MODELO §3): un nuevo solo acepta 0; un usado, de 1 a
     * 999.999.
     *
     * @param kilometraje los kilómetros
     * @throws DatoInvalidoException si no corresponde al estado
     */
    public void setKilometraje(Integer kilometraje) throws DatoInvalidoException {
        exigir("Estado", estado);
        String texto = String.valueOf(kilometraje);
        if (estado == Estado.NUEVO) {
            if (kilometraje == null || kilometraje != 0) {
                throw new DatoInvalidoException("Kilometraje", texto,
                        "Un 0 km tiene que tener 0 kilómetros.");
            }
        } else if (kilometraje == null || kilometraje < 1 || kilometraje > KM_MAXIMO) {
            throw new DatoInvalidoException("Kilometraje", texto,
                    "El kilometraje de un usado tiene que estar entre 1 y 999.999.");
        }
        this.kilometraje = kilometraje;
    }

    /** @return el precio de lista */
    public BigDecimal getPrecioLista() {
        return precioLista;
    }

    /**
     * @param precioLista mayor a 0 y hasta 2.000.000.000
     * @throws DatoInvalidoException si está fuera de rango
     */
    public void setPrecioLista(BigDecimal precioLista) throws DatoInvalidoException {
        String texto = String.valueOf(precioLista);
        if (precioLista == null || precioLista.signum() <= 0) {
            throw new DatoInvalidoException("Precio de lista", texto,
                    "El precio tiene que ser mayor a $ 0.");
        }
        if (precioLista.compareTo(PRECIO_MAXIMO) > 0) {
            throw new DatoInvalidoException("Precio de lista", texto,
                    "El precio no puede superar $ 2.000.000.000.");
        }
        this.precioLista = pesos(precioLista);
    }

    /** @return disponible, vendido o baja */
    public Situacion getSituacion() {
        return situacion;
    }

    /**
     * Setter de JavaBean. El negocio cambia la situación con {@link #marcarVendido()} y
     * {@link #darDeBaja(String, LocalDate)}, que controlan las transiciones.
     *
     * @param situacion la situación
     */
    public void setSituacion(Situacion situacion) {
        this.situacion = situacion;
    }

    /** @return fecha en que se registró */
    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    /** @param fechaIngreso fecha en que se registró */
    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    /** @return fecha de la baja, o {@code null} */
    public LocalDate getFechaBaja() {
        return fechaBaja;
    }

    /** @return motivo de la baja, o {@code null} */
    public String getMotivoBaja() {
        return motivoBaja;
    }

    @Override
    public String getDescripcion() {
        return (marca == null ? "?" : marca.getNombre()) + " " + modelo + " " + anio;
    }

    // ---------------------------------------------------------------- métodos esenciales

    /** Igualdad por código: es el id que asigna el sistema y no cambia nunca. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Vehiculo otro = (Vehiculo) o;
        return codigo != null && codigo.equals(otro.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }

    /** Orden natural: tipo, marca, modelo y código. */
    @Override
    public int compareTo(Vehiculo otro) {
        return ORDEN_NATURAL.compare(this, otro);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + codigo + ", " + getDescripcion() + ", "
                + (estado == null ? "?" : estado.getEtiqueta()) + ", " + kilometraje + " km, "
                + patente + ", " + situacion + "]";
    }
}
