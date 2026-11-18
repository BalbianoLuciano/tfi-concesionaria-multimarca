package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * Raíz de las excepciones propias del sistema (MODELO §4).
 *
 * <p>Es <em>checked</em> (extiende {@link Exception}) porque representa errores esperables,
 * como un dato mal cargado o una patente repetida: el compilador obliga a quien llama a
 * decidir qué hacer. La consola las atrapa de lo específico a lo general.</p>
 */
public class ConcesionariaException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación para el usuario, en español y diciendo cómo seguir
     */
    public ConcesionariaException(String mensaje) {
        super(mensaje);
    }

    /**
     * @param mensaje explicación para el usuario
     * @param causa   la excepción original (encadenamiento)
     */
    public ConcesionariaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
