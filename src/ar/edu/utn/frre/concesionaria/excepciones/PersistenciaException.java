package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * No se pudieron leer o guardar los datos.
 *
 * <p>Envuelve la {@code IOException} original como causa. Así el negocio puede atraparla sin
 * importar {@code java.io}: la regla de capas dice que el negocio no conoce el disco.</p>
 */
public class PersistenciaException extends ConcesionariaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje qué no se pudo hacer y dónde
     */
    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    /**
     * @param mensaje qué no se pudo hacer y dónde
     * @param causa   la excepción de entrada/salida original
     */
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
