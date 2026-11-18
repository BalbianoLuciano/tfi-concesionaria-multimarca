package ar.edu.utn.frre.concesionaria.pruebas;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Verificaciones para las pruebas. Cada método lanza {@link AssertionError} con un mensaje
 * que dice qué se esperaba y qué llegó.
 */
public final class Verificar {

    private Verificar() {
    }

    /** Algo que se espera que lance una excepción. */
    @FunctionalInterface
    public interface Accion {
        /**
         * Ejecuta la acción.
         *
         * @throws Exception la excepción esperada
         */
        void ejecutar() throws Exception;
    }

    /**
     * Verifica que dos valores sean iguales según {@code equals}.
     *
     * @param esperado valor esperado
     * @param real     valor obtenido
     */
    public static void igual(Object esperado, Object real) {
        if (!Objects.equals(esperado, real)) {
            throw new AssertionError("se esperaba «" + esperado + "» y llegó «" + real + "»");
        }
    }

    /**
     * Verifica que dos montos sean iguales sin importar la escala (38000000 = 38000000.00).
     *
     * @param esperado monto esperado, como texto
     * @param real     monto obtenido
     */
    public static void monto(String esperado, BigDecimal real) {
        if (real == null || new BigDecimal(esperado).compareTo(real) != 0) {
            throw new AssertionError("se esperaba $ " + esperado + " y llegó $ " + real);
        }
    }

    /**
     * Verifica que una condición sea verdadera.
     *
     * @param condicion la condición
     * @param mensaje   qué se esperaba
     */
    public static void verdadero(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    /**
     * Verifica que la acción lance una excepción del tipo indicado (o de una subclase).
     *
     * @param <E>    tipo esperado
     * @param tipo   clase de la excepción esperada
     * @param accion la acción
     * @return la excepción lanzada, para poder revisar su mensaje
     */
    public static <E extends Throwable> E lanza(Class<E> tipo, Accion accion) {
        try {
            accion.ejecutar();
        } catch (Throwable t) {
            if (tipo.isInstance(t)) {
                return tipo.cast(t);
            }
            throw new AssertionError("se esperaba " + tipo.getSimpleName() + " y llegó " + t);
        }
        throw new AssertionError("se esperaba " + tipo.getSimpleName() + " y no se lanzó nada");
    }

    /**
     * Verifica que la acción no lance ninguna excepción.
     *
     * @param accion la acción
     */
    public static void noLanza(Accion accion) {
        try {
            accion.ejecutar();
        } catch (Exception e) {
            throw new AssertionError("no se esperaba una excepción y llegó " + e);
        }
    }
}
