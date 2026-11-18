package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * No hay ninguna marca con ese nombre o id.
 */
public class MarcaNoEncontradaException extends EntidadNoEncontradaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public MarcaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
