package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Tipos de carrocería de un automóvil.
 */
public enum CarroceriaAutomovil implements ConEtiqueta {

    /** Sedán. */
    SEDAN("Sedán"),
    /** Hatchback. */
    HATCHBACK("Hatchback"),
    /** Rural. */
    RURAL("Rural"),
    /** Coupé. */
    COUPE("Coupé"),
    /** Convertible. */
    CONVERTIBLE("Convertible");

    private final String etiqueta;

    CarroceriaAutomovil(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }
}
