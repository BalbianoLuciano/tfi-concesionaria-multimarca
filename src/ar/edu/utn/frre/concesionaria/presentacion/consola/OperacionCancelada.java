package ar.edu.utn.frre.concesionaria.presentacion.consola;

/**
 * El usuario canceló la operación (escribió {@code 0} o {@code :q}) o se quedó sin intentos en
 * un campo. No es un error: la pantalla muestra el motivo y vuelve al menú sin guardar nada.
 */
public class OperacionCancelada extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje qué se canceló y por qué
     */
    public OperacionCancelada(String mensaje) {
        super(mensaje);
    }
}
