package ar.edu.utn.frre.concesionaria.rendimiento;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.datos.Almacen;
import ar.edu.utn.frre.concesionaria.datos.AlmacenArchivo;
import ar.edu.utn.frre.concesionaria.datos.DatosDemo;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcasMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioUsuariosMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculosMemoria;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaCamioneta;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoMotocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.negocio.FiltroVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.OrdenVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioAutenticacion;
import ar.edu.utn.frre.concesionaria.negocio.ServicioMarcas;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Mediciones de rendimiento (ARQUITECTURA §7). Se corre con {@code ./build.sh bench}.
 *
 * <ul>
 *   <li>datos sintéticos con semilla fija: siempre los mismos;</li>
 *   <li>5 vueltas de calentamiento antes de medir, para que el JIT optimice;</li>
 *   <li>{@link System#nanoTime()} para medir intervalos;</li>
 *   <li>mediana de 20 repeticiones, que no se deja llevar por una vuelta lenta.</li>
 * </ul>
 *
 * <p>Limitación conocida: es una medición casera; la herramienta profesional es JMH.</p>
 */
public final class MedicionRendimiento {

    private static final int CALENTAMIENTO = 5;
    private static final int REPETICIONES = 20;
    private static final long SEMILLA = 2026L;

    /** Una operación a medir. */
    @FunctionalInterface
    private interface Operacion {
        void ejecutar() throws Exception;
    }

    private MedicionRendimiento() {
    }

    /**
     * @param args no se usan
     * @throws Exception si falla algo del entorno
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Mediciones de rendimiento · Java " + System.getProperty("java.version")
                + " · " + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + " · " + Runtime.getRuntime().availableProcessors() + " núcleos");
        System.out.println("Mediana de " + REPETICIONES + " repeticiones, " + CALENTAMIENTO
                + " de calentamiento.");
        System.out.println();

        Path carpeta = Files.createTempDirectory("concesionaria-bench");
        try {
            medirPersistenciaYListado(carpeta, 10_000);
            System.out.println();
            compararBusqueda();
        } finally {
            try (Stream<Path> archivos = Files.walk(carpeta)) {
                archivos.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static void medirPersistenciaYListado(Path carpeta, int cantidad) throws Exception {
        AlmacenArchivo archivo = new AlmacenArchivo(carpeta);
        Almacen almacen = archivo.cargar();
        RepositorioVehiculosMemoria vehiculos = new RepositorioVehiculosMemoria(almacen);
        RepositorioMarcasMemoria marcas = new RepositorioMarcasMemoria(almacen);
        new DatosDemo(marcas, vehiculos).cargar();
        generar(cantidad - vehiculos.contar(), marcas.listar(), new Random(SEMILLA))
                .forEach(v -> {
                    v.setCodigo(vehiculos.siguienteCodigo(v.getTipo()));
                    vehiculos.guardar(v);
                });
        ServicioAutenticacion auth = new ServicioAutenticacion(
                new RepositorioUsuariosMemoria(almacen), archivo, () -> { });
        Sesion sesion = auth.crearAdministradorInicial("bench", "Bench Medicion", "bench12345");
        ServicioVehiculos servicio = new ServicioVehiculos(vehiculos,
                new ServicioMarcas(marcas, vehiculos, archivo), archivo);

        String n = Formatos.miles(cantidad);
        imprimir("Guardar con " + n + " vehículos", "< 500 ms", medir(archivo::guardar));
        imprimir("Arranque (leer el archivo) con " + n, "< 1 s", medir(() -> {
            AlmacenArchivo otro = new AlmacenArchivo(carpeta);
            Almacen leido = otro.cargar();
            new RepositorioVehiculosMemoria(leido);
            new RepositorioMarcasMemoria(leido);
        }));

        FiltroVehiculos filtro = new FiltroVehiculos();
        filtro.setTipo(TipoVehiculo.CAMIONETA);
        filtro.setPrecioDesde(new BigDecimal("10000000"));
        imprimir("Listar " + n + ", filtrar y ordenar por precio", "< 100 ms",
                medir(() -> servicio.listar(sesion, filtro, OrdenVehiculos.PRECIO_DESC)));
        imprimir("Listar " + n + " sin filtro, orden natural", "< 100 ms",
                medir(() -> servicio.listar(sesion, new FiltroVehiculos(),
                        OrdenVehiculos.TIPO_Y_MARCA)));
        imprimir("Buscar texto libre en " + n, "—",
                medir(() -> servicio.buscarTexto(sesion, "toyota hilux")));
        System.out.println("  (archivo: " + Formatos.miles(Files.size(archivo.getArchivo()) / 1024)
                + " KB)");
    }

    /** HashMap contra ArrayList para buscar por código: por qué se eligió cada estructura. */
    private static void compararBusqueda() {
        System.out.println("Buscar por código: HashMap contra ArrayList (1.000 búsquedas)");
        System.out.printf("  %-12s %16s %16s%n", "Vehículos", "HashMap", "ArrayList");
        for (int cantidad : new int[]{1_000, 10_000, 100_000}) {
            Map<String, String> mapa = new HashMap<>();
            List<String> lista = new ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                String codigo = String.format("A-%04d", i);
                mapa.put(codigo, codigo);
                lista.add(codigo);
            }
            Random azar = new Random(SEMILLA);
            String[] buscados = new String[1000];
            for (int i = 0; i < buscados.length; i++) {
                buscados[i] = String.format("A-%04d", 1 + azar.nextInt(cantidad));
            }
            long[] sumidero = new long[1];
            double hash = medir(() -> {
                for (String c : buscados) {
                    sumidero[0] += mapa.get(c).length();
                }
            });
            double arreglo = medir(() -> {
                for (String c : buscados) {
                    for (String s : lista) {
                        if (s.equals(c)) {
                            sumidero[0] += s.length();
                            break;
                        }
                    }
                }
            });
            System.out.printf("  %-12s %16s %16s%n", Formatos.miles(cantidad), ms(hash), ms(arreglo));
            if (sumidero[0] == 42) {
                System.out.print("");
            }
        }
        System.out.println("  Por búsqueda con 100.000: HashMap O(1), ArrayList O(n).");
    }

    private static double medir(Operacion operacion) {
        try {
            for (int i = 0; i < CALENTAMIENTO; i++) {
                operacion.ejecutar();
            }
            double[] tiempos = new double[REPETICIONES];
            for (int i = 0; i < REPETICIONES; i++) {
                long inicio = System.nanoTime();
                operacion.ejecutar();
                tiempos[i] = (System.nanoTime() - inicio) / 1_000_000.0;
            }
            Arrays.sort(tiempos);
            return (tiempos[REPETICIONES / 2 - 1] + tiempos[REPETICIONES / 2]) / 2;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String ms(double milisegundos) {
        return String.format(Formatos.ARGENTINA, "%,.3f ms", milisegundos);
    }

    private static void imprimir(String escenario, String objetivo, double milisegundos) {
        System.out.printf("  %-48s %14s   objetivo %s%n", escenario, ms(milisegundos), objetivo);
    }

    /** Vehículos sintéticos válidos, con semilla fija. Patentes Mercosur únicas. */
    private static List<Vehiculo> generar(int cantidad, List<Marca> marcas, Random azar)
            throws Exception {
        List<Vehiculo> lista = new ArrayList<>(cantidad);
        Color[] colores = Color.values();
        for (int i = 0; i < cantidad; i++) {
            boolean usado = azar.nextInt(3) > 0;
            Estado estado = usado ? Estado.USADO : Estado.NUEVO;
            int anio = usado ? 2005 + azar.nextInt(20) : 2026;
            Integer km = usado ? 1 + azar.nextInt(250_000) : null;
            String patente = usado ? patente(i) : null;
            BigDecimal precio = BigDecimal.valueOf(3_000_000L + azar.nextInt(60_000_000));
            Color color = colores[azar.nextInt(colores.length)];
            int tipo = azar.nextInt(3);
            Vehiculo v;
            if (tipo == 0) {
                Marca m = marcaQueFabrica(marcas, TipoVehiculo.AUTOMOVIL, azar);
                v = new Automovil(estado, m, "Modelo " + azar.nextInt(50), anio, color, precio, km,
                        patente, CarroceriaAutomovil.SEDAN, 4);
            } else if (tipo == 1) {
                Marca m = marcaQueFabrica(marcas, TipoVehiculo.CAMIONETA, azar);
                v = new Camioneta(estado, m, "Modelo " + azar.nextInt(50), anio, color, precio, km,
                        patente, CarroceriaCamioneta.SUV, azar.nextBoolean(), 800);
            } else {
                Marca m = marcaQueFabrica(marcas, TipoVehiculo.MOTOCICLETA, azar);
                v = new Motocicleta(estado, m, "Modelo " + azar.nextInt(50), anio, color, precio,
                        km, usado ? patenteMoto(i) : null, TipoMotocicleta.CALLE, 150);
            }
            lista.add(v);
        }
        return lista;
    }

    private static Marca marcaQueFabrica(List<Marca> marcas, TipoVehiculo tipo, Random azar) {
        List<Marca> posibles = marcas.stream().filter(m -> m.fabrica(tipo)).toList();
        return posibles.get(azar.nextInt(posibles.size()));
    }

    /** M000AAA a partir de un número, para que no se repitan. */
    private static String patenteMoto(int n) {
        int resto = n / 1000;
        return "M" + String.format("%03d", n % 1000) + (char) ('A' + resto % 26)
                + (char) ('A' + (resto / 26) % 26) + (char) ('A' + (resto / 676) % 26);
    }

    /** AA000AA a partir de un número, para que no se repitan. */
    private static String patente(int n) {
        char a = (char) ('B' + (n / 1_000_000) % 24);
        char b = (char) ('A' + (n / 26_000) % 26);
        String digitos = String.format("%03d", n % 1000);
        char c = (char) ('A' + (n / 1000) % 26);
        char d = (char) ('A' + (n / 676_000) % 26);
        return "" + a + b + digitos + c + d;
    }
}
