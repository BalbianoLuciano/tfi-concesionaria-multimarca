package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Estado de uso del vehículo: 0 km o usado. Define si se deprecia o se cobra patentamiento.
 */
public enum Estado implements ConEtiqueta {

    /** 0 km. */
    NUEVO("0 km"),
    /** Usado. */
    USADO("Usado");

    private final String etiqueta;

    Estado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }
}
