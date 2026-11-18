package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Los tres tipos de vehículo que vende la concesionaria. Cada uno sabe el prefijo de su
 * código interno: A-0001, C-0001, M-0001.
 */
public enum TipoVehiculo implements ConEtiqueta {

    /** Automóvil, prefijo A. */
    AUTOMOVIL("Automóvil", "automóviles", 'A'),
    /** Camioneta, prefijo C. */
    CAMIONETA("Camioneta", "camionetas", 'C'),
    /** Motocicleta, prefijo M. */
    MOTOCICLETA("Motocicleta", "motocicletas", 'M');

    private final String etiqueta;
    private final String plural;
    private final char prefijo;

    TipoVehiculo(String etiqueta, String plural, char prefijo) {
        this.etiqueta = etiqueta;
        this.plural = plural;
        this.prefijo = prefijo;
    }

    /**
     * @return el plural en minúsculas, para los mensajes ("no fabrica motocicletas")
     */
    public String getPlural() {
        return plural;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @return la letra con la que empiezan los códigos de este tipo
     */
    public char getPrefijo() {
        return prefijo;
    }
}
