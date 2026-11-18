package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.Marca;
import java.util.List;
import java.util.Optional;

/**
 * Catálogo de marcas, indexado por id y por nombre normalizado.
 */
public interface RepositorioMarcas extends Repositorio<Marca, Integer> {

    /**
     * @param nombre el nombre, con cualquier capitalización o acentos
     * @return la marca con ese nombre normalizado, o vacío
     */
    Optional<Marca> buscarPorNombre(String nombre);

    /**
     * Marcas cuyo nombre empieza con el prefijo ("toy" → Toyota), sin importar mayúsculas ni
     * acentos. Se resuelve con {@code TreeMap.subMap} en O(log n + k).
     *
     * @param prefijo las primeras letras
     * @return las marcas que coinciden, en orden alfabético
     */
    List<Marca> buscarPorPrefijo(String prefijo);

    /**
     * @return el próximo id libre
     */
    int siguienteId();
}
