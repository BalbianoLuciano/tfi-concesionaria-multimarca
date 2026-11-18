package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Colores de carrocería que ofrece la concesionaria.
 */
public enum Color implements ConEtiqueta {

    /** Blanco. */
    BLANCO("Blanco"),
    /** Negro. */
    NEGRO("Negro"),
    /** Gris. */
    GRIS("Gris"),
    /** Plata. */
    PLATA("Plata"),
    /** Rojo. */
    ROJO("Rojo"),
    /** Azul. */
    AZUL("Azul"),
    /** Verde. */
    VERDE("Verde"),
    /** Bordó. */
    BORDO("Bordó"),
    /** Beige. */
    BEIGE("Beige");

    private final String etiqueta;

    Color(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }
}
