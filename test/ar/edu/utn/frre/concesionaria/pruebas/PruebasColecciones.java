package ar.edu.utn.frre.concesionaria.pruebas;

import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.Rol;
import ar.edu.utn.frre.concesionaria.modelo.Usuario;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * PR-COLECCION: {@code equals}, {@code hashCode} y {@code compareTo} de cada entidad (MODELO §2).
 */
public final class PruebasColecciones implements GrupoPruebas {

    @Override
    public String nombre() {
        return "PR-COLECCION";
    }

    @Override
    public void registrar(Registro r) {
        r.prueba("PR-COLECCION-01", "vehículos iguales por código, aunque cambie la patente", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            a.setCodigo("A-0001");
            Vehiculo b = a.copia();
            b.setPatente("AAA111");
            Verificar.igual(a, b);
            Verificar.igual(a.hashCode(), b.hashCode());
            Set<Vehiculo> conjunto = new HashSet<>(List.of(a, b));
            Verificar.igual(1, conjunto.size());
            Automovil c = Fabrica.autoUsado(2015, 50_000, "1000000");
            c.setCodigo("A-0002");
            Verificar.verdadero(!a.equals(c), "códigos distintos no son iguales");
        });
        r.prueba("PR-COLECCION-02", "la copia es independiente del original", () -> {
            Automovil a = Fabrica.autoUsado(2015, 50_000, "1000000");
            Automovil copia = (Automovil) a.copia();
            copia.setPuertas(2);
            copia.setModelo("Etios");
            Verificar.igual(4, a.getPuertas());
            Verificar.igual("Corolla", a.getModelo());
            Verificar.verdadero(a.getMarca() == copia.getMarca(), "la marca se comparte");
        });
        r.prueba("PR-COLECCION-03", "orden natural: tipo, marca, modelo y código", () -> {
            Motocicleta moto = Fabrica.motoNueva("1000000");
            moto.setCodigo("M-0001");
            Camioneta hilux = Fabrica.hiluxEjemplo();
            hilux.setCodigo("C-0001");
            Automovil corolla = Fabrica.autoUsado(2015, 50_000, "1000000");
            corolla.setCodigo("A-0002");
            Automovil cronos = Fabrica.autoUsado(2015, 50_000, "1000000");
            cronos.setMarca(Fabrica.marca("Fiat"));
            cronos.setCodigo("A-0003");
            TreeSet<Vehiculo> ordenados = new TreeSet<>(List.of(moto, hilux, corolla, cronos));
            List<String> codigos = new ArrayList<>();
            ordenados.forEach(v -> codigos.add(v.getCodigo()));
            Verificar.igual(List.of("A-0003", "A-0002", "C-0001", "M-0001"), codigos);
        });
        r.prueba("PR-COLECCION-04", "marcas iguales sin importar mayúsculas ni acentos", () -> {
            Marca a = Fabrica.marca("Citroën");
            Marca b = Fabrica.marca("CITROEN");
            Verificar.igual(a, b);
            Verificar.igual(a.hashCode(), b.hashCode());
            Verificar.igual(0, a.compareTo(b));
        });
        r.prueba("PR-COLECCION-05", "ventas iguales y ordenadas por número", () -> {
            Venta v1 = new Venta();
            v1.setNumero(1);
            Venta otra1 = new Venta();
            otra1.setNumero(1);
            Venta v2 = new Venta();
            v2.setNumero(2);
            Verificar.igual(v1, otra1);
            Verificar.igual(v1.hashCode(), otra1.hashCode());
            Verificar.verdadero(v1.compareTo(v2) < 0, "la 1 va antes que la 2");
        });
        r.prueba("PR-COLECCION-06", "usuarios iguales por nombre de usuario", () -> {
            Usuario a = new Usuario("ana.perez", "Ana Pérez", Rol.VENDEDOR);
            Usuario b = new Usuario("ANA.PEREZ", "Otra Persona", Rol.DIRECTIVO);
            Verificar.igual(a, b);
            Verificar.igual(a.hashCode(), b.hashCode());
            Verificar.verdadero(!a.toString().contains("hash"), "toString no expone credenciales");
        });
    }
}
