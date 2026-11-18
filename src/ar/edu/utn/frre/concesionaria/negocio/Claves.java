package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Hash de claves con sal (ARQUITECTURA §5). Se guarda {@code SHA-256(sal + clave)} y la sal;
 * nunca la clave.
 *
 * <p>Contenido extra (SPEC §7): {@link MessageDigest} calcula el SHA-256 y {@link SecureRandom}
 * genera una sal impredecible de 16 bytes por usuario, así dos usuarios con la misma clave
 * tienen hashes distintos.</p>
 *
 * <p>Limitación conocida: un sistema real usaría un algoritmo lento como bcrypt o Argon2, que
 * no vienen en el JDK.</p>
 */
public final class Claves {

    private static final SecureRandom AZAR = new SecureRandom();

    private Claves() {
    }

    /**
     * Regla de clave (SPEC §3): mínimo 8 caracteres, con al menos una letra y un número.
     *
     * @param clave la clave
     * @throws DatoInvalidoException si no cumple
     */
    public static void validar(String clave) throws DatoInvalidoException {
        if (clave == null || clave.length() < 8 || !clave.matches(".*\\p{L}.*")
                || !clave.matches(".*\\d.*")) {
            throw new DatoInvalidoException("Clave", null,
                    "La clave necesita al menos 8 caracteres, con letras y números.");
        }
    }

    /**
     * @return una sal nueva en hexadecimal
     */
    public static String generarSal() {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        return HexFormat.of().formatHex(sal);
    }

    /**
     * @param clave la clave en texto plano
     * @param sal   la sal en hexadecimal
     * @return el hash en hexadecimal
     */
    public static String hashear(String clave, String sal) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(HexFormat.of().parseHex(sal));
            return HexFormat.of().formatHex(sha.digest(clave.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Todo JDK trae SHA-256: si falta, el entorno está roto.
            throw new IllegalStateException("El JDK no tiene SHA-256", e);
        }
    }

    /**
     * Compara en tiempo constante para no dar pistas por la demora.
     *
     * @param clave la clave escrita
     * @param sal   la sal guardada
     * @param hash  el hash guardado
     * @return {@code true} si coincide
     */
    public static boolean coincide(String clave, String sal, String hash) {
        if (clave == null || sal == null || hash == null) {
            return false;
        }
        return MessageDigest.isEqual(hashear(clave, sal).getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8));
    }
}
