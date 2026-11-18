package ar.edu.utn.frre.concesionaria.datos;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Anota en {@code error.log} las excepciones no previstas, con su <em>stack trace</em>, para
 * que el usuario nunca lo vea en pantalla y el programa siga (ARQUITECTURA §3). La consola lo
 * recibe como un {@link Consumer}, así no depende de esta capa.
 */
public class RegistroErrores implements Consumer<Throwable> {

    private final Path archivo;

    /**
     * @param carpeta la carpeta de datos
     */
    public RegistroErrores(Path carpeta) {
        this.archivo = carpeta.resolve("error.log");
    }

    @Override
    public void accept(Throwable error) {
        StringWriter texto = new StringWriter();
        texto.append("=== ").append(LocalDateTime.now().toString()).append('\n');
        error.printStackTrace(new PrintWriter(texto));
        try {
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, texto.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Si ni siquiera se puede escribir el log, no hay nada más que hacer.
        }
    }

    /** @return la ruta del log, para mostrarla */
    public Path getArchivo() {
        return archivo;
    }
}
