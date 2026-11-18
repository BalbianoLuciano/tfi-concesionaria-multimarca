package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.negocio.ServicioReportes;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Estilo;
import ar.edu.utn.frre.concesionaria.presentacion.consola.GraficoBarras;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Lector;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Tabla;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reportes para directivos (CU-14). Cada reporte se arma como una lista de líneas con un
 * {@link Estilo}: con color para la pantalla y sin color para exportarlo a {@code .txt}.
 */
final class PantallaReportes extends Pantalla {

    private static final int ANCHO_BARRA = 30;

    /** Un reporte: arma sus líneas con el estilo que le pasen. */
    @FunctionalInterface
    private interface Reporte {
        List<String> armar(Estilo estilo) throws ConcesionariaException;
    }

    PantallaReportes(Entorno entorno, Sesion sesion) {
        super(entorno, sesion);
    }

    void mostrar() {
        List<Opcion> opciones = new ArrayList<>();
        agregar(opciones, "Stock por tipo y estado", false,
                () -> ver("Stock por tipo y estado", "stock-por-tipo", this::porTipo));
        agregar(opciones, "Stock por marca", false,
                () -> ver("Stock por marca", "stock-por-marca", this::porMarca));
        agregar(opciones, "Usados", false, () -> ver("Usados", "usados", this::usados));
        agregar(opciones, "Ventas por período", false, () -> {
            LocalDate[] periodo = leerPeriodo();
            ver("Ventas por período", "ventas-por-mes", es -> porMes(es, periodo));
        });
        agregar(opciones, "Ranking de vendedores", false, () -> {
            LocalDate[] periodo = leerPeriodo();
            ver("Ranking de vendedores", "ranking-vendedores", es -> ranking(es, periodo));
        });
        agregar(opciones, "Valorización del stock", false,
                () -> ver("Valorización del stock", "valorizacion", this::valorizacion));
        menu("Reportes", opciones);
    }

    /** Muestra el reporte y ofrece exportarlo. */
    private void ver(String titulo, String archivo, Reporte reporte)
            throws ConcesionariaException {
        cabecera("Reportes › " + titulo);
        t.titulo(titulo);
        t.info("Al " + Formatos.fecha(Reloj.hoy()));
        t.blanco();
        t.lineas(reporte.armar(estilo));
        t.pie("E exportar a .txt · Enter volver");
        String opcion = lector.linea("Opción").toLowerCase();
        if (opcion.equals("e")) {
            List<String> texto = new ArrayList<>();
            texto.add(" CONCESIONARIA MULTIMARCA · " + titulo);
            texto.add(" Al " + Formatos.fecha(Reloj.hoy()) + " · generado por "
                    + sesion.getNombreUsuario());
            texto.add("");
            texto.addAll(reporte.armar(Estilo.SIN_COLOR));
            String ruta = e.archivos.exportarReporte(sesion, "reporte-" + archivo,
                    String.join(System.lineSeparator(), texto));
            t.exito("Se exportó a " + ruta);
            lector.pausa();
        }
    }

    private LocalDate[] leerPeriodo() throws OperacionCancelada {
        t.info("Enter deja el período abierto.");
        LocalDate desde = lector.leerOpcional("Desde (dd/mm/aaaa)", Lector.fecha("Desde"));
        LocalDate hasta = lector.leerOpcional("Hasta (dd/mm/aaaa)", Lector.fecha("Hasta"));
        return new LocalDate[]{desde, hasta};
    }

    private static String periodo(LocalDate[] p) {
        if (p[0] == null && p[1] == null) {
            return "Todo el historial";
        }
        return "Del " + (p[0] == null ? "inicio" : Formatos.fecha(p[0])) + " al "
                + (p[1] == null ? "hoy" : Formatos.fecha(p[1]));
    }

    private List<String> porTipo(Estilo es) throws ConcesionariaException {
        Map<TipoVehiculo, Map<Estado, Long>> datos = e.reportes.stockPorTipoYEstado(sesion);
        long maximo = datos.values().stream().flatMap(m -> m.values().stream())
                .mapToLong(Long::longValue).max().orElse(0);
        List<String> l = new ArrayList<>();
        long total = 0;
        for (Map.Entry<TipoVehiculo, Map<Estado, Long>> fila : datos.entrySet()) {
            l.add(" " + es.negrita(fila.getKey().getEtiqueta()));
            for (Estado estado : Estado.values()) {
                long n = fila.getValue().get(estado);
                total += n;
                String barra = GraficoBarras.barra(n, maximo, ANCHO_BARRA);
                l.add(String.format("   %-6s ", estado.getEtiqueta())
                        + (estado == Estado.NUEVO ? es.verde(barra) : barra)
                        + String.format(" %4d", n));
            }
        }
        l.add("");
        l.add(" " + es.negrita("Total disponible: " + total + " vehículos"));
        return l;
    }

    private List<String> porMarca(Estilo es) throws ConcesionariaException {
        List<ServicioReportes.FilaMarca> filas = e.reportes.stockPorMarca(sesion);
        long maximo = filas.stream().mapToLong(ServicioReportes.FilaMarca::getCantidad).max()
                .orElse(0);
        Tabla tabla = new Tabla().texto("Marca").fija("Unidades").numero("Cant.")
                .numero("Valor (precio final)");
        BigDecimal total = BigDecimal.ZERO;
        for (ServicioReportes.FilaMarca f : filas) {
            tabla.fila(f.getMarca().getNombre(),
                    GraficoBarras.barra(f.getCantidad(), maximo, 20),
                    String.valueOf(f.getCantidad()), Formatos.moneda(f.getValor()));
            total = total.add(f.getValor());
        }
        List<String> l = new ArrayList<>(tabla.renglones(t.ancho(), es));
        l.add("");
        l.add(" " + es.negrita("Valor total: " + Formatos.moneda(total)));
        return l;
    }

    private List<String> usados(Estilo es) throws ConcesionariaException {
        ServicioReportes.ResumenUsados r = e.reportes.resumenUsados(sesion);
        List<String> l = new ArrayList<>();
        if (r.getCantidad() == 0) {
            l.add(" No hay usados disponibles.");
            return l;
        }
        Vehiculo mayor = r.getMayorKm();
        Vehiculo antiguo = r.getMasAntiguo();
        l.add(dato("Usados disponibles", String.valueOf(r.getCantidad()), es));
        l.add(dato("Kilometraje promedio", Formatos.km((int) Math.round(r.getKmPromedio())), es));
        l.add(dato("Mayor kilometraje", Formatos.km(mayor.getKilometraje()) + " · "
                + mayor.getCodigo() + " " + mayor.getDescripcion(), es));
        l.add(dato("Unidad más antigua", antiguo.getAnio() + " · " + antiguo.getCodigo() + " "
                + antiguo.getDescripcion(), es));
        l.add(dato("Antigüedad promedio", String.format(Formatos.ARGENTINA, "%.1f años",
                r.getAntiguedadPromedio()), es));
        return l;
    }

    private static String dato(String etiqueta, String valor, Estilo es) {
        return "   " + es.tenue(String.format("%-22s", etiqueta)) + " " + valor;
    }

    private List<String> porMes(Estilo es, LocalDate[] periodo) throws ConcesionariaException {
        TreeMap<YearMonth, ServicioReportes.Acumulado> datos =
                e.reportes.ventasPorMes(sesion, periodo[0], periodo[1]);
        List<String> l = new ArrayList<>();
        l.add(" " + es.tenue(periodo(periodo)));
        l.add("");
        if (datos.isEmpty()) {
            l.add(" No hay ventas en el período.");
            return l;
        }
        BigDecimal maximo = datos.values().stream().map(ServicioReportes.Acumulado::getMonto)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        Tabla tabla = new Tabla().fija("Mes").fija("Recaudación").numero("Unid.").numero("Monto");
        long unidades = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<YearMonth, ServicioReportes.Acumulado> f : datos.entrySet()) {
            ServicioReportes.Acumulado a = f.getValue();
            tabla.fila(Formatos.mes(f.getKey()),
                    GraficoBarras.barra(a.getMonto().doubleValue(), maximo.doubleValue(), 24),
                    String.valueOf(a.getUnidades()), Formatos.moneda(a.getMonto()));
            unidades += a.getUnidades();
            total = total.add(a.getMonto());
        }
        l.addAll(tabla.renglones(t.ancho(), es));
        l.add("");
        l.add(" " + es.negrita(unidades + " unidades · " + Formatos.moneda(total)));
        return l;
    }

    private List<String> ranking(Estilo es, LocalDate[] periodo) throws ConcesionariaException {
        List<ServicioReportes.Acumulado> filas = e.reportes.rankingVendedores(sesion, periodo[0],
                periodo[1]);
        List<String> l = new ArrayList<>();
        l.add(" " + es.tenue(periodo(periodo)));
        l.add("");
        if (filas.isEmpty()) {
            l.add(" No hay ventas en el período.");
            return l;
        }
        Tabla tabla = new Tabla().numero("#").fija("Vendedor").numero("Unidades").numero("Monto");
        int puesto = 1;
        for (ServicioReportes.Acumulado a : filas) {
            tabla.fila(String.valueOf(puesto++), a.getClave(), String.valueOf(a.getUnidades()),
                    Formatos.moneda(a.getMonto()));
        }
        l.addAll(tabla.renglones(t.ancho(), es));
        return l;
    }

    private List<String> valorizacion(Estilo es) throws ConcesionariaException {
        ServicioReportes.Valorizacion v = e.reportes.valorizacion(sesion);
        List<String> l = new ArrayList<>();
        l.add(dato("Unidades disponibles", String.valueOf(v.getCantidad()), es));
        l.add(dato("Suma de precios de lista", Formatos.moneda(v.getSumaLista()), es));
        l.add(dato("Suma de precios finales", Formatos.moneda(v.getSumaFinal()), es));
        l.add(dato("Diferencia", Formatos.moneda(v.getSumaFinal().subtract(v.getSumaLista())), es));
        l.add("");
        l.add(" " + es.tenue("El precio final descuenta la depreciación de los usados y suma"
                + " recargos y patentamiento."));
        return l;
    }
}
