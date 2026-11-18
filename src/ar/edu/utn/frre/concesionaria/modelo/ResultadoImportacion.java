package ar.edu.utn.frre.concesionaria.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado de leer un CSV: los vehículos válidos y los errores por línea. Si hay al menos un
 * error, no se importa nada (CU-15).
 */
public final class ResultadoImportacion {

    /** Un error de una fila: línea, campo y motivo. */
    public static final class Error {
        private final int linea;
        private final String campo;
        private final String motivo;

        /**
         * @param linea  número de línea del archivo (la 1 es el encabezado)
         * @param campo  campo con problemas
         * @param motivo qué estuvo mal
         */
        public Error(int linea, String campo, String motivo) {
            this.linea = linea;
            this.campo = campo;
            this.motivo = motivo;
        }

        /** @return el número de línea */
        public int getLinea() {
            return linea;
        }

        /** @return el campo */
        public String getCampo() {
            return campo;
        }

        /** @return el motivo */
        public String getMotivo() {
            return motivo;
        }

        @Override
        public String toString() {
            return "línea " + linea + ", " + campo + ": " + motivo;
        }
    }

    private final List<Vehiculo> vehiculos = new ArrayList<>();
    private final List<Integer> lineas = new ArrayList<>();
    private final List<Error> errores = new ArrayList<>();

    /**
     * Agrega un vehículo válido, con la línea de donde vino.
     *
     * @param vehiculo el vehículo
     * @param linea    la línea del archivo
     */
    public void agregar(Vehiculo vehiculo, int linea) {
        vehiculos.add(vehiculo);
        lineas.add(linea);
    }

    /**
     * @param indice posición en {@link #getVehiculos()}
     * @return la línea del archivo de ese vehículo
     */
    public int getLinea(int indice) {
        return lineas.get(indice);
    }

    /**
     * Agrega un error.
     *
     * @param linea  línea
     * @param campo  campo
     * @param motivo motivo
     */
    public void agregarError(int linea, String campo, String motivo) {
        errores.add(new Error(linea, campo, motivo));
    }

    /** @return los vehículos válidos */
    public List<Vehiculo> getVehiculos() {
        return Collections.unmodifiableList(vehiculos);
    }

    /** @return los errores */
    public List<Error> getErrores() {
        return Collections.unmodifiableList(errores);
    }

    /** @return {@code true} si hubo al menos un error */
    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
