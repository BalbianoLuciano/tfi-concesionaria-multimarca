package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Marca del catálogo (Toyota, Honda). Es una entidad propia para que el nombre se elija de una
 * lista y nunca se tipee a mano en cada alta (CU-12).
 *
 * <p>Dos marcas son iguales si su nombre normalizado coincide: "Toyota", "TOYOTA" y
 * " toyota " son la misma marca. Así el duplicado se detecta solo.</p>
 */
public class Marca implements Identificable<Integer>, Serializable, Comparable<Marca> {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String paisOrigen;
    private Set<TipoVehiculo> tiposQueFabrica = EnumSet.noneOf(TipoVehiculo.class);
    private Boolean activa = Boolean.TRUE;
    /**
     * Catálogo de modelos (CU-12). Puede llegar en {@code null} al leer datos del esquema 1,
     * que no lo tenían: por eso se accede siempre con {@link #catalogo()}.
     */
    private List<Modelo> modelos = new ArrayList<>();
    /**
     * Clave normalizada del nombre, calculada una vez. Es {@code transient}: no se guarda, se
     * recalcula al leer. Ordenar el stock la consulta miles de veces (ARQUITECTURA §7).
     */
    private transient String clave;

    /** Constructor vacío de JavaBean (lo usan la serialización y el importador). */
    public Marca() {
    }

    /**
     * @param nombre     nombre de la marca (se normaliza)
     * @param paisOrigen país de origen
     * @param tipos      tipos de vehículo que fabrica (al menos uno)
     * @throws DatoInvalidoException si algún dato no cumple su regla
     */
    public Marca(String nombre, String paisOrigen, Set<TipoVehiculo> tipos)
            throws DatoInvalidoException {
        setNombre(nombre);
        setPaisOrigen(paisOrigen);
        setTiposQueFabrica(tipos);
    }

    /**
     * @param tipo un tipo de vehículo
     * @return {@code true} si la marca fabrica ese tipo
     */
    public boolean fabrica(TipoVehiculo tipo) {
        return tiposQueFabrica.contains(tipo);
    }

    /**
     * @return {@code true} si la marca está activa (se ofrece en las altas)
     */
    public boolean estaActiva() {
        return Boolean.TRUE.equals(activa);
    }

    /**
     * @return la clave de comparación del nombre (sin acentos, en minúsculas)
     */
    public String getClave() {
        if (clave == null) {
            clave = Textos.clave(nombre);
        }
        return clave;
    }

    @Override
    public Integer getId() {
        return id;
    }

    /** @param id identificador asignado por el repositorio */
    public void setId(Integer id) {
        this.id = id;
    }

    /** @return el nombre normalizado */
    public String getNombre() {
        return nombre;
    }

    /**
     * Normaliza y valida el nombre: "  toyota " queda "Toyota".
     *
     * @param nombre el nombre
     * @throws DatoInvalidoException si está vacío, es largo o tiene caracteres raros
     */
    public void setNombre(String nombre) throws DatoInvalidoException {
        String limpio = Textos.nombrePropio(nombre);
        if (!limpio.matches("[\\p{L}\\d][\\p{L}\\d .&'-]{0,29}")) {
            throw new DatoInvalidoException("Nombre", nombre,
                    "El nombre de la marca va de 1 a 30 caracteres: letras, números, espacios,"
                            + " guiones y puntos.");
        }
        this.nombre = limpio;
        this.clave = null;
    }

    /** @return el país de origen */
    public String getPaisOrigen() {
        return paisOrigen;
    }

    /**
     * @param paisOrigen país de origen (2 a 40 letras)
     * @throws DatoInvalidoException si no cumple la regla
     */
    public void setPaisOrigen(String paisOrigen) throws DatoInvalidoException {
        String limpio = Textos.nombrePropio(paisOrigen);
        if (!limpio.matches("[\\p{L} ]{2,40}")) {
            throw new DatoInvalidoException("País", paisOrigen,
                    "El país va de 2 a 40 letras, por ejemplo Japón.");
        }
        this.paisOrigen = limpio;
    }

    /** @return los tipos que fabrica (no modificable) */
    public Set<TipoVehiculo> getTiposQueFabrica() {
        return Collections.unmodifiableSet(tiposQueFabrica);
    }

    /**
     * @param tipos tipos de vehículo que fabrica (al menos uno)
     * @throws DatoInvalidoException si está vacío
     */
    public void setTiposQueFabrica(Set<TipoVehiculo> tipos) throws DatoInvalidoException {
        if (tipos == null || tipos.isEmpty()) {
            throw new DatoInvalidoException("Tipos", "",
                    "Elegí al menos un tipo de vehículo que fabrique la marca.");
        }
        this.tiposQueFabrica = EnumSet.copyOf(tipos);
    }

    /** @return si está activa (wrapper: {@code null} en datos viejos se toma como activa) */
    public Boolean getActiva() {
        return activa;
    }

    /** @param activa si se ofrece en las altas */
    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    /** @return la lista de modelos, creada si venía de un archivo del esquema 1 */
    private List<Modelo> catalogo() {
        if (modelos == null) {
            modelos = new ArrayList<>();
        }
        return modelos;
    }

    /**
     * @return todos los modelos del catálogo, en orden alfabético (sin modificar)
     */
    public List<Modelo> getModelos() {
        return catalogo().stream().sorted().toList();
    }

    /**
     * @param tipo un tipo de vehículo
     * @return los modelos de ese tipo, en orden alfabético
     */
    public List<Modelo> modelosDe(TipoVehiculo tipo) {
        return catalogo().stream().filter(m -> m.getTipo() == tipo).sorted().toList();
    }

    /**
     * Busca un modelo por su nombre, sin importar mayúsculas, acentos ni espacios.
     *
     * @param nombre el nombre
     * @return el modelo, si está en el catálogo
     */
    public Optional<Modelo> buscarModelo(String nombre) {
        String clave = Textos.compacta(nombre == null ? "" : nombre);
        return catalogo().stream().filter(m -> m.getClave().equals(clave)).findFirst();
    }

    /**
     * Agrega un modelo al catálogo. Las reglas (que no se repita, que la marca fabrique el
     * tipo) las controla {@code ServicioMarcas}, que es quien tiene el contexto.
     *
     * @param modelo el modelo
     */
    public void agregarModelo(Modelo modelo) {
        catalogo().add(Objects.requireNonNull(modelo));
    }

    /**
     * @param modelo el modelo a quitar
     * @return {@code true} si estaba
     */
    public boolean quitarModelo(Modelo modelo) {
        return catalogo().remove(modelo);
    }

    /**
     * @return los tipos como texto: "Automóvil, Camioneta"
     */
    public String getTiposComoTexto() {
        return tiposQueFabrica.stream().map(TipoVehiculo::getEtiqueta)
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Marca)) {
            return false;
        }
        Marca otra = (Marca) o;
        return Objects.equals(getClave(), otra.getClave());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClave());
    }

    /** Orden alfabético por nombre, sin importar acentos. */
    @Override
    public int compareTo(Marca otra) {
        return getClave().compareTo(otra.getClave());
    }

    @Override
    public String toString() {
        return "Marca[" + id + ", " + nombre + ", " + paisOrigen + ", " + tiposQueFabrica
                + (estaActiva() ? "" : ", inactiva") + "]";
    }
}
