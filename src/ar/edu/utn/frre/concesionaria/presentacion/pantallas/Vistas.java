package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.modelo.DesglosePrecio;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Situacion;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Estilo;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Tabla;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Bloques que se repiten en varias pantallas: la tabla del stock, la ficha, el desglose del
 * precio y el comprobante de venta. Devuelven líneas, así se pueden mostrar o exportar.
 */
final class Vistas {

    private static final int ANCHO_BLOQUE = 60;

    private Vistas() {
    }

    /**
     * Tabla del stock.
     *
     * @param lista        los vehículos
     * @param estilo       con o sin color
     * @param conSituacion si se agrega la columna de situación (búsquedas)
     * @return la tabla, para agregarle filas o mostrarla
     */
    static Tabla tablaStock(List<Vehiculo> lista, Estilo estilo, boolean conSituacion) {
        return tablaStock(lista, estilo, conSituacion, null);
    }

    /**
     * Tabla de stock agrupada por tipo (SPEC §5), con un subtítulo por grupo que dice cuántas
     * unidades hay y cuánto valen. La lista tiene que venir ordenada por tipo.
     *
     * @param lista        los vehículos de la página
     * @param estilo       con o sin color
     * @param conSituacion si agrega la columna de situación
     * @param todos        todos los vehículos del listado, para los totales de cada grupo, o
     *                     {@code null} para no agrupar
     * @return la tabla
     */
    static Tabla tablaStock(List<Vehiculo> lista, Estilo estilo, boolean conSituacion,
            List<Vehiculo> todos) {
        Tabla tabla = new Tabla().fija("Código").texto("Marca").texto("Modelo").numero("Año")
                .fija("Estado").numero("Km").texto("Detalle");
        if (conSituacion) {
            tabla.fija("Situación");
        }
        tabla.numero("Precio final");
        LocalDate hoy = Reloj.hoy();
        TipoVehiculo grupoActual = null;
        for (Vehiculo v : lista) {
            if (todos != null && v.getTipo() != grupoActual) {
                grupoActual = v.getTipo();
                tabla.grupo(subtitulo(grupoActual, todos, hoy, estilo));
            }
            List<String> celdas = new ArrayList<>(List.of(v.getCodigo(), v.getMarca().getNombre(),
                    v.getModelo(), String.valueOf(v.getAnio()), estado(v.getEstado(), estilo),
                    v.getEstado() == Estado.NUEVO ? "—" : Formatos.miles(v.getKilometraje()),
                    v.getDetalleTipo()));
            if (conSituacion) {
                celdas.add(situacion(v.getSituacion(), estilo));
            }
            celdas.add(Formatos.moneda(v.calcularPrecioFinal(hoy)));
            tabla.fila(celdas.toArray(new String[0]));
        }
        return tabla;
    }

    private static String subtitulo(TipoVehiculo tipo, List<Vehiculo> todos, LocalDate hoy,
            Estilo estilo) {
        List<Vehiculo> delTipo = todos.stream().filter(v -> v.getTipo() == tipo).toList();
        BigDecimal valor = delTipo.stream().map(v -> v.calcularPrecioFinal(hoy))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String plural = tipo.getPlural();
        return estilo.titulo(Character.toUpperCase(plural.charAt(0)) + plural.substring(1))
                + estilo.tenue(" · " + delTipo.size() + (delTipo.size() == 1 ? " unidad"
                : " unidades") + " · " + Formatos.moneda(valor));
    }

    /**
     * Tarjetas de resumen una al lado de la otra: título tenue, valor en negrita y detalle.
     *
     * @param tarjetas cada una con {título, valor, detalle}
     * @param estilo   con o sin color
     * @param ancho    ancho de cada tarjeta
     * @return las tres líneas
     */
    static List<String> tarjetas(List<String[]> tarjetas, Estilo estilo, int ancho) {
        StringBuilder[] l = {new StringBuilder("   "), new StringBuilder("   "),
                new StringBuilder("   ")};
        for (String[] tarjeta : tarjetas) {
            l[0].append(relleno(estilo.tenue(tarjeta[0]), ancho));
            l[1].append(relleno(estilo.negrita(tarjeta[1]), ancho));
            l[2].append(relleno(estilo.tenue(tarjeta[2]), ancho));
        }
        List<String> salida = new ArrayList<>();
        for (StringBuilder b : l) {
            salida.add(b.toString().stripTrailing());
        }
        return salida;
    }

    private static String relleno(String texto, int ancho) {
        return texto + " ".repeat(Math.max(2, ancho - Estilo.largoVisible(texto)));
    }

    /**
     * @param estado nuevo o usado
     * @param estilo con o sin color
     * @return "0 km" en verde, o "Usado"
     */
    static String estado(Estado estado, Estilo estilo) {
        return estado == Estado.NUEVO ? estilo.verde(estado.getEtiqueta()) : estado.getEtiqueta();
    }

    /**
     * @param situacion la situación
     * @param estilo    con o sin color
     * @return la situación; lo que no está disponible, tenue
     */
    static String situacion(Situacion situacion, Estilo estilo) {
        return situacion == Situacion.DISPONIBLE ? situacion.getEtiqueta()
                : estilo.tenue(situacion.getEtiqueta());
    }

    /**
     * Ficha del vehículo (CU-06), sin el desglose.
     *
     * @param v      el vehículo
     * @param estilo con o sin color
     * @return las líneas
     */
    static List<String> ficha(Vehiculo v, Estilo estilo) {
        List<String> l = new ArrayList<>();
        String titulo = (v.getCodigo() == null ? "" : v.getCodigo() + "  ") + v.getDescripcion();
        String sit = v.getCodigo() == null ? "Sin registrar"
                : v.getSituacion().getEtiqueta();
        String marcaSit = v.getSituacion() == Situacion.DISPONIBLE && v.getCodigo() != null
                ? estilo.verde(sit) : estilo.tenue(sit);
        l.add(" " + estilo.titulo(titulo) + "  " + estilo.tenue("·") + " " + marcaSit);
        l.add("");

        List<String> izquierda = new ArrayList<>();
        izquierda.add("   " + estilo.negrita("Vehículo"));
        izquierda.add(dato("Tipo", v.getTipo().getEtiqueta(), estilo));
        izquierda.add(dato("Detalle", v.getDetalleTipo(), estilo));
        izquierda.add(dato("Marca", v.getMarca().getNombre() + " ("
                + v.getMarca().getPaisOrigen() + ")", estilo));
        izquierda.add(dato("Año", String.valueOf(v.getAnio()), estilo));
        izquierda.add(dato("Color", v.getColor().getEtiqueta(), estilo));

        List<String> derecha = new ArrayList<>();
        derecha.add(estilo.negrita("Registro"));
        derecha.add(datoCorto("Estado", estado(v.getEstado(), estilo), estilo));
        derecha.add(datoCorto("Kilometraje", v.getEstado() == Estado.USADO
                ? Formatos.km(v.getKilometraje()) : "—", estilo));
        derecha.add(datoCorto("Patente", v.getPatente() == null ? estilo.tenue("sin patentar")
                : v.getPatente(), estilo));
        if (v.getCodigo() != null) {
            derecha.add(datoCorto("Ingreso", Formatos.fecha(v.getFechaIngreso()), estilo));
        }
        l.addAll(columnas(izquierda, derecha, 46));
        if (v.getSituacion() == Situacion.BAJA) {
            l.add("");
            l.add("   " + estilo.tenue("Dado de baja el " + Formatos.fecha(v.getFechaBaja())
                    + ": " + v.getMotivoBaja()));
        }
        return l;
    }

    /**
     * Pone dos bloques de líneas uno al lado del otro.
     *
     * @param izquierda    líneas de la columna izquierda
     * @param derecha      líneas de la columna derecha
     * @param anchoIzquierda ancho visible de la columna izquierda
     * @return las líneas combinadas
     */
    static List<String> columnas(List<String> izquierda, List<String> derecha,
            int anchoIzquierda) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < Math.max(izquierda.size(), derecha.size()); i++) {
            String a = i < izquierda.size() ? izquierda.get(i) : "";
            String b = i < derecha.size() ? derecha.get(i) : "";
            int falta = Math.max(1, anchoIzquierda - Estilo.largoVisible(a));
            l.add((a + " ".repeat(falta) + b).stripTrailing());
        }
        return l;
    }

    private static String datoCorto(String etiqueta, String valor, Estilo estilo) {
        return estilo.tenue(String.format("%-13s", etiqueta)) + valor;
    }

    private static String dato(String etiqueta, String valor, Estilo estilo) {
        return "   " + estilo.tenue(String.format("%-10s", etiqueta)) + " " + valor;
    }

    /**
     * Desglose del precio (SPEC §4): lo que se le muestra al cliente.
     *
     * @param d      el desglose
     * @param tipo   tipo de vehículo, para nombrar el recargo
     * @param estilo con o sin color
     * @return las líneas
     */
    static List<String> desglose(DesglosePrecio d, TipoVehiculo tipo, Estilo estilo) {
        List<String> l = new ArrayList<>();
        l.add(renglon("Precio de lista", Formatos.moneda(d.getPrecioLista()), estilo, false));
        if (d.getDepreciacionAntiguedad().signum() > 0) {
            l.add(renglon("Depreciación por antigüedad ("
                    + Formatos.porcentaje(d.getPorcentajeAntiguedad()) + ")",
                    "-" + Formatos.moneda(d.getDepreciacionAntiguedad()), estilo, false));
        }
        if (d.getDepreciacionKm().signum() > 0) {
            l.add(renglon("Depreciación por km (" + Formatos.porcentaje(d.getPorcentajeKm()) + ")",
                    "-" + Formatos.moneda(d.getDepreciacionKm()), estilo, false));
        }
        if (d.getRecargos().signum() > 0) {
            String nombre = tipo == TipoVehiculo.CAMIONETA ? "Recargo 4x4" : "Recargos";
            l.add(renglon(nombre + " (" + Formatos.porcentaje(d.getPorcentajeRecargos()) + ")",
                    "+" + Formatos.moneda(d.getRecargos()), estilo, false));
        }
        if (d.getPatentamiento().signum() > 0) {
            l.add(renglon("Patentamiento", "+" + Formatos.moneda(d.getPatentamiento()), estilo,
                    false));
        }
        l.add("   " + estilo.tenue("─".repeat(ANCHO_BLOQUE - 3)));
        l.add(renglon("Precio final", Formatos.moneda(d.getPrecioFinal()), estilo, true));
        return l;
    }

    private static String renglon(String etiqueta, String monto, Estilo estilo, boolean destacado) {
        int espacio = Math.max(1, ANCHO_BLOQUE - 3 - etiqueta.length() - monto.length());
        String texto = etiqueta + " ".repeat(espacio) + monto;
        return "   " + (destacado ? estilo.negrita(texto) : texto);
    }

    /**
     * Comprobante de venta (SPEC §5): número, fecha, comprador, vehículo, desglose y total.
     *
     * @param v      la venta
     * @param estilo con o sin color
     * @return las líneas
     */
    static List<String> comprobante(Venta v, Estilo estilo) {
        List<String> l = new ArrayList<>();
        String borde = "   " + "─".repeat(ANCHO_BLOQUE - 3);
        l.add(borde);
        l.add(renglon("COMPROBANTE DE VENTA", "N.º " + String.format("%06d", v.getNumero()),
                estilo, true));
        l.add(renglon("Fecha", Formatos.fecha(v.getFecha()), estilo, false));
        l.add(renglon("Vendedor", v.getUsuarioVendedor(), estilo, false));
        l.add(borde);
        l.add(renglon("Comprador", v.getComprador().getNombreCompleto(), estilo, false));
        l.add(renglon("DNI", Formatos.miles(Long.parseLong(v.getComprador().getDni())), estilo,
                false));
        l.add(renglon("Vehículo", v.getCodigoVehiculo() + " · " + v.getDescripcionVehiculo(),
                estilo, false));
        l.add(borde);
        l.addAll(desglose(v.getDesglose(), v.getTipoVehiculo(), estilo));
        l.add(borde);
        return l;
    }
}
