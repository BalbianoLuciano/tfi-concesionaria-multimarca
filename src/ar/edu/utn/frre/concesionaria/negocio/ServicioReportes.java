package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculos;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVentas;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Reportes para directivos (CU-14). Calculan; no imprimen: la consola arma las tablas y los
 * gráficos con lo que devuelven. Cada reporte es una sola pasada con
 * {@code Collectors.groupingBy} (ARQUITECTURA §7).
 */
public class ServicioReportes {

    /** Una fila de "stock por marca". */
    public static final class FilaMarca {
        private final Marca marca;
        private final long cantidad;
        private final BigDecimal valor;

        FilaMarca(Marca marca, long cantidad, BigDecimal valor) {
            this.marca = marca;
            this.cantidad = cantidad;
            this.valor = valor;
        }

        /** @return la marca */
        public Marca getMarca() {
            return marca;
        }

        /** @return cantidad de unidades disponibles */
        public long getCantidad() {
            return cantidad;
        }

        /** @return suma de precios finales */
        public BigDecimal getValor() {
            return valor;
        }
    }

    /** Resumen de los usados disponibles. */
    public static final class ResumenUsados {
        private final long cantidad;
        private final double kmPromedio;
        private final Vehiculo mayorKm;
        private final Vehiculo masAntiguo;
        private final double antiguedadPromedio;

        ResumenUsados(long cantidad, double kmPromedio, Vehiculo mayorKm, Vehiculo masAntiguo,
                double antiguedadPromedio) {
            this.cantidad = cantidad;
            this.kmPromedio = kmPromedio;
            this.mayorKm = mayorKm;
            this.masAntiguo = masAntiguo;
            this.antiguedadPromedio = antiguedadPromedio;
        }

        /** @return cuántos usados hay */
        public long getCantidad() {
            return cantidad;
        }

        /** @return kilometraje promedio */
        public double getKmPromedio() {
            return kmPromedio;
        }

        /** @return el de mayor kilometraje, o {@code null} */
        public Vehiculo getMayorKm() {
            return mayorKm;
        }

        /** @return el más antiguo, o {@code null} */
        public Vehiculo getMasAntiguo() {
            return masAntiguo;
        }

        /** @return antigüedad promedio en años */
        public double getAntiguedadPromedio() {
            return antiguedadPromedio;
        }
    }

    /** Unidades y monto acumulados (por mes o por vendedor). */
    public static final class Acumulado {
        private final String clave;
        private final long unidades;
        private final BigDecimal monto;

        Acumulado(String clave, long unidades, BigDecimal monto) {
            this.clave = clave;
            this.unidades = unidades;
            this.monto = monto;
        }

        /** @return el vendedor (en el ranking) o vacío (en el reporte por mes) */
        public String getClave() {
            return clave;
        }

        /** @return unidades vendidas */
        public long getUnidades() {
            return unidades;
        }

        /** @return monto recaudado */
        public BigDecimal getMonto() {
            return monto;
        }
    }

    /** Valorización del stock disponible. */
    public static final class Valorizacion {
        private final long cantidad;
        private final BigDecimal sumaLista;
        private final BigDecimal sumaFinal;

        Valorizacion(long cantidad, BigDecimal sumaLista, BigDecimal sumaFinal) {
            this.cantidad = cantidad;
            this.sumaLista = sumaLista;
            this.sumaFinal = sumaFinal;
        }

        /** @return unidades disponibles */
        public long getCantidad() {
            return cantidad;
        }

        /** @return suma de precios de lista */
        public BigDecimal getSumaLista() {
            return sumaLista;
        }

        /** @return suma de precios finales */
        public BigDecimal getSumaFinal() {
            return sumaFinal;
        }
    }

    private final RepositorioVehiculos vehiculos;
    private final RepositorioVentas ventas;

    /**
     * @param vehiculos repositorio de vehículos
     * @param ventas    repositorio de ventas
     */
    public ServicioReportes(RepositorioVehiculos vehiculos, RepositorioVentas ventas) {
        this.vehiculos = vehiculos;
        this.ventas = ventas;
    }

    private List<Vehiculo> disponibles() {
        return vehiculos.listar().stream().filter(Vehiculo::estaDisponible).toList();
    }

    /**
     * Stock disponible por tipo y estado.
     *
     * @param sesion la sesión
     * @return tipo → (estado → cantidad), con todos los tipos y estados aunque estén en cero
     * @throws ConcesionariaException si no tiene permiso
     */
    public Map<TipoVehiculo, Map<Estado, Long>> stockPorTipoYEstado(Sesion sesion)
            throws ConcesionariaException {
        sesion.exigir(Permiso.VER_REPORTES);
        Map<TipoVehiculo, Map<Estado, Long>> resultado = new EnumMap<>(TipoVehiculo.class);
        for (TipoVehiculo t : TipoVehiculo.values()) {
            Map<Estado, Long> porEstado = new EnumMap<>(Estado.class);
            for (Estado e : Estado.values()) {
                porEstado.put(e, 0L);
            }
            resultado.put(t, porEstado);
        }
        disponibles().forEach(v -> resultado.get(v.getTipo()).merge(v.getEstado(), 1L, Long::sum));
        return resultado;
    }

    /**
     * Stock disponible por marca, de mayor a menor cantidad.
     *
     * @param sesion la sesión
     * @return las filas
     * @throws ConcesionariaException si no tiene permiso
     */
    public List<FilaMarca> stockPorMarca(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.VER_REPORTES);
        LocalDate hoy = Reloj.hoy();
        Map<Marca, List<Vehiculo>> porMarca = disponibles().stream()
                .collect(Collectors.groupingBy(Vehiculo::getMarca));
        return porMarca.entrySet().stream()
                .map(e -> new FilaMarca(e.getKey(), e.getValue().size(), e.getValue().stream()
                        .map(v -> v.calcularPrecioFinal(hoy))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparingLong(FilaMarca::getCantidad).reversed()
                        .thenComparing(FilaMarca::getValor, Comparator.reverseOrder()))
                .toList();
    }

    /**
     * Resumen de usados disponibles.
     *
     * @param sesion la sesión
     * @return el resumen (con cantidad 0 si no hay)
     * @throws ConcesionariaException si no tiene permiso
     */
    public ResumenUsados resumenUsados(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.VER_REPORTES);
        int anioActual = Reloj.anioActual();
        List<Vehiculo> usados = disponibles().stream()
                .filter(v -> v.getEstado() == Estado.USADO).toList();
        return new ResumenUsados(usados.size(),
                usados.stream().mapToInt(Vehiculo::getKilometraje).average().orElse(0),
                usados.stream().max(Comparator.comparing(Vehiculo::getKilometraje)).orElse(null),
                usados.stream().min(Comparator.comparing(Vehiculo::getAnio)).orElse(null),
                usados.stream().mapToInt(v -> anioActual - v.getAnio()).average().orElse(0));
    }

    /**
     * Ventas por mes del período.
     *
     * @param sesion la sesión
     * @param desde  fecha inicial, o {@code null}
     * @param hasta  fecha final, o {@code null}
     * @return mes → acumulado, en orden cronológico
     * @throws ConcesionariaException si no tiene permiso
     */
    public TreeMap<YearMonth, Acumulado> ventasPorMes(Sesion sesion, LocalDate desde,
            LocalDate hasta) throws ConcesionariaException {
        sesion.exigir(Permiso.VER_REPORTES);
        Map<YearMonth, List<Venta>> porMes = ventas.listar().stream()
                .filter(ServicioVentas.enPeriodo(desde, hasta))
                .collect(Collectors.groupingBy(v -> YearMonth.from(v.getFecha())));
        TreeMap<YearMonth, Acumulado> resultado = new TreeMap<>();
        porMes.forEach((mes, lista) -> resultado.put(mes,
                new Acumulado("", lista.size(), ServicioVentas.total(lista))));
        return resultado;
    }

    /**
     * Ranking de vendedores del período, por monto.
     *
     * @param sesion la sesión
     * @param desde  fecha inicial, o {@code null}
     * @param hasta  fecha final, o {@code null}
     * @return una fila por vendedor, de mayor a menor monto
     * @throws ConcesionariaException si no tiene permiso
     */
    public List<Acumulado> rankingVendedores(Sesion sesion, LocalDate desde, LocalDate hasta)
            throws ConcesionariaException {
        sesion.exigir(Permiso.VER_REPORTES);
        Map<String, List<Venta>> porVendedor = ventas.listar().stream()
                .filter(ServicioVentas.enPeriodo(desde, hasta))
                .collect(Collectors.groupingBy(Venta::getUsuarioVendedor));
        return porVendedor.entrySet().stream()
                .map(e -> new Acumulado(e.getKey(), e.getValue().size(),
                        ServicioVentas.total(e.getValue())))
                .sorted(Comparator.comparing(Acumulado::getMonto).reversed()
                        .thenComparing(Acumulado::getClave))
                .toList();
    }

    /**
     * Valorización del stock disponible.
     *
     * @param sesion la sesión
     * @return cantidad, suma de precios de lista y suma de precios finales
     * @throws ConcesionariaException si no tiene permiso
     */
    public Valorizacion valorizacion(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.VER_REPORTES);
        LocalDate hoy = Reloj.hoy();
        List<Vehiculo> lista = disponibles();
        return new Valorizacion(lista.size(),
                lista.stream().map(Vehiculo::getPrecioLista).reduce(BigDecimal.ZERO, BigDecimal::add),
                lista.stream().map(v -> v.calcularPrecioFinal(hoy))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
