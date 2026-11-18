package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * Se buscó algo por su identificador y no existe.
 */
public class EntidadNoEncontradaException extends ConcesionariaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
