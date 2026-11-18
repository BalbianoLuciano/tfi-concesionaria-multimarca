package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * Usuario o clave incorrectos. No dice cuál de los dos, para no revelar qué usuarios existen.
 */
public class CredencialesInvalidasException extends SeguridadException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
