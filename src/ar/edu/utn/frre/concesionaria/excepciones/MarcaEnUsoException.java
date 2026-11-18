package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * La marca tiene vehículos y no se puede borrar; solo desactivar.
 */
public class MarcaEnUsoException extends ReglaNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public MarcaEnUsoException(String mensaje) {
        super(mensaje);
    }
}
