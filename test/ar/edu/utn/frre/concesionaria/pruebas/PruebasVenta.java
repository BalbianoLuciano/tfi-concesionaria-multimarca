package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.modelo.ResultadoImportacion;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.PatenteDuplicadaException;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.excepciones.VehiculoNoDisponibleException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Situacion;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import ar.edu.utn.frre.concesionaria.negocio.FiltroVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.OrdenVehiculos;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PR-VENTA, más la unicidad de patente (PR-PATENTE) y la importación todo o nada (PR-CSV), que
 * garantiza el negocio.
 */
public final class PruebasVenta implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-VENTA";
    }

    private static Comprador comprador() throws DatoInvalidoException {
        return new Comprador("Juan Pérez", "30.123.456");
    }

    private static Automovil autoConPatente(Contexto c, String patente) throws Exception {
        return new Automovil(Estado.USADO, c.repoMarcas.buscarPorNombre("Toyota").get(), "Etios",
                2018, Color.GRIS, new BigDecimal("9000000"), 80_000, patente,
                CarroceriaAutomovil.SEDAN, 4);
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-VENTA-01", "venta exitosa: número, vendedor, precio y situación", () -> {
            Contexto c = new Contexto();
            Venta v = c.ventas.vender(c.vendedor, "c-1", comprador());
            Verificar.igual(1, v.getNumero());
            Verificar.igual("vendedor", v.getUsuarioVendedor());
            Verificar.igual("C-0001", v.getCodigoVehiculo());
            Verificar.monto("22850920", v.getPrecioFinal());
            Verificar.igual(Situacion.VENDIDO, c.repoVehiculos.buscar("C-0001").get().getSituacion());
            Verificar.igual(2, c.ventas.vender(c.vendedor, "A-0001", comprador()).getNumero());
        });
        r.prueba("PR-VENTA-02", "no se puede vender algo ya vendido", () -> {
            Contexto c = new Contexto();
            c.ventas.vender(c.vendedor, "C-0001", comprador());
            VehiculoNoDisponibleException e = Verificar.lanza(VehiculoNoDisponibleException.class,
                    () -> c.ventas.vender(c.vendedor, "C-0001", comprador()));
            Verificar.verdadero(e.getMessage().contains("vendido"), e.getMessage());
        });
        r.prueba("PR-VENTA-03", "no se puede vender algo dado de baja", () -> {
            Contexto c = new Contexto();
            c.vehiculos.darDeBaja(c.vendedor, "M-0002", "Robo");
            Verificar.lanza(VehiculoNoDisponibleException.class,
                    () -> c.ventas.vender(c.vendedor, "M-0002", comprador()));
            Verificar.lanza(VehiculoNoDisponibleException.class,
                    () -> c.vehiculos.darDeBaja(c.vendedor, "M-0002", "Otra vez"));
        });
        r.prueba("PR-VENTA-04", "el precio queda congelado aunque cambie el vehículo", () -> {
            Contexto c = new Contexto();
            Venta v = c.ventas.vender(c.vendedor, "C-0001", comprador());
            Vehiculo hilux = c.repoVehiculos.buscar("C-0001").get();
            hilux.setSituacion(Situacion.DISPONIBLE);
            hilux.setPrecioLista(new BigDecimal("99000000"));
            Verificar.monto("22850920", v.getPrecioFinal());
            Verificar.igual("Toyota Hilux 2019", v.getDescripcionVehiculo());
        });
        r.prueba("PR-VENTA-05", "si falla el guardado, la venta se deshace entera", () -> {
            Contexto c = new Contexto();
            c.persistencia.fallar = true;
            Verificar.lanza(PersistenciaException.class,
                    () -> c.ventas.vender(c.vendedor, "C-0001", comprador()));
            Verificar.igual(Situacion.DISPONIBLE,
                    c.repoVehiculos.buscar("C-0001").get().getSituacion());
            Verificar.igual(0, c.repoVentas.contar());
            c.persistencia.fallar = false;
            Verificar.igual(1, c.ventas.vender(c.vendedor, "C-0001", comprador()).getNumero());
        });
        r.prueba("PR-VENTA-06", "un vendedor ve solo sus ventas; un directivo, todas", () -> {
            Contexto c = new Contexto();
            c.ventas.vender(c.vendedor, "C-0001", comprador());
            c.ventas.vender(c.autenticacion.iniciarSesion("vendedor2", Contexto.CLAVE), "A-0001",
                    comprador());
            Verificar.igual(1, c.ventas.historial(c.vendedor, null, null, null).size());
            Verificar.igual(1, c.ventas.historial(c.vendedor, null, null, "vendedor2").size());
            List<Venta> todas = c.ventas.historial(c.directivo, null, null, null);
            Verificar.igual(2, todas.size());
            Verificar.monto(c.repoVentas.buscar(1).get().getPrecioFinal()
                    .add(c.repoVentas.buscar(2).get().getPrecioFinal()).toPlainString(),
                    ar.edu.utn.frre.concesionaria.negocio.ServicioVentas.total(todas));
        });
        r.prueba("PR-PATENTE-06", "patente única también contra vendidos y bajas", () -> {
            Contexto c = new Contexto();
            c.ventas.vender(c.vendedor, "C-0001", comprador());
            PatenteDuplicadaException e = Verificar.lanza(PatenteDuplicadaException.class,
                    () -> c.vehiculos.registrar(c.vendedor, autoConPatente(c, "ad-456-fg")));
            Verificar.igual("La patente AD456FG ya está registrada en C-0001.", e.getMessage());
            c.vehiculos.darDeBaja(c.vendedor, "M-0002", "Robo");
            Verificar.lanza(DatoInvalidoException.class,
                    () -> c.vehiculos.validarPatenteParaAlta("A123BCD"));
            Verificar.igual("A-0005", c.vehiculos.registrar(c.vendedor, autoConPatente(c, "AC111BB")));
        });
        r.prueba("PR-VENTA-07", "modificar trabaja sobre una copia y respeta la patente", () -> {
            Contexto c = new Contexto();
            Vehiculo copia = c.vehiculos.buscar(c.vendedor, "A-0002").copia();
            copia.setPatente("AE789HJ");
            Verificar.lanza(PatenteDuplicadaException.class, () -> c.vehiculos.modificar(c.vendedor, copia));
            Verificar.igual("PQR456", c.repoVehiculos.buscar("A-0002").get().getPatente());
            copia.setPatente("PQR457");
            copia.setColor(Color.ROJO);
            c.vehiculos.modificar(c.vendedor, copia);
            Verificar.igual(Color.ROJO, c.repoVehiculos.buscar("A-0002").get().getColor());
            Verificar.igual("A-0002",
                    c.repoVehiculos.buscarPorPatente("PQR457").get().getCodigo());
            Verificar.verdadero(c.repoVehiculos.buscarPorPatente("PQR456").isEmpty(),
                    "la patente vieja se libera");
        });
        r.prueba("PR-VENTA-08", "listar con filtros, orden y búsqueda libre", () -> {
            Contexto c = new Contexto();
            FiltroVehiculos f = new FiltroVehiculos();
            f.setTipo(TipoVehiculo.CAMIONETA);
            f.setEstado(Estado.USADO);
            List<Vehiculo> lista = c.vehiculos.listar(c.directivo, f, OrdenVehiculos.PRECIO_DESC);
            Verificar.igual(List.of("C-0001", "C-0004", "C-0003"),
                    lista.stream().map(Vehiculo::getCodigo).toList());
            Verificar.igual(10, c.vehiculos.listar(c.directivo, new FiltroVehiculos(),
                    OrdenVehiculos.TIPO_Y_MARCA).size());
            Verificar.igual("A-0004", c.vehiculos.buscarTexto(c.directivo, "peugeot 208")
                    .get(0).getCodigo());
            Verificar.igual("A-0002", c.vehiculos.buscarTexto(c.directivo, "pqr456").get(0)
                    .getCodigo());
            Verificar.igual(0, c.vehiculos.buscarTexto(c.directivo, "CITROËN").size());
        });
        r.prueba("PR-CSV-05", "importación con una fila mala: no se importa nada", () -> {
            Path carpeta = PruebasPersistencia.carpetaTemporal();
            Contexto c = new Contexto(carpeta);
            String cab = "tipo;estado;marca;modelo;anio;color;kilometraje;patente;precio_lista;"
                    + "carroceria_o_tipo;puertas;traccion_4x4;carga_kg;cilindrada_cc";
            Files.write(carpeta.resolve("malo.csv"), List.of(cab,
                    "Automóvil;Usado;Toyota;Etios;2018;Gris;80000;AC111BB;9000000;Sedán;4;;;",
                    "Automóvil;Usado;Toyota;Etios;2018;Gris;80000;AD456FG;9000000;Sedán;4;;;"),
                    StandardCharsets.UTF_8);
            int antes = c.repoVehiculos.contar();
            ResultadoImportacion r1 = c.archivos.importarVehiculos(c.admin, "malo.csv");
            Verificar.igual(1, r1.getErrores().size());
            Verificar.igual(3, r1.getErrores().get(0).getLinea());
            Verificar.igual(antes, c.repoVehiculos.contar());

            Files.write(carpeta.resolve("bueno.csv"), List.of(cab,
                    "Automóvil;Usado;Toyota;Etios;2018;Gris;80000;AC111BB;9000000;Sedán;4;;;",
                    "Motocicleta;0 km;Yamaha;MT-03;2026;Azul;;;9000000;Naked;;;;321"),
                    StandardCharsets.UTF_8);
            ResultadoImportacion r2 = c.archivos.importarVehiculos(c.admin, "bueno.csv");
            Verificar.igual(List.of(), r2.getErrores());
            Verificar.igual(antes + 2, c.repoVehiculos.contar());
            Verificar.igual("M-0003", r2.getVehiculos().get(1).getCodigo());
        });
    }
}
