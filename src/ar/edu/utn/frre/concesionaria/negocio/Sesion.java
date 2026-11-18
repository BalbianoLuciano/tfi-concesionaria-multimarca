package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.excepciones.AccesoDenegadoException;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Rol;
import ar.edu.utn.frre.concesionaria.modelo.Usuario;

/**
 * Sesión iniciada. Cada operación del negocio la recibe y exige el permiso que necesita: la
 * consola arma el menú según el rol, pero no es la que decide la seguridad (SPEC §1).
 */
public final class Sesion {

    private final Usuario usuario;

    /**
     * @param usuario el usuario autenticado
     */
    Sesion(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * Verifica un permiso.
     *
     * @param permiso el permiso que pide la operación
     * @throws AccesoDenegadoException si el rol no lo tiene o el usuario fue desactivado
     */
    public void exigir(Permiso permiso) throws AccesoDenegadoException {
        if (!usuario.puede(permiso)) {
            throw new AccesoDenegadoException("Tu rol (" + usuario.getRol().getEtiqueta()
                    + ") no tiene permiso para " + permiso.getEtiqueta().toLowerCase() + ".");
        }
    }

    /**
     * @param permiso el permiso
     * @return {@code true} si la sesión lo tiene (para armar menús)
     */
    public boolean puede(Permiso permiso) {
        return usuario.puede(permiso);
    }

    /** @return el usuario de la sesión */
    public Usuario getUsuario() {
        return usuario;
    }

    /** @return el nombre de usuario */
    public String getNombreUsuario() {
        return usuario.getUsuario();
    }

    /** @return el rol */
    public Rol getRol() {
        return usuario.getRol();
    }
}
