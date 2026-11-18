package ar.edu.utn.frre.concesionaria.presentacion.consola;

/**
 * Se cerró la entrada estándar (Ctrl+D, o el fin de un archivo redirigido). Es de runtime
 * porque puede pasar en cualquier lectura; {@code App} la atrapa y sale guardando.
 */
public class FinDeEntrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Crea la excepción. */
    public FinDeEntrada() {
        super("Se cerró la entrada estándar.");
    }
}
