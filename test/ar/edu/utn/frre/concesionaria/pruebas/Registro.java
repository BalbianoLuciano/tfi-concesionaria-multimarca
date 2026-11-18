package ar.edu.utn.frre.concesionaria.pruebas;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de casos que arma cada grupo de pruebas.
 */
public final class Registro {

    /** Una prueba ejecutable. Puede lanzar cualquier excepción: el runner la atrapa. */
    @FunctionalInterface
    public interface Prueba {
        /**
         * Ejecuta la prueba.
         *
         * @throws Exception si algo falla
         */
        void ejecutar() throws Exception;
    }

    /** Un caso registrado: identificador, descripción y la prueba. */
    public static final class Caso {
        private final String id;
        private final String descripcion;
        private final Prueba prueba;

        Caso(String id, String descripcion, Prueba prueba) {
            this.id = id;
            this.descripcion = descripcion;
            this.prueba = prueba;
        }

        /** @return el identificador, por ejemplo PR-PRECIO-01 */
        public String id() {
            return id;
        }

        /** @return qué verifica */
        public String descripcion() {
            return descripcion;
        }

        /** @return la prueba */
        public Prueba prueba() {
            return prueba;
        }
    }

    private final List<Caso> casos = new ArrayList<>();

    /**
     * Agrega un caso.
     *
     * @param id          identificador (PR-PRECIO-01)
     * @param descripcion qué verifica
     * @param prueba      el código de la prueba
     */
    public void prueba(String id, String descripcion, Prueba prueba) {
        casos.add(new Caso(id, descripcion, prueba));
    }

    List<Caso> casos() {
        return casos;
    }
}
