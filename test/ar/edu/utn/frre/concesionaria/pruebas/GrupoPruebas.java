package ar.edu.utn.frre.concesionaria.pruebas;

/**
 * Un grupo del plan de pruebas (SPEC §8), por ejemplo PR-PRECIO.
 */
public interface GrupoPruebas {

    /**
     * @return el nombre del grupo, tal como figura en la spec (PR-PRECIO)
     */
    String nombre();

    /**
     * Registra las pruebas del grupo.
     *
     * @param registro donde se anotan los casos
     */
    void registrar(Registro registro);
}
