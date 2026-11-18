package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * Ya existe una marca con ese nombre, comparando sin mayúsculas ni acentos.
 */
public class MarcaDuplicadaException extends ReglaNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public MarcaDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
