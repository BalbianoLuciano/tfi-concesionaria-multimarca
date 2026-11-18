package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Catálogo de marcas en memoria. Además del mapa id → marca, mantiene un {@link TreeMap}
 * nombre normalizado → marca, ordenado, para buscar por prefijo.
 *
 * <p>Contenido extra (SPEC §7): {@code TreeMap.subMap(desde, hasta)} devuelve solo las claves
 * que empiezan con el prefijo, en O(log n + k), sin recorrer todo el catálogo.</p>
 */
public class RepositorioMarcasMemoria extends RepositorioMemoria<Marca, Integer>
        implements RepositorioMarcas {

    private final Almacen almacen;
    private final TreeMap<String, Marca> porNombre = new TreeMap<>();

    /**
     * @param almacen el almacén cargado
     */
    public RepositorioMarcasMemoria(Almacen almacen) {
        super(almacen.getMarcas());
        this.almacen = almacen;
        mapa.values().forEach(m -> porNombre.put(m.getClave(), m));
    }

    @Override
    public void guardar(Marca marca) {
        Marca anterior = mapa.get(marca.getId());
        if (anterior != null) {
            porNombre.remove(anterior.getClave());
        }
        super.guardar(marca);
        porNombre.put(marca.getClave(), marca);
    }

    @Override
    public void eliminar(Integer id) {
        Marca anterior = mapa.get(id);
        if (anterior != null) {
            porNombre.remove(anterior.getClave());
        }
        super.eliminar(id);
    }

    @Override
    public Optional<Marca> buscarPorNombre(String nombre) {
        return Optional.ofNullable(porNombre.get(Textos.clave(nombre)));
    }

    @Override
    public List<Marca> buscarPorPrefijo(String prefijo) {
        String desde = Textos.clave(prefijo);
        // Character.MAX_VALUE es mayor que cualquier letra: el rango cubre todo lo que empieza
        // con el prefijo.
        return new ArrayList<>(porNombre.subMap(desde, desde + Character.MAX_VALUE).values());
    }

    @Override
    public List<Marca> listar() {
        return new ArrayList<>(porNombre.values());
    }

    @Override
    public int siguienteId() {
        almacen.setUltimaMarca(almacen.getUltimaMarca() + 1);
        return almacen.getUltimaMarca();
    }
}
