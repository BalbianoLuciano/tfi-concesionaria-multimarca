package ar.edu.utn.frre.concesionaria.modelo;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Roles de usuario. La matriz de permisos de SPEC §1 está escrita una sola vez, acá.
 */
public enum Rol implements ConEtiqueta {

    /** Gestiona usuarios y marcas; puede hacer todo lo demás. */
    ADMINISTRADOR("Administrador", EnumSet.allOf(Permiso.class)),

    /** Carga y mantiene el stock, cotiza y vende. Ve solo sus ventas. */
    VENDEDOR("Vendedor", EnumSet.of(Permiso.VER_STOCK, Permiso.GESTIONAR_STOCK, Permiso.VENDER,
            Permiso.VER_VENTAS_PROPIAS)),

    /** Consulta stock, ventas y reportes; puede exportar. No modifica nada. */
    DIRECTIVO("Directivo", EnumSet.of(Permiso.VER_STOCK, Permiso.VER_VENTAS,
            Permiso.VER_VENTAS_PROPIAS, Permiso.VER_REPORTES, Permiso.EXPORTAR));

    private final String etiqueta;
    private final Set<Permiso> permisos;

    Rol(String etiqueta, Set<Permiso> permisos) {
        this.etiqueta = etiqueta;
        this.permisos = Collections.unmodifiableSet(permisos);
    }

    @Override
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @return los permisos del rol (no modificable)
     */
    public Set<Permiso> getPermisos() {
        return permisos;
    }

    /**
     * @param permiso el permiso a consultar
     * @return {@code true} si el rol lo tiene
     */
    public boolean tiene(Permiso permiso) {
        return permisos.contains(permiso);
    }
}
