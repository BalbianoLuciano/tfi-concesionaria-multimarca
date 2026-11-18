package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.ResultadoImportacion;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Exportación e importación de archivos de texto (CU-14 y CU-15). Los archivos se escriben en
 * la carpeta de datos; el negocio solo maneja nombres, nunca rutas ni {@code java.io}.
 */
public interface ArchivosExportacion {

    /**
     * @param vehiculos los vehículos
     * @return la ruta del archivo escrito, para informarla
     * @throws PersistenciaException si no se pudo escribir
     */
    String exportarVehiculos(List<Vehiculo> vehiculos) throws PersistenciaException;

    /**
     * @param marcas las marcas
     * @return la ruta del archivo escrito
     * @throws PersistenciaException si no se pudo escribir
     */
    String exportarMarcas(List<Marca> marcas) throws PersistenciaException;

    /**
     * @param ventas las ventas
     * @return la ruta del archivo escrito
     * @throws PersistenciaException si no se pudo escribir
     */
    String exportarVentas(List<Venta> ventas) throws PersistenciaException;

    /**
     * Escribe un texto ya armado (un reporte) en un {@code .txt}.
     *
     * @param nombreBase nombre sin extensión ("reporte-stock-por-marca")
     * @param contenido  el texto, sin códigos de color
     * @return la ruta del archivo escrito
     * @throws PersistenciaException si no se pudo escribir
     */
    String exportarTexto(String nombreBase, String contenido) throws PersistenciaException;

    /**
     * Lee un CSV de vehículos y valida cada fila con las mismas reglas que el alta. No guarda
     * nada: devuelve los vehículos leídos y los errores, y el negocio decide.
     *
     * @param nombreArchivo nombre del archivo dentro de la carpeta de datos, o ruta absoluta
     * @param marcas        cómo resolver el nombre de una marca del catálogo
     * @return vehículos válidos y errores encontrados
     * @throws PersistenciaException si no se pudo leer el archivo
     */
    ResultadoImportacion importarVehiculos(String nombreArchivo,
            Function<String, Optional<Marca>> marcas) throws PersistenciaException;

    /**
     * @return la carpeta donde se escriben los archivos, para mostrarla
     */
    String carpeta();
}
