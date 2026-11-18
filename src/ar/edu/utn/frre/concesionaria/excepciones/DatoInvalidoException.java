package ar.edu.utn.frre.concesionaria.excepciones;

/**
 * Un dato no cumple su regla (SPEC §3). La lanzan los setters del modelo y las validaciones
 * del negocio; la consola la atrapa y vuelve a preguntar el campo.
 *
 * <p>Lleva el nombre del campo y el valor recibido para que la consola y el importador CSV
 * puedan armar un mensaje preciso ("línea 4, campo Año: ...") sin parsear texto.</p>
 */
public class DatoInvalidoException extends ConcesionariaException {

    private static final long serialVersionUID = 1L;

    private final String campo;
    private final String valor;

    /**
     * @param campo   nombre del campo tal como lo ve el usuario ("Año")
     * @param valor   el valor que llegó, como texto (puede ser {@code null})
     * @param mensaje qué estuvo mal y cómo corregirlo
     */
    public DatoInvalidoException(String campo, String valor, String mensaje) {
        super(mensaje);
        this.campo = campo;
        this.valor = valor;
    }

    /**
     * @param campo   nombre del campo
     * @param valor   el valor que llegó
     * @param mensaje qué estuvo mal y cómo corregirlo
     * @param causa   la excepción original, por ejemplo una {@link NumberFormatException}
     */
    public DatoInvalidoException(String campo, String valor, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.campo = campo;
        this.valor = valor;
    }

    /** @return el nombre del campo ("Año") */
    public String getCampo() {
        return campo;
    }

    /** @return el valor recibido, como texto */
    public String getValor() {
        return valor;
    }
}
