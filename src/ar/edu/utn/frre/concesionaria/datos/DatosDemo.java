package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaCamioneta;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Modelo;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoMotocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

/**
 * Datos de demostración del primer arranque (CU-01): 8 marcas y 10 vehículos, mezclando tipos,
 * 0 km y usados. Los 0 km toman el año actual, así siguen siendo válidos con el paso del
 * tiempo.
 */
public class DatosDemo implements CargadorDemo {

    private static final EnumSet<TipoVehiculo> AUTOS_Y_CAMIONETAS =
            EnumSet.of(TipoVehiculo.AUTOMOVIL, TipoVehiculo.CAMIONETA);

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
        Marca toyota = marca("Toyota", "Japón", AUTOS_Y_CAMIONETAS);
        Marca ford = marca("Ford", "Estados Unidos", AUTOS_Y_CAMIONETAS);
        Marca volkswagen = marca("Volkswagen", "Alemania", AUTOS_Y_CAMIONETAS);
        Marca chevrolet = marca("Chevrolet", "Estados Unidos", AUTOS_Y_CAMIONETAS);
        Marca fiat = marca("Fiat", "Italia", EnumSet.of(TipoVehiculo.AUTOMOVIL));
        Marca peugeot = marca("Peugeot", "Francia", AUTOS_Y_CAMIONETAS);
        Marca honda = marca("Honda", "Japón",
                EnumSet.of(TipoVehiculo.AUTOMOVIL, TipoVehiculo.MOTOCICLETA));
        Marca yamaha = marca("Yamaha", "Japón", EnumSet.of(TipoVehiculo.MOTOCICLETA));

        // Catálogo de modelos (CU-12): los del stock de ejemplo y otros habituales en el país.
        TipoVehiculo auto = TipoVehiculo.AUTOMOVIL;
        TipoVehiculo camioneta = TipoVehiculo.CAMIONETA;
        TipoVehiculo moto = TipoVehiculo.MOTOCICLETA;
        modelos(toyota, auto, "Corolla", "Etios", "Yaris");
        modelos(toyota, camioneta, "Hilux", "SW4", "Corolla Cross", "RAV4");
        modelos(ford, auto, "Ka", "Fiesta", "Focus", "Mustang");
        modelos(ford, camioneta, "Ranger", "Territory", "Bronco", "Maverick");
        modelos(volkswagen, auto, "Gol Trend", "Polo", "Virtus", "Vento");
        modelos(volkswagen, camioneta, "Amarok", "Taos", "T-Cross", "Nivus");
        modelos(chevrolet, auto, "Onix", "Cruze", "Prisma");
        modelos(chevrolet, camioneta, "Tracker", "S10", "Equinox");
        modelos(fiat, auto, "Cronos", "Argo", "Mobi", "Uno", "Pulse");
        modelos(peugeot, auto, "208", "308", "408");
        modelos(peugeot, camioneta, "2008", "3008", "Partner");
        modelos(honda, auto, "Civic", "City", "Fit");
        modelos(honda, moto, "CB 500F", "CB 190R", "Wave", "XR 150L", "Navi");
        modelos(yamaha, moto, "FZ", "YBR 125", "XTZ 250", "MT-03");

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
                new Motocicleta(Estado.USADO, yamaha, "FZ", 2020, Color.AZUL,
                        pesos(3_900_000), 21_000, "A123BCD", TipoMotocicleta.CALLE, 150));

        for (Vehiculo v : lista) {
            v.setCodigo(vehiculos.siguienteCodigo(v.getTipo()));
            v.setFechaIngreso(Reloj.hoy());
            vehiculos.guardar(v);
        }
    }

    private Marca marca(String nombre, String pais, EnumSet<TipoVehiculo> tipos)
            throws ConcesionariaException {
        Marca existente = marcas.buscarPorNombre(nombre).orElse(null);
        if (existente != null) {
            return existente;
        }
        Marca m = new Marca(nombre, pais, tipos);
        m.setId(marcas.siguienteId());
        marcas.guardar(m);
        return m;
    }

    /** Agrega al catálogo de la marca los modelos que todavía no tiene. */
    private static void modelos(Marca marca, TipoVehiculo tipo, String... nombres)
            throws ConcesionariaException {
        for (String nombre : nombres) {
            if (marca.buscarModelo(nombre).isEmpty()) {
                marca.agregarModelo(new Modelo(nombre, tipo));
            }
        }
    }

    private static BigDecimal pesos(long monto) {
        return BigDecimal.valueOf(monto);
    }
}
