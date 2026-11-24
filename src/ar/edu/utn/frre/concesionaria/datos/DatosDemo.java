package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaCamioneta;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoMotocicleta;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de demostración del primer arranque (CU-01): 10 vehículos de marcas del catálogo de
 * referencia ({@link CatalogoReferencia}), mezclando tipos, 0 km y usados. Los 0 km toman el año actual, así siguen siendo válidos con el paso del
 * tiempo.
 */
public class DatosDemo implements CargadorDemo {


    private final RepositorioMarcas marcas;
    private final RepositorioVehiculos vehiculos;

    /**
     * @param marcas    repositorio de marcas
     * @param vehiculos repositorio de vehículos
     */
    public DatosDemo(RepositorioMarcas marcas, RepositorioVehiculos vehiculos) {
        this.marcas = marcas;
        this.vehiculos = vehiculos;
    }

    @Override
    public void cargar() throws ConcesionariaException {
        // Las marcas y los modelos salen del catálogo de referencia (CU-12); acá solo se
        // agregan los vehículos de ejemplo.
        CatalogoReferencia.incorporar(marcas);
        Marca toyota = marca("Toyota");
        Marca volkswagen = marca("Volkswagen");
        Marca chevrolet = marca("Chevrolet");
        Marca ford = marca("Ford");
        Marca fiat = marca("Fiat");
        Marca peugeot = marca("Peugeot");
        Marca honda = marca("Honda");
        Marca yamaha = marca("Yamaha");

        int anio = Reloj.anioActual();
        List<Vehiculo> lista = List.of(
                new Camioneta(Estado.USADO, toyota, "Hilux", 2019, Color.BLANCO,
                        pesos(38_000_000), 112_900, "AD456FG",
                        CarroceriaCamioneta.PICKUP_DOBLE_CABINA, true, 1000),
                new Automovil(Estado.NUEVO, toyota, "Corolla", anio, Color.BLANCO,
                        pesos(32_500_000), null, null, CarroceriaAutomovil.SEDAN, 4),
                new Automovil(Estado.USADO, volkswagen, "Gol Trend", 2015, Color.GRIS,
                        pesos(9_800_000), 98_000, "PQR456", CarroceriaAutomovil.HATCHBACK, 5),
                new Automovil(Estado.NUEVO, fiat, "Cronos", anio, Color.ROJO,
                        pesos(24_900_000), null, null, CarroceriaAutomovil.SEDAN, 4),
                new Automovil(Estado.USADO, peugeot, "208", 2021, Color.NEGRO,
                        pesos(18_700_000), 45_300, "AE789HJ", CarroceriaAutomovil.HATCHBACK, 5),
                new Camioneta(Estado.NUEVO, ford, "Ranger", anio, Color.GRIS,
                        pesos(52_000_000), null, null, CarroceriaCamioneta.PICKUP_DOBLE_CABINA,
                        true, 1100),
                new Camioneta(Estado.USADO, volkswagen, "Amarok", 2017, Color.PLATA,
                        pesos(34_000_000), 150_000, "AB321CD",
                        CarroceriaCamioneta.PICKUP_DOBLE_CABINA, true, 1000),
                new Camioneta(Estado.USADO, chevrolet, "Tracker", 2023, Color.AZUL,
                        pesos(27_500_000), 31_200, "AF112GH", CarroceriaCamioneta.SUV, false, 400),
                new Motocicleta(Estado.NUEVO, honda, "CB 500F", anio, Color.ROJO,
                        pesos(12_500_000), null, null, TipoMotocicleta.NAKED, 471),
                new Motocicleta(Estado.USADO, yamaha, "FZ-S", 2020, Color.AZUL,
                        pesos(3_900_000), 21_000, "A123BCD", TipoMotocicleta.CALLE, 150));

        for (Vehiculo v : lista) {
            v.setCodigo(vehiculos.siguienteCodigo(v.getTipo()));
            v.setFechaIngreso(Reloj.hoy());
            vehiculos.guardar(v);
        }
    }

    private Marca marca(String nombre) throws PersistenciaException {
        return marcas.buscarPorNombre(nombre).orElseThrow(() -> new PersistenciaException(
                "El catálogo de referencia no tiene la marca " + nombre + "."));
    }

    private static BigDecimal pesos(long monto) {
        return BigDecimal.valueOf(monto);
    }
}
