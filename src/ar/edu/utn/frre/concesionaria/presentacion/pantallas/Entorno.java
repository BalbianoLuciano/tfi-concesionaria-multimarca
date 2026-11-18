package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.negocio.ServicioArchivos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioAutenticacion;
import ar.edu.utn.frre.concesionaria.negocio.ServicioMarcas;
import ar.edu.utn.frre.concesionaria.negocio.ServicioReportes;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVentas;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Lector;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Terminal;
import java.util.function.Consumer;

/**
 * Lo que comparten todas las pantallas: la terminal, el lector y los servicios. Lo arma
 * {@code App} (inyección de dependencias a mano). La consola conoce al negocio, nunca a
 * {@code datos}: el registro de errores llega como un {@link Consumer}.
 */
public final class Entorno {

    final Terminal terminal;
    final Lector lector;
    final ServicioAutenticacion autenticacion;
    final ServicioVehiculos vehiculos;
    final ServicioVentas ventas;
    final ServicioMarcas marcas;
    final ServicioReportes reportes;
    final ServicioArchivos archivos;
    final Consumer<Throwable> registroErrores;

    /**
     * @param terminal        salida
     * @param lector          entrada
     * @param autenticacion   servicio de usuarios
     * @param vehiculos       servicio de stock
     * @param ventas          servicio de ventas
     * @param marcas          servicio de marcas
     * @param reportes        servicio de reportes
     * @param archivos        servicio de exportación e importación
     * @param registroErrores dónde anotar los errores no previstos
     */
    public Entorno(Terminal terminal, Lector lector, ServicioAutenticacion autenticacion,
            ServicioVehiculos vehiculos, ServicioVentas ventas, ServicioMarcas marcas,
            ServicioReportes reportes, ServicioArchivos archivos,
            Consumer<Throwable> registroErrores) {
        this.terminal = terminal;
        this.lector = lector;
        this.autenticacion = autenticacion;
        this.vehiculos = vehiculos;
        this.ventas = ventas;
        this.marcas = marcas;
        this.reportes = reportes;
        this.archivos = archivos;
        this.registroErrores = registroErrores;
    }
}
