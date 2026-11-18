package ar.edu.utn.frre.concesionaria.datos;

import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

/**
 * Guarda y carga el {@link Almacen} en {@code datos.ser} (ARQUITECTURA §4).
 *
 * <p><b>Guardado atómico:</b> se escribe en {@code datos.ser.tmp}; si quedó completo, el
 * archivo anterior pasa a {@code datos.ser.bak} y el temporal reemplaza al principal con un
 * movimiento atómico. En ningún momento el archivo principal está a medio escribir.</p>
 *
 * <p><b>Carga con recuperación:</b> si {@code datos.ser} no se puede leer, se usa el
 * {@code .bak} y el dañado se renombra a {@code .corrupto}; si tampoco hay copia, no se pisa
 * nada y se informa dónde están los datos.</p>
 *
 * <p>Contenido extra (SPEC §7): {@code Files.move(..., ATOMIC_MOVE)}.</p>
 */
public class AlmacenArchivo implements Persistencia {

    /** Cómo terminó la carga, para que la consola lo informe. */
    public enum EstadoCarga {
        /** No había datos: primer arranque. */
        NUEVO,
        /** Se leyó el archivo principal. */
        NORMAL,
        /** El principal estaba dañado y se recuperó la copia anterior. */
        RECUPERADO,
        /** El archivo era de una versión anterior y se migró. */
        MIGRADO
    }

    private static final String NOMBRE = "datos.ser";

    private final Path carpeta;
    private final Path principal;
    private final Path respaldo;
    private final Path temporal;
    private Almacen almacen;
    private EstadoCarga estadoCarga;
    private boolean cambiosPendientes;

    /**
     * @param carpeta la carpeta de datos ({@code ~/.concesionaria} por defecto)
     */
    public AlmacenArchivo(Path carpeta) {
        this.carpeta = carpeta;
        this.principal = carpeta.resolve(NOMBRE);
        this.respaldo = carpeta.resolve(NOMBRE + ".bak");
        this.temporal = carpeta.resolve(NOMBRE + ".tmp");
    }

    /**
     * Carga los datos, o crea un almacén vacío si es el primer arranque.
     *
     * @return el almacén cargado
     * @throws PersistenciaException si no se pudieron leer los datos ni la copia, o si son de
     *                               una versión más nueva del programa
     */
    public Almacen cargar() throws PersistenciaException {
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo crear la carpeta de datos " + carpeta
                    + ". Revisá los permisos o usá -Dconcesionaria.dir=<otra carpeta>.", e);
        }

        if (!Files.exists(principal) && !Files.exists(respaldo)) {
            almacen = new Almacen();
            estadoCarga = EstadoCarga.NUEVO;
            return almacen;
        }

        Almacen leido;
        try {
            leido = leer(principal);
            estadoCarga = EstadoCarga.NORMAL;
        } catch (IOException | ClassNotFoundException | ClassCastException errorPrincipal) {
            try {
                leido = leer(respaldo);
            } catch (IOException | ClassNotFoundException | ClassCastException errorRespaldo) {
                throw new PersistenciaException("No se pueden leer los datos de " + principal
                        + " ni su copia. No se tocó nada: revisá esos archivos antes de volver"
                        + " a abrir el programa.", errorPrincipal);
            }
            apartarDaniado();
            estadoCarga = EstadoCarga.RECUPERADO;
            cambiosPendientes = true;
        }

        if (MigradorAlmacen.migrar(leido) && estadoCarga == EstadoCarga.NORMAL) {
            estadoCarga = EstadoCarga.MIGRADO;
            cambiosPendientes = true;
        }
        almacen = leido;
        return almacen;
    }

    private Almacen leer(Path archivo) throws IOException, ClassNotFoundException {
        if (!Files.exists(archivo)) {
            throw new IOException("No existe " + archivo);
        }
        try (InputStream entrada = new BufferedInputStream(Files.newInputStream(archivo));
             ObjectInputStream objetos = new ObjectInputStream(entrada)) {
            return (Almacen) objetos.readObject();
        }
    }

    private void apartarDaniado() {
        if (Files.exists(principal)) {
            try {
                Files.move(principal, carpeta.resolve(NOMBRE + ".corrupto"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Si no se puede renombrar, el próximo guardado lo reemplaza igual.
            }
        }
    }

    @Override
    public void guardar() throws PersistenciaException {
        if (almacen == null) {
            throw new PersistenciaException("No hay datos cargados para guardar.");
        }
        almacen.setGuardadoEn(LocalDateTime.now());
        try {
            escribirTemporal();
            if (Files.exists(principal)) {
                Files.copy(principal, respaldo, StandardCopyOption.REPLACE_EXISTING);
            }
            moverAtomico(temporal, principal);
            cambiosPendientes = false;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException ignorada) {
                // El .tmp se pisa en el próximo intento.
            }
            throw new PersistenciaException("No se pudieron guardar los datos en " + principal
                    + ". Lo que cargaste sigue en memoria: liberá espacio o revisá los permisos"
                    + " y volvé a intentar.", e);
        }
    }

    private void escribirTemporal() throws IOException {
        try (FileOutputStream archivo = new FileOutputStream(temporal.toFile());
             ObjectOutputStream objetos = new ObjectOutputStream(new BufferedOutputStream(archivo))) {
            objetos.writeObject(almacen);
            objetos.flush();
            // Fuerza la escritura al disco antes del movimiento: si se corta la luz, el .tmp
            // está completo o no se usa.
            archivo.getFD().sync();
        }
    }

    private static void moverAtomico(Path origen, Path destino) throws IOException {
        try {
            Files.move(origen, destino, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Algunos sistemas de archivos no lo soportan: se reemplaza igual, el .bak protege.
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public boolean hayCambiosPendientes() {
        return cambiosPendientes;
    }

    @Override
    public void marcarCambio() {
        cambiosPendientes = true;
    }

    /** @return cómo terminó la última carga */
    public EstadoCarga getEstadoCarga() {
        return estadoCarga;
    }

    /** @return la carpeta de datos */
    public Path getCarpeta() {
        return carpeta;
    }

    /** @return el archivo principal */
    public Path getArchivo() {
        return principal;
    }
}
