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

    /** Una opción de menú. */
    static final class Opcion {
        final String tecla;
        final String etiqueta;
        final Accion accion;
        final boolean pausa;

        Opcion(String tecla, String etiqueta, boolean pausa, Accion accion) {
            this.tecla = tecla;
            this.etiqueta = etiqueta;
            this.accion = accion;
            this.pausa = pausa;
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
     * Limpia la pantalla y muestra el encabezado fijo con la ruta.
     *
     * @param ruta la ruta debajo de "Inicio" ("Vehículos › Registrar"), o vacía
     */
    protected void cabecera(String ruta) {
        t.limpiar();
        String fecha = Formatos.fechaEncabezado(Reloj.hoy());
        String derecha = sesion == null ? fecha
                : sesion.getNombreUsuario() + " · " + sesion.getRol().getEtiqueta() + " · " + fecha;
        t.encabezado(derecha, ruta.isEmpty() ? "Inicio" : "Inicio › " + ruta);
        t.blanco();
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
            t.error(ex.getMessage());
        } catch (EntidadNoEncontradaException | ReglaNegocioException ex) {
            t.error(ex.getMessage());
        } catch (SeguridadException ex) {
            t.error(ex.getMessage());
        } catch (PersistenciaException ex) {
            t.error(ex.getMessage());
        } catch (ConcesionariaException ex) {
            t.error(ex.getMessage());
        } catch (FinDeEntrada ex) {
            throw ex;
        } catch (RuntimeException ex) {
            e.registroErrores.accept(ex);
            t.error("Ocurrió un error inesperado. El detalle quedó en error.log de la carpeta de"
                    + " datos; el programa sigue funcionando.");
        }
        return false;
    }

    /**
     * Muestra un menú numerado hasta que el usuario elige 0.
     *
     * @param ruta     la ruta de navegación
     * @param opciones las opciones (las que no corresponden al rol no se agregan)
     */
    protected void menu(String ruta, List<Opcion> opciones) {
        while (true) {
            cabecera(ruta);
            List<String> validas = new ArrayList<>();
            for (Opcion o : opciones) {
                t.linea("  " + estilo.negrita(String.format("%2s", o.tecla)) + "  " + o.etiqueta);
                validas.add(o.tecla);
            }
            validas.add("0");
            t.pie("0 Volver");
            String elegida = lector.opcion(validas);
            if (elegida.equals("0")) {
                return;
            }
            for (Opcion o : opciones) {
                if (o.tecla.equals(elegida)) {
                    t.blanco();
                    ejecutar(o.accion);
                    if (o.pausa) {
                        lector.pausa();
                    }
                }
            }
        }
    }

    /**
     * @param opciones lista a la que se agrega
     * @param etiqueta texto de la opción
     * @param pausa    si al terminar espera Enter
     * @param accion   la acción
     */
    protected static void agregar(List<Opcion> opciones, String etiqueta, boolean pausa,
            Accion accion) {
        opciones.add(new Opcion(String.valueOf(opciones.size() + 1), etiqueta, pausa, accion));
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
