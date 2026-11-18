package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Vehículos en memoria. Además del mapa código → vehículo, mantiene un índice patente → código
 * para que controlar una patente repetida sea O(1) y no un recorrido por cada alta.
 */
public class RepositorioVehiculosMemoria extends RepositorioMemoria<Vehiculo, String>
        implements RepositorioVehiculos {

    private final Almacen almacen;
    private final Map<String, String> indicePatentes = new HashMap<>();

    /**
     * @param almacen el almacén cargado
     */
    public RepositorioVehiculosMemoria(Almacen almacen) {
        super(almacen.getVehiculos());
        this.almacen = almacen;
        for (Vehiculo v : mapa.values()) {
            if (v.getPatente() != null) {
                indicePatentes.put(v.getPatente(), v.getCodigo());
            }
        }
    }

    @Override
    public void guardar(Vehiculo vehiculo) {
        Vehiculo anterior = mapa.get(vehiculo.getCodigo());
        if (anterior != null && anterior.getPatente() != null) {
            indicePatentes.remove(anterior.getPatente());
        }
        super.guardar(vehiculo);
        if (vehiculo.getPatente() != null) {
            indicePatentes.put(vehiculo.getPatente(), vehiculo.getCodigo());
        }
    }

    @Override
    public void eliminar(String codigo) {
        Vehiculo anterior = mapa.get(codigo);
        if (anterior != null && anterior.getPatente() != null) {
            indicePatentes.remove(anterior.getPatente());
        }
        super.eliminar(codigo);
    }

    @Override
    public Optional<Vehiculo> buscarPorPatente(String patente) {
        String codigo = indicePatentes.get(Vehiculo.normalizarPatente(patente));
        return codigo == null ? Optional.empty() : buscar(codigo);
    }

    @Override
    public String siguienteCodigo(TipoVehiculo tipo) {
        // merge: suma 1 al contador del tipo, o lo arranca en 1 (autoboxing de Integer).
        int numero = almacen.getUltimosCodigos().merge(tipo, 1, Integer::sum);
        return String.format("%c-%04d", tipo.getPrefijo(), numero);
    }
}
