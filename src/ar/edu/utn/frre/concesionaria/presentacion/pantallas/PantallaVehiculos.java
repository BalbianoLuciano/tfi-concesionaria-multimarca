package ar.edu.utn.frre.concesionaria.presentacion.pantallas;

import ar.edu.utn.frre.concesionaria.comun.Formatos;
import ar.edu.utn.frre.concesionaria.comun.Reloj;
import ar.edu.utn.frre.concesionaria.excepciones.ConcesionariaException;
import ar.edu.utn.frre.concesionaria.excepciones.DatoInvalidoException;
import ar.edu.utn.frre.concesionaria.excepciones.PatenteDuplicadaException;
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
import ar.edu.utn.frre.concesionaria.modelo.Permiso;
import ar.edu.utn.frre.concesionaria.modelo.Situacion;
import ar.edu.utn.frre.concesionaria.modelo.TipoMotocicleta;
import ar.edu.utn.frre.concesionaria.modelo.TipoVehiculo;
import ar.edu.utn.frre.concesionaria.modelo.Vehiculo;
import ar.edu.utn.frre.concesionaria.negocio.FiltroVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.OrdenVehiculos;
import ar.edu.utn.frre.concesionaria.negocio.Sesion;
import ar.edu.utn.frre.concesionaria.presentacion.consola.Lector;
import ar.edu.utn.frre.concesionaria.presentacion.consola.OperacionCancelada;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Stock de vehículos (CU-03 a CU-09).
 */
final class PantallaVehiculos extends Pantalla {

    /** Filas por página del listado (CU-04). */
    static final int FILAS_POR_PAGINA = 15;

    private static final String AYUDA_ANIO = "Escribí el año con cuatro dígitos, por ejemplo 2019.";

    private final FiltroVehiculos filtro = new FiltroVehiculos();
    private OrdenVehiculos orden = OrdenVehiculos.TIPO_Y_MARCA;

    PantallaVehiculos(Entorno entorno, Sesion sesion) {
        super(entorno, sesion);
    }

    void mostrar() {
        List<Opcion> opciones = new ArrayList<>();
        agregar(opciones, "Listar stock", false, this::listar);
        agregar(opciones, "Buscar", true, this::buscar);
        agregar(opciones, "Ver ficha", true, this::verFicha);
        if (sesion.puede(Permiso.VENDER)) {
            agregar(opciones, "Cotizar", true, this::cotizar);
        }
        if (sesion.puede(Permiso.GESTIONAR_STOCK)) {
            agregar(opciones, "Registrar vehículo", true, this::registrar);
            agregar(opciones, "Modificar vehículo", true, this::modificar);
            agregar(opciones, "Dar de baja", true, this::darDeBaja);
        }
        menu("Vehículos", opciones);
    }

    // ---------------------------------------------------------------- CU-04 listar

    private void listar() throws ConcesionariaException {
        int pagina = 0;
        while (true) {
            List<Vehiculo> lista = e.vehiculos.listar(sesion, filtro, orden);
            int paginas = Math.max(1, (lista.size() + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
            pagina = Math.min(pagina, paginas - 1);
            List<Vehiculo> visibles = lista.subList(pagina * FILAS_POR_PAGINA,
                    Math.min(lista.size(), (pagina + 1) * FILAS_POR_PAGINA));

            cabecera("Vehículos › Stock");
            t.info("Filtro: " + filtro.describir() + "   Orden: " + orden.getEtiqueta());
            t.blanco();
            if (lista.isEmpty()) {
                t.linea(" No hay vehículos con ese filtro. Probá con L para limpiarlo.");
            } else {
                t.lineas(Vistas.tablaStock(visibles, estilo, filtro.getSituacion() == null)
                        .renglones(t.ancho(), estilo));
            }
            long nuevos = lista.stream().filter(v -> v.getEstado() == Estado.NUEVO).count();
            t.blanco();
            t.linea(" " + estilo.negrita(lista.size() + " vehículos") + estilo.tenue(" · 0 km: "
                    + nuevos + " · usados: " + (lista.size() - nuevos) + " · página "
                    + (pagina + 1) + " de " + paginas));

            List<String> validas = new ArrayList<>(List.of("f", "o", "l", "v", "0"));
            StringBuilder teclas = new StringBuilder();
            if (pagina < paginas - 1) {
                teclas.append("S siguiente · ");
                validas.add("s");
            }
            if (pagina > 0) {
                teclas.append("A anterior · ");
                validas.add("a");
            }
            t.pie(teclas + "F filtrar · O ordenar · L limpiar filtros · V ver ficha · 0 volver");
            String opcion = lector.opcion(validas);
            switch (opcion) {
                case "s":
                    pagina++;
                    break;
                case "a":
                    pagina--;
                    break;
                case "f":
                    ejecutar(this::elegirFiltro);
                    pagina = 0;
                    break;
                case "o":
                    ejecutar(() -> orden = lector.elegir("Orden",
                            Arrays.asList(OrdenVehiculos.values()), OrdenVehiculos::getEtiqueta));
                    pagina = 0;
                    break;
                case "l":
                    filtro.limpiar();
                    pagina = 0;
                    break;
                case "v":
                    if (ejecutar(this::verFicha)) {
                        lector.pausa();
                    }
                    break;
                default:
                    return;
            }
        }
    }

    private void elegirFiltro() throws ConcesionariaException, OperacionCancelada {
        t.blanco();
        List<String> campos = List.of("Tipo", "Estado", "Marca", "Rango de año",
                "Rango de precio final", "Situación");
        String campo = lector.elegir("Filtrar por", campos, c -> c);
        switch (campo) {
            case "Tipo":
                filtro.setTipo(elegirOTodos("Tipo", Arrays.asList(TipoVehiculo.values())));
                break;
            case "Estado":
                filtro.setEstado(elegirOTodos("Estado", Arrays.asList(Estado.values())));
                break;
            case "Marca":
                filtro.setMarca(elegirOTodosMarca());
                break;
            case "Rango de año":
                filtro.setAnioDesde(lector.leerOpcional("Desde el año (Enter: sin límite)",
                        Lector.entero("Año", AYUDA_ANIO)));
                filtro.setAnioHasta(lector.leerOpcional("Hasta el año (Enter: sin límite)",
                        Lector.entero("Año", AYUDA_ANIO)));
                break;
            case "Rango de precio final":
                filtro.setPrecioDesde(lector.leerOpcional("Desde $ (Enter: sin límite)",
                        Lector.monto("Precio")));
                filtro.setPrecioHasta(lector.leerOpcional("Hasta $ (Enter: sin límite)",
                        Lector.monto("Precio")));
                break;
            default:
                List<String> situaciones = new ArrayList<>();
                situaciones.add("Todas");
                Arrays.stream(Situacion.values()).forEach(s -> situaciones.add(s.getEtiqueta()));
                String elegida = lector.elegir("Situación", situaciones, s -> s);
                filtro.setSituacion(elegida.equals("Todas") ? null
                        : ConEtiqueta.desdeTexto(Situacion.class, "Situación", elegida));
                break;
        }
    }

    private <E extends Enum<E> & ConEtiqueta> E elegirOTodos(String etiqueta, List<E> valores)
            throws OperacionCancelada {
        List<String> nombres = new ArrayList<>();
        nombres.add("Todos");
        valores.forEach(v -> nombres.add(v.getEtiqueta()));
        String elegido = lector.elegir(etiqueta, nombres, n -> n);
        return elegido.equals("Todos") ? null : valores.get(nombres.indexOf(elegido) - 1);
    }

    private Marca elegirOTodosMarca() throws ConcesionariaException, OperacionCancelada {
        List<Marca> marcas = e.marcas.listar(sesion);
        List<String> nombres = new ArrayList<>();
        nombres.add("Todas");
        marcas.forEach(m -> nombres.add(m.getNombre()));
        String elegido = lector.elegir("Marca", nombres, n -> n);
        return elegido.equals("Todas") ? null : marcas.get(nombres.indexOf(elegido) - 1);
    }

    // ---------------------------------------------------------------- CU-05 y CU-06

    private void buscar() throws ConcesionariaException, OperacionCancelada {
        t.info("Buscá por código, patente, marca o modelo. Por ejemplo: peugeot 208, ab123cd, c-2.");
        String texto = lector.leer("Buscar", s -> s);
        List<Vehiculo> encontrados = e.vehiculos.buscarTexto(sesion, texto);
        t.blanco();
        if (encontrados.isEmpty()) {
            t.linea(" No se encontró nada con \"" + texto + "\". Probá con menos palabras.");
            return;
        }
        t.lineas(Vistas.tablaStock(encontrados, estilo, true).renglones(t.ancho(), estilo));
        t.blanco();
        t.info(encontrados.size() + " resultado(s). Para ver uno, usá Ver ficha con su código.");
    }

    private void verFicha() throws ConcesionariaException, OperacionCancelada {
        Vehiculo v = leerVehiculo();
        t.blanco();
        t.lineas(Vistas.ficha(v, estilo));
        t.blanco();
        t.titulo("Precio");
        t.lineas(Vistas.desglose(v.desglosarPrecio(Reloj.hoy()), v.getTipo(), estilo));
    }

    /**
     * Pide un código y busca el vehículo; 3 intentos.
     */
    private Vehiculo leerVehiculo() throws OperacionCancelada {
        return lector.leer("Código", codigo -> {
            try {
                return e.vehiculos.buscar(sesion, codigo);
            } catch (ConcesionariaException ex) {
                throw new DatoInvalidoException("Código", codigo, ex.getMessage());
            }
        });
    }

    // ---------------------------------------------------------------- CU-09 cotizar

    private void cotizar() throws ConcesionariaException, OperacionCancelada {
        Vehiculo v = leerVehiculo();
        t.blanco();
        t.titulo("Cotización · " + v.getCodigo() + " · " + v.getDescripcion());
        t.info(v.getTipo().getEtiqueta() + " · " + v.getDetalleTipo() + " · "
                + v.getEstado().getEtiqueta()
                + (v.getEstado() == Estado.USADO ? " · " + Formatos.km(v.getKilometraje()) : ""));
        t.blanco();
        t.lineas(Vistas.desglose(e.vehiculos.cotizar(sesion, v.getCodigo()), v.getTipo(), estilo));
        t.blanco();
        t.info("Precio a hoy, " + Formatos.fecha(Reloj.hoy()) + ". Para vender, andá a Ventas.");
    }

    // ---------------------------------------------------------------- CU-03 registrar

    private void registrar() throws ConcesionariaException, OperacionCancelada {
        lector.operacion("el registro");
        t.info("Escribí :q en cualquier campo para cancelar.");
        t.blanco();
        TipoVehiculo tipo = lector.elegir("Tipo", Arrays.asList(TipoVehiculo.values()),
                TipoVehiculo::getEtiqueta);
        Vehiculo v = tipo == TipoVehiculo.AUTOMOVIL ? new Automovil()
                : tipo == TipoVehiculo.CAMIONETA ? new Camioneta() : new Motocicleta();
        t.blanco();
        Estado estado = lector.elegir("Estado", Arrays.asList(Estado.values()), Estado::getEtiqueta);
        v.setEstado(estado);
        t.blanco();
        Marca marca = elegirMarca(tipo, null);
        v.setMarca(marca);

        lector.leer("Modelo", m -> {
            v.setModelo(m);
            return m;
        });
        lector.leer("Año", a -> {
            v.setAnio(Lector.entero("Año", AYUDA_ANIO).convertir(a));
            return a;
        });
        t.blanco();
        v.setColor(lector.elegir("Color", Arrays.asList(Color.values()), Color::getEtiqueta));
        lector.leer("Precio de lista $", p -> {
            v.setPrecioLista(Lector.monto("Precio de lista").convertir(p));
            return p;
        });
        if (estado == Estado.USADO) {
            lector.leer("Kilometraje", k -> {
                v.setKilometraje(Lector.entero("Kilometraje",
                        "Escribí solo números, por ejemplo 84500 u 84.500.").convertir(k));
                return k;
            });
            lector.leer("Patente", p -> asignarPatente(v, p));
        } else {
            lector.leerOpcional("Patente (Enter si todavía no está patentado)",
                    p -> asignarPatente(v, p));
        }
        leerDatosPropios(v, false);

        t.blanco();
        t.lineas(Vistas.ficha(v, estilo));
        t.blanco();
        t.lineas(Vistas.desglose(v.desglosarPrecio(Reloj.hoy()), tipo, estilo));
        t.blanco();
        if (!lector.confirmar("¿Registrar este vehículo?")) {
            t.info("No se registró nada.");
            return;
        }
        try {
            String codigo = e.vehiculos.registrar(sesion, v);
            t.exito("Registrado como " + codigo + ".");
        } catch (PersistenciaException ex) {
            t.error(ex.getMessage());
            t.info("Quedó cargado como " + v.getCodigo() + " y se guarda en el próximo intento.");
        }
    }

    /** Patente: formato (en el setter) y unicidad (en el negocio), para repreguntar enseguida. */
    private String asignarPatente(Vehiculo v, String texto) throws DatoInvalidoException {
        v.setPatente(texto);
        try {
            e.vehiculos.exigirPatenteLibre(v.getPatente(), v.getCodigo());
        } catch (PatenteDuplicadaException ex) {
            throw new DatoInvalidoException("Patente", texto, ex.getMessage());
        }
        return texto;
    }

    /**
     * Elige una marca del catálogo escribiendo las primeras letras (CU-03, paso 3). Solo
     * aparecen las activas que fabrican el tipo. El administrador puede crear una nueva.
     */
    private Marca elegirMarca(TipoVehiculo tipo, Marca actual)
            throws ConcesionariaException, OperacionCancelada {
        for (int intento = 1; intento <= Lector.INTENTOS; intento++) {
            String prefijo = actual == null
                    ? lector.leer("Marca (primeras letras)", s -> s)
                    : lector.leer("Marca (primeras letras)", actual.getNombre(), s -> s);
            if (prefijo == null) {
                return actual;
            }
            List<Marca> coinciden = e.marcas.paraAlta(tipo, prefijo);
            if (coinciden.size() == 1) {
                t.info("→ " + coinciden.get(0).getNombre());
                return coinciden.get(0);
            }
            if (!coinciden.isEmpty()) {
                return lector.elegir("Marca", coinciden, Marca::getNombre);
            }
            t.error("No hay marcas activas de " + tipo.getPlural() + " que empiecen con \""
                    + prefijo + "\".");
            if (sesion.puede(Permiso.GESTIONAR_MARCAS)
                    && lector.confirmar("¿Crear la marca \"" + prefijo + "\" ahora?")) {
                return new PantallaMarcas(e, sesion).crear(prefijo, EnumSet.of(tipo));
            }
        }
        throw new OperacionCancelada("Se canceló el registro: demasiados intentos en «Marca».");
    }

    /**
     * Datos propios de cada tipo. Con {@code modificando}, muestra el valor actual y Enter lo
     * mantiene.
     */
    private void leerDatosPropios(Vehiculo v, boolean modificando) throws OperacionCancelada {
        t.blanco();
        if (v instanceof Automovil) {
            Automovil a = (Automovil) v;
            CarroceriaAutomovil c = lector.elegir("Carrocería",
                    Arrays.asList(CarroceriaAutomovil.values()), CarroceriaAutomovil::getEtiqueta,
                    modificando ? a.getCarroceria() : null);
            aplicar(() -> a.setCarroceria(c));
            lector.leer("Puertas", modificando ? String.valueOf(a.getPuertas()) : null, p -> {
                a.setPuertas(Lector.entero("Puertas", "Escribí un número del 2 al 5.").convertir(p));
                return p;
            });
        } else if (v instanceof Camioneta) {
            Camioneta c = (Camioneta) v;
            CarroceriaCamioneta car = lector.elegir("Carrocería",
                    Arrays.asList(CarroceriaCamioneta.values()), CarroceriaCamioneta::getEtiqueta,
                    modificando ? c.getCarroceria() : null);
            aplicar(() -> c.setCarroceria(car));
            Boolean es4x4 = lector.leer("¿Tracción 4x4? (s/n)",
                    modificando ? (c.getTraccion4x4() ? "s" : "n") : null, r -> {
                        String clave = r.toLowerCase();
                        if (clave.equals("s") || clave.equals("si") || clave.equals("sí")) {
                            return Boolean.TRUE;
                        }
                        if (clave.equals("n") || clave.equals("no")) {
                            return Boolean.FALSE;
                        }
                        throw new DatoInvalidoException("Tracción 4x4", r, "Respondé s o n.");
                    });
            if (es4x4 != null) {
                aplicar(() -> c.setTraccion4x4(es4x4));
            }
            lector.leer("Carga útil (kg)", modificando ? String.valueOf(c.getCargaUtilKg()) : null,
                    k -> {
                        c.setCargaUtilKg(Lector.entero("Carga útil",
                                "Escribí los kilos, por ejemplo 1000.").convertir(k));
                        return k;
                    });
        } else {
            Motocicleta m = (Motocicleta) v;
            TipoMotocicleta tm = lector.elegir("Tipo de moto",
                    Arrays.asList(TipoMotocicleta.values()), TipoMotocicleta::getEtiqueta,
                    modificando ? m.getTipoMoto() : null);
            aplicar(() -> m.setTipoMoto(tm));
            lector.leer("Cilindrada (cc)", modificando ? String.valueOf(m.getCilindradaCc()) : null,
                    c -> {
                        m.setCilindradaCc(Lector.entero("Cilindrada",
                                "Escribí los cc, por ejemplo 150.").convertir(c));
                        return c;
                    });
        }
    }

    /** Un setter que no puede fallar con un valor elegido de una lista. */
    @FunctionalInterface
    private interface Setter {
        void aplicar() throws DatoInvalidoException;
    }

    private static void aplicar(Setter setter) {
        try {
            setter.aplicar();
        } catch (DatoInvalidoException ex) {
            // Un valor elegido de la lista siempre es válido.
            throw new IllegalStateException(ex);
        }
    }

    // ---------------------------------------------------------------- CU-07 modificar

    private void modificar() throws ConcesionariaException, OperacionCancelada {
        lector.operacion("la modificación");
        Vehiculo original = leerVehiculo();
        original.exigirDisponible("modificar");
        Vehiculo v = original.copia();
        t.blanco();
        t.titulo(v.getCodigo() + " · " + v.getTipo().getEtiqueta() + " · "
                + v.getEstado().getEtiqueta());
        t.info("Enter deja el valor actual, que se muestra entre corchetes. :q cancela.");
        t.blanco();

        v.setMarca(elegirMarca(v.getTipo(), v.getMarca()));
        lector.leer("Modelo", v.getModelo(), m -> {
            v.setModelo(m);
            return m;
        });
        lector.leer("Año", String.valueOf(v.getAnio()), a -> {
            v.setAnio(Lector.entero("Año", AYUDA_ANIO).convertir(a));
            return a;
        });
        t.blanco();
        Color color = lector.elegir("Color", Arrays.asList(Color.values()), Color::getEtiqueta,
                v.getColor());
        v.setColor(color);
        lector.leer("Precio de lista $", Formatos.moneda(v.getPrecioLista()).replace("$ ", ""),
                p -> {
                    v.setPrecioLista(Lector.monto("Precio de lista").convertir(p));
                    return p;
                });
        if (v.getEstado() == Estado.USADO) {
            lector.leer("Kilometraje", Formatos.miles(v.getKilometraje()), k -> {
                v.setKilometraje(Lector.entero("Kilometraje",
                        "Escribí solo números, por ejemplo 84500.").convertir(k));
                return k;
            });
        }
        lector.leer("Patente", v.getPatente() == null ? "sin patente" : v.getPatente(),
                p -> asignarPatente(v, p));
        leerDatosPropios(v, true);

        t.blanco();
        t.lineas(Vistas.ficha(v, estilo));
        t.blanco();
        t.lineas(Vistas.desglose(v.desglosarPrecio(Reloj.hoy()), v.getTipo(), estilo));
        t.blanco();
        if (!lector.confirmar("¿Guardar los cambios?")) {
            t.info("No se cambió nada.");
            return;
        }
        e.vehiculos.modificar(sesion, v);
        t.exito("Se guardaron los cambios de " + v.getCodigo() + ".");
    }

    // ---------------------------------------------------------------- CU-08 baja

    private void darDeBaja() throws ConcesionariaException, OperacionCancelada {
        lector.operacion("la baja");
        Vehiculo v = leerVehiculo();
        v.exigirDisponible("dar de baja");
        t.blanco();
        t.lineas(Vistas.ficha(v, estilo));
        t.blanco();
        t.info("La baja es lógica: el vehículo queda en el historial pero no se puede vender.");
        String motivo = lector.leer("Motivo (por ejemplo: robo, destrucción, devolución)", m -> {
            if (m.length() > 80) {
                throw new DatoInvalidoException("Motivo", m, "Escribí un motivo de hasta 80 caracteres.");
            }
            return m;
        });
        lector.leer("Para confirmar, escribí el código " + v.getCodigo(), c -> {
            if (!v.getCodigo().equalsIgnoreCase(c.replace(" ", ""))) {
                throw new DatoInvalidoException("Confirmación", c, "No coincide. Escribí "
                        + v.getCodigo() + " para confirmar, o :q para cancelar.");
            }
            return c;
        });
        e.vehiculos.darDeBaja(sesion, v.getCodigo(), motivo);
        t.exito(v.getCodigo() + " quedó dado de baja.");
    }
}
