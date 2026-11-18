package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;

/**
 * Único lugar donde se adaptan datos guardados por versiones anteriores del programa
 * (ARQUITECTURA §4).
 *
 * <p>Los cambios que no rompen (un campo nuevo) los resuelve la serialización con
 * {@code serialVersionUID} fijo: el campo nuevo llega en {@code null}. Los que sí rompen suben
 * {@link Almacen#VERSION_ESQUEMA_ACTUAL} y agregan un paso acá.</p>
 */
public final class MigradorAlmacen {

    private MigradorAlmacen() {
    }

    /**
     * Lleva el almacén a la versión actual.
     *
     * @param almacen el almacén leído
     * @return {@code true} si hubo que migrar
     * @throws PersistenciaException si los datos son de una versión más nueva del programa
     */
    public static boolean migrar(Almacen almacen) throws PersistenciaException {
        int version = almacen.getVersionEsquema();
        if (version > Almacen.VERSION_ESQUEMA_ACTUAL) {
            throw new PersistenciaException("Los datos son de una versión más nueva del programa"
                    + " (esquema " + version + "). No se tocaron: abrilos con esa versión.");
        }
        if (version == Almacen.VERSION_ESQUEMA_ACTUAL) {
            return false;
        }
        // Acá van los pasos: if (version < 2) { ...; version = 2; }
        almacen.setVersionEsquema(Almacen.VERSION_ESQUEMA_ACTUAL);
        return true;
    }
}
