package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.Identificable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación genérica en memoria sobre un {@link Map}: buscar por id es O(1) con un
 * {@code HashMap} (ARQUITECTURA §7). El mapa es el del {@link Almacen}, así lo que se guarda
 * acá queda en el próximo snapshot.
 *
 * @param <T> tipo de entidad
 * @param <K> tipo de id
 */
public abstract class RepositorioMemoria<T extends Identificable<K>, K> implements Repositorio<T, K> {

    /** El mapa id → entidad, compartido con el almacén. */
    protected final Map<K, T> mapa;

    /**
     * @param mapa el mapa del almacén
     */
    protected RepositorioMemoria(Map<K, T> mapa) {
        this.mapa = mapa;
    }

    @Override
    public void guardar(T entidad) {
        mapa.put(entidad.getId(), entidad);
    }

    @Override
    public Optional<T> buscar(K id) {
        return Optional.ofNullable(id == null ? null : mapa.get(id));
    }

    @Override
    public List<T> listar() {
        return new ArrayList<>(mapa.values());
    }

    @Override
    public void eliminar(K id) {
        mapa.remove(id);
    }

    @Override
    public boolean existe(K id) {
        return id != null && mapa.containsKey(id);
    }

    @Override
    public int contar() {
        return mapa.size();
    }
}
