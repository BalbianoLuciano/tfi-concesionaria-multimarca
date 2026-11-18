package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.util.Optional;

/**
 * Repositorio de vehículos, indexado por código. Mantiene además un índice de patentes para
 * controlar la unicidad en O(1), incluyendo vendidos y dados de baja.
 */
public interface RepositorioVehiculos extends Repositorio<Vehiculo, String> {

    /**
     * @param patente patente normalizada
     * @return el vehículo con esa patente, o vacío
     */
    Optional<Vehiculo> buscarPorPatente(String patente);

    /**
     * Reserva el próximo código del tipo: A-0001, A-0002… Nunca reutiliza un código.
     *
     * @param tipo el tipo de vehículo
     * @return el código nuevo
     */
    String siguienteCodigo(TipoVehiculo tipo);
}
