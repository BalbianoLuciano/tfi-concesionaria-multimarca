package ar.edu.utn.frre.concesionaria.comun;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades de texto que comparten todas las capas.
 *
 * <p>Contenido extra (SPEC §7): {@link Normalizer} se usa para quitar acentos y poder comparar
 * "Citroën" con "citroen" sin importar cómo lo escriba el usuario.</p>
 */
public final class Textos {

    private Textos() {
    }

    /**
     * Recorta los espacios de los extremos y colapsa los espacios internos repetidos.
     *
     * @param texto el texto (puede ser {@code null})
     * @return el texto limpio, o {@code ""} si era {@code null}
     */
    public static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().replaceAll("\\s+", " ");
    }

    /**
     * Indica si el texto es {@code null} o queda vacío después de limpiarlo.
     *
     * @param texto el texto
     * @return {@code true} si no tiene contenido
     */
    public static boolean estaVacio(String texto) {
        return limpiar(texto).isEmpty();
    }

    /**
     * Quita tildes y diéresis: "Citroën" → "Citroen". La ñ se conserva como n.
     *
     * @param texto el texto
     * @return el texto sin marcas diacríticas
     */
    public static String sinAcentos(String texto) {
        // SPEC §7: Normalizer separa la letra de su acento (NFD) y el regex borra los acentos.
        String descompuesto = Normalizer.normalize(limpiar(texto), Normalizer.Form.NFD);
        return descompuesto.replaceAll("\\p{M}", "");
    }

    /**
     * Clave para comparar textos sin importar mayúsculas, acentos ni espacios repetidos.
     * "  CITROËN " y "citroen" tienen la misma clave.
     *
     * @param texto el texto
     * @return la clave de comparación
     */
    public static String clave(String texto) {
        return sinAcentos(texto).toLowerCase();
    }

    /**
     * Escribe un nombre propio con mayúscula inicial en cada palabra: "  toyota " → "Toyota",
     * "MERCEDES-BENZ" → "Mercedes-Benz". Las siglas cortas que ya vienen en mayúsculas
     * ("BMW", "KTM") se respetan.
     *
     * @param texto el nombre tal como lo escribió el usuario
     * @return el nombre normalizado
     */
    public static String nombrePropio(String texto) {
        String limpio = limpiar(texto);
        StringBuilder resultado = new StringBuilder(limpio.length());
        for (String palabra : limpio.split(" ")) {
            if (resultado.length() > 0) {
                resultado.append(' ');
            }
            boolean esSigla = palabra.length() <= 3 && palabra.equals(palabra.toUpperCase());
            resultado.append(esSigla ? palabra : capitalizarConGuiones(palabra));
        }
        return resultado.toString();
    }

    private static String capitalizarConGuiones(String palabra) {
        StringBuilder sb = new StringBuilder(palabra.length());
        boolean inicio = true;
        for (char c : palabra.toCharArray()) {
            sb.append(inicio ? Character.toUpperCase(c) : Character.toLowerCase(c));
            inicio = c == '-';
        }
        return sb.toString();
    }

    /**
     * Opciones que coinciden con lo que escribió el usuario, para elegir de una lista por el
     * nombre en lugar del número (SPEC §3): primero las iguales, si no las que empiezan así y
     * si no las que lo contienen. No importan mayúsculas, acentos, espacios ni guiones:
     * "sedan" encuentra "Sedán" y "pickup", "Pick-up cabina simple".
     *
     * @param texto     lo que escribió el usuario
     * @param etiquetas el texto de cada opción
     * @return las posiciones (desde 0) de las que coinciden; vacía si ninguna
     */
    public static List<Integer> coincidencias(String texto, List<String> etiquetas) {
        String buscado = compacta(texto);
        if (buscado.isEmpty()) {
            return List.of();
        }
        List<List<Integer>> niveles = List.of(new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>());
        for (int i = 0; i < etiquetas.size(); i++) {
            String etiqueta = compacta(etiquetas.get(i));
            if (etiqueta.equals(buscado)) {
                niveles.get(0).add(i);
            } else if (etiqueta.startsWith(buscado)) {
                niveles.get(1).add(i);
            } else if (etiqueta.contains(buscado)) {
                niveles.get(2).add(i);
            }
        }
        return niveles.stream().filter(n -> !n.isEmpty()).findFirst().orElse(List.of());
    }

    /** @return la clave sin espacios, guiones ni signos: "Pick-up" → "pickup" */
    private static String compacta(String texto) {
        return clave(texto).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Distancia de edición entre dos textos (cantidad de letras a cambiar, agregar o quitar).
     * Se usa para avisar de marcas parecidas: "Tollota" está a 2 de "Toyota".
     *
     * @param a primer texto
     * @param b segundo texto
     * @return la distancia de Levenshtein
     */
    public static int distancia(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1, anterior[j] + 1),
                        anterior[j - 1] + costo);
            }
            int[] tmp = anterior;
            anterior = actual;
            actual = tmp;
        }
        return anterior[b.length()];
    }

    /**
     * Recorta un texto al ancho indicado agregando "…" si no entra.
     *
     * @param texto el texto
     * @param ancho cantidad máxima de caracteres
     * @return el texto recortado
     */
    public static String recortar(String texto, int ancho) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= ancho) {
            return texto;
        }
        if (ancho <= 1) {
            return "…".substring(0, Math.max(ancho, 0));
        }
        return texto.substring(0, ancho - 1) + "…";
    }
}
