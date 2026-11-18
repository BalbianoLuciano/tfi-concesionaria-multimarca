package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.Usuario;

/**
 * Usuarios en memoria. Todo lo resuelve la clase genérica.
 */
public class RepositorioUsuariosMemoria extends RepositorioMemoria<Usuario, String>
        implements RepositorioUsuarios {

    /**
     * @param almacen el almacén cargado
     */
    public RepositorioUsuariosMemoria(Almacen almacen) {
        super(almacen.getUsuarios());
    }
}
