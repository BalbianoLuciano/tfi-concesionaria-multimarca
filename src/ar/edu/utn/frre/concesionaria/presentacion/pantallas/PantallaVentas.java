package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
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
            agregar(opciones, "Vender", true, this::vender);
        }
        agregar(opciones, sesion.puede(Permiso.VER_VENTAS) ? "Historial de ventas"
                : "Mis ventas", true, this::historial);
        menu("Ventas", opciones);
    }

    private void vender() throws ConcesionariaException, OperacionCancelada {
        lector.operacion("la venta");
        t.info("Escribí el código del vehículo o una búsqueda (marca, modelo, patente). :q cancela.");
        Vehiculo v = elegirDisponible();
        t.blanco();
        t.titulo(v.getCodigo() + " · " + v.getDescripcion());
        t.info(v.getTipo().getEtiqueta() + " · " + v.getDetalleTipo() + " · "
                + v.getEstado().getEtiqueta());
        t.blanco();
        t.lineas(Vistas.desglose(v.desglosarPrecio(Reloj.hoy()), v.getTipo(), estilo));
        t.blanco();
        t.titulo("Comprador");
        String nombre = lector.leer("Nombre y apellido",
                n -> Comprador.validarNombre("Nombre y apellido", n));
        String dni = lector.leer("DNI", Comprador::validarDni);
        Comprador comprador = new Comprador(nombre, dni);
        t.blanco();
        if (!lector.confirmar("¿Confirmar la venta de " + v.getCodigo() + " a " + nombre + " por "
                + Formatos.moneda(v.calcularPrecioFinal(Reloj.hoy())) + "?")) {
            t.info("No se vendió nada.");
            return;
        }
        Venta venta = e.ventas.vender(sesion, v.getCodigo(), comprador);
        t.blanco();
        t.exito("Venta registrada.");
        t.blanco();
        t.lineas(Vistas.comprobante(venta, estilo));
    }

    /** Código directo, o búsqueda entre los disponibles. */
    private Vehiculo elegirDisponible() throws ConcesionariaException, OperacionCancelada {
        List<Vehiculo> candidatos = lector.leer("Vehículo", texto -> {
            List<Vehiculo> encontrados;
            try {
                encontrados = e.vehiculos.buscarTexto(sesion, texto).stream()
                        .filter(Vehiculo::estaDisponible).toList();
            } catch (ConcesionariaException ex) {
                throw new DatoInvalidoException("Vehículo", texto, ex.getMessage());
            }
            if (encontrados.isEmpty()) {
                throw new DatoInvalidoException("Vehículo", texto, "No hay vehículos disponibles"
                        + " con \"" + texto + "\". Probá con el código o con la marca.");
            }
            return encontrados;
        });
        if (candidatos.size() == 1) {
            return candidatos.get(0);
        }
        t.blanco();
        return lector.elegir("Vehículo", candidatos, v -> v.getCodigo() + "  "
                + v.getDescripcion() + "  " + Formatos.moneda(v.calcularPrecioFinal(Reloj.hoy())));
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
