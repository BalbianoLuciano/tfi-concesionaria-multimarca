package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculos;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.PatenteDuplicadaException;
import ar.edu.utn.frre.concesionaria.excepciones.ReglaNegocioException;
import ar.edu.utn.frre.concesionaria.excepciones.VehiculoNoEncontradoException;
import ar.edu.utn.frre.concesionaria.modelo.DesglosePrecio;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stock de vehículos: registrar, listar, buscar, ver, modificar, dar de baja y cotizar
 * (CU-03 a CU-09).
 */
public class ServicioVehiculos extends ServicioBase {

    private final RepositorioVehiculos vehiculos;
    private final ServicioMarcas catalogo;

    /**
     * @param vehiculos    repositorio de vehículos
     * @param catalogo     marcas, para controlar que el modelo esté en su catálogo
     * @param persistencia guardado
     */
    public ServicioVehiculos(RepositorioVehiculos vehiculos, ServicioMarcas catalogo,
            Persistencia persistencia) {
        super(persistencia);
        this.vehiculos = vehiculos;
        this.catalogo = catalogo;
    }

    /**
     * Registra un vehículo cargado por la consola o el importador (CU-03). Revalida lo que
     * mira el conjunto: datos completos, marca activa y patente única. Asigna el código.
     *
     * @param sesion   la sesión
     * @param vehiculo el vehículo, sin código
     * @return el código asignado (C-0004)
     * @throws ConcesionariaException si no tiene permiso, falta un dato, la patente ya existe o
     *                                no se pudo guardar (en ese caso el vehículo ya tiene código
     *                                y queda pendiente de guardar)
     */
    public String registrar(Sesion sesion, Vehiculo vehiculo) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_STOCK);
        validarParaAlta(vehiculo);
        vehiculo.setCodigo(vehiculos.siguienteCodigo(vehiculo.getTipo()));
        vehiculo.setFechaIngreso(Reloj.hoy());
        vehiculos.guardar(vehiculo);
        guardarCambios();
        return vehiculo.getCodigo();
    }

    /**
     * Controles del alta que no puede hacer el modelo solo. Lo usa también la importación.
     *
     * @param vehiculo el vehículo
     * @throws ConcesionariaException si algo no corresponde
     */
    void validarParaAlta(Vehiculo vehiculo) throws ConcesionariaException {
        vehiculo.validarCompleto();
        if (!vehiculo.getMarca().estaActiva()) {
            throw new ReglaNegocioException("La marca " + vehiculo.getMarca().getNombre()
                    + " está desactivada. Reactivala en Marcas o elegí otra.");
        }
        exigirPatenteLibre(vehiculo.getPatente(), null);
        catalogo.exigirModeloDelCatalogo(vehiculo.getMarca(), vehiculo.getTipo(),
                vehiculo.getModelo());
    }

    /**
     * Verifica que la patente no esté en otro vehículo (incluidos vendidos y bajas). Lo usa la
     * consola para repreguntar enseguida, sin esperar al final del alta.
     *
     * @param patente       la patente (puede ser {@code null})
     * @param codigoPropio  el código del vehículo que se está modificando, o {@code null}
     * @throws PatenteDuplicadaException si ya está registrada en otro
     */
    public void exigirPatenteLibre(String patente, String codigoPropio)
            throws PatenteDuplicadaException {
        if (patente == null) {
            return;
        }
        Vehiculo otro = vehiculos.buscarPorPatente(patente).orElse(null);
        if (otro != null && !otro.getCodigo().equals(codigoPropio)) {
            throw new PatenteDuplicadaException("La patente " + otro.getPatente()
                    + " ya está registrada en " + otro.getCodigo() + ".");
        }
    }

    /**
     * Ver ficha (CU-06).
     *
     * @param sesion la sesión
     * @param codigo el código ("c-4" y "C-0004" valen igual)
     * @return el vehículo
     * @throws ConcesionariaException si no tiene permiso o no existe
     */
    public Vehiculo buscar(Sesion sesion, String codigo) throws ConcesionariaException {
        sesion.exigir(Permiso.VER_STOCK);
        String normalizado = normalizarCodigo(codigo);
        return vehiculos.buscar(normalizado).orElseThrow(() -> new VehiculoNoEncontradoException(
                "No hay ningún vehículo con el código " + Textos.limpiar(codigo).toUpperCase()
                        + ". Buscalo por marca o modelo en Buscar."));
    }

    /**
     * Acepta códigos abreviados: "c4", "C-4" y "c-0004" son C-0004.
     *
     * @param codigo lo que escribió el usuario
     * @return el código normalizado, o el texto en mayúsculas si no tiene forma de código
     */
    public static String normalizarCodigo(String codigo) {
        String limpio = Textos.limpiar(codigo).toUpperCase().replace(" ", "");
        if (limpio.matches("[ACM]-?\\d{1,6}")) {
            int numero = Integer.parseInt(limpio.replaceAll("\\D", ""));
            return String.format("%c-%04d", limpio.charAt(0), numero);
        }
        return limpio;
    }

    /**
     * Listado con filtros y orden (CU-04).
     *
     * @param sesion la sesión
     * @param filtro los filtros
     * @param orden  el orden
     * @return los vehículos que cumplen, ordenados
     * @throws ConcesionariaException si no tiene permiso
     */
    public List<Vehiculo> listar(Sesion sesion, FiltroVehiculos filtro, OrdenVehiculos orden)
            throws ConcesionariaException {
        sesion.exigir(Permiso.VER_STOCK);
        LocalDate hoy = Reloj.hoy();
        List<Vehiculo> filtrados = vehiculos.listar().stream()
                .filter(filtro.comoPredicado(hoy))
                .toList();
        // El precio se calcula una vez por vehículo, no en cada comparación del orden.
        Map<Vehiculo, BigDecimal> precios = new HashMap<>();
        if (orden == OrdenVehiculos.PRECIO_ASC || orden == OrdenVehiculos.PRECIO_DESC) {
            filtrados.forEach(v -> precios.put(v, v.calcularPrecioFinal(hoy)));
        }
        return filtrados.stream().sorted(orden.comparador(precios::get)).toList();
    }

    /**
     * Búsqueda libre en código, patente, marca y modelo, sin distinguir mayúsculas ni acentos
     * (CU-05). Cada palabra tiene que aparecer en alguno de esos campos: "peugeot 208" encuentra
     * el Peugeot 208. Incluye vendidos y bajas.
     *
     * @param sesion la sesión
     * @param texto  el texto
     * @return los vehículos que coinciden, en orden natural
     * @throws ConcesionariaException si no tiene permiso
     */
    public List<Vehiculo> buscarTexto(Sesion sesion, String texto) throws ConcesionariaException {
        sesion.exigir(Permiso.VER_STOCK);
        String codigo = normalizarCodigo(texto);
        List<String> palabras = Arrays.asList(Textos.clave(texto).split(" "));
        return vehiculos.listar().stream()
                .filter(v -> codigo.equals(v.getCodigo()) || palabras.stream().allMatch(p -> {
                    String patente = v.getPatente() == null ? "" : v.getPatente().toLowerCase();
                    String todo = Textos.clave(v.getCodigo() + " " + patente + " "
                            + v.getMarca().getNombre() + " " + v.getModelo());
                    return todo.contains(p) || patente.contains(p.replace("-", ""));
                }))
                .sorted()
                .toList();
    }

    /**
     * Modifica un vehículo (CU-07). La consola trabaja sobre {@code Vehiculo.copia()} y recién
     * acá se reemplaza el original, si sigue disponible y la patente no choca con otro.
     *
     * @param sesion     la sesión
     * @param modificado la copia con los cambios
     * @throws ConcesionariaException si no tiene permiso, no está disponible, cambió el tipo o
     *                                el estado, la patente ya existe o no se pudo guardar
     */
    public void modificar(Sesion sesion, Vehiculo modificado) throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_STOCK);
        Vehiculo original = buscar(sesion, modificado.getCodigo());
        original.exigirDisponible("modificar");
        if (original.getTipo() != modificado.getTipo()
                || original.getEstado() != modificado.getEstado()) {
            throw new ReglaNegocioException("No se puede cambiar el tipo ni el estado de un"
                    + " vehículo. Dalo de baja y registralo de nuevo.");
        }
        modificado.validarCompleto();
        exigirPatenteLibre(modificado.getPatente(), modificado.getCodigo());
        catalogo.exigirModeloDelCatalogo(modificado.getMarca(), modificado.getTipo(),
                modificado.getModelo());
        vehiculos.guardar(modificado);
        guardarCambios();
    }

    /**
     * Baja lógica (CU-08).
     *
     * @param sesion la sesión
     * @param codigo el código
     * @param motivo el motivo
     * @throws ConcesionariaException si no tiene permiso, no existe, no está disponible, el
     *                                motivo no es válido o no se pudo guardar
     */
    public void darDeBaja(Sesion sesion, String codigo, String motivo)
            throws ConcesionariaException {
        sesion.exigir(Permiso.GESTIONAR_STOCK);
        Vehiculo v = buscar(sesion, codigo);
        v.darDeBaja(motivo, Reloj.hoy());
        guardarCambios();
    }

    /**
     * Cotizar (CU-09): el desglose de un vehículo disponible, sin venderlo.
     *
     * @param sesion la sesión
     * @param codigo el código
     * @return el desglose a hoy
     * @throws ConcesionariaException si no tiene permiso, no existe o no está disponible
     */
    public DesglosePrecio cotizar(Sesion sesion, String codigo) throws ConcesionariaException {
        sesion.exigir(Permiso.VENDER);
        Vehiculo v = buscar(sesion, codigo);
        v.exigirDisponible("cotizar");
        return v.desglosarPrecio(Reloj.hoy());
    }

    /**
     * Verifica un campo suelto contra la regla de unicidad, para el alta campo por campo.
     *
     * @param patente la patente ya normalizada por el setter
     * @throws DatoInvalidoException si está repetida (como dato inválido, para repreguntar)
     */
    public void validarPatenteParaAlta(String patente) throws DatoInvalidoException {
        try {
            exigirPatenteLibre(patente, null);
        } catch (PatenteDuplicadaException e) {
            throw new DatoInvalidoException("Patente", patente, e.getMessage());
        }
    }

    /**
     * @return cantidad total de vehículos (todas las situaciones)
     */
    public int contar() {
        return vehiculos.contar();
    }
}
