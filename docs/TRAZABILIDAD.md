# Trazabilidad

Cada requisito del [enunciado](ENUNCIADO.md) y dónde se cumple. Las rutas de clases son
relativas a `src/ar/edu/utn/frre/concesionaria/`. Cuando el código exista, la columna
"Prueba" apunta al grupo que lo verifica.

## Requisitos del enunciado

| # | Requisito | Dónde se cumple | Prueba |
|---|---|---|---|
| 1 | Dominio del problema, abstracción, modelado, tipos de datos | `modelo/`: `Vehiculo` y subclases, `Marca` con su catálogo de `Modelo`, `Venta`, `Usuario`. [MODELO.md](MODELO.md) | PR-COLECCION |
| 2 | Diseño vs. Negocio | Paquetes `presentacion` / `negocio` / `datos` con dependencias en un solo sentido. [ARQUITECTURA.md §1–2](ARQUITECTURA.md#1-vista-general-diseño-vs-negocio) | — (revisión de imports) |
| 3 | Remarks y JavaDoc | JavaDoc en todas las clases y métodos públicos; comentarios que explican el porqué. `./build.sh docs` | — |
| 4 | Clases abstractas, herencia, polimorfismo | `Vehiculo` abstracta; `Automovil`, `Camioneta`, `Motocicleta`; template method `calcularPrecioFinal()` y `getDetalleTipo()` | PR-PRECIO |
| 5 | JavaBean y POJO | Entidades con constructor vacío, getters/setters y `Serializable`. `DesglosePrecio` y `Comprador` como POJOs inmutables | PR-COLECCION, PR-PERSIST |
| 6 | Interfaces, transmisión del comportamiento | `Vendible`, `Identificable<K>`, `Comparable<Vehiculo>`, `Repositorio<T,K>` y sus implementaciones | PR-VENTA |
| 7 | Excepciones, incluyendo propias | Jerarquía `ConcesionariaException` en `excepciones/`; `catch` escalonado en la consola; `PersistenciaException` encadena la `IOException`. [MODELO.md §4](MODELO.md#4-excepciones-propias) | PR-VALID, PR-VENTA, PR-PERMISO, PR-PERSIST |
| 8 | Tipos enumerativos | `TipoVehiculo`, `Estado`, `Situacion`, `Color`, `CarroceriaAutomovil`, `CarroceriaCamioneta`, `TipoMotocicleta`, `Rol`, `Permiso` | PR-VALID |
| 9 | Interacción con el usuario | `presentacion/consola/` (`Lector` con reintentos, `Tabla`, `Terminal`, `Estilo`, `GraficoBarras`) y `presentacion/pantallas/` (`Pantalla` base con el `catch` escalonado, `Vistas` para ficha, desglose y comprobante). [SPEC §3 y §5](SPEC.md#5-la-consola) | Plan manual |
| 10 | Wrappers | `Integer`/`Boolean` en el modelo (null = sin cargar), `parseInt` en el Lector, autoboxing en mapas de reportes | PR-VALID |
| 11 | Colección, sobrescritura de métodos esenciales | `HashMap`, `TreeMap`, `ArrayList` en `datos/`; `equals`, `hashCode`, `toString`, `compareTo` en las entidades | PR-COLECCION |

## Lo que agrega la infografía

| Punto | Dónde se cumple |
|---|---|
| Vehículos multimarca en un único sistema | `Vehiculo` + catálogo `Marca`, cada una con su catálogo de `Modelo` |
| Clasificación por estado (0 km / usado con km) | Enum `Estado` y la regla de `setKilometraje()`. [MODELO.md §3](MODELO.md#nuevo-y-usado) |
| Atributos obligatorios: marca, modelo, año, color, carrocería | Atributos de `Vehiculo` y subclases, validados en los setters |
| Esquema en capas: interfaz, negocio, datos | Requisito 2 |
| Control y colecciones: excepciones, colecciones, wrappers, JavaDoc | Requisitos 3, 7, 10 y 11 |

## Casos de uso → clases

| Caso de uso | Pantalla | Servicio |
|---|---|---|
| CU-01 Primer arranque | `PantallaInicio`; `App` incorpora el catálogo de referencia | `ServicioAutenticacion`, `DatosDemo`, `datos/CatalogoReferencia` (`catalogo-ar-2026.csv`) |
| CU-02 Sesión | `PantallaInicio`, `MenuPrincipal` (cerrar sesión) | `ServicioAutenticacion`, `Sesion` |
| CU-03 a CU-09 Vehículos | `PantallaVehiculos` (modelo con `elegirModelo`); selector común en `Pantalla.elegirVehiculo` (SPEC §5) | `ServicioVehiculos`, `FiltroVehiculos`, `OrdenVehiculos`; `ServicioMarcas.paraAlta` y `exigirModeloDelCatalogo` |
| CU-10, CU-11 Ventas | `PantallaVentas` (comprobante en `Vistas`; vehículo con `Pantalla.elegirVehiculo`) | `ServicioVentas` |
| CU-12 Marcas y modelos | `PantallaMarcas` (opción Modelos) | `ServicioMarcas` (`agregarModelo`, `quitarModelo`, `modelosParecidos`); `modelo/Modelo`; `Textos.fonetica` y `Textos.parecidas` (pruebas PR-MARCA-09 a 12, PR-COMUN-12) |
| CU-13, CU-16 Usuarios | `PantallaUsuarios` | `ServicioAutenticacion`, `Claves` |
| CU-14 Reportes | `PantallaReportes` | `ServicioReportes` (calcula) + `ServicioArchivos` (exporta a .txt) |
| CU-15 CSV | `PantallaDatos` | `ServicioArchivos` + `datos/ArchivosCsv` |
| CU-17 Salir | `App` (shutdown hook) | `Persistencia` → `AlmacenArchivo` |

## Requisitos no funcionales → clases

| Requisito | Dónde se cumple | Verificación |
|---|---|---|
| Rendimiento (ARQUITECTURA §7) | Índices en `RepositorioVehiculosMemoria` y `RepositorioMarcasMemoria`; clave cacheada en `Marca` | `rendimiento/MedicionRendimiento` (`./build.sh bench`) |
| Consola trabajada (SPEC §5) | `presentacion/consola/` y `presentacion/pantallas/`; aire entre campos con `Terminal.separarCampo` | Plan manual PM-01 a PM-18 (SPEC §8); PR-COMUN-10 a PR-COMUN-12 |
| Compatibilidad de datos (ARQUITECTURA §4) | `MigradorAlmacen`: esquema 1 → 2 arma el catálogo de modelos; 2 → 3 suma el catálogo de referencia | PR-PERSIST-09 |
| Catálogo de referencia (CU-12) | `datos/CatalogoReferencia` lee `catalogo-ar-2026.csv` (52 marcas, unos 490 modelos) | PR-MARCA-13 |
