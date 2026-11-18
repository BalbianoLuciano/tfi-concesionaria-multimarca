package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * El vehículo no está DISPONIBLE: no se puede modificar, vender ni dar de baja. El mensaje dice en qué situación está.
 */
public class VehiculoNoDisponibleException extends ReglaNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public VehiculoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
