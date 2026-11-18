package ar.edu.utn.frre.concesionaria.modelo;

/**
 * Operaciones que se controlan por rol (SPEC §1). El negocio exige el permiso en cada operación.
 */
public enum Permiso implements ConEtiqueta {

    /** Ver stock. */
    VER_STOCK("Ver stock"),
    /** Gestionar stock. */
    GESTIONAR_STOCK("Gestionar stock"),
    /** Cotizar y vender. */
    VENDER("Cotizar y vender"),
    /** Ver todas las ventas. */
    VER_VENTAS("Ver todas las ventas"),
    /** Ver las ventas propias. */
    VER_VENTAS_PROPIAS("Ver las ventas propias"),
    /** Ver reportes. */
    VER_REPORTES("Ver reportes"),
    /** Gestionar marcas. */
    GESTIONAR_MARCAS("Gestionar marcas"),
    /** Gestionar usuarios. */
    GESTIONAR_USUARIOS("Gestionar usuarios"),
    /** Exportar. */
    EXPORTAR("Exportar"),
    /** Importar. */
    IMPORTAR("Importar");

    private final String etiqueta;

    Permiso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }
}
