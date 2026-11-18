package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Tipos de carrocería de una camioneta.
 */
public enum CarroceriaCamioneta implements ConEtiqueta {

    /** Pick-up cabina simple. */
    PICKUP_CABINA_SIMPLE("Pick-up cabina simple"),
    /** Doble cabina. */
    PICKUP_DOBLE_CABINA("Doble cabina"),
    /** SUV. */
    SUV("SUV"),
    /** Utilitario. */
    UTILITARIO("Utilitario");

    private final String etiqueta;

    CarroceriaCamioneta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }
}
