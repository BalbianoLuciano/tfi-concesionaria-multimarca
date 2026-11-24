package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.datos.Almacen;
import ar.edu.utn.frre.concesionaria.datos.ArchivosCsv;
import ar.edu.utn.frre.concesionaria.datos.DatosDemo;
import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcasMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioUsuariosMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculosMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVentasMemoria;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.modelo.Rol;
import ar.edu.utn.frre.concesionaria.negocio.ServicioArchivos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioAutenticacion;
import ar.edu.utn.frre.concesionaria.negocio.ServicioMarcas;
import ar.edu.utn.frre.concesionaria.negocio.ServicioReportes;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVentas;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import java.nio.file.Path;

/**
 * Todo el negocio armado en memoria, como lo arma {@code App} pero sin archivo: la
 * persistencia es falsa y se le puede pedir que falle. Es la prueba de que el negocio no
 * depende del disco (ARQUITECTURA §1).
 */
public final class Contexto {

    /** Persistencia que cuenta guardados y puede fallar a pedido. */
    public static final class PersistenciaFalsa implements Persistencia {
        /** Cuántas veces se guardó. */
        public int guardados;
        /** Si es {@code true}, el próximo guardado falla. */
        public boolean fallar;
        private boolean pendientes;

        @Override
        public void guardar() throws PersistenciaException {
            if (fallar) {
                throw new PersistenciaException("Disco lleno (simulado).");
            }
            guardados++;
            pendientes = false;
        }

        @Override
        public boolean hayCambiosPendientes() {
            return pendientes;
        }

        @Override
        public void marcarCambio() {
            pendientes = true;
        }
    }

    /** Clave de todos los usuarios de prueba. */
    public static final String CLAVE = "clave1234";

    public final Almacen almacen = new Almacen();
    public final PersistenciaFalsa persistencia = new PersistenciaFalsa();
    public final RepositorioVehiculosMemoria repoVehiculos = new RepositorioVehiculosMemoria(almacen);
    public final RepositorioMarcasMemoria repoMarcas = new RepositorioMarcasMemoria(almacen);
    public final RepositorioVentasMemoria repoVentas = new RepositorioVentasMemoria(almacen);
    public final RepositorioUsuariosMemoria repoUsuarios = new RepositorioUsuariosMemoria(almacen);
    public final ServicioAutenticacion autenticacion = new ServicioAutenticacion(repoUsuarios,
            persistencia, new DatosDemo(repoMarcas, repoVehiculos));
    public final ServicioMarcas marcas = new ServicioMarcas(repoMarcas, repoVehiculos, persistencia);
    public final ServicioVehiculos vehiculos = new ServicioVehiculos(repoVehiculos, marcas,
            persistencia);
    public final ServicioVentas ventas = new ServicioVentas(repoVehiculos, repoVentas, persistencia);
    public final ServicioReportes reportes = new ServicioReportes(repoVehiculos, repoVentas);
    public final ServicioArchivos archivos;

    /** Sesiones de cada rol. */
    public final Sesion admin;
    public final Sesion vendedor;
    public final Sesion directivo;

    /**
     * Arma el contexto con los datos de demostración y un usuario de cada rol.
     *
     * @param carpeta carpeta para los archivos CSV (puede ser {@code null} si no se usan)
     * @throws Exception no debería pasar
     */
    public Contexto(Path carpeta) throws Exception {
        Fabrica.fijarHoy();
        archivos = new ServicioArchivos(new ArchivosCsv(carpeta == null ? Path.of(".") : carpeta),
                repoVehiculos, repoMarcas, repoVentas, vehiculos, persistencia);
        admin = autenticacion.crearAdministradorInicial("admin", "Ana Admin", CLAVE);
        autenticacion.cargarDatosDemo(admin);
        autenticacion.crearUsuario(admin, "vendedor", "Victor Vendedor", Rol.VENDEDOR, CLAVE);
        autenticacion.crearUsuario(admin, "vendedor2", "Vera Vendedora", Rol.VENDEDOR, CLAVE);
        autenticacion.crearUsuario(admin, "directivo", "Dora Directiva", Rol.DIRECTIVO, CLAVE);
        vendedor = autenticacion.iniciarSesion("vendedor", CLAVE);
        directivo = autenticacion.iniciarSesion("directivo", CLAVE);
    }

    /**
     * @throws Exception no debería pasar
     */
    public Contexto() throws Exception {
        this(null);
    }
}
