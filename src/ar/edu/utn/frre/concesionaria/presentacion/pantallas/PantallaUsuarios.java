package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.modelo.Rol;
import ar.edu.utn.frre.concesionaria.modelo.Usuario;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Tabla;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ABM de usuarios (CU-13) y cambio de la propia clave (CU-16).
 */
final class PantallaUsuarios extends Pantalla {

    PantallaUsuarios(Entorno entorno, Sesion sesion) {
        super(entorno, sesion);
    }

    void mostrar() {
        List<Opcion> opciones = new ArrayList<>();
        seccion(opciones, "Cuentas");
        agregar(opciones, "Listar usuarios", "rol y estado de cada uno", true, this::listar);
        agregar(opciones, "Nuevo usuario", "con rol y clave inicial", true, this::crear);
        agregar(opciones, "Desactivar", "ya no puede iniciar sesión", true, () -> {
            Usuario u = elegir();
            e.autenticacion.desactivar(sesion, u.getUsuario());
            t.exito(u.getUsuario() + " quedó desactivado: no puede iniciar sesión.");
        });
        agregar(opciones, "Reactivar", "vuelve a poder entrar", true, () -> {
            Usuario u = elegir();
            e.autenticacion.reactivar(sesion, u.getUsuario());
            t.exito(u.getUsuario() + " está activo.");
        });
        seccion(opciones, "Permisos y claves");
        agregar(opciones, "Cambiar rol", "Administrador, Vendedor o Directivo", true, () -> {
            Usuario u = elegir();
            t.blanco();
            Rol rol = lector.elegir("Rol", Arrays.asList(Rol.values()), Rol::getEtiqueta, u.getRol());
            e.autenticacion.cambiarRol(sesion, u.getUsuario(), rol);
            t.exito(u.getUsuario() + " ahora es " + rol.getEtiqueta() + ".");
        });
        agregar(opciones, "Blanquear clave", "si alguien se la olvidó", true, () -> {
            Usuario u = elegir();
            t.info("Asignale una clave nueva y pasásela por un medio seguro.");
            String clave = leerClaveNueva();
            e.autenticacion.blanquearClave(sesion, u.getUsuario(), clave);
            t.exito("Se cambió la clave de " + u.getUsuario() + ".");
        });
        menu("Usuarios", () -> {
            List<Usuario> usuarios = e.autenticacion.listar(sesion);
            long activos = usuarios.stream().filter(Usuario::estaActivo).count();
            return usuarios.size() + " usuarios · activos: " + activos;
        }, opciones);
    }

    private void listar() throws ConcesionariaException {
        Tabla tabla = new Tabla().fija("Usuario").texto("Nombre").fija("Rol").fija("Estado");
        for (Usuario u : e.autenticacion.listar(sesion)) {
            tabla.fila(u.getUsuario(), u.getNombreCompleto(), u.getRol().getEtiqueta(),
                    u.estaActivo() ? "Activo" : estilo.tenue("Inactivo"));
        }
        t.lineas(tabla.renglones(t.ancho(), estilo));
    }

    private Usuario elegir() throws ConcesionariaException, OperacionCancelada {
        List<Usuario> usuarios = e.autenticacion.listar(sesion);
        return lector.elegir("Usuario", usuarios, u -> String.format("%-20s %s · %s%s",
                u.getUsuario(), u.getNombreCompleto(), u.getRol().getEtiqueta(),
                u.estaActivo() ? "" : " · inactivo"));
    }

    private void crear() throws ConcesionariaException, OperacionCancelada {
        lector.operacion("el alta del usuario");
        t.paso(1, 3, "Identificación");
        t.ayuda("Usuario: 4 a 20 caracteres, minúsculas, números, punto y guion bajo.");
        String usuario = lector.leer("Usuario", u -> {
            e.autenticacion.validarUsuarioDisponible(u);
            return u.toLowerCase();
        });
        String nombre = lector.leer("Nombre y apellido", n -> Comprador.validarNombre("Nombre", n));
        t.paso(2, 3, "Rol");
        t.ayuda("Vendedor: stock y ventas. Directivo: consulta, reportes y exportar."
                + " Administrador: todo.");
        Rol rol = lector.elegir("Rol", Arrays.asList(Rol.values()), Rol::getEtiqueta);
        t.paso(3, 3, "Clave inicial");
        t.ayuda("Pasásela por un medio seguro; la puede cambiar desde Mi cuenta.");
        String clave = leerClaveNueva();
        e.autenticacion.crearUsuario(sesion, usuario, nombre, rol, clave);
        t.exito("Se creó el usuario " + usuario + " (" + rol.getEtiqueta() + ").");
    }

    /** CU-16: lo puede hacer cualquier rol. */
    void cambiarClavePropia() {
        cabecera("Cambiar mi clave");
        ejecutar(() -> {
            String actual = lector.clave("Clave actual");
            String nueva = leerClaveNueva();
            e.autenticacion.cambiarClave(sesion, actual, nueva);
            t.exito("Se cambió tu clave.");
        });
        lector.pausa();
    }
}
