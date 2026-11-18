package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import java.util.ArrayList;
import java.util.List;

/**
 * Menú principal. Muestra solo lo que el rol puede hacer (SPEC §1); el negocio igual vuelve a
 * verificar cada permiso.
 */
public final class MenuPrincipal extends Pantalla {

    /** Cómo se salió del menú. */
    public enum Salida {
        /** Volver al login. */
        CERRAR_SESION,
        /** Terminar el programa. */
        SALIR
    }

    /**
     * @param entorno terminal, lector y servicios
     * @param sesion  la sesión iniciada
     */
    public MenuPrincipal(Entorno entorno, Sesion sesion) {
        super(entorno, sesion);
    }

    /**
     * Muestra el menú hasta que el usuario cierra sesión o sale.
     *
     * @return cómo salió
     */
    public Salida mostrar() {
        while (true) {
            cabecera("");
            t.linea(" Hola, " + sesion.getUsuario().getNombreCompleto() + ".");
            t.blanco();

            List<String> teclas = new ArrayList<>();
            List<Runnable> acciones = new ArrayList<>();
            if (sesion.puede(Permiso.VER_STOCK)) {
                opcion(teclas, acciones, "Vehículos", "stock, búsqueda, altas y bajas",
                        () -> new PantallaVehiculos(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.VENDER) || sesion.puede(Permiso.VER_VENTAS)) {
                opcion(teclas, acciones, "Ventas", "vender e historial",
                        () -> new PantallaVentas(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.VER_REPORTES)) {
                opcion(teclas, acciones, "Reportes", "stock, ventas y vendedores",
                        () -> new PantallaReportes(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.GESTIONAR_MARCAS)) {
                opcion(teclas, acciones, "Marcas", "catálogo",
                        () -> new PantallaMarcas(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.GESTIONAR_USUARIOS)) {
                opcion(teclas, acciones, "Usuarios", "altas, roles y claves",
                        () -> new PantallaUsuarios(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.EXPORTAR) || sesion.puede(Permiso.IMPORTAR)) {
                opcion(teclas, acciones, "Datos", "exportar e importar CSV",
                        () -> new PantallaDatos(e, sesion).mostrar());
            }
            opcion(teclas, acciones, "Cambiar mi clave", "",
                    () -> new PantallaUsuarios(e, sesion).cambiarClavePropia());
            t.blanco();
            t.linea("  " + estilo.negrita(" C") + "  Cerrar sesión");
            t.pie("0 Salir del programa");

            List<String> validas = new ArrayList<>(teclas);
            validas.add("c");
            validas.add("0");
            String elegida = lector.opcion(validas);
            if (elegida.equals("0")) {
                return Salida.SALIR;
            }
            if (elegida.equals("c")) {
                return Salida.CERRAR_SESION;
            }
            acciones.get(Integer.parseInt(elegida) - 1).run();
        }
    }

    private void opcion(List<String> teclas, List<Runnable> acciones, String nombre,
            String detalle, Runnable accion) {
        String tecla = String.valueOf(teclas.size() + 1);
        teclas.add(tecla);
        acciones.add(accion);
        t.linea("  " + estilo.negrita(String.format("%2s", tecla)) + "  "
                + String.format("%-18s", nombre) + estilo.tenue(detalle));
    }
}
