package ar.edu.utn.frre.concesionaria.modelo;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum con un texto para mostrar ("Doble cabina") distinto de su nombre de constante
 * ({@code PICKUP_DOBLE_CABINA}).
 *
 * <p>Todos los enums del modelo la implementan, así la consola y el CSV los muestran y los
 * leen de la misma forma.</p>
 */
public interface ConEtiqueta {

    /**
     * @return el texto para mostrar al usuario
     */
    String getEtiqueta();

    /**
     * Busca la constante cuyo nombre o etiqueta coincida con el texto, sin importar mayúsculas
     * ni acentos: "sedan", "Sedán" y "SEDAN" devuelven {@code SEDAN}.
     *
     * @param <E>   el tipo de enum
     * @param tipo  la clase del enum
     * @param campo nombre del campo, para el mensaje de error
     * @param texto lo que escribió el usuario o vino del CSV
     * @return la constante
     * @throws DatoInvalidoException si no coincide con ninguna, con la lista de opciones válidas
     */
    static <E extends Enum<E> & ConEtiqueta> E desdeTexto(Class<E> tipo, String campo, String texto)
            throws DatoInvalidoException {
        String clave = Textos.clave(texto).replace('_', ' ');
        for (E constante : tipo.getEnumConstants()) {
            if (Textos.clave(constante.getEtiqueta()).equals(clave)
                    || Textos.clave(constante.name()).replace('_', ' ').equals(clave)) {
                return constante;
            }
        }
        String opciones = Arrays.stream(tipo.getEnumConstants())
                .map(ConEtiqueta::getEtiqueta)
                .collect(Collectors.joining(", "));
        throw new DatoInvalidoException(campo, texto,
                "\"" + Textos.limpiar(texto) + "\" no es un valor válido de " + campo
                        + ". Opciones: " + opciones + ".");
    }
}
