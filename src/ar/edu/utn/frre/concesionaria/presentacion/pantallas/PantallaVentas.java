package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import ar.edu.utn.frre.concesionaria.negocio.FiltroVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.OrdenVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVentas;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Lector;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Tabla;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventas (CU-10) e historial (CU-11).
 */
final class PantallaVentas extends Pantalla {

    PantallaVentas(Entorno entorno, Sesion sesion) {
        super(entorno, sesion);
    }

    void mostrar() {
        List<Opcion> opciones = new ArrayList<>();
        if (sesion.puede(Permiso.VENDER)) {
            agregar(opciones, "Vender", "precio, comprador y comprobante", true, this::vender);
        }
        boolean todas = sesion.puede(Permiso.VER_VENTAS);
        agregar(opciones, todas ? "Historial de ventas" : "Mis ventas",
                todas ? "por período y vendedor, con total" : "las que registraste vos, con total",
                true, this::historial);
        menu("Ventas", () -> {
            List<Venta> ventas = e.ventas.historial(sesion, null, null, null);
            boolean una = ventas.size() == 1;
            String cuantas = una ? "1 venta" : ventas.size() + " ventas";
            String quienes = todas ? (una ? " registrada" : " registradas")
                    : (una ? " tuya" : " tuyas");
            return cuantas + quienes + " · total "
                    + Formatos.moneda(ServicioVentas.total(ventas));
        }, opciones);
    }

    private void vender() throws ConcesionariaException, OperacionCancelada {
        lector.operacion("la venta");
        t.info("Son 3 pasos. :q en cualquier campo cancela sin vender nada.");
        t.paso(1, 3, "Vehículo");
        Vehiculo v = elegirDisponible();
        t.blanco();
        t.lineas(Vistas.ficha(v, estilo));
        t.blanco();
        t.lineas(Vistas.desglose(v.desglosarPrecio(Reloj.hoy()), v.getTipo(), estilo));

        t.paso(2, 3, "Comprador");
        t.ayuda("Nombre y apellido como figuran en el DNI; el DNI, con o sin puntos.");
        String nombre = lector.leer("Nombre y apellido",
                n -> Comprador.validarNombre("Nombre y apellido", n));
        String dni = lector.leer("DNI", Comprador::validarDni);
        Comprador comprador = new Comprador(nombre, dni);

        t.paso(3, 3, "Confirmación");
        t.ayuda("El precio queda congelado en la venta: si después cambia, la venta no cambia.");
        if (!lector.confirmar("¿Confirmar la venta de " + v.getCodigo() + " a " + nombre + " por "
                + Formatos.moneda(v.calcularPrecioFinal(Reloj.hoy())) + "?")) {
            t.info("No se vendió nada.");
            return;
        }
        Venta venta = e.ventas.vender(sesion, v.getCodigo(), comprador);
        t.exito("Venta registrada.");
        t.blanco();
        t.lineas(Vistas.comprobante(venta, estilo));
    }

    /** Elige entre los disponibles con el selector de SPEC §5. */
    private Vehiculo elegirDisponible() throws ConcesionariaException, OperacionCancelada {
        return elegirVehiculo(e.vehiculos.listar(sesion, new FiltroVehiculos(),
                OrdenVehiculos.TIPO_Y_MARCA));
    }

    private void historial() throws ConcesionariaException, OperacionCancelada {
        t.info("Enter deja cada filtro vacío.");
        LocalDate desde = lector.leerOpcional("Desde (dd/mm/aaaa)", Lector.fecha("Desde"));
        LocalDate hasta = lector.leerOpcional("Hasta (dd/mm/aaaa)", Lector.fecha("Hasta"));
        String vendedor = null;
        if (sesion.puede(Permiso.VER_VENTAS)) {
            vendedor = lector.leerOpcional("Vendedor (usuario)", s -> s.toLowerCase());
        }
        List<Venta> ventas = e.ventas.historial(sesion, desde, hasta, vendedor);
        t.blanco();
        if (ventas.isEmpty()) {
            t.linea(" No hay ventas con esos filtros.");
            return;
        }
        Tabla tabla = new Tabla().numero("N.º").fija("Fecha").fija("Código").texto("Vehículo")
                .texto("Comprador").fija("Vendedor").numero("Precio final");
        for (Venta v : ventas) {
            tabla.fila(String.valueOf(v.getNumero()), Formatos.fecha(v.getFecha()),
                    v.getCodigoVehiculo(), v.getDescripcionVehiculo(),
                    v.getComprador().getNombreCompleto(), v.getUsuarioVendedor(),
                    Formatos.moneda(v.getPrecioFinal()));
        }
        t.lineas(tabla.renglones(t.ancho(), estilo));
        t.blanco();
        t.linea(" " + estilo.negrita(ventas.size() + " venta(s) · total recaudado "
                + Formatos.moneda(ServicioVentas.total(ventas))));
    }
}
