package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.modelo.Estado;
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Venta;
import ar.edu.utn.frre.concesionaria.negocio.FiltroVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.OrdenVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.ServicioVentas;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import java.time.LocalDate;
import java.time.YearMonth;
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
            cabecera("", "Hola, " + sesion.getUsuario().getNombreCompleto() + ".");
            List<String[]> resumen = resumen();
            if (!resumen.isEmpty()) {
                t.lineas(Vistas.tarjetas(resumen, estilo, 30));
                t.blanco();
            }

            List<String> teclas = new ArrayList<>();
            List<Runnable> acciones = new ArrayList<>();
            List<String[]> filas = new ArrayList<>();
            seccionMenu(filas, "Operación");
            if (sesion.puede(Permiso.VER_STOCK)) {
                opcion(teclas, acciones, filas, "Vehículos", "stock, búsqueda, altas y bajas",
                        () -> new PantallaVehiculos(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.VENDER) || sesion.puede(Permiso.VER_VENTAS)) {
                opcion(teclas, acciones, filas, "Ventas", "vender e historial",
                        () -> new PantallaVentas(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.VER_REPORTES)) {
                opcion(teclas, acciones, filas, "Reportes", "stock, ventas y vendedores",
                        () -> new PantallaReportes(e, sesion).mostrar());
            }
            boolean administra = sesion.puede(Permiso.GESTIONAR_MARCAS)
                    || sesion.puede(Permiso.GESTIONAR_USUARIOS)
                    || sesion.puede(Permiso.EXPORTAR) || sesion.puede(Permiso.IMPORTAR);
            if (administra) {
                seccionMenu(filas, "Administración");
            }
            if (sesion.puede(Permiso.GESTIONAR_MARCAS)) {
                opcion(teclas, acciones, filas, "Marcas", "catálogo de marcas",
                        () -> new PantallaMarcas(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.GESTIONAR_USUARIOS)) {
                opcion(teclas, acciones, filas, "Usuarios", "altas, roles y claves",
                        () -> new PantallaUsuarios(e, sesion).mostrar());
            }
            if (sesion.puede(Permiso.EXPORTAR) || sesion.puede(Permiso.IMPORTAR)) {
                opcion(teclas, acciones, filas, "Datos", "exportar e importar CSV",
                        () -> new PantallaDatos(e, sesion).mostrar());
            }
            seccionMenu(filas, "Mi cuenta");
            opcion(teclas, acciones, filas, "Cambiar mi clave", "pide la clave actual",
                    () -> new PantallaUsuarios(e, sesion).cambiarClavePropia());
            filas.add(new String[]{"C", "Cerrar sesión", "volver al inicio de sesión"});

            String salir = "Salir del programa";
            int ancho = Math.max(salir.length(),
                    filas.stream().mapToInt(f -> f[1].length()).max().orElse(0));
            boolean primera = true;
            for (String[] f : filas) {
                if (f[0] == null) {
                    if (!primera) {
                        t.blanco();
                    }
                    t.seccion(f[1]);
                } else {
                    t.opcion(f[0], f[1], f[2], ancho);
                }
                primera = false;
            }
            t.blanco();
            t.opcion("0", salir, "guarda los cambios pendientes", ancho);
            t.pie("");

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

    /**
     * Tarjetas del inicio según lo que el rol puede ver (SPEC §5). Si un dato no se puede
     * calcular, esa tarjeta no aparece.
     */
    private List<String[]> resumen() {
        List<String[]> tarjetas = new ArrayList<>();
        LocalDate hoy = Reloj.hoy();
        try {
            List<Vehiculo> stock = e.vehiculos.listar(sesion, new FiltroVehiculos(),
                    OrdenVehiculos.TIPO_Y_MARCA);
            long nuevos = stock.stream().filter(v -> v.getEstado() == Estado.NUEVO).count();
            tarjetas.add(new String[]{"Stock disponible", stock.size() + " vehículos",
                    "0 km: " + nuevos + " · usados: " + (stock.size() - nuevos)});
        } catch (ConcesionariaException ex) {
            // Sin permiso para ver el stock: no se muestra la tarjeta.
        }
        try {
            List<Venta> delMes = e.ventas.historial(sesion, hoy.withDayOfMonth(1), hoy, null);
            boolean todas = sesion.puede(Permiso.VER_VENTAS);
            tarjetas.add(new String[]{(todas ? "Ventas de " : "Tus ventas de ")
                    + Formatos.mes(YearMonth.from(hoy)),
                    delMes.size() + (delMes.size() == 1 ? " venta" : " ventas"),
                    Formatos.moneda(ServicioVentas.total(delMes))});
        } catch (ConcesionariaException ex) {
            // Sin permiso para ver ventas.
        }
        if (sesion.puede(Permiso.VER_REPORTES)) {
            try {
                tarjetas.add(new String[]{"Valor del stock",
                        Formatos.moneda(e.reportes.valorizacion(sesion).getSumaFinal()),
                        "a precio final de hoy"});
            } catch (ConcesionariaException ex) {
                // No debería pasar: ya se verificó el permiso.
            }
        }
        return tarjetas;
    }

    private static void seccionMenu(List<String[]> filas, String nombre) {
        filas.add(new String[]{null, nombre, null});
    }

    private void opcion(List<String> teclas, List<Runnable> acciones, List<String[]> filas,
            String nombre, String detalle, Runnable accion) {
        String tecla = String.valueOf(teclas.size() + 1);
        teclas.add(tecla);
        acciones.add(accion);
        filas.add(new String[]{tecla, nombre, detalle});
    }
}
