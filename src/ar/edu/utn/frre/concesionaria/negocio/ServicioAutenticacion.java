package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.datos.CargadorDemo;
import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.datos.RepositorioUsuarios;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.CredencialesInvalidasException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.EntidadNoEncontradaException;
import ar.edu.utn.frre.concesionaria.excepciones.ReglaNegocioException;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Rol;
import ar.edu.utn.frre.concesionaria.modelo.Usuario;
import java.util.List;

/**
 * Primer arranque, inicio de sesión y ABM de usuarios (CU-01, CU-02, CU-13 y CU-16).
 */
public class ServicioAutenticacion extends ServicioBase {

    private final RepositorioUsuarios usuarios;
    private final CargadorDemo demo;

    /**
     * @param usuarios     repositorio de usuarios
     * @param persistencia guardado
     * @param demo         carga de los datos de demostración
     */
    public ServicioAutenticacion(RepositorioUsuarios usuarios, Persistencia persistencia,
            CargadorDemo demo) {
        super(persistencia);
        this.usuarios = usuarios;
        this.demo = demo;
    }

    /**
     * @return {@code true} si ya hay usuarios; si no, es el primer arranque
     */
    public boolean hayUsuarios() {
        return usuarios.contar() > 0;
    }

    /**
     * Crea el primer administrador (CU-01). Solo funciona si no hay ningún usuario.
     *
     * @param usuario nombre de usuario
     * @param nombre  nombre y apellido
     * @param clave   clave
     * @return la sesión del administrador recién creado
     * @throws ConcesionariaException si ya hay usuarios, si algún dato no es válido o si no se
     *                                pudo guardar
     */
    public Sesion crearAdministradorInicial(String usuario, String nombre, String clave)
            throws ConcesionariaException {
        if (hayUsuarios()) {
            throw new ReglaNegocioException("El administrador inicial ya fue creado.");
        }
        Usuario admin = nuevoUsuario(usuario, nombre, Rol.ADMINISTRADOR, clave);
        usuarios.guardar(admin);
        guardarCambios();
        return new Sesion(admin);
    }

    /**
     * Carga las marcas y vehículos de ejemplo (CU-01, paso 3).
     *
     * @param sesion sesión de un administrador
     * @throws ConcesionariaException si no tiene permiso o no se pudo guardar
     */
    public void cargarDatosDemo(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        demo.cargar();
        guardarCambios();
    }

    /**
     * Inicia sesión (CU-02). El mensaje de error no dice si falló el usuario o la clave.
     *
     * @param usuario nombre de usuario
     * @param clave   clave
     * @return la sesión
     * @throws CredencialesInvalidasException si no coinciden o el usuario está inactivo
     */
    public Sesion iniciarSesion(String usuario, String clave) throws CredencialesInvalidasException {
        String clave1 = usuario == null ? "" : usuario.trim().toLowerCase();
        Usuario u = usuarios.buscar(clave1).orElse(null);
        if (u == null || !u.estaActivo() || !Claves.coincide(clave, u.getSal(), u.getHashClave())) {
            throw new CredencialesInvalidasException("Usuario o clave incorrectos.");
        }
        return new Sesion(u);
    }

    /**
     * Cambia la clave propia (CU-16).
     *
     * @param sesion la sesión
     * @param actual la clave actual
     * @param nueva  la clave nueva
     * @throws ConcesionariaException si la actual no coincide, la nueva no cumple la regla o no
     *                                se pudo guardar
     */
    public void cambiarClave(Sesion sesion, String actual, String nueva)
            throws ConcesionariaException {
        Usuario u = sesion.getUsuario();
        if (!Claves.coincide(actual, u.getSal(), u.getHashClave())) {
            throw new DatoInvalidoException("Clave actual", null, "La clave actual no es correcta.");
        }
        asignarClave(u, nueva);
        guardarCambios();
    }

    /**
     * @param sesion sesión de un administrador
     * @return todos los usuarios, por nombre de usuario
     * @throws ConcesionariaException si no tiene permiso
     */
    public List<Usuario> listar(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        return usuarios.listar().stream().sorted().toList();
    }

    /**
     * Da de alta un usuario (CU-13).
     *
     * @param sesion  sesión de un administrador
     * @param usuario nombre de usuario
     * @param nombre  nombre y apellido
     * @param rol     rol
     * @param clave   clave inicial
     * @return el usuario creado
     * @throws ConcesionariaException si no tiene permiso, el usuario existe, algún dato no es
     *                                válido o no se pudo guardar
     */
    public Usuario crearUsuario(Sesion sesion, String usuario, String nombre, Rol rol, String clave)
            throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        Usuario u = nuevoUsuario(usuario, nombre, rol, clave);
        usuarios.guardar(u);
        guardarCambios();
        return u;
    }

    /**
     * Verifica que el nombre de usuario sea válido y esté libre, para repreguntar en el alta.
     *
     * @param usuario el nombre de usuario
     * @throws DatoInvalidoException si no es válido o ya existe
     */
    public void validarUsuarioDisponible(String usuario) throws DatoInvalidoException {
        Usuario prueba = new Usuario();
        prueba.setUsuario(usuario);
        if (usuarios.existe(prueba.getUsuario())) {
            throw new DatoInvalidoException("Usuario", usuario, "Ese usuario ya existe.");
        }
    }

    /**
     * Desactiva un usuario: no puede iniciar sesión, pero queda en el historial de ventas.
     *
     * @param sesion  sesión de un administrador
     * @param usuario el usuario a desactivar
     * @throws ConcesionariaException si es uno mismo, si es el último administrador activo, si
     *                                no existe o no se pudo guardar
     */
    public void desactivar(Sesion sesion, String usuario) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        Usuario u = buscar(usuario);
        if (u.equals(sesion.getUsuario())) {
            throw new ReglaNegocioException("No te podés desactivar a vos mismo.");
        }
        exigirOtroAdministrador(u);
        u.setActivo(false);
        guardarCambios();
    }

    /**
     * @param sesion  sesión de un administrador
     * @param usuario el usuario a reactivar
     * @throws ConcesionariaException si no existe o no se pudo guardar
     */
    public void reactivar(Sesion sesion, String usuario) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        buscar(usuario).setActivo(true);
        guardarCambios();
    }

    /**
     * @param sesion  sesión de un administrador
     * @param usuario el usuario
     * @param rol     el rol nuevo
     * @throws ConcesionariaException si deja el sistema sin administradores activos
     */
    public void cambiarRol(Sesion sesion, String usuario, Rol rol) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        Usuario u = buscar(usuario);
        if (rol != Rol.ADMINISTRADOR) {
            exigirOtroAdministrador(u);
        }
        u.setRol(rol);
        guardarCambios();
    }

    /**
     * Blanquea la clave de otro usuario: el administrador le asigna una nueva.
     *
     * @param sesion  sesión de un administrador
     * @param usuario el usuario
     * @param nueva   la clave nueva
     * @throws ConcesionariaException si no existe, la clave no cumple o no se pudo guardar
     */
    public void blanquearClave(Sesion sesion, String usuario, String nueva)
            throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        asignarClave(buscar(usuario), nueva);
        guardarCambios();
    }

    private Usuario buscar(String usuario) throws EntidadNoEncontradaException {
        return usuarios.buscar(usuario == null ? "" : usuario.trim().toLowerCase())
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "No existe el usuario \"" + usuario + "\"."));
    }

    /** Siempre tiene que quedar al menos un administrador activo además de {@code u}. */
    private void exigirOtroAdministrador(Usuario u) throws ReglaNegocioException {
        if (u.getRol() != Rol.ADMINISTRADOR || !u.estaActivo()) {
            return;
        }
        long otros = usuarios.listar().stream()
                .filter(o -> !o.equals(u) && o.estaActivo() && o.getRol() == Rol.ADMINISTRADOR)
                .count();
        if (otros == 0) {
            throw new ReglaNegocioException(
                    "Tiene que quedar al menos un administrador activo. Creá otro primero.");
        }
    }

    private Usuario nuevoUsuario(String usuario, String nombre, Rol rol, String clave)
            throws DatoInvalidoException {
        validarUsuarioDisponible(usuario);
        Usuario u = new Usuario(usuario, nombre, rol);
        asignarClave(u, clave);
        return u;
    }

    private static void asignarClave(Usuario u, String clave) throws DatoInvalidoException {
        Claves.validar(clave);
        String sal = Claves.generarSal();
        u.setCredenciales(Claves.hashear(clave, sal), sal);
    }
}
