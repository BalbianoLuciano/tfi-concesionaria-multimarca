package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.modelo.Automovil;
import ar.edu.utn.frre.concesionaria.modelo.Camioneta;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaAutomovil;
import ar.edu.utn.frre.concesionaria.modelo.CarroceriaCamioneta;
import ar.edu.utn.frre.concesionaria.modelo.Color;
import ar.edu.utn.frre.concesionaria.modelo.ConEtiqueta;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Motocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoMotocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * CSV y TXT en la carpeta de datos (CU-14 y CU-15): separador {@code ;}, UTF-8 y encabezado.
 *
 * <p>Los números se escriben sin separador de miles ({@code 38000000}) para que otro programa
 * los lea sin ambigüedad. Los enums, con su etiqueta ("Doble cabina").</p>
 */
public class ArchivosCsv implements ArchivosExportacion {

    /** Separador de campos. */
    public static final char SEPARADOR = ';';

    /** Columnas del CSV de vehículos, en orden. */
    public static final String[] COLUMNAS_VEHICULOS = {"codigo", "tipo", "estado", "marca",
        "modelo", "anio", "color", "kilometraje", "patente", "precio_lista", "carroceria_o_tipo",
        "puertas", "traccion_4x4", "carga_kg", "cilindrada_cc", "situacion", "precio_final"};

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path carpeta;

    /**
     * @param carpeta la carpeta de datos
     */
    public ArchivosCsv(Path carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public String carpeta() {
        return carpeta.toString();
    }

    // ---------------------------------------------------------------- exportar

    @Override
    public String exportarVehiculos(List<Vehiculo> vehiculos) throws PersistenciaException {
        List<String> lineas = new ArrayList<>();
        lineas.add(String.join(String.valueOf(SEPARADOR), COLUMNAS_VEHICULOS));
        for (Vehiculo v : vehiculos) {
            String detalle1 = "";
            String puertas = "";
            String traccion = "";
            String carga = "";
            String cilindrada = "";
            if (v instanceof Automovil) {
                Automovil a = (Automovil) v;
                detalle1 = a.getCarroceria().getEtiqueta();
                puertas = String.valueOf(a.getPuertas());
            } else if (v instanceof Camioneta) {
                Camioneta c = (Camioneta) v;
                detalle1 = c.getCarroceria().getEtiqueta();
                traccion = Boolean.TRUE.equals(c.getTraccion4x4()) ? "si" : "no";
                carga = String.valueOf(c.getCargaUtilKg());
            } else if (v instanceof Motocicleta) {
                Motocicleta m = (Motocicleta) v;
                detalle1 = m.getTipoMoto().getEtiqueta();
                cilindrada = String.valueOf(m.getCilindradaCc());
            }
            lineas.add(linea(v.getCodigo(), v.getTipo().getEtiqueta(), v.getEstado().getEtiqueta(),
                    v.getMarca().getNombre(), v.getModelo(), String.valueOf(v.getAnio()),
                    v.getColor().getEtiqueta(), String.valueOf(v.getKilometraje()),
                    v.getPatente() == null ? "" : v.getPatente(),
                    v.getPrecioLista().toPlainString(), detalle1, puertas, traccion, carga,
                    cilindrada, v.getSituacion().getEtiqueta(),
                    v.calcularPrecioFinal(Reloj.hoy()).toPlainString()));
        }
        return escribir("vehiculos", "csv", lineas);
    }

    @Override
    public String exportarMarcas(List<Marca> marcas) throws PersistenciaException {
        List<String> lineas = new ArrayList<>();
        lineas.add(linea("id", "nombre", "pais_origen", "tipos", "activa"));
        for (Marca m : marcas) {
            lineas.add(linea(String.valueOf(m.getId()), m.getNombre(), m.getPaisOrigen(),
                    m.getTiposComoTexto(), m.estaActiva() ? "si" : "no"));
        }
        return escribir("marcas", "csv", lineas);
    }

    @Override
    public String exportarVentas(List<Venta> ventas) throws PersistenciaException {
        List<String> lineas = new ArrayList<>();
        lineas.add(linea("numero", "fecha", "codigo_vehiculo", "vehiculo", "comprador", "dni",
                "vendedor", "precio_lista", "precio_final"));
        for (Venta v : ventas) {
            lineas.add(linea(String.valueOf(v.getNumero()), v.getFecha().toString(),
                    v.getCodigoVehiculo(), v.getDescripcionVehiculo(),
                    v.getComprador().getNombreCompleto(), v.getComprador().getDni(),
                    v.getUsuarioVendedor(), v.getDesglose().getPrecioLista().toPlainString(),
                    v.getPrecioFinal().toPlainString()));
        }
        return escribir("ventas", "csv", lineas);
    }

    @Override
    public String exportarTexto(String nombreBase, String contenido) throws PersistenciaException {
        return escribir(nombreBase, "txt", List.of(contenido));
    }

    private String escribir(String base, String extension, List<String> lineas)
            throws PersistenciaException {
        Path archivo = carpeta.resolve(base + "-" + SELLO.format(java.time.LocalDateTime.now())
                + "." + extension);
        try {
            Files.createDirectories(carpeta);
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
            return archivo.toString();
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo escribir " + archivo
                    + ". Revisá los permisos de la carpeta.", e);
        }
    }

    /**
     * Arma una línea CSV. Un campo con separador, comillas o salto de línea va entre comillas,
     * con las comillas internas duplicadas.
     *
     * @param campos los campos
     * @return la línea
     */
    static String linea(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            String c = campos[i] == null ? "" : campos[i];
            if (c.indexOf(SEPARADOR) >= 0 || c.indexOf('"') >= 0 || c.indexOf('\n') >= 0) {
                sb.append('"').append(c.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Separa una línea CSV en campos, respetando comillas.
     *
     * @param linea la línea
     * @return los campos
     */
    static List<String> separar(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (entreComillas) {
                if (c == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else if (c == '"') {
                    entreComillas = false;
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == SEPARADOR) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    // ---------------------------------------------------------------- importar

    @Override
    public ResultadoImportacion importarVehiculos(String nombreArchivo,
            Function<String, Optional<Marca>> marcas) throws PersistenciaException {
        Path archivo = carpeta.resolve(Textos.limpiar(nombreArchivo));
        List<String> lineas;
        try {
            lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer " + archivo
                    + ". Copiá el archivo a " + carpeta + " y escribí solo su nombre.", e);
        }

        ResultadoImportacion resultado = new ResultadoImportacion();
        if (lineas.isEmpty()) {
            resultado.agregarError(1, "Encabezado", "El archivo está vacío.");
            return resultado;
        }

        Map<String, Integer> columnas = new HashMap<>();
        List<String> encabezado = separar(quitarBom(lineas.get(0)));
        for (int i = 0; i < encabezado.size(); i++) {
            columnas.put(Textos.clave(encabezado.get(i)), i);
        }
        for (String obligatoria : new String[]{"tipo", "estado", "marca", "modelo", "anio",
            "color", "precio_lista", "carroceria_o_tipo"}) {
            if (!columnas.containsKey(obligatoria)) {
                resultado.agregarError(1, obligatoria, "Falta la columna \"" + obligatoria
                        + "\" en el encabezado.");
            }
        }
        if (resultado.tieneErrores()) {
            return resultado;
        }

        for (int n = 1; n < lineas.size(); n++) {
            if (lineas.get(n).isBlank()) {
                continue;
            }
            Fila fila = new Fila(separar(lineas.get(n)), columnas);
            try {
                resultado.agregar(armarVehiculo(fila, marcas), n + 1);
            } catch (DatoInvalidoException e) {
                resultado.agregarError(n + 1, e.getCampo(), e.getMessage());
            }
        }
        return resultado;
    }

    private static String quitarBom(String texto) {
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }

    /** Acceso a los campos de una fila por nombre de columna. */
    private static final class Fila {
        private final List<String> campos;
        private final Map<String, Integer> columnas;

        Fila(List<String> campos, Map<String, Integer> columnas) {
            this.campos = campos;
            this.columnas = columnas;
        }

        String get(String columna) {
            Integer i = columnas.get(columna);
            return i == null || i >= campos.size() ? "" : Textos.limpiar(campos.get(i));
        }

        Integer entero(String columna, String campo) throws DatoInvalidoException {
            String texto = get(columna);
            if (texto.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(texto.replace(".", ""));
            } catch (NumberFormatException e) {
                throw new DatoInvalidoException(campo, texto,
                        "\"" + texto + "\" no es un número entero.", e);
            }
        }
    }

    private Vehiculo armarVehiculo(Fila f, Function<String, Optional<Marca>> marcas)
            throws DatoInvalidoException {
        TipoVehiculo tipo = ConEtiqueta.desdeTexto(TipoVehiculo.class, "Tipo", f.get("tipo"));
        Estado estado = ConEtiqueta.desdeTexto(Estado.class, "Estado", f.get("estado"));
        Vehiculo v;
        switch (tipo) {
            case AUTOMOVIL:
                v = new Automovil();
                break;
            case CAMIONETA:
                v = new Camioneta();
                break;
            default:
                v = new Motocicleta();
                break;
        }
        v.setEstado(estado);
        String nombreMarca = f.get("marca");
        Marca marca = marcas.apply(nombreMarca).orElseThrow(() -> new DatoInvalidoException(
                "Marca", nombreMarca, "La marca \"" + nombreMarca
                        + "\" no está en el catálogo. Dala de alta antes de importar."));
        v.setMarca(marca);
        v.setModelo(f.get("modelo"));
        v.setAnio(f.entero("anio", "Año"));
        v.setColor(ConEtiqueta.desdeTexto(Color.class, "Color", f.get("color")));
        if (estado == Estado.USADO) {
            v.setKilometraje(f.entero("kilometraje", "Kilometraje"));
        }
        v.setPatente(f.get("patente"));
        String precio = f.get("precio_lista");
        try {
            v.setPrecioLista(new BigDecimal(precio));
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("Precio de lista", precio,
                    "\"" + precio + "\" no es un precio. Escribilo sin puntos: 38000000.", e);
        }

        String detalle = f.get("carroceria_o_tipo");
        if (v instanceof Automovil) {
            Automovil a = (Automovil) v;
            a.setCarroceria(ConEtiqueta.desdeTexto(CarroceriaAutomovil.class, "Carrocería", detalle));
            a.setPuertas(f.entero("puertas", "Puertas"));
        } else if (v instanceof Camioneta) {
            Camioneta c = (Camioneta) v;
            c.setCarroceria(ConEtiqueta.desdeTexto(CarroceriaCamioneta.class, "Carrocería", detalle));
            c.setTraccion4x4(leerSiNo(f.get("traccion_4x4")));
            c.setCargaUtilKg(f.entero("carga_kg", "Carga útil"));
        } else {
            Motocicleta m = (Motocicleta) v;
            m.setTipoMoto(ConEtiqueta.desdeTexto(TipoMotocicleta.class, "Tipo de moto", detalle));
            m.setCilindradaCc(f.entero("cilindrada_cc", "Cilindrada"));
        }
        v.validarCompleto();
        return v;
    }

    private static Boolean leerSiNo(String texto) throws DatoInvalidoException {
        String clave = Textos.clave(texto);
        if (clave.equals("si") || clave.equals("s") || clave.equals("true")) {
            return Boolean.TRUE;
        }
        if (clave.equals("no") || clave.equals("n") || clave.equals("false")) {
            return Boolean.FALSE;
        }
        throw new DatoInvalidoException("Tracción 4x4", texto, "Escribí si o no.");
    }
}
