package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaCamioneta;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.DesglosePrecio;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoMotocicleta;
import java.math.BigDecimal;

/**
 * PR-PRECIO: reglas de precio de SPEC §4.
 */
public final class PruebasPrecio implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-PRECIO";
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-PRECIO-01", "ejemplo 1: Hilux 2019 usada 4x4 = $ 22.850.920", () -> {
            DesglosePrecio d = Fabrica.hiluxEjemplo().desglosarPrecio(Fabrica.HOY);
            Verificar.monto("13300000", d.getDepreciacionAntiguedad());
            Verificar.monto("3344000", d.getDepreciacionKm());
            Verificar.monto("1494920", d.getRecargos());
            Verificar.monto("0", d.getPatentamiento());
            Verificar.monto("22850920", d.getPrecioFinal());
        });
        r.prueba("PR-PRECIO-02", "ejemplo 2: Honda CB 500F 0 km = $ 12.750.000", () -> {
            DesglosePrecio d = Fabrica.motoNueva("12500000").desglosarPrecio(Fabrica.HOY);
            Verificar.monto("250000", d.getPatentamiento());
            Verificar.monto("12750000", d.getPrecioFinal());
        });
        r.prueba("PR-PRECIO-03", "topes de depreciación de un auto (45 % + 15 %)", () -> {
            DesglosePrecio d = Fabrica.autoUsado(1995, 900_000, "10000000")
                    .desglosarPrecio(Fabrica.HOY);
            Verificar.monto("0.45", d.getPorcentajeAntiguedad());
            Verificar.monto("0.15", d.getPorcentajeKm());
            Verificar.monto("4000000", d.getPrecioFinal());
        });
        r.prueba("PR-PRECIO-04", "un 0 km no se deprecia y paga patentamiento del 3 %", () -> {
            DesglosePrecio d = Fabrica.autoNuevo("10000000").desglosarPrecio(Fabrica.HOY);
            Verificar.monto("0", d.getDepreciacionAntiguedad());
            Verificar.monto("0", d.getDepreciacionKm());
            Verificar.monto("10300000", d.getPrecioFinal());
        });
        r.prueba("PR-PRECIO-05", "patentamiento mínimo de moto: $ 150.000", () -> {
            DesglosePrecio d = Fabrica.motoNueva("2000000").desglosarPrecio(Fabrica.HOY);
            Verificar.monto("150000", d.getPatentamiento());
            Verificar.monto("2150000", d.getPrecioFinal());
        });
        r.prueba("PR-PRECIO-06", "camioneta 0 km 4x4: recargo y patentamiento", () -> {
            Fabrica.fijarHoy();
            Camioneta c = new Camioneta(Estado.NUEVO, Fabrica.marca("Ford"), "Ranger", 2026,
                    Color.NEGRO, new BigDecimal("50000000"), null, null,
                    CarroceriaCamioneta.PICKUP_DOBLE_CABINA, true, 1000);
            DesglosePrecio d = c.desglosarPrecio(Fabrica.HOY);
            Verificar.monto("3500000", d.getRecargos());
            Verificar.monto("1605000", d.getPatentamiento());
            Verificar.monto("55105000", d.getPrecioFinal());
        });
        r.prueba("PR-PRECIO-07", "moto usada: tramos de 5.000 km", () -> {
            Fabrica.fijarHoy();
            Motocicleta m = new Motocicleta(Estado.USADO, Fabrica.marca("Honda"), "Wave", 2024,
                    Color.AZUL, new BigDecimal("10000000"), 12_000, "A123BCD",
                    TipoMotocicleta.CALLE, 110);
            Verificar.monto("8200000", m.calcularPrecioFinal(Fabrica.HOY));
        });
        r.prueba("PR-PRECIO-08", "camioneta 4x2 sin recargo", () -> {
            Camioneta c = Fabrica.hiluxEjemplo();
            c.setTraccion4x4(false);
            Verificar.monto("21356000", c.calcularPrecioFinal(Fabrica.HOY));
        });
        r.prueba("PR-PRECIO-09", "el precio se redondea a pesos enteros", () -> {
            DesglosePrecio d = Fabrica.autoNuevo("999999.5").desglosarPrecio(Fabrica.HOY);
            Verificar.igual(0, d.getPrecioFinal().scale());
            Verificar.monto("1030000", d.getPrecioFinal());
            Verificar.verdadero(d.getPrecioLista().compareTo(new BigDecimal("1000000")) == 0,
                    "el precio de lista se redondea al cargarlo");
        });
    }
}
