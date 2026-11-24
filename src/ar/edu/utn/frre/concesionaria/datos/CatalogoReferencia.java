package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Modelo;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Catálogo de referencia de marcas y modelos del mercado argentino (CU-12), que viaja dentro
 * del {@code .jar} como {@code catalogo-ar-2026.csv}. Se incorpora en el primer arranque y al
 * migrar datos de una versión anterior, para que el alta ofrezca desde el principio las marcas
 * y los modelos reales en lugar de pedir que se escriban.
 *
 * <p>Los datos están en un archivo y no en el código para poder actualizarlos sin tocar Java:
 * una línea por marca y tipo, con los modelos separados por coma.</p>
 *
 * <p>Contenido extra (SPEC §7): {@code Class.getResourceAsStream} para leer un archivo
 * empaquetado en el {@code .jar}.</p>
 */
public final class CatalogoReferencia {

    /** Nombre del recurso, junto a esta clase dentro del {@code .jar}. */
    public static final String RECURSO = "catalogo-ar-2026.csv";

    private CatalogoReferencia() {
    }

    /** Una línea del catálogo: una marca y los modelos de un tipo. */
    public static final class Linea {
        private final String marca;
        private final String pais;
        private final TipoVehiculo tipo;
        private final List<String> modelos;

        Linea(String marca, String pais, TipoVehiculo tipo, List<String> modelos) {
            this.marca = marca;
            this.pais = pais;
            this.tipo = tipo;
            this.modelos = modelos;
        }

        /** @return el nombre de la marca */
        public String getMarca() {
            return marca;
        }

        /** @return el país de origen */
        public String getPais() {
            return pais;
        }

        /** @return el tipo de vehículo */
        public TipoVehiculo getTipo() {
            return tipo;
        }

        /** @return los nombres de los modelos */
        public List<String> getModelos() {
            return modelos;
        }
    }

    /**
     * Lee el catálogo empaquetado.
     *
     * @return las líneas, en el orden del archivo
     * @throws PersistenciaException si falta el archivo o una línea está mal formada
     */
    public static List<Linea> leer() throws PersistenciaException {
        InputStream entrada = CatalogoReferencia.class.getResourceAsStream(RECURSO);
        if (entrada == null) {
            throw new PersistenciaException("No se encontró el catálogo de referencia " + RECURSO
                    + " dentro del programa. Volvé a descargar el .jar.");
        }
        List<Linea> lineas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String texto;
            int numero = 0;
            while ((texto = lector.readLine()) != null) {
                numero++;
                texto = texto.strip();
                if (texto.isEmpty() || texto.startsWith("#")) {
                    continue;
                }
                lineas.add(convertir(texto, numero));
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el catálogo de referencia.", e);
        }
        return lineas;
    }

    private static Linea convertir(String texto, int numero) throws PersistenciaException {
        String[] campos = texto.split(";");
        if (campos.length != 4) {
            throw new PersistenciaException("Catálogo de referencia, línea " + numero
                    + ": se esperaban 4 campos (marca;país;tipo;modelos).");
        }
        TipoVehiculo tipo = Arrays.stream(TipoVehiculo.values())
                .filter(t -> t.getEtiqueta().equals(campos[2].strip()))
                .findFirst().orElse(null);
        if (tipo == null) {
            throw new PersistenciaException("Catálogo de referencia, línea " + numero
                    + ": el tipo «" + campos[2] + "» no existe.");
        }
        List<String> modelos = Arrays.stream(campos[3].split(",")).map(String::strip)
                .filter(m -> !m.isEmpty()).toList();
        return new Linea(campos[0].strip(), campos[1].strip(), tipo, modelos);
    }

    /**
     * Incorpora el catálogo a las marcas existentes: crea las marcas que faltan, suma a cada una
     * los tipos y los modelos que no tiene, y no toca nada de lo que ya estaba (ni borra, ni
     * renombra, ni reactiva). Se puede llamar más de una vez: la segunda no agrega nada.
     *
     * @param marcas el repositorio de marcas
     * @return cuántas marcas y cuántos modelos se agregaron: {@code {marcas, modelos}}
     * @throws PersistenciaException si el catálogo no se pudo leer o tiene un dato inválido
     */
    public static int[] incorporar(RepositorioMarcas marcas) throws PersistenciaException {
        int marcasNuevas = 0;
        int modelosNuevos = 0;
        for (Linea linea : leer()) {
            try {
                Marca marca = marcas.buscarPorNombre(linea.getMarca()).orElse(null);
                if (marca == null) {
                    marca = new Marca(linea.getMarca(), linea.getPais(),
                            EnumSet.of(linea.getTipo()));
                    marca.setId(marcas.siguienteId());
                    marcasNuevas++;
                } else if (!marca.fabrica(linea.getTipo())) {
                    Set<TipoVehiculo> tipos = EnumSet.copyOf(marca.getTiposQueFabrica());
                    tipos.add(linea.getTipo());
                    marca.setTiposQueFabrica(tipos);
                }
                for (String nombre : linea.getModelos()) {
                    if (marca.buscarModelo(nombre).isEmpty()) {
                        marca.agregarModelo(new Modelo(nombre, linea.getTipo()));
                        modelosNuevos++;
                    }
                }
                marcas.guardar(marca);
            } catch (DatoInvalidoException e) {
                throw new PersistenciaException("Catálogo de referencia, marca "
                        + linea.getMarca() + ": " + e.getMessage(), e);
            }
        }
        return new int[] {marcasNuevas, modelosNuevos};
    }
}
