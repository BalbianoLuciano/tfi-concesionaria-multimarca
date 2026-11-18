package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Usuario del sistema. Nunca guarda la clave: guarda un hash SHA-256 de la clave con una sal
 * aleatoria (ARQUITECTURA §5). El cálculo del hash lo hace el negocio.
 */
public class Usuario implements Identificable<String>, Serializable, Comparable<Usuario> {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String nombreCompleto;
    private Rol rol;
    private String hashClave;
    private String sal;
    private Boolean activo = Boolean.TRUE;

    /** Constructor vacío de JavaBean. */
    public Usuario() {
    }

    /**
     * @param usuario        nombre de usuario
     * @param nombreCompleto nombre y apellido
     * @param rol            rol
     * @throws DatoInvalidoException si algún dato no cumple su regla
     */
    public Usuario(String usuario, String nombreCompleto, Rol rol) throws DatoInvalidoException {
        setUsuario(usuario);
        setNombreCompleto(nombreCompleto);
        setRol(rol);
    }

    /**
     * Un usuario inactivo no tiene ningún permiso.
     *
     * @param permiso el permiso
     * @return {@code true} si está activo y su rol lo tiene
     */
    public boolean puede(Permiso permiso) {
        return estaActivo() && rol != null && rol.tiene(permiso);
    }

    /** @return {@code true} si está activo */
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    @Override
    public String getId() {
        return usuario;
    }

    /** @return el nombre de usuario */
    public String getUsuario() {
        return usuario;
    }

    /**
     * @param usuario 4 a 20 caracteres: minúsculas, números, punto y guion bajo
     * @throws DatoInvalidoException si no cumple
     */
    public void setUsuario(String usuario) throws DatoInvalidoException {
        String limpio = Textos.limpiar(usuario).toLowerCase();
        if (!limpio.matches("[a-z0-9._]{4,20}")) {
            throw new DatoInvalidoException("Usuario", usuario,
                    "El usuario va de 4 a 20 caracteres: minúsculas, números, punto y guion bajo.");
        }
        this.usuario = limpio;
    }

    /** @return el nombre y apellido */
    public String getNombreCompleto() {
        return nombreCompleto;
    }

    /**
     * @param nombreCompleto 2 a 60 caracteres, letras y espacios
     * @throws DatoInvalidoException si no cumple
     */
    public void setNombreCompleto(String nombreCompleto) throws DatoInvalidoException {
        this.nombreCompleto = Comprador.validarNombre("Nombre", nombreCompleto);
    }

    /** @return el rol */
    public Rol getRol() {
        return rol;
    }

    /**
     * @param rol el rol
     * @throws DatoInvalidoException si es {@code null}
     */
    public void setRol(Rol rol) throws DatoInvalidoException {
        if (rol == null) {
            throw new DatoInvalidoException("Rol", null, "Elegí un rol.");
        }
        this.rol = rol;
    }

    /** @return el hash de la clave en hexadecimal */
    public String getHashClave() {
        return hashClave;
    }

    /** @return la sal en hexadecimal */
    public String getSal() {
        return sal;
    }

    /**
     * Guarda las credenciales ya calculadas. Nunca recibe la clave en texto plano.
     *
     * @param hashClave hash de la clave
     * @param sal       sal usada
     */
    public void setCredenciales(String hashClave, String sal) {
        this.hashClave = hashClave;
        this.sal = sal;
    }

    /** @return si está activo */
    public Boolean getActivo() {
        return activo;
    }

    /** @param activo si puede iniciar sesión */
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    /** Igualdad por nombre de usuario, que es único. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario)) {
            return false;
        }
        Usuario otro = (Usuario) o;
        return Objects.equals(usuario, otro.usuario);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(usuario);
    }

    /** Orden alfabético por nombre de usuario. */
    @Override
    public int compareTo(Usuario otro) {
        return usuario.compareTo(otro.usuario);
    }

    /** No incluye el hash ni la sal: nada de credenciales en los logs. */
    @Override
    public String toString() {
        return "Usuario[" + usuario + ", " + nombreCompleto + ", " + rol
                + (estaActivo() ? "" : ", inactivo") + "]";
    }
}
