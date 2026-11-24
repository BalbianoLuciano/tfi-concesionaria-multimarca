package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcas;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculos;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.MarcaDuplicadaException;
import ar.edu.utn.frre.concesionaria.excepciones.MarcaEnUsoException;
import ar.edu.utn.frre.concesionaria.excepciones.MarcaNoEncontradaException;
import ar.edu.utn.frre.concesionaria.excepciones.ReglaNegocioException;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Modelo;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import java.util.List;
import java.util.Map;
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
     * Cómo se nombra a algunas marcas en la calle (SPEC §3, tolerancia): "vw" es Volkswagen y
     * "chevy", Chevrolet. Solo se usan para buscar; el nombre guardado es siempre el oficial.
     */
    private static final Map<String, String> APODOS = Map.of(
            "vw", "Volkswagen",
            "chevy", "Chevrolet",
            "mb", "Mercedes-Benz",
            "benz", "Mercedes-Benz",
            "hd", "Harley-Davidson",
            "harley", "Harley-Davidson",
            "lr", "Land Rover",
            "alfa", "Alfa Romeo",
            "citroen", "Citroën");

    /**
     * Marcas para elegir en el alta: activas, que fabrican el tipo y empiezan con el prefijo
     * (o con el nombre oficial, si se escribió un apodo como "vw").
     *
     * @param tipo    el tipo de vehículo
     * @param prefijo las primeras letras ("toy"), o vacío para todas
     * @return las marcas que coinciden
     */
    public List<Marca> paraAlta(TipoVehiculo tipo, String prefijo) {
        String buscado = prefijo == null ? "" : prefijo;
        String apodo = APODOS.get(Textos.compacta(buscado));
        return marcas.buscarPorPrefijo(apodo != null ? apodo : buscado).stream()
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

    // ---------------------------------------------------------------- catálogo de modelos

    /**
     * Modelos del catálogo que coinciden con lo escrito, por nombre o por su comienzo, sin
     * importar mayúsculas ni acentos (SPEC §3). Texto vacío: todos los del tipo.
     *
     * @param marca la marca
     * @param tipo  el tipo de vehículo
     * @param texto lo que escribió el usuario, o {@code null}
     * @return los que coinciden, en orden alfabético
     */
    public List<Modelo> modelosQueCoinciden(Marca marca, TipoVehiculo tipo, String texto) {
        List<Modelo> delTipo = marca.modelosDe(tipo);
        if (texto == null || texto.isBlank()) {
            return delTipo;
        }
        List<String> nombres = delTipo.stream().map(Modelo::getNombre).toList();
        return Textos.coincidencias(texto, nombres).stream().map(delTipo::get).toList();
    }

    /**
     * Modelos parecidos a lo escrito cuando no coincide ninguno: errores de tipeo y de
     * ortografía ("korola" → Corolla), SPEC §3.
     *
     * @param marca la marca
     * @param tipo  el tipo de vehículo, o {@code null} para todos
     * @param texto lo que escribió el usuario
     * @return los parecidos, del más al menos parecido
     */
    public List<Modelo> modelosParecidos(Marca marca, TipoVehiculo tipo, String texto) {
        List<Modelo> candidatos = tipo == null ? marca.getModelos() : marca.modelosDe(tipo);
        List<String> nombres = candidatos.stream().map(Modelo::getNombre).toList();
        return Textos.parecidas(texto, nombres).stream().map(candidatos::get).toList();
    }

    /**
     * Controla que un modelo pertenezca al catálogo de la marca para su tipo (CU-03). Lo usan
     * el alta, la modificación y la importación.
     *
     * @param marca  la marca
     * @param tipo   el tipo del vehículo
     * @param modelo el nombre del modelo
     * @throws ReglaNegocioException si no está, con la sugerencia del más parecido
     */
    public void exigirModeloDelCatalogo(Marca marca, TipoVehiculo tipo, String modelo)
            throws ReglaNegocioException {
        Optional<Modelo> encontrado = marca.buscarModelo(modelo);
        if (encontrado.isPresent() && encontrado.get().getTipo() == tipo) {
            return;
        }
        String mensaje = "«" + modelo + "» no está en el catálogo de " + marca.getNombre()
                + " para " + tipo.getPlural() + ".";
        List<Modelo> parecidos = modelosParecidos(marca, tipo, modelo);
        if (!parecidos.isEmpty()) {
            mensaje += " ¿Quisiste decir " + parecidos.get(0).getNombre() + "?";
        } else {
            mensaje += " Un administrador lo puede agregar en Marcas › Modelos.";
        }
        throw new ReglaNegocioException(mensaje);
    }

    /**
     * Agrega un modelo al catálogo de una marca (CU-12).
     *
     * @param sesion sesión de un administrador
     * @param marca  la marca
     * @param nombre nombre del modelo
     * @param tipo   tipo de vehículo
     * @return el modelo agregado
     * @throws ConcesionariaException si no tiene permiso, el nombre no es válido, la marca no
     *                                fabrica ese tipo, ya existe o no se pudo guardar
     */
    public Modelo agregarModelo(Sesion sesion, Marca marca, String nombre, TipoVehiculo tipo)
            throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_MARCAS);
        Modelo nuevo = new Modelo(nombre, tipo);
        if (!marca.fabrica(tipo)) {
            throw new ReglaNegocioException(marca.getNombre() + " no fabrica "
                    + tipo.getPlural() + ". Agregá el tipo en Marcas › Modificar país o tipos.");
        }
        Optional<Modelo> existente = marca.buscarModelo(nuevo.getNombre());
        if (existente.isPresent()) {
            throw new ReglaNegocioException(marca.getNombre() + " ya tiene el modelo "
                    + existente.get() + ".");
        }
        marca.agregarModelo(nuevo);
        guardarCambios();
        return nuevo;
    }

    /**
     * Quita un modelo del catálogo, si ningún vehículo lo usa.
     *
     * @param sesion sesión de un administrador
     * @param marca  la marca
     * @param modelo el modelo
     * @throws ConcesionariaException si no tiene permiso, está en uso o no se pudo guardar
     */
    public void quitarModelo(Sesion sesion, Marca marca, Modelo modelo)
            throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_MARCAS);
        long enUso = cantidadVehiculos(marca, modelo);
        if (enUso > 0) {
            throw new ReglaNegocioException("El " + modelo.getNombre() + " tiene " + enUso
                    + " vehículo(s) y no se puede quitar del catálogo.");
        }
        marca.quitarModelo(modelo);
        guardarCambios();
    }

    /**
     * @param marca  la marca
     * @param modelo un modelo de su catálogo
     * @return cuántos vehículos (de cualquier situación) son de ese modelo
     */
    public long cantidadVehiculos(Marca marca, Modelo modelo) {
        return vehiculos.listar().stream()
                .filter(v -> marca.equals(v.getMarca())
                        && modelo.getClave().equals(Textos.compacta(v.getModelo())))
                .count();
    }

    private Marca buscar(int id) throws MarcaNoEncontradaException {
        return marcas.buscar(id).orElseThrow(() -> new MarcaNoEncontradaException(
                "No hay ninguna marca con el número " + id + "."));
    }
}
