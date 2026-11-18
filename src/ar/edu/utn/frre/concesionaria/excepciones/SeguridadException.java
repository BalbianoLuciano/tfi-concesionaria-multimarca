package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * Problema de autenticación o de permisos.
 */
public class SeguridadException extends ConcesionariaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public SeguridadException(String mensaje) {
        super(mensaje);
    }
}
