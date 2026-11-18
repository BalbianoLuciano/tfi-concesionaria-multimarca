package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcas;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculos;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.MarcaDuplicadaException;
import ar.edu.utn.frre.concesionaria.excepciones.MarcaEnUsoException;
import ar.edu.utn.frre.concesionaria.excepciones.MarcaNoEncontradaException;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo de marcas (CU-12) y la búsqueda por prefijo del alta (CU-03).
 */
public class ServicioMarcas extends ServicioBase {

    private final RepositorioMarcas marcas;
    private final RepositorioVehiculos vehiculos;

    /**
     * @param marcas       repositorio de marcas
     * @param vehiculos    repositorio de vehículos (para saber si una marca está en uso)
     * @param persistencia guardado
     */
    public ServicioMarcas(RepositorioMarcas marcas, RepositorioVehiculos vehiculos,
            Persistencia persistencia) {
        super(persistencia);
        this.marcas = marcas;
        this.vehiculos = vehiculos;
    }

    /**
     * @param sesion la sesión
     * @return todas las marcas, en orden alfabético
     * @throws ConcesionariaException si no tiene permiso
     */
    public List<Marca> listar(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.VER_STOCK);
        return marcas.listar();
    }

    /**
     * Marcas para elegir en el alta: activas, que fabrican el tipo y empiezan con el prefijo.
     *
     * @param tipo    el tipo de vehículo
     * @param prefijo las primeras letras ("toy"), o vacío para todas
     * @return las marcas que coinciden
     */
    public List<Marca> paraAlta(TipoVehiculo tipo, String prefijo) {
        return marcas.buscarPorPrefijo(prefijo == null ? "" : prefijo).stream()
                .filter(Marca::estaActiva)
                .filter(m -> m.fabrica(tipo))
                .toList();
    }

    /**
     * Marcas parecidas a un nombre nuevo, para avisar antes de crear un duplicado como
     * "Tollota": distancia de edición de hasta 2 o mismo comienzo.
     *
     * @param nombre el nombre que se quiere crear
     * @return las marcas parecidas
     */
    public List<Marca> parecidas(String nombre) {
        String clave = Textos.clave(nombre);
        if (clave.isEmpty()) {
            return List.of();
        }
        String inicio = clave.substring(0, Math.min(3, clave.length()));
        return marcas.listar().stream()
                .filter(m -> Textos.distancia(clave, m.getClave()) <= 2
                        || m.getClave().startsWith(inicio))
                .toList();
    }

    /**
     * @param nombre un nombre
     * @return la marca con ese nombre normalizado, si existe
     */
    public Optional<Marca> buscarPorNombre(String nombre) {
        return marcas.buscarPorNombre(nombre);
    }

    /**
     * Alta de marca (CU-12).
     *
     * @param sesion sesión de un administrador
     * @param nombre nombre (se normaliza)
     * @param pais   país de origen
     * @param tipos  tipos que fabrica
     * @return la marca creada
     * @throws ConcesionariaException si no tiene permiso, ya existe, algún dato no es válido o
     *                                no se pudo guardar
     */
    public Marca crear(Sesion sesion, String nombre, String pais, Set<TipoVehiculo> tipos)
            throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_MARCAS);
        Marca nueva = new Marca(nombre, pais, tipos);
        Optional<Marca> existente = marcas.buscarPorNombre(nueva.getNombre());
        if (existente.isPresent()) {
            throw new MarcaDuplicadaException("La marca " + existente.get().getNombre()
                    + " ya existe" + (existente.get().estaActiva() ? "." : " (desactivada):"
                    + " reactivala en lugar de crearla."));
        }
        nueva.setId(marcas.siguienteId());
        marcas.guardar(nueva);
        guardarCambios();
        return nueva;
    }

    /**
     * Cambia el país y los tipos que fabrica. No se puede quitar un tipo que tenga vehículos
     * cargados con esa marca.
     *
     * @param sesion sesión de un administrador
     * @param id     id de la marca
     * @param pais   país
     * @param tipos  tipos
     * @throws ConcesionariaException si no tiene permiso, no existe, se quita un tipo en uso o
     *                                no se pudo guardar
     */
    public void modificar(Sesion sesion, int id, String pais, Set<TipoVehiculo> tipos)
            throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_MARCAS);
        Marca m = buscar(id);
        for (TipoVehiculo quitado : m.getTiposQueFabrica()) {
            if (!tipos.contains(quitado) && vehiculos.listar().stream()
                    .anyMatch(v -> m.equals(v.getMarca()) && v.getTipo() == quitado)) {
                throw new MarcaEnUsoException("No se puede quitar " + quitado.getEtiqueta()
                        + ": hay " + quitado.getPlural() + " de " + m.getNombre()
                        + " en el stock.");
            }
        }
        m.setPaisOrigen(pais);
        m.setTiposQueFabrica(tipos);
        guardarCambios();
    }

    /**
     * Borra una marca sin vehículos. Si tiene, hay que desactivarla.
     *
     * @param sesion sesión de un administrador
     * @param id     id de la marca
     * @throws ConcesionariaException si no tiene permiso, no existe, está en uso o no se pudo
     *                                guardar
     */
    public void eliminar(Sesion sesion, int id) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_MARCAS);
        Marca m = buscar(id);
        long enUso = cantidadVehiculos(m);
        if (enUso > 0) {
            throw new MarcaEnUsoException("La marca " + m.getNombre() + " tiene " + enUso
                    + " vehículo(s) y no se puede borrar. Desactivala: deja de ofrecerse en las"
                    + " altas.");
        }
        marcas.eliminar(id);
        guardarCambios();
    }

    /**
     * Activa o desactiva una marca. Una marca desactivada no se ofrece en las altas.
     *
     * @param sesion sesión de un administrador
     * @param id     id de la marca
     * @param activa el estado nuevo
     * @throws ConcesionariaException si no tiene permiso, no existe o no se pudo guardar
     */
    public void cambiarActiva(Sesion sesion, int id, boolean activa) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_MARCAS);
        buscar(id).setActiva(activa);
        guardarCambios();
    }

    /**
     * @param marca la marca
     * @return cuántos vehículos (de cualquier situación) tiene
     */
    public long cantidadVehiculos(Marca marca) {
        return vehiculos.listar().stream().filter(v -> marca.equals(v.getMarca())).count();
    }

    private Marca buscar(int id) throws MarcaNoEncontradaException {
        return marcas.buscar(id).orElseThrow(() -> new MarcaNoEncontradaException(
                "No hay ninguna marca con el número " + id + "."));
    }
}
