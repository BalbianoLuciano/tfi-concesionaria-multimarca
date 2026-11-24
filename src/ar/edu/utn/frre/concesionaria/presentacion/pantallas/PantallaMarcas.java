package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Textos;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.modelo.Marca;
import ar.edu.utn.frre.concesionaria.modelo.Modelo;
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
        agregar(opciones, "Listar marcas", "país, tipos y vehículos de cada una", true,
                this::listar);
        agregar(opciones, "Modelos", "catálogo de modelos de cada marca", false, this::modelos);
        agregar(opciones, "Nueva marca", "avisa si hay una parecida", true, () -> crear(null, null));
        agregar(opciones, "Modificar país o tipos", "el nombre no cambia", true, this::modificar);
        agregar(opciones, "Activar o desactivar", "una desactivada no se ofrece en las altas", true,
                this::cambiarActiva);
        agregar(opciones, "Borrar", "solo si no tiene vehículos", true, this::eliminar);
        menu("Marcas", () -> {
            List<Marca> marcas = e.marcas.listar(sesion);
            long activas = marcas.stream().filter(Marca::estaActiva).count();
            return marcas.size() + " marcas · activas: " + activas;
        }, opciones);
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
     * Catálogo de modelos de una marca (CU-12): listado por tipo, alta y baja.
     */
    private void modelos() throws ConcesionariaException, OperacionCancelada {
        List<Marca> marcas = e.marcas.listar(sesion);
        if (marcas.isEmpty()) {
            t.info("Todavía no hay marcas. Creá una con Nueva marca.");
            lector.pausa();
            return;
        }
        Marca marca = lector.elegir("Marca", marcas, Marca::getNombre);
        while (true) {
            cabecera("Marcas › Modelos", marca.getNombre() + " · " + marca.getModelos().size()
                    + " modelos");
            Tabla tabla = new Tabla().texto("Modelo").numero("Vehículos");
            for (TipoVehiculo tipo : TipoVehiculo.values()) {
                List<Modelo> delTipo = marca.modelosDe(tipo);
                if (delTipo.isEmpty()) {
                    continue;
                }
                tabla.grupo(tipo.getPlural().substring(0, 1).toUpperCase()
                        + tipo.getPlural().substring(1) + " · " + delTipo.size());
                for (Modelo m : delTipo) {
                    tabla.fila(m.getNombre(),
                            String.valueOf(e.marcas.cantidadVehiculos(marca, m)));
                }
            }
            if (tabla.estaVacia()) {
                t.linea(" " + marca.getNombre() + " todavía no tiene modelos en el catálogo.");
            } else {
                t.lineas(tabla.renglones(t.ancho(), estilo));
            }
            t.pie("A agregar modelo · Q quitar modelo · 0 Volver a Marcas");
            String opcion = lector.opcion(List.of("a", "q", "0"));
            if (opcion.equals("0")) {
                return;
            }
            ejecutar(opcion.equals("a") ? () -> agregarModelo(marca) : () -> quitarModelo(marca));
            lector.pausa();
        }
    }

    private void agregarModelo(Marca marca) throws ConcesionariaException, OperacionCancelada {
        lector.operacion("el alta del modelo");
        List<TipoVehiculo> tipos = new ArrayList<>(marca.getTiposQueFabrica());
        TipoVehiculo tipo = tipos.size() == 1 ? tipos.get(0)
                : lector.elegir("Tipo", tipos, TipoVehiculo::getEtiqueta);
        String nombre = lector.leer("Nombre del modelo", Modelo::normalizar);
        List<Modelo> parecidos = e.marcas.modelosParecidos(marca, null, nombre).stream()
                .filter(m -> !m.getClave().equals(Textos.compacta(nombre))).toList();
        if (!parecidos.isEmpty()) {
            t.aviso("Ya hay modelos parecidos: " + parecidos.stream().map(Modelo::getNombre)
                    .collect(Collectors.joining(", ")) + ".");
            if (!lector.confirmar("¿Agregar \"" + nombre + "\" igual?")) {
                throw new OperacionCancelada("No se agregó el modelo.");
            }
        }
        Modelo nuevo = e.marcas.agregarModelo(sesion, marca, nombre, tipo);
        t.exito("Se agregó " + nuevo + " al catálogo de " + marca.getNombre() + ".");
    }

    private void quitarModelo(Marca marca) throws ConcesionariaException, OperacionCancelada {
        List<Modelo> todos = marca.getModelos();
        if (todos.isEmpty()) {
            t.info("No hay modelos para quitar.");
            return;
        }
        Modelo modelo = lector.elegir("Modelo a quitar", todos, Modelo::toString);
        if (lector.confirmar("¿Quitar " + modelo.getNombre() + " del catálogo de "
                + marca.getNombre() + "?")) {
            e.marcas.quitarModelo(sesion, marca, modelo);
            t.exito("Se quitó " + modelo.getNombre() + ".");
        }
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
