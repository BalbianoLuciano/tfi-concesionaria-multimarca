package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.Venta;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ventas en memoria sobre un {@link ArrayList}: se agregan al final, así quedan en orden
 * cronológico y agregar es O(1). Buscar por número es O(1) porque el número es correlativo y
 * coincide con la posición, salvo que se haya deshecho alguna; en ese caso se recorre.
 */
public class RepositorioVentasMemoria implements RepositorioVentas {

    private final Almacen almacen;
    private final List<Venta> ventas;

    /**
     * @param almacen el almacén cargado
     */
    public RepositorioVentasMemoria(Almacen almacen) {
        this.almacen = almacen;
        this.ventas = almacen.getVentas();
    }

    @Override
    public void guardar(Venta venta) {
        eliminar(venta.getNumero());
        ventas.add(venta);
    }

    @Override
    public Optional<Venta> buscar(Integer numero) {
        if (numero == null) {
            return Optional.empty();
        }
        int posicion = numero - 1;
        if (posicion >= 0 && posicion < ventas.size()
                && numero.equals(ventas.get(posicion).getNumero())) {
            return Optional.of(ventas.get(posicion));
        }
        return ventas.stream().filter(v -> numero.equals(v.getNumero())).findFirst();
    }

    @Override
    public List<Venta> listar() {
        return new ArrayList<>(ventas);
    }

    @Override
    public void eliminar(Integer numero) {
        ventas.removeIf(v -> v.getNumero().equals(numero));
    }

    @Override
    public int contar() {
        return ventas.size();
    }

    @Override
    public int siguienteNumero() {
        almacen.setUltimaVenta(almacen.getUltimaVenta() + 1);
        return almacen.getUltimaVenta();
    }

    @Override
    public void liberarNumero(int numero) {
        if (almacen.getUltimaVenta() == numero) {
            almacen.setUltimaVenta(numero - 1);
        }
    }
}
