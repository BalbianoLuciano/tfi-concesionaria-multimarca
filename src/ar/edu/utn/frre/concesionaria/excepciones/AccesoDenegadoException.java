package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * El rol de la sesión no tiene el permiso que pide la operación. Lo verifica el negocio, no la consola.
 */
public class AccesoDenegadoException extends SeguridadException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
