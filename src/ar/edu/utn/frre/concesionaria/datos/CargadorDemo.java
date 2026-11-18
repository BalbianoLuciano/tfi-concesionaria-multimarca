package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;

/**
 * Carga los datos de demostración del primer arranque (CU-01).
 */
public interface CargadorDemo {

    /**
     * Agrega las marcas y vehículos de ejemplo.
     *
     * @throws ConcesionariaException si algún dato de ejemplo no es válido
     */
    void cargar() throws ConcesionariaException;
}
