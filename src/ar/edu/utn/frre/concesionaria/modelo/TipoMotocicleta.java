package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Tipos de motocicleta.
 */
public enum TipoMotocicleta implements ConEtiqueta {

    /** Calle. */
    CALLE("Calle"),
    /** Naked. */
    NAKED("Naked"),
    /** Deportiva. */
    DEPORTIVA("Deportiva"),
    /** Touring. */
    TOURING("Touring"),
    /** Enduro. */
    ENDURO("Enduro"),
    /** Scooter. */
    SCOOTER("Scooter"),
    /** Custom. */
    CUSTOM("Custom");

    private final String etiqueta;

    TipoMotocicleta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }
}
