package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaCamioneta;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.Comprador;
import ar.edu.utn.frre.concesionaria.modelo.ConEtiqueta;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Usuario;
import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * PR-VALID: cada fila de la tabla de SPEC §3 que vive en el modelo. La regla de la clave vive
 * en el negocio y se prueba en {@link PruebasPermisos}.
 */
public final class PruebasValidaciones implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-VALID";
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-VALID-01", "año entre 1990 y el año próximo", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            Verificar.noLanza(() -> a.setAnio(1990));
            Verificar.noLanza(() -> a.setAnio(2027));
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    () -> a.setAnio(1989));
            Verificar.igual("El año tiene que estar entre 1990 y 2027.", e.getMessage());
            Verificar.igual("Año", e.getCampo());
            Verificar.lanza(DatoInvalidoException.class, () -> a.setAnio(2028));
            Verificar.lanza(DatoInvalidoException.class, () -> a.setAnio(null));
        });
        r.prueba("PR-VALID-02", "un 0 km no puede ser anterior al año pasado", () -> {
            Automovil a = Fabrica.autoNuevo("1000000");
            Verificar.noLanza(() -> a.setAnio(2025));
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    () -> a.setAnio(2024));
            Verificar.igual("Un 0 km no puede ser anterior a 2025.", e.getMessage());
        });
        r.prueba("PR-VALID-03", "kilometraje de un usado: 1 a 999.999", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            Verificar.noLanza(() -> a.setKilometraje(1));
            Verificar.noLanza(() -> a.setKilometraje(999_999));
            Verificar.lanza(DatoInvalidoException.class, () -> a.setKilometraje(0));
            Verificar.lanza(DatoInvalidoException.class, () -> a.setKilometraje(1_000_000));
        });
        r.prueba("PR-VALID-04", "un 0 km tiene 0 km", () -> {
            Automovil a = Fabrica.autoNuevo("1000000");
            Verificar.igual(0, a.getKilometraje());
            Verificar.lanza(DatoInvalidoException.class, () -> a.setKilometraje(10));
        });
        r.prueba("PR-VALID-05", "modelo: 1 a 40 caracteres permitidos", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            Verificar.noLanza(() -> a.setModelo("  CB  500F "));
            Verificar.igual("CB 500F", a.getModelo());
            Verificar.noLanza(() -> a.setModelo("Clase A-200 2.0"));
            Verificar.lanza(DatoInvalidoException.class, () -> a.setModelo(""));
            Verificar.lanza(DatoInvalidoException.class, () -> a.setModelo("Hilux/SRX"));
            Verificar.lanza(DatoInvalidoException.class, () -> a.setModelo("x".repeat(41)));
        });
        r.prueba("PR-VALID-06", "precio de lista: mayor a 0 y hasta 2.000.000.000", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            Verificar.noLanza(() -> a.setPrecioLista(new BigDecimal("2000000000")));
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    () -> a.setPrecioLista(BigDecimal.ZERO));
            Verificar.igual("El precio tiene que ser mayor a $ 0.", e.getMessage());
            Verificar.lanza(DatoInvalidoException.class,
                    () -> a.setPrecioLista(new BigDecimal("2000000001")));
        });
        r.prueba("PR-VALID-07", "puertas, carga útil y cilindrada", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            Verificar.lanza(DatoInvalidoException.class, () -> a.setPuertas(1));
            Verificar.lanza(DatoInvalidoException.class, () -> a.setPuertas(6));
            Verificar.noLanza(() -> a.setPuertas(2));
            Camioneta c = Fabrica.hiluxEjemplo();
            Verificar.lanza(DatoInvalidoException.class, () -> c.setCargaUtilKg(299));
            Verificar.lanza(DatoInvalidoException.class, () -> c.setCargaUtilKg(3501));
            Verificar.noLanza(() -> c.setCargaUtilKg(3500));
            Motocicleta m = Fabrica.motoNueva("1000000");
            Verificar.lanza(DatoInvalidoException.class, () -> m.setCilindradaCc(49));
            Verificar.lanza(DatoInvalidoException.class, () -> m.setCilindradaCc(2501));
            Verificar.noLanza(() -> m.setCilindradaCc(50));
        });
        r.prueba("PR-VALID-08", "DNI de 7 u 8 dígitos, acepta puntos", () -> {
            Verificar.igual("30123456", new Comprador("Ana Pérez", "30.123.456").getDni());
            Verificar.igual("1234567", new Comprador("Ana Pérez", "1234567").getDni());
            Verificar.lanza(DatoInvalidoException.class, () -> new Comprador("Ana", "123456"));
            Verificar.lanza(DatoInvalidoException.class, () -> new Comprador("Ana", "123456789"));
        });
        r.prueba("PR-VALID-09", "nombre de persona: letras y espacios", () -> {
            Verificar.igual("Ana María Pérez",
                    new Comprador("  ana   maría PÉREZ ", "30123456").getNombreCompleto());
            Verificar.lanza(DatoInvalidoException.class, () -> new Comprador("Ana 2", "30123456"));
            Verificar.lanza(DatoInvalidoException.class, () -> new Comprador("A", "30123456"));
        });
        r.prueba("PR-VALID-10", "usuario: 4 a 20 minúsculas, números, punto y guion bajo", () -> {
            Usuario u = new Usuario("lbalbiano", "Luciano Balbiano",
                    ar.edu.utn.frre.concesionaria.modelo.Rol.VENDEDOR);
            Verificar.noLanza(() -> u.setUsuario("j.perez_2"));
            Verificar.lanza(DatoInvalidoException.class, () -> u.setUsuario("abc"));
            Verificar.lanza(DatoInvalidoException.class, () -> u.setUsuario("juan perez"));
        });
        r.prueba("PR-VALID-11", "enums desde texto sin importar mayúsculas ni acentos", () -> {
            Verificar.igual(CarroceriaAutomovil.SEDAN,
                    ConEtiqueta.desdeTexto(CarroceriaAutomovil.class, "Carrocería", "sedan"));
            Verificar.igual(CarroceriaCamioneta.PICKUP_DOBLE_CABINA,
                    ConEtiqueta.desdeTexto(CarroceriaCamioneta.class, "Carrocería", "DOBLE CABINA"));
            Verificar.igual(Color.BORDO, ConEtiqueta.desdeTexto(Color.class, "Color", "bordo"));
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    () -> ConEtiqueta.desdeTexto(Color.class, "Color", "fucsia"));
            Verificar.verdadero(e.getMessage().contains("Opciones: Blanco"),
                    "el mensaje lista las opciones: " + e.getMessage());
        });
        r.prueba("PR-VALID-12", "un usado necesita patente", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            Verificar.lanza(DatoInvalidoException.class, () -> a.setPatente(""));
            Automovil b = new Automovil();
            b.setEstado(Estado.USADO);
            b.setMarca(Fabrica.marca("Fiat"));
            b.setModelo("Cronos");
            b.setAnio(2020);
            b.setColor(Color.BLANCO);
            b.setPrecioLista(new BigDecimal("1000000"));
            b.setKilometraje(1000);
            b.setCarroceria(CarroceriaAutomovil.SEDAN);
            b.setPuertas(4);
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    b::validarCompleto);
            Verificar.igual("Patente", e.getCampo());
        });
        r.prueba("PR-VALID-13", "la marca tiene que fabricar el tipo", () -> {
            Marca soloMotos = new Marca("Yamaha", "Japón", EnumSet.of(TipoVehiculo.MOTOCICLETA));
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    () -> a.setMarca(soloMotos));
            Verificar.igual("Yamaha no fabrica automóviles.", e.getMessage());
        });
        r.prueba("PR-VALID-14", "nombre de marca normalizado", () -> {
            Marca m = new Marca("  mercedes-benz ", "alemania", EnumSet.of(TipoVehiculo.AUTOMOVIL));
            Verificar.igual("Mercedes-Benz", m.getNombre());
            Verificar.igual("Alemania", m.getPaisOrigen());
            Verificar.lanza(DatoInvalidoException.class,
                    () -> new Marca("Ford", "EEUU", EnumSet.noneOf(TipoVehiculo.class)));
        });
    }
}
