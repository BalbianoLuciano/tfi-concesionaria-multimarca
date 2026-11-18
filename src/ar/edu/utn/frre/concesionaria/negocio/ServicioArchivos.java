package ar.edu.utn.frre.concesionaria.negocio;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.datos.ArchivosExportacion;
import ar.edu.utn.frre.concesionaria.datos.Persistencia;
import ar.edu.utn.frre.concesionaria.datos.RepositorioMarcas;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVehiculos;
import ar.edu.utn.frre.concesionaria.datos.RepositorioVentas;
import ar.edu.utn.frre.concesionaria.datos.ResultadoImportacion;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exportar e importar (CU-15) y exportar reportes a texto (CU-14).
 */
public class ServicioArchivos extends ServicioBase {

    private final ArchivosExportacion archivos;
    private final RepositorioVehiculos vehiculos;
    private final RepositorioMarcas marcas;
    private final RepositorioVentas ventas;
    private final ServicioVehiculos servicioVehiculos;

    /**
     * @param archivos          acceso a los archivos
     * @param vehiculos         repositorio de vehículos
     * @param marcas            repositorio de marcas
     * @param ventas            repositorio de ventas
     * @param servicioVehiculos para validar cada vehículo importado como un alta
     * @param persistencia      guardado
     */
    public ServicioArchivos(ArchivosExportacion archivos, RepositorioVehiculos vehiculos,
            RepositorioMarcas marcas, RepositorioVentas ventas,
            ServicioVehiculos servicioVehiculos, Persistencia persistencia) {
        super(persistencia);
        this.archivos = archivos;
        this.vehiculos = vehiculos;
        this.marcas = marcas;
        this.ventas = ventas;
        this.servicioVehiculos = servicioVehiculos;
    }

    /** @return la carpeta donde se leen y escriben los archivos */
    public String carpeta() {
        return archivos.carpeta();
    }

    /**
     * @param sesion la sesión
     * @return la ruta del CSV con todos los vehículos
     * @throws ConcesionariaException si no tiene permiso o no se pudo escribir
     */
    public String exportarVehiculos(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.EXPORTAR);
        return archivos.exportarVehiculos(vehiculos.listar().stream().sorted().toList());
    }

    /**
     * @param sesion la sesión
     * @return la ruta del CSV con las marcas
     * @throws ConcesionariaException si no tiene permiso o no se pudo escribir
     */
    public String exportarMarcas(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.EXPORTAR);
        return archivos.exportarMarcas(marcas.listar());
    }

    /**
     * @param sesion la sesión
     * @return la ruta del CSV con las ventas
     * @throws ConcesionariaException si no tiene permiso o no se pudo escribir
     */
    public String exportarVentas(Sesion sesion) throws ConcesionariaException {
        sesion.exigir(Permiso.EXPORTAR);
        return archivos.exportarVentas(ventas.listar().stream().sorted().toList());
    }

    /**
     * Exporta un reporte ya armado a {@code .txt}.
     *
     * @param sesion     la sesión
     * @param nombreBase nombre del archivo sin extensión
     * @param texto      el reporte, sin color
     * @return la ruta del archivo
     * @throws ConcesionariaException si no tiene permiso o no se pudo escribir
     */
    public String exportarReporte(Sesion sesion, String nombreBase, String texto)
            throws ConcesionariaException {
        sesion.exigir(Permiso.VER_REPORTES);
        return archivos.exportarTexto(nombreBase, texto);
    }

    /**
     * Importa vehículos de un CSV (CU-15): todo o nada. Cada fila se valida como un alta, y
     * además se controla que las patentes no choquen con el stock ni entre sí. Si hay un solo
     * error, no se importa nada y se devuelve el informe.
     *
     * @param sesion        la sesión
     * @param nombreArchivo nombre del archivo en la carpeta de datos
     * @return el resultado: si no tiene errores, sus vehículos ya están registrados con código
     * @throws ConcesionariaException si no tiene permiso, no se pudo leer o no se pudo guardar
     */
    public ResultadoImportacion importarVehiculos(Sesion sesion, String nombreArchivo)
            throws ConcesionariaException {
        sesion.exigir(Permiso.IMPORTAR);
        ResultadoImportacion leido = archivos.importarVehiculos(nombreArchivo,
                marcas::buscarPorNombre);

        ResultadoImportacion resultado = new ResultadoImportacion();
        leido.getErrores().forEach(e -> resultado.agregarError(e.getLinea(), e.getCampo(),
                e.getMotivo()));
        Map<String, Integer> patentesDelArchivo = new HashMap<>();
        List<Vehiculo> leidos = leido.getVehiculos();
        for (int i = 0; i < leidos.size(); i++) {
            Vehiculo v = leidos.get(i);
            int linea = leido.getLinea(i);
            try {
                servicioVehiculos.validarParaAlta(v);
                if (v.getPatente() != null) {
                    Integer anterior = patentesDelArchivo.putIfAbsent(v.getPatente(), linea);
                    if (anterior != null) {
                        resultado.agregarError(linea, "Patente", "La patente " + v.getPatente()
                                + " ya aparece en la línea " + anterior + " del archivo.");
                    }
                }
                resultado.agregar(v, linea);
            } catch (DatoInvalidoException e) {
                resultado.agregarError(linea, e.getCampo(), e.getMessage());
            } catch (ConcesionariaException e) {
                resultado.agregarError(linea, "Patente", e.getMessage());
            }
        }
        if (resultado.tieneErrores()) {
            return resultado;
        }

        for (Vehiculo v : resultado.getVehiculos()) {
            v.setCodigo(vehiculos.siguienteCodigo(v.getTipo()));
            v.setFechaIngreso(Reloj.hoy());
            vehiculos.guardar(v);
        }
        guardarCambios();
        return resultado;
    }
}
