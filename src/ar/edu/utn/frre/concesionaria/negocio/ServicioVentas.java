package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculos;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVentas;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.excepciones.VehiculoNoEncontradoException;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.modelo.DesglosePrecio;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vendible;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * Ventas e historial (CU-10 y CU-11).
 */
public class ServicioVentas extends ServicioBase {

    private final RepositorioVehiculos vehiculos;
    private final RepositorioVentas ventas;

    /**
     * @param vehiculos    repositorio de vehículos
     * @param ventas       repositorio de ventas
     * @param persistencia guardado
     */
    public ServicioVentas(RepositorioVehiculos vehiculos, RepositorioVentas ventas,
            Persistencia persistencia) {
        super(persistencia);
        this.vehiculos = vehiculos;
        this.ventas = ventas;
    }

    /**
     * Vende un vehículo (CU-10): todo o nada. Se congela el precio, se marca vendido, se
     * registra la venta y se guarda. Si el guardado falla, se deshace en memoria: nunca queda
     * un vehículo vendido sin venta, ni una venta sin vehículo vendido.
     *
     * @param sesion    la sesión del vendedor
     * @param codigo    código del vehículo
     * @param comprador el comprador
     * @return la venta, para imprimir el comprobante
     * @throws ConcesionariaException si no tiene permiso, no existe, no está disponible o no se
     *                                pudo guardar (en ese caso no cambió nada)
     */
    public Venta vender(Sesion sesion, String codigo, Comprador comprador)
            throws ConcesionariaException {
        sesion.exigir(Permiso.VENDER);
        Vehiculo vehiculo = vehiculos.buscar(ServicioVehiculos.normalizarCodigo(codigo))
                .orElseThrow(() -> new VehiculoNoEncontradoException(
                        "No hay ningún vehículo con el código " + codigo + "."));
        return vender(sesion, vehiculo, vehiculo.getTipo(), vehiculo.getMarca().getNombre(),
                comprador);
    }

    /**
     * El núcleo de la venta trabaja con {@link Vendible}, no con {@code Vehiculo}: es la
     * transmisión del comportamiento que pide el enunciado.
     */
    private Venta vender(Sesion sesion, Vendible vendible,
            ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo tipo, String marca,
            Comprador comprador) throws ConcesionariaException {
        LocalDate hoy = Reloj.hoy();
        DesglosePrecio precioCongelado = vendible.desglosarPrecio(hoy);
        vendible.marcarVendido();

        int numero = ventas.siguienteNumero();
        Venta venta = new Venta(numero, hoy, vendible.getCodigo(), vendible.getDescripcion(),
                tipo, marca, comprador, sesion.getNombreUsuario(), precioCongelado);
        ventas.guardar(venta);
        try {
            guardarCambios();
        } catch (PersistenciaException e) {
            ventas.eliminar(numero);
            ventas.liberarNumero(numero);
            vendible.revertirVenta();
            throw new PersistenciaException("No se pudo registrar la venta: " + e.getMessage()
                    + " La venta no se hizo; el vehículo sigue disponible.", e);
        }
        return venta;
    }

    /**
     * Historial de ventas (CU-11). Un vendedor ve solo las suyas, aunque pida otro vendedor.
     *
     * @param sesion   la sesión
     * @param desde    fecha inicial, o {@code null}
     * @param hasta    fecha final, o {@code null}
     * @param vendedor usuario vendedor, o {@code null} para todos
     * @return las ventas del período, en orden cronológico
     * @throws ConcesionariaException si no tiene permiso
     */
    public List<Venta> historial(Sesion sesion, LocalDate desde, LocalDate hasta, String vendedor)
            throws ConcesionariaException {
        String filtroVendedor = vendedor;
        if (!sesion.puede(Permiso.VER_VENTAS)) {
            sesion.exigir(Permiso.VER_VENTAS_PROPIAS);
            filtroVendedor = sesion.getNombreUsuario();
        }
        Predicate<Venta> condicion = enPeriodo(desde, hasta);
        if (filtroVendedor != null) {
            String quien = filtroVendedor;
            condicion = condicion.and(v -> quien.equalsIgnoreCase(v.getUsuarioVendedor()));
        }
        return ventas.listar().stream().filter(condicion).sorted().toList();
    }

    /**
     * @param desde fecha inicial, o {@code null}
     * @param hasta fecha final, o {@code null}
     * @return condición de que la venta esté en el período (extremos incluidos)
     */
    static Predicate<Venta> enPeriodo(LocalDate desde, LocalDate hasta) {
        return v -> (desde == null || !v.getFecha().isBefore(desde))
                && (hasta == null || !v.getFecha().isAfter(hasta));
    }

    /**
     * @param lista ventas
     * @return la suma de los precios finales
     */
    public static BigDecimal total(List<Venta> lista) {
        return lista.stream().map(Venta::getPrecioFinal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
