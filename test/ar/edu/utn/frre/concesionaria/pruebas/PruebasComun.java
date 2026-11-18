package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Textos;
import java.math.BigDecimal;

/**
 * PR-COMUN: textos y formatos argentinos que usan todas las capas.
 */
public final class PruebasComun implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-COMUN";
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-COMUN-01", "limpiar recorta y colapsa espacios", () ->
                Verificar.igual("Peugeot 208", Textos.limpiar("  Peugeot    208 ")));
        r.prueba("PR-COMUN-02", "la clave ignora acentos y mayúsculas", () ->
                Verificar.igual(Textos.clave("citroen"), Textos.clave(" CITROËN ")));
        r.prueba("PR-COMUN-03", "nombre propio normaliza marcas", () -> {
            Verificar.igual("Toyota", Textos.nombrePropio("  toyota "));
            Verificar.igual("Mercedes-Benz", Textos.nombrePropio("MERCEDES-BENZ"));
            Verificar.igual("BMW", Textos.nombrePropio("BMW"));
        });
        r.prueba("PR-COMUN-04", "distancia de edición", () ->
                Verificar.igual(2, Textos.distancia("tollota", "toyota")));
        r.prueba("PR-COMUN-05", "moneda con separador de miles argentino", () -> {
            Verificar.igual("$ 22.850.920", Formatos.moneda(new BigDecimal("22850920")));
            Verificar.igual("-$ 3.344.000", Formatos.moneda(new BigDecimal("-3344000")));
        });
        r.prueba("PR-COMUN-06", "kilometraje y porcentaje", () -> {
            Verificar.igual("112.900 km", Formatos.km(112900));
            Verificar.igual("8,8 %", Formatos.porcentaje(new BigDecimal("0.088")));
        });
        r.prueba("PR-COMUN-07", "leer enteros con y sin puntos de miles", () -> {
            Verificar.igual(84500L, Formatos.leerEntero("84.500"));
            Verificar.igual(84500L, Formatos.leerEntero("84500"));
            Verificar.lanza(NumberFormatException.class, () -> Formatos.leerEntero("84.50"));
            Verificar.lanza(NumberFormatException.class, () -> Formatos.leerEntero("dos mil"));
        });
        r.prueba("PR-COMUN-08", "leer montos", () -> {
            Verificar.monto("38000000", Formatos.leerMonto("38.000.000"));
            Verificar.monto("38000000", Formatos.leerMonto("$ 38000000"));
            Verificar.monto("1234.5", Formatos.leerMonto("1.234,50"));
        });
        r.prueba("PR-COMUN-09", "recortar con puntos suspensivos", () ->
                Verificar.igual("Mercedes…", Textos.recortar("Mercedes-Benz", 9)));
    }
}
