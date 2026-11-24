package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Modelo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;

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
        if (version < 2) {
            armarCatalogoDeModelos(almacen);
        }
        almacen.setVersionEsquema(Almacen.VERSION_ESQUEMA_ACTUAL);
        return true;
    }

    /**
     * Esquema 1 → 2: las marcas no tenían catálogo de modelos (CU-12). Se arma con los modelos
     * de los vehículos que ya existen, así nada queda fuera del catálogo.
     */
    private static void armarCatalogoDeModelos(Almacen almacen) {
        for (Vehiculo v : almacen.getVehiculos().values()) {
            Marca marca = v.getMarca();
            if (marca.buscarModelo(v.getModelo()).isEmpty()) {
                try {
                    marca.agregarModelo(new Modelo(v.getModelo(), v.getTipo()));
                } catch (DatoInvalidoException e) {
                    // Un modelo viejo que no cumple la regla nueva (por ejemplo, de una sola
                    // letra) no entra al catálogo, pero el vehículo se conserva tal cual: al
                    // modificarlo se elige un modelo válido. Nunca se bloquea el arranque.
                }
            }
        }
    }
}
