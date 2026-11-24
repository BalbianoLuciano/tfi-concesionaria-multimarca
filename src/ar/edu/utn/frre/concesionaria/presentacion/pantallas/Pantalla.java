package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.EntidadNoEncontradaException;
import ar.edu.utn.frre.concesionaria.excepciones.PersistenciaException;
import ar.edu.utn.frre.concesionaria.excepciones.ReglaNegocioException;
import ar.edu.utn.frre.concesionaria.excepciones.SeguridadException;
import ar.edu.utn.frre.concesionaria.negocio.Claves;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Estilo;
import ar.edu.utn.frre.concesionaria.presentacion.consola.FinDeEntrada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Lector;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Terminal;
import java.util.ArrayList;
import java.util.List;

/**
 * Base de las pantallas: encabezado, menús y el manejo de errores escalonado de
 * ARQUITECTURA §3, de lo específico a lo general.
 */
abstract class Pantalla {

    /** Una acción de menú. Puede lanzar las excepciones del negocio o cancelarse. */
    @FunctionalInterface
    interface Accion {
        /**
         * @throws ConcesionariaException un error del negocio
         * @throws OperacionCancelada     si el usuario canceló
         */
        void ejecutar() throws ConcesionariaException, OperacionCancelada;
    }

    /** Dato de contexto de un menú; puede fallar si el rol no tiene permiso. */
    @FunctionalInterface
    interface Contexto {
        /**
         * @return el texto de la línea de contexto
         * @throws ConcesionariaException si no se puede calcular
         */
        String texto() throws ConcesionariaException;
    }

    /** Una opción de menú, o el título de una sección si no tiene acción. */
    static final class Opcion {
        final String tecla;
        final String etiqueta;
        final String detalle;
        final Accion accion;
        final boolean pausa;

        Opcion(String tecla, String etiqueta, String detalle, boolean pausa, Accion accion) {
            this.tecla = tecla;
            this.etiqueta = etiqueta;
            this.detalle = detalle;
            this.accion = accion;
            this.pausa = pausa;
        }

        boolean esSeccion() {
            return accion == null;
        }
    }

    protected final Entorno e;
    protected final Terminal t;
    protected final Lector lector;
    protected final Estilo estilo;
    protected final Sesion sesion;

    /**
     * @param entorno terminal, lector y servicios
     * @param sesion  la sesión (o {@code null} antes del login)
     */
    protected Pantalla(Entorno entorno, Sesion sesion) {
        this.e = entorno;
        this.t = entorno.terminal;
        this.lector = entorno.lector;
        this.estilo = entorno.terminal.estilo();
        this.sesion = sesion;
    }

    /**
     * Limpia la pantalla y muestra el encabezado fijo con el título y la ruta (SPEC §5).
     *
     * @param ruta la ruta debajo de "Inicio" ("Vehículos › Registrar"), o vacía para el inicio
     */
    protected void cabecera(String ruta) {
        cabecera(ruta, null);
    }

    /**
     * Como {@link #cabecera(String)}, con una línea de contexto debajo del título.
     *
     * @param ruta     la ruta debajo de "Inicio"
     * @param contexto datos de la sección, o {@code null}
     */
    protected void cabecera(String ruta, String contexto) {
        t.limpiar();
        String fecha = Formatos.fechaEncabezado(Reloj.hoy());
        String derecha = sesion == null ? fecha
                : sesion.getNombreUsuario() + " · " + sesion.getRol().getEtiqueta() + " · " + fecha;
        String completa = ruta.isEmpty() ? "Inicio" : "Inicio › " + ruta;
        t.encabezado(derecha, ultimo(completa), ruta.isEmpty() ? "" : completa);
        if (contexto != null && !contexto.isEmpty()) {
            t.contexto(contexto);
        }
        t.blanco();
    }

    /** @return el último tramo de una ruta ("Inicio › Vehículos" → "Vehículos") */
    private static String ultimo(String ruta) {
        int i = ruta.lastIndexOf(" › ");
        return i < 0 ? ruta : ruta.substring(i + 3);
    }

    /** @return la pantalla anterior de una ruta ("Vehículos › Stock" → "Vehículos") */
    private static String anterior(String ruta) {
        String completa = "Inicio › " + ruta;
        return ultimo(completa.substring(0, completa.lastIndexOf(" › ")));
    }

    /**
     * Ejecuta una acción y traduce cada excepción a un mensaje (ARQUITECTURA §3). Una excepción
     * no prevista se anota en {@code error.log} y el programa sigue.
     *
     * @param accion la acción
     * @return {@code true} si terminó bien
     */
    protected boolean ejecutar(Accion accion) {
        lector.operacion("la operación");
        try {
            accion.ejecutar();
            return true;
        } catch (OperacionCancelada ex) {
            t.blanco();
            t.info(ex.getMessage());
        } catch (DatoInvalidoException ex) {
            mostrarError(ex.getMessage());
        } catch (EntidadNoEncontradaException | ReglaNegocioException ex) {
            mostrarError(ex.getMessage());
        } catch (SeguridadException ex) {
            mostrarError(ex.getMessage());
        } catch (PersistenciaException ex) {
            mostrarError(ex.getMessage());
        } catch (ConcesionariaException ex) {
            mostrarError(ex.getMessage());
        } catch (FinDeEntrada ex) {
            throw ex;
        } catch (RuntimeException ex) {
            e.registroErrores.accept(ex);
            mostrarError("Ocurrió un error inesperado. El detalle quedó en error.log de la carpeta"
                    + " de datos; el programa sigue funcionando.");
        }
        return false;
    }

    /** Error que termina una operación: separado por una línea en blanco (SPEC §5). */
    private void mostrarError(String mensaje) {
        t.blanco();
        t.error(mensaje);
    }

    /**
     * Muestra un menú hasta que el usuario elige 0. Cada opción abre su propia pantalla, con
     * su título y su ruta (SPEC §5).
     *
     * @param ruta     la ruta de navegación ("Vehículos")
     * @param opciones las opciones y secciones (las que no corresponden al rol no se agregan)
     */
    protected void menu(String ruta, List<Opcion> opciones) {
        menu(ruta, null, opciones);
    }

    /**
     * Como {@link #menu(String, List)}, con una línea de contexto que se recalcula cada vez
     * que se vuelve al menú.
     *
     * @param ruta     la ruta de navegación
     * @param contexto datos de la sección, o {@code null}
     * @param opciones las opciones y secciones
     */
    protected void menu(String ruta, Contexto contexto, List<Opcion> opciones) {
        String volver = "Volver a " + anterior(ruta);
        int ancho = Math.max(volver.length(),
                opciones.stream().mapToInt(o -> o.etiqueta.length()).max().orElse(0));
        while (true) {
            cabecera(ruta, textoDe(contexto));
            List<String> validas = new ArrayList<>();
            boolean primera = true;
            for (Opcion o : opciones) {
                if (o.esSeccion()) {
                    if (!primera) {
                        t.blanco();
                    }
                    t.seccion(o.etiqueta);
                } else {
                    t.opcion(o.tecla, o.etiqueta, o.detalle, ancho);
                    validas.add(o.tecla);
                }
                primera = false;
            }
            t.blanco();
            t.opcion("0", volver, "", ancho);
            validas.add("0");
            t.pie("");
            String elegida = lector.opcion(validas);
            if (elegida.equals("0")) {
                return;
            }
            for (Opcion o : opciones) {
                if (!o.esSeccion() && o.tecla.equals(elegida)) {
                    cabecera(ruta + " › " + o.etiqueta);
                    ejecutar(o.accion);
                    if (o.pausa) {
                        lector.pausa();
                    }
                }
            }
        }
    }

    /** @return el texto del contexto, o {@code null} si no hay o no se pudo calcular */
    private static String textoDe(Contexto contexto) {
        if (contexto == null) {
            return null;
        }
        try {
            return contexto.texto();
        } catch (ConcesionariaException ex) {
            return null;
        }
    }

    /**
     * Agrega una opción numerada.
     *
     * @param opciones lista a la que se agrega
     * @param etiqueta texto de la opción
     * @param detalle  descripción corta, en tenue
     * @param pausa    si al terminar espera Enter (las que redibujan su pantalla, no)
     * @param accion   la acción
     */
    protected static void agregar(List<Opcion> opciones, String etiqueta, String detalle,
            boolean pausa, Accion accion) {
        long numeradas = opciones.stream().filter(o -> !o.esSeccion()).count();
        opciones.add(new Opcion(String.valueOf(numeradas + 1), etiqueta, detalle, pausa, accion));
    }

    /**
     * Agrega el título de un grupo de opciones.
     *
     * @param opciones lista a la que se agrega
     * @param nombre   por ejemplo "Consultar"
     */
    protected static void seccion(List<Opcion> opciones, String nombre) {
        opciones.add(new Opcion("", nombre, "", false, null));
    }

    /**
     * Pide una clave nueva dos veces, con la regla de SPEC §3.
     *
     * @return la clave
     * @throws OperacionCancelada si agotó los intentos
     */
    protected String leerClaveNueva() throws OperacionCancelada {
        for (int intento = 1; intento <= Lector.INTENTOS; intento++) {
            String clave = lector.clave("Clave (8 o más, con letras y números)");
            try {
                Claves.validar(clave);
                if (!clave.equals(lector.clave("Repetí la clave"))) {
                    throw new DatoInvalidoException("Clave", null,
                            "Las dos claves no coinciden. Escribilas de nuevo.");
                }
                return clave;
            } catch (DatoInvalidoException ex) {
                int quedan = Lector.INTENTOS - intento;
                t.error(ex.getMessage() + (quedan > 0 ? " Te quedan " + quedan + " intentos." : ""));
            }
        }
        throw new OperacionCancelada("Se canceló: demasiados intentos en «Clave».");
    }
}
