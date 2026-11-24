package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.datos.Almacen;
import ar.edu.utn.frre.concesionaria.datos.AlmacenArchivo;
import ar.edu.utn.frre.concesionaria.datos.CatalogoReferencia;
import ar.edu.utn.frre.concesionaria.datos.DatosDemo;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcasMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculosMemoria;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.Stream;

/**
 * PR-PERSIST: guardar y cargar, recuperación desde el {@code .bak} y versión de esquema.
 */
public final class PruebasPersistencia implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-PERSIST";
    }

    /**
     * @return una carpeta temporal nueva
     * @throws Exception si no se pudo crear
     */
    static Path carpetaTemporal() throws Exception {
        Path carpeta = Files.createTempDirectory("concesionaria-prueba");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> borrar(carpeta)));
        return carpeta;
    }

    private static void borrar(Path carpeta) {
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            archivos.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (Exception e) {
            // Es una carpeta temporal: si no se borra, la limpia el sistema.
        }
    }

    private static Almacen conDemo(AlmacenArchivo archivo) throws Exception {
        Almacen almacen = archivo.cargar();
        new DatosDemo(new RepositorioMarcasMemoria(almacen), new RepositorioVehiculosMemoria(almacen))
                .cargar();
        return almacen;
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-PERSIST-01", "sin archivo es primer arranque", () -> {
            AlmacenArchivo archivo = new AlmacenArchivo(carpetaTemporal());
            Almacen a = archivo.cargar();
            Verificar.igual(AlmacenArchivo.EstadoCarga.NUEVO, archivo.getEstadoCarga());
            Verificar.verdadero(a.estaVacio(), "el almacén arranca vacío");
        });
        r.prueba("PR-PERSIST-02", "guardar y volver a cargar conserva todo", () -> {
            Fabrica.fijarHoy();
            Path carpeta = carpetaTemporal();
            AlmacenArchivo archivo = new AlmacenArchivo(carpeta);
            conDemo(archivo);
            archivo.guardar();

            AlmacenArchivo otro = new AlmacenArchivo(carpeta);
            Almacen leido = otro.cargar();
            Verificar.igual(AlmacenArchivo.EstadoCarga.NORMAL, otro.getEstadoCarga());
            Verificar.igual(10, leido.getVehiculos().size());
            Verificar.igual((int) CatalogoReferencia.leer().stream()
                    .map(CatalogoReferencia.Linea::getMarca).distinct().count(),
                    leido.getMarcas().size());
            Vehiculo hilux = leido.getVehiculos().get("C-0001");
            Verificar.igual("Hilux", hilux.getModelo());
            Verificar.monto("22850920", hilux.calcularPrecioFinal(Fabrica.HOY));
            Verificar.igual(Integer.valueOf(4),
                    leido.getUltimosCodigos().get(TipoVehiculo.AUTOMOVIL));
            Verificar.verdadero(!Files.exists(carpeta.resolve("datos.ser.tmp")),
                    "no queda el temporal");
        });
        r.prueba("PR-PERSIST-03", "el segundo guardado deja un .bak con el anterior", () -> {
            Path carpeta = carpetaTemporal();
            AlmacenArchivo archivo = new AlmacenArchivo(carpeta);
            Almacen a = conDemo(archivo);
            archivo.guardar();
            a.getVehiculos().remove("M-0002");
            archivo.guardar();
            Verificar.verdadero(Files.exists(carpeta.resolve("datos.ser.bak")), "existe el .bak");
        });
        r.prueba("PR-PERSIST-04", "archivo corrupto: se recupera del .bak", () -> {
            Path carpeta = carpetaTemporal();
            AlmacenArchivo archivo = new AlmacenArchivo(carpeta);
            conDemo(archivo);
            archivo.guardar();
            archivo.guardar();
            Files.write(carpeta.resolve("datos.ser"), new byte[]{1, 2, 3, 4});

            AlmacenArchivo otro = new AlmacenArchivo(carpeta);
            Almacen leido = otro.cargar();
            Verificar.igual(AlmacenArchivo.EstadoCarga.RECUPERADO, otro.getEstadoCarga());
            Verificar.igual(10, leido.getVehiculos().size());
            Verificar.verdadero(Files.exists(carpeta.resolve("datos.ser.corrupto")),
                    "el dañado se aparta como .corrupto");
            Verificar.verdadero(otro.hayCambiosPendientes(), "queda pendiente volver a guardar");
        });
        r.prueba("PR-PERSIST-05", "corrupto y sin copia: no se pisa nada", () -> {
            Path carpeta = carpetaTemporal();
            Files.write(carpeta.resolve("datos.ser"), new byte[]{1, 2, 3});
            Verificar.lanza(PersistenciaException.class, () -> new AlmacenArchivo(carpeta).cargar());
            Verificar.igual(3L, Files.size(carpeta.resolve("datos.ser")));
        });
        r.prueba("PR-PERSIST-06", "datos de una versión más nueva: no se abren", () -> {
            Path carpeta = carpetaTemporal();
            Almacen futuro = new Almacen();
            futuro.setVersionEsquema(Almacen.VERSION_ESQUEMA_ACTUAL + 1);
            try (ObjectOutputStream salida = new ObjectOutputStream(
                    Files.newOutputStream(carpeta.resolve("datos.ser")))) {
                salida.writeObject(futuro);
            }
            PersistenciaException e = Verificar.lanza(PersistenciaException.class,
                    () -> new AlmacenArchivo(carpeta).cargar());
            Verificar.verdadero(e.getMessage().contains("versión más nueva"), e.getMessage());
        });
        r.prueba("PR-PERSIST-07", "datos de una versión anterior: se migran", () -> {
            Path carpeta = carpetaTemporal();
            Almacen viejo = new Almacen();
            viejo.setVersionEsquema(0);
            try (ObjectOutputStream salida = new ObjectOutputStream(
                    Files.newOutputStream(carpeta.resolve("datos.ser")))) {
                salida.writeObject(viejo);
            }
            AlmacenArchivo archivo = new AlmacenArchivo(carpeta);
            Almacen leido = archivo.cargar();
            Verificar.igual(AlmacenArchivo.EstadoCarga.MIGRADO, archivo.getEstadoCarga());
            Verificar.igual(Almacen.VERSION_ESQUEMA_ACTUAL, leido.getVersionEsquema());
        });
        r.prueba("PR-PERSIST-09", "esquemas 1 → 3: catálogo con los vehículos y el de referencia", () -> {
            Fabrica.fijarHoy();
            Path carpeta = carpetaTemporal();
            Almacen viejo = new Almacen();
            viejo.setVersionEsquema(1);
            Marca fiat = new Marca("Fiat", "Italia", EnumSet.of(TipoVehiculo.AUTOMOVIL));
            fiat.setId(1);
            viejo.getMarcas().put(1, fiat);
            viejo.setUltimaMarca(1);
            Automovil cronos = new Automovil(Estado.NUEVO, fiat, "cronos", 2026, Color.ROJO,
                    new BigDecimal("25000000"), null, null, CarroceriaAutomovil.SEDAN, 4);
            cronos.setCodigo("A-0001");
            Automovil raro = new Automovil(Estado.NUEVO, fiat, "X", 2026, Color.ROJO,
                    new BigDecimal("25000000"), null, null, CarroceriaAutomovil.SEDAN, 4);
            raro.setCodigo("A-0002");
            viejo.getVehiculos().put("A-0001", cronos);
            viejo.getVehiculos().put("A-0002", raro);
            try (ObjectOutputStream salida = new ObjectOutputStream(
                    Files.newOutputStream(carpeta.resolve("datos.ser")))) {
                salida.writeObject(viejo);
            }
            AlmacenArchivo archivo = new AlmacenArchivo(carpeta);
            Almacen leido = archivo.cargar();
            Verificar.igual(AlmacenArchivo.EstadoCarga.MIGRADO, archivo.getEstadoCarga());
            Marca migrada = leido.getMarcas().get(1);
            Verificar.igual("Cronos", migrada.buscarModelo("CRONOS").get().getNombre());
            // un modelo que no cumple la regla nueva no entra, pero el vehículo se conserva
            Verificar.verdadero(migrada.buscarModelo("X").isEmpty(), "X no entra al catálogo");
            Verificar.igual(2, leido.getVehiculos().size());
            // esquema 3: se suma el catálogo de referencia sin pisar la marca que ya estaba
            Verificar.igual("Italia", migrada.getPaisOrigen());
            Verificar.verdadero(migrada.buscarModelo("Toro").isPresent(), "Fiat suma la Toro");
            Verificar.verdadero(leido.getMarcas().values().stream()
                    .anyMatch(m -> m.getNombre().equals("Toyota")), "se suma Toyota");
            Verificar.igual(1, (int) leido.getMarcas().values().stream()
                    .filter(m -> m.getNombre().equals("Fiat")).count());
        });
        r.prueba("PR-PERSIST-08", "índices de patente y de prefijo de marca", () -> {
            Almacen a = conDemo(new AlmacenArchivo(carpetaTemporal()));
            RepositorioVehiculosMemoria vehiculos = new RepositorioVehiculosMemoria(a);
            Verificar.igual("C-0001", vehiculos.buscarPorPatente("ad 456 fg").get().getCodigo());
            RepositorioMarcasMemoria marcas = new RepositorioMarcasMemoria(a);
            Verificar.igual("Toyota", marcas.buscarPorPrefijo("TOY").get(0).getNombre());
            Verificar.igual(2, marcas.buscarPorPrefijo("f").size());
            Marca toyota = marcas.buscarPorNombre("toyota").get();
            Verificar.igual("Toyota", toyota.getNombre());
        });
    }
}
