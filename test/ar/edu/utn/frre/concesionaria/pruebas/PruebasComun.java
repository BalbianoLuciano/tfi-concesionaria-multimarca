package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Textos;
import java.math.BigDecimal;
import java.util.List;

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
        r.prueba("PR-COMUN-10", "leer enteros con la unidad escrita al final", () -> {
            Verificar.igual(85000L, Formatos.leerEntero("85.000 km"));
            Verificar.igual(190L, Formatos.leerEntero("190cc"));
            Verificar.igual(4L, Formatos.leerEntero(" 4 puertas "));
            Verificar.igual(1000L, Formatos.leerEntero("1.000 kg."));
            Verificar.lanza(NumberFormatException.class, () -> Formatos.leerEntero("km 85"));
        });
        r.prueba("PR-COMUN-11", "elegir una opción por su nombre o su comienzo", () -> {
            List<String> tipos = List.of("Automóvil", "Camioneta", "Motocicleta");
            Verificar.igual(List.of(0), Textos.coincidencias("auto", tipos));
            Verificar.igual(List.of(2), Textos.coincidencias("MOTO", tipos));
            List<String> carrocerias = List.of("Sedán", "Hatchback", "Rural", "Coupé",
                    "Convertible");
            Verificar.igual(List.of(0), Textos.coincidencias("sedan", carrocerias));
            Verificar.igual(List.of(3, 4), Textos.coincidencias("c", carrocerias));
            Verificar.igual(List.of(0), Textos.coincidencias("pickup",
                    List.of("Pick-up cabina simple", "Doble cabina")));
            // la coincidencia exacta gana sobre el comienzo: "Usado" no es ambiguo
            Verificar.igual(List.of(1), Textos.coincidencias("usado", List.of("Usados", "Usado")));
            Verificar.igual(List.of(), Textos.coincidencias("rojo", tipos));
        });
    }
}
