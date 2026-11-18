package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.datos.Almacen;
import ar.edu.utn.frre.concesionaria.datos.AlmacenArchivo;
import ar.edu.utn.frre.concesionaria.datos.ArchivosCsv;
import ar.edu.utn.frre.concesionaria.datos.DatosDemo;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcasMemoria;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculosMemoria;
import ar.edu.utn.frre.concesionaria.datos.ResultadoImportacion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PR-CSV: exportar e importar vehículos. Que con una fila mala no se importe nada lo garantiza
 * el negocio y se prueba en {@link PruebasVenta}; acá se prueba la lectura y el informe.
 */
public final class PruebasCsv implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-CSV";
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-CSV-01", "exportar y volver a leer los 10 vehículos de la demo", () -> {
            Fabrica.fijarHoy();
            Path carpeta = PruebasPersistencia.carpetaTemporal();
            Almacen a = new AlmacenArchivo(carpeta).cargar();
            RepositorioMarcasMemoria marcas = new RepositorioMarcasMemoria(a);
            RepositorioVehiculosMemoria vehiculos = new RepositorioVehiculosMemoria(a);
            new DatosDemo(marcas, vehiculos).cargar();

            ArchivosCsv csv = new ArchivosCsv(carpeta);
            String ruta = csv.exportarVehiculos(vehiculos.listar());
            List<String> lineas = Files.readAllLines(Path.of(ruta), StandardCharsets.UTF_8);
            Verificar.igual(11, lineas.size());
            Verificar.verdadero(lineas.get(0).startsWith("codigo;tipo;estado;marca"),
                    "encabezado: " + lineas.get(0));

            ResultadoImportacion leido = csv.importarVehiculos(Path.of(ruta).getFileName().toString(),
                    marcas::buscarPorNombre);
            Verificar.igual(List.of(), leido.getErrores());
            Verificar.igual(10, leido.getVehiculos().size());
        });
        r.prueba("PR-CSV-02", "una fila mala se informa con línea, campo y motivo", () -> {
            Fabrica.fijarHoy();
            Path carpeta = PruebasPersistencia.carpetaTemporal();
            Almacen a = new AlmacenArchivo(carpeta).cargar();
            RepositorioMarcasMemoria marcas = new RepositorioMarcasMemoria(a);
            new DatosDemo(marcas, new RepositorioVehiculosMemoria(a)).cargar();
            List<String> lineas = new ArrayList<>();
            lineas.add("tipo;estado;marca;modelo;anio;color;kilometraje;patente;precio_lista;"
                    + "carroceria_o_tipo;puertas;traccion_4x4;carga_kg;cilindrada_cc");
            lineas.add("Automóvil;Usado;toyota;Etios;2018;Gris;80000;AC111BB;9000000;Sedán;4;;;");
            lineas.add("Automóvil;Usado;Toyota;Etios;dos mil;Gris;80000;AC111BC;9000000;Sedán;4;;;");
            lineas.add("Motocicleta;0 km;Yamaha;MT-03;2026;Azul;;;9000000;Naked;;;;321");
            lineas.add("Camioneta;Usado;Tollota;Hilux;2018;Gris;80000;AC111BD;9000000;SUV;;si;800;");
            Files.write(carpeta.resolve("entrada.csv"), lineas, StandardCharsets.UTF_8);

            ResultadoImportacion leido = new ArchivosCsv(carpeta).importarVehiculos("entrada.csv",
                    marcas::buscarPorNombre);
            Verificar.igual(2, leido.getVehiculos().size());
            Verificar.igual(2, leido.getErrores().size());
            ResultadoImportacion.Error primero = leido.getErrores().get(0);
            Verificar.igual(3, primero.getLinea());
            Verificar.igual("Año", primero.getCampo());
            Verificar.igual(5, leido.getErrores().get(1).getLinea());
            Verificar.verdadero(leido.getErrores().get(1).getMotivo().contains("Tollota"),
                    leido.getErrores().get(1).getMotivo());
        });
        r.prueba("PR-CSV-03", "campos con separador van entre comillas", () -> {
            Path carpeta = PruebasPersistencia.carpetaTemporal();
            Almacen a = new AlmacenArchivo(carpeta).cargar();
            RepositorioMarcasMemoria marcas = new RepositorioMarcasMemoria(a);
            new DatosDemo(marcas, new RepositorioVehiculosMemoria(a)).cargar();
            List<String> lineas = Files.readAllLines(
                    Path.of(new ArchivosCsv(carpeta).exportarMarcas(marcas.listar())));
            Verificar.verdadero(lineas.stream().anyMatch(l -> l.contains("\"Automóvil, Camioneta\"")
                    || l.contains("Automóvil, Camioneta")), "tipos en una sola columna");
        });
        r.prueba("PR-CSV-04", "falta una columna obligatoria", () -> {
            Path carpeta = PruebasPersistencia.carpetaTemporal();
            Files.writeString(carpeta.resolve("malo.csv"), "tipo;estado\nAutomóvil;Usado\n");
            ResultadoImportacion leido = new ArchivosCsv(carpeta).importarVehiculos("malo.csv",
                    n -> java.util.Optional.empty());
            Verificar.verdadero(leido.tieneErrores(), "hay errores");
            Verificar.igual(1, leido.getErrores().get(0).getLinea());
        });
    }
}
