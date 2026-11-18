package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;

/**
 * PR-PATENTE: formatos de auto y de moto. La unicidad entre vendidos y bajas se prueba en
 * {@link PruebasVenta}, porque la garantiza el negocio.
 */
public final class PruebasPatente implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-PATENTE";
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-PATENTE-01", "auto: ABC123 y AB123CD, normalizando espacios y guiones", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            a.setPatente("abc 123");
            Verificar.igual("ABC123", a.getPatente());
            a.setPatente("ab-123-cd");
            Verificar.igual("AB123CD", a.getPatente());
        });
        r.prueba("PR-PATENTE-02", "auto: rechaza formatos inválidos y de moto", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    () -> a.setPatente("AB12CD"));
            Verificar.igual("\"AB12CD\" no es una patente válida. Formatos: ABC123 o AB123CD.",
                    e.getMessage());
            Verificar.lanza(DatoInvalidoException.class, () -> a.setPatente("123ABC"));
        });
        r.prueba("PR-PATENTE-03", "moto: 123ABC y A123BCD", () -> {
            Motocicleta m = Fabrica.motoNueva("1000000");
            m.setPatente("123abc");
            Verificar.igual("123ABC", m.getPatente());
            m.setPatente("A123BCD");
            Verificar.igual("A123BCD", m.getPatente());
        });
        r.prueba("PR-PATENTE-04", "moto: rechaza una patente de auto", () -> {
            Motocicleta m = Fabrica.motoNueva("1000000");
            DatoInvalidoException e = Verificar.lanza(DatoInvalidoException.class,
                    () -> m.setPatente("AB123CD"));
            Verificar.igual("\"AB123CD\" no es una patente de moto. Formatos: 123ABC o A123BCD.",
                    e.getMessage());
        });
        r.prueba("PR-PATENTE-05", "un 0 km puede no tener patente", () -> {
            Motocicleta m = Fabrica.motoNueva("1000000");
            m.setPatente("  ");
            Verificar.igual(null, m.getPatente());
        });
    }
}
