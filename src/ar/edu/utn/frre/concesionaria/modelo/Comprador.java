package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Comprador de una venta. POJO inmutable: se valida al construirlo y después no cambia, porque
 * forma parte de un comprobante.
 */
public final class Comprador implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nombreCompleto;
    private final String dni;

    /**
     * @param nombreCompleto nombre y apellido (2 a 60 caracteres, letras y espacios)
     * @param dni            7 u 8 dígitos; acepta puntos ("30.123.456")
     * @throws DatoInvalidoException si algún dato no cumple su regla
     */
    public Comprador(String nombreCompleto, String dni) throws DatoInvalidoException {
        this.nombreCompleto = validarNombre("Nombre y apellido", nombreCompleto);
        this.dni = validarDni(dni);
    }

    /**
     * Regla de nombre de persona (SPEC §3). La comparten comprador y usuario.
     *
     * @param campo  nombre del campo para el mensaje
     * @param nombre el nombre escrito
     * @return el nombre limpio y con mayúscula inicial
     * @throws DatoInvalidoException si no cumple
     */
    public static String validarNombre(String campo, String nombre) throws DatoInvalidoException {
        String limpio = Textos.limpiar(nombre);
        if (!limpio.matches("[\\p{L} ]*")) {
            throw new DatoInvalidoException(campo, nombre,
                    "El nombre solo puede tener letras y espacios.");
        }
        if (limpio.length() < 2 || limpio.length() > 60) {
            throw new DatoInvalidoException(campo, nombre,
                    "El nombre tiene que tener entre 2 y 60 letras.");
        }
        return Textos.nombrePropio(limpio.toLowerCase());
    }

    /**
     * Regla de DNI (SPEC §3).
     *
     * @param dni el DNI escrito, con o sin puntos
     * @return el DNI solo con dígitos
     * @throws DatoInvalidoException si no tiene 7 u 8 dígitos
     */
    public static String validarDni(String dni) throws DatoInvalidoException {
        String limpio = Textos.limpiar(dni).replace(".", "").replace(" ", "");
        if (!limpio.matches("\\d{7,8}")) {
            throw new DatoInvalidoException("DNI", dni, "El DNI tiene 7 u 8 dígitos.");
        }
        return limpio;
    }

    /** @return el nombre y apellido */
    public String getNombreCompleto() {
        return nombreCompleto;
    }

    /** @return el DNI, solo dígitos */
    public String getDni() {
        return dni;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Comprador)) {
            return false;
        }
        Comprador otro = (Comprador) o;
        return dni.equals(otro.dni) && nombreCompleto.equals(otro.nombreCompleto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dni, nombreCompleto);
    }

    @Override
    public String toString() {
        return nombreCompleto + " (DNI " + dni + ")";
    }
}
