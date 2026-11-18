package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.excepciones.MarcaDuplicadaException;
import ar.edu.utn.frre.concesionaria.excepciones.MarcaEnUsoException;
import ar.edu.utn.frre.concesionaria.excepciones.ReglaNegocioException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * PR-MARCA: normalización, duplicados, marcas parecidas y marcas en uso (CU-12).
 */
public final class PruebasMarca implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-MARCA";
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-MARCA-01", "alta normalizada", () -> {
            Contexto c = new Contexto();
            Marca m = c.marcas.crear(c.admin, "  renault ", "francia",
                    EnumSet.of(TipoVehiculo.AUTOMOVIL));
            Verificar.igual("Renault", m.getNombre());
            Verificar.igual(9, m.getId());
        });
        r.prueba("PR-MARCA-02", "duplicado con otra capitalización o acentos", () -> {
            Contexto c = new Contexto();
            MarcaDuplicadaException e = Verificar.lanza(MarcaDuplicadaException.class,
                    () -> c.marcas.crear(c.admin, "TOYOTA", "Japón",
                            EnumSet.of(TipoVehiculo.AUTOMOVIL)));
            Verificar.igual("La marca Toyota ya existe.", e.getMessage());
        });
        r.prueba("PR-MARCA-03", "avisa de marcas parecidas: Tollota → Toyota", () -> {
            Contexto c = new Contexto();
            Verificar.igual("Toyota", c.marcas.parecidas("Tollota").get(0).getNombre());
            Verificar.igual(0, c.marcas.parecidas("Renault").size());
        });
        r.prueba("PR-MARCA-04", "una marca en uso no se borra, se desactiva", () -> {
            Contexto c = new Contexto();
            Marca fiat = c.repoMarcas.buscarPorNombre("Fiat").get();
            MarcaEnUsoException e = Verificar.lanza(MarcaEnUsoException.class,
                    () -> c.marcas.eliminar(c.admin, fiat.getId()));
            Verificar.verdadero(e.getMessage().contains("Desactivala"), e.getMessage());
            c.marcas.cambiarActiva(c.admin, fiat.getId(), false);
            Verificar.igual(0, c.marcas.paraAlta(TipoVehiculo.AUTOMOVIL, "fi").size());
        });
        r.prueba("PR-MARCA-05", "con una marca desactivada no se registra", () -> {
            Contexto c = new Contexto();
            Marca fiat = c.repoMarcas.buscarPorNombre("Fiat").get();
            Automovil a = new Automovil(Estado.NUEVO, fiat, "Pulse", 2026, Color.BLANCO,
                    new BigDecimal("20000000"), null, null, CarroceriaAutomovil.HATCHBACK, 5);
            c.marcas.cambiarActiva(c.admin, fiat.getId(), false);
            Verificar.lanza(ReglaNegocioException.class, () -> c.vehiculos.registrar(c.admin, a));
        });
        r.prueba("PR-MARCA-06", "una marca sin vehículos se borra", () -> {
            Contexto c = new Contexto();
            Marca m = c.marcas.crear(c.admin, "Renault", "Francia", EnumSet.of(TipoVehiculo.AUTOMOVIL));
            c.marcas.eliminar(c.admin, m.getId());
            Verificar.verdadero(c.repoMarcas.buscarPorNombre("renault").isEmpty(), "se borró");
        });
        r.prueba("PR-MARCA-07", "para el alta: solo activas que fabrican el tipo", () -> {
            Contexto c = new Contexto();
            Verificar.igual(2, c.marcas.paraAlta(TipoVehiculo.MOTOCICLETA, "").size());
            Verificar.igual("Honda", c.marcas.paraAlta(TipoVehiculo.MOTOCICLETA, "ho").get(0)
                    .getNombre());
            Verificar.igual(0, c.marcas.paraAlta(TipoVehiculo.CAMIONETA, "fiat").size());
        });
        r.prueba("PR-MARCA-08", "no se puede quitar un tipo con vehículos cargados", () -> {
            Contexto c = new Contexto();
            Marca toyota = c.repoMarcas.buscarPorNombre("Toyota").get();
            Verificar.lanza(MarcaEnUsoException.class, () -> c.marcas.modificar(c.admin,
                    toyota.getId(), "Japón", EnumSet.of(TipoVehiculo.AUTOMOVIL)));
        });
    }
}
