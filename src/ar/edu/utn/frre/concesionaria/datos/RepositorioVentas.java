package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.Venta;

/**
 * Historial de ventas, en orden cronológico.
 */
public interface RepositorioVentas extends Repositorio<Venta, Integer> {

    /**
     * Reserva el próximo número correlativo.
     *
     * @return el número
     */
    int siguienteNumero();

    /**
     * Devuelve el último número reservado. Lo usa la venta para deshacer si no se pudo
     * guardar.
     *
     * @param numero el número a liberar (solo si es el último)
     */
    void liberarNumero(int numero);
}
