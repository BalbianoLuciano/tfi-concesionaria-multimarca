package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Situación comercial del vehículo (MODELO §3). Solo DISPONIBLE admite modificar, vender o dar de baja.
 */
public enum Situacion implements ConEtiqueta {

    /** Disponible. */
    DISPONIBLE("Disponible"),
    /** Vendido. */
    VENDIDO("Vendido"),
    /** Baja. */
    BAJA("Baja");

    private final String etiqueta;

    Situacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }
}
