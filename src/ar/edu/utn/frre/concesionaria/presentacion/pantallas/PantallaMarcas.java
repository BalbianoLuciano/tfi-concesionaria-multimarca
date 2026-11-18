package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Tabla;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ABM de marcas (CU-12).
 */
final class PantallaMarcas extends Pantalla {

    PantallaMarcas(Entorno entorno, Sesion sesion) {
        super(entorno, sesion);
    }

    void mostrar() {
        List<Opcion> opciones = new ArrayList<>();
        agregar(opciones, "Listar marcas", true, this::listar);
        agregar(opciones, "Nueva marca", true, () -> crear(null, null));
        agregar(opciones, "Modificar país o tipos", true, this::modificar);
        agregar(opciones, "Activar o desactivar", true, this::cambiarActiva);
        agregar(opciones, "Borrar (solo sin vehículos)", true, this::eliminar);
        menu("Marcas", opciones);
    }

    private void listar() throws ConcesionariaException {
        Tabla tabla = new Tabla().numero("N.º").texto("Marca").texto("País").texto("Fabrica")
                .numero("Vehículos").fija("Estado");
        for (Marca m : e.marcas.listar(sesion)) {
            tabla.fila(String.valueOf(m.getId()), m.getNombre(), m.getPaisOrigen(),
                    m.getTiposComoTexto(), String.valueOf(e.marcas.cantidadVehiculos(m)),
                    m.estaActiva() ? "Activa" : estilo.tenue("Desactivada"));
        }
        t.lineas(tabla.renglones(t.ancho(), estilo));
    }

    /**
     * Alta de marca. La usa también el registro de vehículos cuando la marca no existe.
     *
     * @param nombreSugerido nombre ya escrito, o {@code null} para pedirlo
     * @param tiposMinimos   tipos que tiene que fabricar sí o sí, o {@code null}
     * @return la marca creada
     */
    Marca crear(String nombreSugerido, Set<TipoVehiculo> tiposMinimos)
            throws ConcesionariaException, OperacionCancelada {
        String nombre = nombreSugerido;
        if (nombre == null) {
            nombre = lector.leer("Nombre", n -> {
                Marca prueba = new Marca();
                prueba.setNombre(n);
                if (e.marcas.buscarPorNombre(prueba.getNombre()).isPresent()) {
                    throw new DatoInvalidoException("Nombre", n, "La marca "
                            + prueba.getNombre() + " ya existe.");
                }
                return prueba.getNombre();
            });
        }
        List<Marca> parecidas = e.marcas.parecidas(nombre);
        if (!parecidas.isEmpty()) {
            t.aviso("Ya hay marcas parecidas: " + parecidas.stream().map(Marca::getNombre)
                    .collect(Collectors.joining(", ")) + ".");
            if (!lector.confirmar("¿Crear \"" + Textos.nombrePropio(nombre) + "\" igual?")) {
                throw new OperacionCancelada("No se creó la marca.");
            }
        }
        String pais = lector.leer("País de origen", p -> p);
        Set<TipoVehiculo> tipos = leerTipos(tiposMinimos, null);
        Marca nueva = e.marcas.crear(sesion, nombre, pais, tipos);
        t.exito("Se creó la marca " + nueva.getNombre() + " (n.º " + nueva.getId() + ").");
        return nueva;
    }

    /**
     * Tipos que fabrica, como números separados por coma ("1,3").
     */
    private Set<TipoVehiculo> leerTipos(Set<TipoVehiculo> minimos, Set<TipoVehiculo> actuales)
            throws OperacionCancelada {
        TipoVehiculo[] todos = TipoVehiculo.values();
        StringBuilder lista = new StringBuilder(" ");
        for (int i = 0; i < todos.length; i++) {
            lista.append(String.format(" %d %s  ", i + 1, todos[i].getEtiqueta()));
        }
        t.linea(lista.toString());
        String actualTexto = actuales == null ? null : actuales.stream()
                .map(tv -> String.valueOf(tv.ordinal() + 1)).collect(Collectors.joining(","));
        Set<TipoVehiculo> elegidos = lector.leer("Tipos que fabrica (por ejemplo 1,2)", actualTexto,
                texto -> {
                    Set<TipoVehiculo> tipos = EnumSet.noneOf(TipoVehiculo.class);
                    for (String parte : texto.split("[,\\s]+")) {
                        int n;
                        try {
                            n = Integer.parseInt(parte);
                        } catch (NumberFormatException ex) {
                            n = 0;
                        }
                        if (n < 1 || n > todos.length) {
                            throw new DatoInvalidoException("Tipos", texto, "Escribí números del 1 al "
                                    + todos.length + " separados por coma, por ejemplo 1,3.");
                        }
                        tipos.add(todos[n - 1]);
                    }
                    if (minimos != null && !tipos.containsAll(minimos)) {
                        throw new DatoInvalidoException("Tipos", texto, "Tiene que incluir "
                                + minimos.iterator().next().getEtiqueta() + ".");
                    }
                    return tipos;
                });
        return elegidos == null ? actuales : elegidos;
    }

    private Marca elegir() throws ConcesionariaException, OperacionCancelada {
        listar();
        t.blanco();
        List<Marca> marcas = e.marcas.listar(sesion);
        return lector.leer("Marca (número o nombre)", texto -> marcas.stream()
                .filter(m -> String.valueOf(m.getId()).equals(texto)
                        || m.getClave().equals(Textos.clave(texto)))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("Marca", texto,
                        "No hay una marca \"" + texto + "\". Escribí el número de la lista.")));
    }

    private void modificar() throws ConcesionariaException, OperacionCancelada {
        Marca m = elegir();
        t.info("Enter deja el valor actual.");
        String pais = lector.leer("País de origen", m.getPaisOrigen(), p -> p);
        Set<TipoVehiculo> tipos = leerTipos(null, m.getTiposQueFabrica());
        e.marcas.modificar(sesion, m.getId(), pais == null ? m.getPaisOrigen() : pais, tipos);
        t.exito("Se guardaron los cambios de " + m.getNombre() + ".");
    }

    private void cambiarActiva() throws ConcesionariaException, OperacionCancelada {
        Marca m = elegir();
        boolean activar = !m.estaActiva();
        if (lector.confirmar((activar ? "¿Activar " : "¿Desactivar ") + m.getNombre() + "?")) {
            e.marcas.cambiarActiva(sesion, m.getId(), activar);
            t.exito(m.getNombre() + (activar ? " está activa." : " quedó desactivada: ya no se"
                    + " ofrece en las altas."));
        }
    }

    private void eliminar() throws ConcesionariaException, OperacionCancelada {
        Marca m = elegir();
        if (lector.confirmar("¿Borrar " + m.getNombre() + "?")) {
            e.marcas.eliminar(sesion, m.getId());
            t.exito("Se borró " + m.getNombre() + ".");
        }
    }
}
