package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.Identificable;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio genérico: guardar, buscar, listar y eliminar cualquier entidad
 * {@link Identificable}. El negocio solo conoce estas interfaces, nunca el archivo; cambiar el
 * {@code .ser} por MySQL sería escribir otra implementación (ARQUITECTURA §1).
 *
 * @param <T> tipo de entidad
 * @param <K> tipo de su identificador
 */
public interface Repositorio<T extends Identificable<K>, K> {

    /**
     * Agrega o reemplaza la entidad con ese id.
     *
     * @param entidad la entidad, con id asignado
     */
    void guardar(T entidad);

    /**
     * @param id el identificador
     * @return la entidad, o vacío si no existe
     */
    Optional<T> buscar(K id);

    /**
     * @return todas las entidades, en una lista nueva que se puede ordenar o filtrar
     */
    List<T> listar();

    /**
     * Quita la entidad. Solo se usa para deshacer una operación que no se pudo guardar.
     *
     * @param id el identificador
     */
    void eliminar(K id);

    /**
     * @param id el identificador
     * @return {@code true} si existe
     */
    default boolean existe(K id) {
        return buscar(id).isPresent();
    }

    /**
     * @return la cantidad de entidades
     */
    default int contar() {
        return listar().size();
    }
}
