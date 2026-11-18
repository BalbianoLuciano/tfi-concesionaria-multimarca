package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;

/**
 * Guardado del estado completo. El negocio la llama después de cada operación que cambia algo;
 * no sabe si abajo hay un archivo, una base de datos o nada (en las pruebas).
 */
public interface Persistencia {

    /**
     * Guarda todo. Si falla, lo que está en memoria queda intacto y sigue marcado como
     * pendiente.
     *
     * @throws PersistenciaException si no se pudo guardar
     */
    void guardar() throws PersistenciaException;

    /**
     * @return {@code true} si hay cambios que todavía no se guardaron
     */
    boolean hayCambiosPendientes();

    /** Marca que hubo un cambio. */
    void marcarCambio();
}
