package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * No hay ningún vehículo con ese código.
 */
public class VehiculoNoEncontradoException extends EntidadNoEncontradaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public VehiculoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
