package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;

/**
 * Lo común a los servicios que modifican datos: después de cada cambio, guardar.
 */
abstract class ServicioBase {

    /** Guardado del estado completo. */
    protected final Persistencia persistencia;

    /**
     * @param persistencia el guardado
     */
    protected ServicioBase(Persistencia persistencia) {
        this.persistencia = persistencia;
    }

    /**
     * Marca el cambio y guarda. Si falla, el cambio queda en memoria y pendiente: se vuelve a
     * intentar en la próxima operación o al salir (CU-17).
     *
     * @throws PersistenciaException si no se pudo guardar
     */
    protected void guardarCambios() throws PersistenciaException {
        persistencia.marcarCambio();
        persistencia.guardar();
    }
}
