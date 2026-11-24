package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Usuario;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Foto (<em>snapshot</em>) completa del sistema que se guarda en un único archivo
 * (ARQUITECTURA §4). Un solo archivo hace que guardar una venta, que toca vehículos y ventas a
 * la vez, sea una operación única: o queda todo o no queda nada.
 */
public class Almacen implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Versión del formato. Cambia solo cuando un cambio rompe la lectura de datos viejos. */
    public static final int VERSION_ESQUEMA_ACTUAL = 2;

    private int versionEsquema = VERSION_ESQUEMA_ACTUAL;
    private LocalDateTime guardadoEn;
    private HashMap<String, Vehiculo> vehiculos = new HashMap<>();
    private HashMap<Integer, Marca> marcas = new HashMap<>();
    private ArrayList<Venta> ventas = new ArrayList<>();
    private HashMap<String, Usuario> usuarios = new HashMap<>();
    private EnumMap<TipoVehiculo, Integer> ultimosCodigos = new EnumMap<>(TipoVehiculo.class);
    private int ultimaVenta;
    private int ultimaMarca;

    /** Crea un almacén vacío, para el primer arranque. */
    public Almacen() {
    }

    /** @return la versión de esquema con que se guardó */
    public int getVersionEsquema() {
        return versionEsquema;
    }

    /** @param versionEsquema la versión de esquema */
    public void setVersionEsquema(int versionEsquema) {
        this.versionEsquema = versionEsquema;
    }

    /** @return cuándo se guardó por última vez */
    public LocalDateTime getGuardadoEn() {
        return guardadoEn;
    }

    /** @param guardadoEn cuándo se guardó */
    public void setGuardadoEn(LocalDateTime guardadoEn) {
        this.guardadoEn = guardadoEn;
    }

    /** @return código → vehículo */
    public Map<String, Vehiculo> getVehiculos() {
        return vehiculos;
    }

    /** @return id → marca */
    public Map<Integer, Marca> getMarcas() {
        return marcas;
    }

    /** @return las ventas en orden cronológico */
    public List<Venta> getVentas() {
        return ventas;
    }

    /** @return usuario → usuario */
    public Map<String, Usuario> getUsuarios() {
        return usuarios;
    }

    /** @return el último número de código usado por tipo */
    public Map<TipoVehiculo, Integer> getUltimosCodigos() {
        return ultimosCodigos;
    }

    /** @return el último número de venta usado */
    public int getUltimaVenta() {
        return ultimaVenta;
    }

    /** @param ultimaVenta el último número de venta usado */
    public void setUltimaVenta(int ultimaVenta) {
        this.ultimaVenta = ultimaVenta;
    }

    /** @return el último id de marca usado */
    public int getUltimaMarca() {
        return ultimaMarca;
    }

    /** @param ultimaMarca el último id de marca usado */
    public void setUltimaMarca(int ultimaMarca) {
        this.ultimaMarca = ultimaMarca;
    }

    /**
     * @return {@code true} si no hay ningún usuario (primer arranque)
     */
    public boolean estaVacio() {
        return usuarios.isEmpty();
    }
}
