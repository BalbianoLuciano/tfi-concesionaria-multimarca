package ar.edu.utn.frre.concesionaria.pruebas;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner de pruebas propio, sin JUnit (SPEC §8).
 *
 * <p>Cada grupo ({@link GrupoPruebas}) registra sus pruebas con un identificador
 * (PR-PRECIO-01) y una descripción. El runner las ejecuta, atrapa el {@link AssertionError}
 * o la excepción de cada una sin cortar las demás, y al final muestra un resumen. Si alguna
 * falla, el proceso termina con código 1 para que {@code build.sh} lo note.</p>
 *
 * <p>Uso: {@code ./build.sh test} corre todo; {@code ./build.sh test PR-PRECIO} corre un grupo.</p>
 */
public final class PruebasRunner {

    private PruebasRunner() {
    }

    /** Lista de todos los grupos. Agregar un grupo nuevo es sumar una línea acá. */
    private static List<GrupoPruebas> grupos() {
        return List.of(
                new PruebasComun());
    }

    /**
     * Punto de entrada.
     *
     * @param args opcionalmente, el nombre de un grupo (por ejemplo {@code PR-PRECIO})
     */
    public static void main(String[] args) {
        String filtro = args.length > 0 ? args[0].toUpperCase() : null;
        List<String> fallas = new ArrayList<>();
        int ejecutadas = 0;
        long inicio = System.nanoTime();

        for (GrupoPruebas grupo : grupos()) {
            if (filtro != null && !grupo.nombre().equals(filtro)) {
                continue;
            }
            System.out.println();
            System.out.println(grupo.nombre());
            Registro registro = new Registro();
            grupo.registrar(registro);
            for (Registro.Caso caso : registro.casos()) {
                ejecutadas++;
                try {
                    caso.prueba().ejecutar();
                    System.out.println("  ✓ " + caso.id() + "  " + caso.descripcion());
                } catch (AssertionError e) {
                    System.out.println("  ✗ " + caso.id() + "  " + caso.descripcion());
                    System.out.println("      " + e.getMessage());
                    fallas.add(caso.id());
                } catch (Exception | Error e) {
                    System.out.println("  ✗ " + caso.id() + "  " + caso.descripcion());
                    System.out.println("      excepción inesperada: " + e);
                    fallas.add(caso.id());
                }
            }
        }

        long ms = (System.nanoTime() - inicio) / 1_000_000;
        System.out.println();
        if (ejecutadas == 0) {
            System.out.println("No hay pruebas para «" + filtro + "». Grupos: "
                    + grupos().stream().map(GrupoPruebas::nombre).toList());
            System.exit(2);
        }
        System.out.printf("%d pruebas, %d bien, %d fallidas (%d ms)%n",
                ejecutadas, ejecutadas - fallas.size(), fallas.size(), ms);
        if (!fallas.isEmpty()) {
            System.out.println("Fallaron: " + String.join(", ", fallas));
            System.exit(1);
        }
    }
}
