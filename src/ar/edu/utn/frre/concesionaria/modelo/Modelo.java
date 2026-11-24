package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Modelo del catálogo de una marca (Corolla, Hilux, CB 500F), con el tipo de vehículo al que
 * corresponde (CU-12). Existe para que el modelo se elija de una lista y no se escriba libre en
 * cada alta: así "Corolla", "COROLLA" y "korola" no terminan siendo tres modelos distintos.
 *
 * <p>Es un POJO inmutable. Dos modelos son iguales si su nombre compacto coincide (sin
 * mayúsculas, acentos ni espacios): dentro de una marca, "Gol Trend" y "goltrend" son el
 * mismo, aunque se hayan cargado para tipos distintos.</p>
 */
public final class Modelo implements Serializable, Comparable<Modelo> {

    private static final long serialVersionUID = 1L;

    /** Largo máximo del nombre. */
    public static final int LARGO_MAXIMO = 40;

    private final String nombre;
    private final TipoVehiculo tipo;

    /**
     * @param nombre nombre del modelo (se normaliza: "corolla" → "Corolla")
     * @param tipo   tipo de vehículo al que corresponde
     * @throws DatoInvalidoException si el nombre no cumple la regla de CU-12 o falta el tipo
     */
    public Modelo(String nombre, TipoVehiculo tipo) throws DatoInvalidoException {
        this.nombre = normalizar(nombre);
        if (tipo == null) {
            throw new DatoInvalidoException("Tipo", null, "Elegí el tipo de vehículo del modelo.");
        }
        this.tipo = tipo;
    }

    /**
     * Valida y normaliza el nombre de un modelo: 2 a 40 caracteres entre letras, números,
     * espacios, guiones y puntos. Escrito todo en minúsculas y sin números, se guarda con
     * inicial mayúscula.
     *
     * @param nombre el nombre escrito por el usuario
     * @return el nombre normalizado
     * @throws DatoInvalidoException si no cumple la regla
     */
    public static String normalizar(String nombre) throws DatoInvalidoException {
        String limpio = Textos.limpiar(nombre == null ? "" : nombre);
        if (limpio.length() < 2 || limpio.length() > LARGO_MAXIMO) {
            throw new DatoInvalidoException("Modelo", nombre, "El modelo tiene que tener entre 2 y "
                    + LARGO_MAXIMO + " caracteres, por ejemplo Corolla o C3.");
        }
        if (!limpio.matches("[\\p{L}\\d .\\-]+")) {
            throw new DatoInvalidoException("Modelo", nombre,
                    "El modelo admite letras, números, espacios, guiones y puntos.");
        }
        // "corolla" → "Corolla", pero "i10" o "e-208" quedan como están: con números, la
        // minúscula suele ser parte del nombre.
        if (limpio.equals(limpio.toLowerCase()) && !limpio.matches(".*\\d.*")) {
            return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
        }
        return limpio;
    }

    /** @return el nombre */
    public String getNombre() {
        return nombre;
    }

    /** @return el tipo de vehículo */
    public TipoVehiculo getTipo() {
        return tipo;
    }

    /** @return la clave de comparación: sin mayúsculas, acentos, espacios ni signos */
    public String getClave() {
        return Textos.compacta(nombre);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Modelo)) {
            return false;
        }
        return getClave().equals(((Modelo) o).getClave());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClave());
    }

    /** Orden alfabético por nombre, sin importar mayúsculas ni acentos. */
    @Override
    public int compareTo(Modelo otro) {
        return Textos.clave(nombre).compareTo(Textos.clave(otro.nombre));
    }

    @Override
    public String toString() {
        return nombre + " (" + tipo.getEtiqueta() + ")";
    }
}
