package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.modelo.ResultadoImportacion;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Tabla;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exportar e importar CSV (CU-15).
 */
final class PantallaDatos extends Pantalla {

    PantallaDatos(Entorno entorno, Sesion sesion) {
        super(entorno, sesion);
    }

    void mostrar() {
        List<Opcion> opciones = new ArrayList<>();
        if (sesion.puede(Permiso.EXPORTAR)) {
            seccion(opciones, "Exportar a CSV");
            agregar(opciones, "Exportar vehículos", "todo el stock, con su situación", true,
                    () -> t.exito("Se exportó a " + e.archivos.exportarVehiculos(sesion)));
            agregar(opciones, "Exportar marcas", "el catálogo completo", true,
                    () -> t.exito("Se exportó a " + e.archivos.exportarMarcas(sesion)));
            agregar(opciones, "Exportar ventas", "el historial con sus precios", true,
                    () -> t.exito("Se exportó a " + e.archivos.exportarVentas(sesion)));
        }
        if (sesion.puede(Permiso.IMPORTAR)) {
            seccion(opciones, "Importar");
            agregar(opciones, "Importar vehículos", "todo o nada, con errores por línea", true,
                    this::importar);
        }
        menu("Datos", () -> "Carpeta: " + e.archivos.carpeta(), opciones);
    }

    private void importar() throws ConcesionariaException, OperacionCancelada {
        t.info("El archivo tiene que estar en " + e.archivos.carpeta() + ", separado por ;, en");
        t.info("UTF-8 y con encabezado. El formato es el mismo que el de Exportar vehículos.");
        t.info("Si una sola fila tiene un error, no se importa nada.");
        t.blanco();
        String archivo = lector.leer("Nombre del archivo", s -> s);
        ResultadoImportacion resultado = e.archivos.importarVehiculos(sesion, archivo);
        t.blanco();
        if (resultado.tieneErrores()) {
            t.error("No se importó nada: hay " + resultado.getErrores().size()
                    + " error(es). Corregilos en el archivo y volvé a intentar.");
            t.blanco();
            Tabla tabla = new Tabla().numero("Línea").fija("Campo").texto("Motivo");
            resultado.getErrores().forEach(err -> tabla.fila(String.valueOf(err.getLinea()),
                    err.getCampo(), err.getMotivo()));
            t.lineas(tabla.renglones(t.ancho(), estilo));
            return;
        }
        List<Vehiculo> importados = resultado.getVehiculos();
        t.exito("Se importaron " + importados.size() + " vehículos: "
                + importados.stream().map(Vehiculo::getCodigo).collect(Collectors.joining(", "))
                + ".");
    }
}
