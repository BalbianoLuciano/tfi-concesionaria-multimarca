package ar.edu.utn.frre.concesionaria;

import ar.edu.utn.frre.concesionaria.datos.Almacen;
import ar.edu.utn.frre.concesionaria.datos.AlmacenArchivo;
import ar.edu.utn.frre.concesionaria.datos.ArchivosCsv;
import ar.edu.utn.frre.concesionaria.datos.DatosDemo;
import ar.edu.utn.frre.concesionaria.datos.RegistroErrores;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcasMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioUsuariosMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculosMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVentasMemoria;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.negocio.ServicioArchivos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioAutenticacion;
import ar.edu.utn.frre.concesionaria.negocio.ServicioMarcas;
import ar.edu.utn.frre.concesionaria.negocio.ServicioReportes;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVentas;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Estilo;
import ar.edu.utn.frre.concesionaria.presentacion.consola.FinDeEntrada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Lector;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Terminal;
import ar.edu.utn.frre.concesionaria.presentacion.pantallas.Entorno;
import ar.edu.utn.frre.concesionaria.presentacion.pantallas.MenuPrincipal;
import ar.edu.utn.frre.concesionaria.presentacion.pantallas.PantallaInicio;
import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Punto de entrada. Es la única clase que conoce todas las piezas: crea los repositorios, se
 * los pasa a los servicios y pasa los servicios a la consola (inyección de dependencias a
 * mano, sin framework; ARQUITECTURA §1).
 *
 * <p>Uso: {@code java [-Dconcesionaria.dir=<carpeta>] -jar concesionaria.jar [--sin-color]}</p>
 */
public final class App {

    /** Versión del programa. */
    public static final String VERSION = "1.0";

    private App() {
    }

    /**
     * @param args {@code --sin-color} para desactivar los colores; {@code --ayuda} para ver el
     *             uso
     */
    public static void main(String[] args) {
        List<String> argumentos = Arrays.asList(args);
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        if (argumentos.contains("--ayuda") || argumentos.contains("-h")) {
            out.println("Concesionaria multimarca " + VERSION);
            out.println("Uso: java [-Dconcesionaria.dir=<carpeta>] -jar concesionaria.jar"
                    + " [--sin-color]");
            out.println("  --sin-color   sin códigos de color (también con la variable NO_COLOR)");
            out.println("  Datos en ~/.concesionaria/ salvo que se indique otra carpeta.");
            return;
        }

        Console consola = System.console();
        boolean interactivo = consola != null && consola.isTerminal();
        boolean color = interactivo && System.getenv("NO_COLOR") == null
                && !argumentos.contains("--sin-color");
        Terminal terminal = new Terminal(out, new Estilo(color), Terminal.anchoDelEntorno());
        Lector lector = new Lector(new BufferedReader(new InputStreamReader(System.in,
                StandardCharsets.UTF_8)), terminal, interactivo);

        Path carpeta = carpetaDeDatos();
        AlmacenArchivo archivo = new AlmacenArchivo(carpeta);
        Almacen almacen;
        try {
            almacen = archivo.cargar();
        } catch (PersistenciaException e) {
            terminal.error(e.getMessage());
            System.exit(1);
            return;
        }
        if (archivo.getEstadoCarga() == AlmacenArchivo.EstadoCarga.RECUPERADO) {
            terminal.aviso("El archivo de datos estaba dañado: se recuperó la copia anterior."
                    + " El dañado quedó como datos.ser.corrupto.");
        } else if (archivo.getEstadoCarga() == AlmacenArchivo.EstadoCarga.MIGRADO) {
            terminal.aviso("Los datos eran de una versión anterior y se actualizaron.");
        }

        RepositorioVehiculosMemoria vehiculos = new RepositorioVehiculosMemoria(almacen);
        RepositorioMarcasMemoria marcas = new RepositorioMarcasMemoria(almacen);
        RepositorioVentasMemoria ventas = new RepositorioVentasMemoria(almacen);
        RepositorioUsuariosMemoria usuarios = new RepositorioUsuariosMemoria(almacen);

        ServicioVehiculos servicioVehiculos = new ServicioVehiculos(vehiculos, archivo);
        Entorno entorno = new Entorno(terminal, lector,
                new ServicioAutenticacion(usuarios, archivo, new DatosDemo(marcas, vehiculos)),
                servicioVehiculos,
                new ServicioVentas(vehiculos, ventas, archivo),
                new ServicioMarcas(marcas, vehiculos, archivo),
                new ServicioReportes(vehiculos, ventas),
                new ServicioArchivos(new ArchivosCsv(carpeta), vehiculos, marcas, ventas,
                        servicioVehiculos, archivo),
                new RegistroErrores(carpeta));

        // SPEC §7: el shutdown hook guarda si el usuario cierra con Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> guardarAlSalir(archivo, null)));

        try {
            while (true) {
                Sesion sesion = new PantallaInicio(entorno, carpeta.toString()).iniciar();
                if (sesion == null) {
                    break;
                }
                if (new MenuPrincipal(entorno, sesion).mostrar() == MenuPrincipal.Salida.SALIR) {
                    break;
                }
            }
        } catch (FinDeEntrada e) {
            terminal.blanco();
        }
        guardarAlSalir(archivo, terminal);
        terminal.blanco();
        terminal.info("Hasta luego.");
    }

    /**
     * CU-17: guarda si hay cambios pendientes.
     *
     * @param archivo  el almacén
     * @param terminal dónde avisar, o {@code null} desde el shutdown hook
     */
    private static synchronized void guardarAlSalir(AlmacenArchivo archivo, Terminal terminal) {
        if (!archivo.hayCambiosPendientes()) {
            return;
        }
        try {
            archivo.guardar();
            if (terminal != null) {
                terminal.exito("Se guardaron los cambios pendientes.");
            }
        } catch (PersistenciaException e) {
            if (terminal != null) {
                terminal.error(e.getMessage());
            } else {
                System.err.println("No se pudieron guardar los cambios: " + e.getMessage());
            }
        }
    }

    /**
     * @return la carpeta de {@code -Dconcesionaria.dir}, o {@code ~/.concesionaria}
     */
    static Path carpetaDeDatos() {
        String propiedad = System.getProperty("concesionaria.dir");
        if (propiedad != null && !propiedad.isBlank()) {
            return Path.of(propiedad).toAbsolutePath();
        }
        return Path.of(System.getProperty("user.home"), ".concesionaria");
    }
}
