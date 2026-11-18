package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * La operación viola una regla del negocio (vender algo vendido, una patente repetida). La consola muestra el motivo y vuelve al menú.
 */
public class ReglaNegocioException extends ConcesionariaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
