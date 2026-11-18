package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * La patente ya está registrada en otro vehículo (incluidos los vendidos y dados de baja).
 */
public class PatenteDuplicadaException extends ReglaNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public PatenteDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
