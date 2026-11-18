package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Todo lo que se guarda tiene un identificador. Es la base del repositorio genérico
 * {@code Repositorio<T extends Identificable<K>, K>}: el código de guardar, buscar y listar
 * se escribe una sola vez.
 *
 * @param <K> tipo del identificador (String para vehículos y usuarios, Integer para marcas
 *            y ventas)
 */
public interface Identificable<K> {

    /**
     * @return el identificador, o {@code null} si todavía no se asignó
     */
    K getId();
}
