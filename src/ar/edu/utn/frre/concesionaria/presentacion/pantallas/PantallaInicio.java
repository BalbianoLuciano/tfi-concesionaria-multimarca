package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.CredencialesInvalidasException;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;

/**
 * Primer arranque (CU-01) e inicio de sesión (CU-02).
 */
public final class PantallaInicio extends Pantalla {

    /** Intentos de inicio de sesión antes de terminar el programa. */
    public static final int INTENTOS_LOGIN = 3;

    private final String carpetaDatos;

    /**
     * @param entorno      terminal, lector y servicios
     * @param carpetaDatos carpeta de datos, para mostrarla en el primer arranque
     */
    public PantallaInicio(Entorno entorno, String carpetaDatos) {
        super(entorno, null);
        this.carpetaDatos = carpetaDatos;
    }

    /**
     * Si no hay usuarios, crea el administrador y ofrece los datos de demostración; si no, pide
     * usuario y clave.
     *
     * @return la sesión, o {@code null} si se agotaron los intentos o se canceló
     */
    public Sesion iniciar() {
        if (!e.autenticacion.hayUsuarios()) {
            return primerArranque();
        }
        return login();
    }

    private Sesion primerArranque() {
        while (true) {
            cabecera("Primer arranque");
            t.titulo("Bienvenido");
            t.linea(" No hay datos todavía. Se van a guardar en " + carpetaDatos + ".");
            t.linea(" Primero creá el usuario administrador: gestiona usuarios y marcas, y puede");
            t.linea(" hacer todo lo demás.");
            t.blanco();
            lector.operacion("la creación del administrador");
            try {
                String usuario = lector.leer("Usuario", u -> {
                    e.autenticacion.validarUsuarioDisponible(u);
                    return u.toLowerCase();
                });
                String nombre = lector.leer("Nombre y apellido",
                        n -> Comprador.validarNombre("Nombre", n));
                String clave = leerClaveNueva();
                Sesion sesion = e.autenticacion.crearAdministradorInicial(usuario, nombre, clave);
                t.exito("Se creó el administrador " + usuario + ".");
                t.blanco();
                t.linea(" El catálogo de marcas y modelos ya está cargado. ¿Querés sumar datos de");
                t.linea(" demostración? Son 10 vehículos de ejemplo, útiles para probar el sistema.");
                t.linea(" Después los podés dar de baja.");
                if (lector.confirmar("Cargar datos de demostración")) {
                    e.autenticacion.cargarDatosDemo(sesion);
                    t.exito("Se cargaron los datos de demostración.");
                }
                lector.pausa();
                return sesion;
            } catch (OperacionCancelada ex) {
                t.info(ex.getMessage() + " Hace falta un administrador para empezar.");
                lector.pausa();
            } catch (ConcesionariaException ex) {
                t.error(ex.getMessage());
                lector.pausa();
            }
        }
    }

    private Sesion login() {
        cabecera("Iniciar sesión");
        for (int intento = 1; intento <= INTENTOS_LOGIN; intento++) {
            String usuario = lector.linea("Usuario");
            String clave = lector.clave("Clave");
            try {
                Sesion sesion = e.autenticacion.iniciarSesion(usuario, clave);
                return sesion;
            } catch (CredencialesInvalidasException ex) {
                int quedan = INTENTOS_LOGIN - intento;
                t.error(ex.getMessage() + (quedan == 0 ? ""
                        : quedan == 1 ? " Te queda 1 intento." : " Te quedan " + quedan + " intentos."));
                t.blanco();
            }
        }
        t.error("Se agotaron los intentos. El programa se cierra por seguridad.");
        return null;
    }
}
