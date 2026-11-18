package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.excepciones.AccesoDenegadoException;
import ar.edu.utn.frre.concesionaria.excepciones.CredencialesInvalidasException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.ReglaNegocioException;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.modelo.Rol;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Usuario;
import ar.edu.utn.frre.concesionaria.negocio.Claves;
import ar.edu.utn.frre.concesionaria.negocio.FiltroVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.OrdenVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import java.util.EnumSet;

/**
 * PR-PERMISO: cada rol contra cada operación de la matriz de SPEC §1, más la autenticación
 * (CU-02, CU-13, CU-16) y la regla de la clave.
 */
public final class PruebasPermisos implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-PERMISO";
    }

    /** Una operación del negocio que puede ser denegada. */
    @FunctionalInterface
    private interface Operacion {
        void ejecutar(Contexto c, Sesion s) throws Exception;
    }

    private static void permitido(Contexto c, Sesion s, Operacion op) throws Exception {
        try {
            op.ejecutar(c, s);
        } catch (AccesoDenegadoException e) {
            throw new AssertionError(s.getRol() + " debería poder: " + e.getMessage());
        }
    }

    private static void denegado(Contexto c, Sesion s, Operacion op) throws Exception {
        Verificar.lanza(AccesoDenegadoException.class, () -> op.ejecutar(c, s));
    }

    /**
     * Verifica una fila de la matriz: qué roles pueden.
     */
    private static void fila(Operacion op, boolean admin, boolean vendedor, boolean directivo)
            throws Exception {
        Contexto c = new Contexto();
        if (admin) {
            permitido(c, c.admin, op);
        } else {
            denegado(c, c.admin, op);
        }
        c = new Contexto();
        if (vendedor) {
            permitido(c, c.vendedor, op);
        } else {
            denegado(c, c.vendedor, op);
        }
        c = new Contexto();
        if (directivo) {
            permitido(c, c.directivo, op);
        } else {
            denegado(c, c.directivo, op);
        }
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-PERMISO-01", "ver stock, buscar, ver ficha: todos", () ->
                fila((c, s) -> {
                    c.vehiculos.listar(s, new FiltroVehiculos(), OrdenVehiculos.TIPO_Y_MARCA);
                    c.vehiculos.buscarTexto(s, "toyota");
                    c.vehiculos.buscar(s, "C-0001");
                }, true, true, true));
        r.prueba("PR-PERMISO-02", "registrar, modificar, dar de baja: admin y vendedor", () ->
                fila((c, s) -> c.vehiculos.darDeBaja(s, "A-0001", "Prueba"), true, true, false));
        r.prueba("PR-PERMISO-03", "cotizar y vender: admin y vendedor", () ->
                fila((c, s) -> {
                    c.vehiculos.cotizar(s, "A-0001");
                    c.ventas.vender(s, "A-0001", new Comprador("Juan Pérez", "30123456"));
                }, true, true, false));
        r.prueba("PR-PERMISO-04", "historial de ventas: todos (el vendedor, las propias)", () ->
                fila((c, s) -> c.ventas.historial(s, null, null, null), true, true, true));
        r.prueba("PR-PERMISO-05", "reportes: admin y directivo", () ->
                fila((c, s) -> c.reportes.valorizacion(s), true, false, true));
        r.prueba("PR-PERMISO-06", "ABM de marcas: solo admin", () ->
                fila((c, s) -> c.marcas.crear(s, "Renault", "Francia",
                        EnumSet.of(TipoVehiculo.AUTOMOVIL)), true, false, false));
        r.prueba("PR-PERMISO-07", "ABM de usuarios: solo admin", () ->
                fila((c, s) -> c.autenticacion.listar(s), true, false, false));
        r.prueba("PR-PERMISO-08", "exportar: admin y directivo; importar: solo admin", () -> {
            Contexto c = new Contexto();
            Verificar.verdadero(c.admin.puede(ar.edu.utn.frre.concesionaria.modelo.Permiso.IMPORTAR)
                    && !c.directivo.puede(ar.edu.utn.frre.concesionaria.modelo.Permiso.IMPORTAR)
                    && !c.vendedor.puede(ar.edu.utn.frre.concesionaria.modelo.Permiso.IMPORTAR),
                    "solo el admin importa");
            Verificar.verdadero(c.directivo.puede(ar.edu.utn.frre.concesionaria.modelo.Permiso.EXPORTAR)
                    && !c.vendedor.puede(ar.edu.utn.frre.concesionaria.modelo.Permiso.EXPORTAR),
                    "el directivo exporta, el vendedor no");
            denegado(c, c.vendedor, (cc, s) -> cc.archivos.exportarVehiculos(s));
            denegado(c, c.directivo, (cc, s) -> cc.archivos.importarVehiculos(s, "x.csv"));
        });
        r.prueba("PR-PERMISO-09", "login: mismo mensaje para usuario o clave incorrectos", () -> {
            Contexto c = new Contexto();
            CredencialesInvalidasException a = Verificar.lanza(CredencialesInvalidasException.class,
                    () -> c.autenticacion.iniciarSesion("noexiste", Contexto.CLAVE));
            CredencialesInvalidasException b = Verificar.lanza(CredencialesInvalidasException.class,
                    () -> c.autenticacion.iniciarSesion("vendedor", "otraclave1"));
            Verificar.igual(a.getMessage(), b.getMessage());
            Verificar.noLanza(() -> c.autenticacion.iniciarSesion(" VENDEDOR ", Contexto.CLAVE));
        });
        r.prueba("PR-PERMISO-10", "un usuario desactivado no entra ni opera", () -> {
            Contexto c = new Contexto();
            c.autenticacion.desactivar(c.admin, "vendedor");
            Verificar.lanza(CredencialesInvalidasException.class,
                    () -> c.autenticacion.iniciarSesion("vendedor", Contexto.CLAVE));
            Verificar.lanza(AccesoDenegadoException.class, () -> c.vehiculos.listar(c.vendedor,
                    new FiltroVehiculos(), OrdenVehiculos.TIPO_Y_MARCA));
        });
        r.prueba("PR-PERMISO-11", "siempre queda un administrador activo", () -> {
            Contexto c = new Contexto();
            Verificar.lanza(ReglaNegocioException.class,
                    () -> c.autenticacion.desactivar(c.admin, "admin"));
            Verificar.lanza(ReglaNegocioException.class,
                    () -> c.autenticacion.cambiarRol(c.admin, "admin", Rol.VENDEDOR));
            c.autenticacion.crearUsuario(c.admin, "admin2", "Otro Admin", Rol.ADMINISTRADOR,
                    Contexto.CLAVE);
            Verificar.noLanza(() -> c.autenticacion.cambiarRol(c.admin, "admin", Rol.VENDEDOR));
        });
        r.prueba("PR-PERMISO-12", "regla de clave y hash con sal", () -> {
            Verificar.lanza(DatoInvalidoException.class, () -> Claves.validar("corta1"));
            Verificar.lanza(DatoInvalidoException.class, () -> Claves.validar("sinnumeros"));
            Verificar.lanza(DatoInvalidoException.class, () -> Claves.validar("12345678"));
            Verificar.noLanza(() -> Claves.validar("clave1234"));
            String sal1 = Claves.generarSal();
            String sal2 = Claves.generarSal();
            Verificar.verdadero(!Claves.hashear("clave1234", sal1)
                    .equals(Claves.hashear("clave1234", sal2)), "misma clave, hashes distintos");
            Contexto c = new Contexto();
            Usuario u = c.repoUsuarios.buscar("vendedor").get();
            Verificar.verdadero(!u.getHashClave().contains("clave1234"), "no se guarda la clave");
        });
        r.prueba("PR-PERMISO-13", "cambiar la propia clave y blanquear la de otro", () -> {
            Contexto c = new Contexto();
            Verificar.lanza(DatoInvalidoException.class,
                    () -> c.autenticacion.cambiarClave(c.vendedor, "mal12345", "nueva12345"));
            c.autenticacion.cambiarClave(c.vendedor, Contexto.CLAVE, "nueva12345");
            Verificar.noLanza(() -> c.autenticacion.iniciarSesion("vendedor", "nueva12345"));
            c.autenticacion.blanquearClave(c.admin, "vendedor", "blanqueo99");
            Verificar.noLanza(() -> c.autenticacion.iniciarSesion("vendedor", "blanqueo99"));
            Verificar.lanza(DatoInvalidoException.class, () -> c.autenticacion.crearUsuario(
                    c.admin, "vendedor", "Repetido Pérez", Rol.VENDEDOR, Contexto.CLAVE));
        });
        r.prueba("PR-PERMISO-14", "el administrador inicial se crea una sola vez", () -> {
            Contexto c = new Contexto();
            Verificar.lanza(ReglaNegocioException.class, () ->
                    c.autenticacion.crearAdministradorInicial("otro", "Otro Admin", Contexto.CLAVE));
        });
    }
}
