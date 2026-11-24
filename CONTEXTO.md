# Contexto del proyecto

Archivo de traspaso para retomar el trabajo en cualquier sesión (persona o agente). Leelo
entero antes de escribir código.

## Qué es

El Trabajo Final Integrador (TFI) de Programación II, TUP, UTN FRRe, 2026: un sistema de
consola en Java para una concesionaria multimarca. La consigna completa está en
[docs/ENUNCIADO.md](docs/ENUNCIADO.md).

La metodología es Spec-Driven Development (SDD) en versión liviana: primero se escribe la
especificación, después el código, y cada requisito del enunciado queda trazado a la clase
que lo cumple. La spec está en [docs/SPEC.md](docs/SPEC.md), con
[docs/MODELO.md](docs/MODELO.md), [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md) y
[docs/TRAZABILIDAD.md](docs/TRAZABILIDAD.md).

## Datos administrativos

- **Trabajo individual.** La fecha de entrega no condiciona el plan.
- **Alumno:** Luciano Balbiano. DNI y legajo van solo en la carátula del PDF de entrega.
  GitHub: https://github.com/BalbianoLuciano · Portfolio: https://balbianoluciano.github.io
  (en la carátula van GitHub y portfolio en lugar del email).
- **Carátula:** la plantilla oficial TUP UTN FRRe, en `docs/assets/caratula/` (PDF de
  referencia y los logos TUP y UTN extraídos de ahí).
- **Rúbrica:** usar lo visto en clase (referencia: `~/Documents/PersonalRepos/CursoDeJava`).
  Todo lo que no se haya dado tiene que estar explicado en la sección "Contenido extra" de la
  spec.

## Decisiones tomadas

Todas confirmadas por el alumno el 2026-11-17 y el 2026-11-18. Cuando dijo "lo recomendado",
se tomó la opción recomendada.

| Tema | Decisión |
|---|---|
| Repo | Propio, separado del fork del curso |
| Lenguaje | Java SE puro, sin frameworks ni librerías externas. Código en español |
| Java | **25 (LTS)**, compilado con `--release 25` |
| Build | `javac` + `jar` desde un `build.sh` (en la línea del `run.sh` del curso). Sin Maven |
| Pruebas | Sin JUnit. Runner de pruebas propio y plan de pruebas manual |
| Interfaz | Consola muy trabajada ("no lazy console", palabras del profe; nada de estética IA slop). Menús numéricos, ANSI con alternativa sin color |
| Distribución | `.jar` en un release de GitHub. CheerpJ descartado (Java ≤ 21, sin JDBC, consola sin confirmar) |
| API HTTP | No |
| Alcance | Inventario + ventas con precio + reportes + usuarios con roles |
| Roles | Administrador, Vendedor y Directivo, con login |
| Moneda | Pesos argentinos (ARS), con `BigDecimal` |
| Identificador | Código interno generado (A-0001, C-0001, M-0001) + patente opcional y única |
| Subclases | Automóvil: carrocería + puertas. Camioneta: carrocería + 4x4 + carga útil kg. Moto: tipo + cilindrada cc |
| Marca | Entidad propia con catálogo y ABM. Se elige del catálogo, nunca se tipea a mano |
| Color | Enum |
| Precio | Precio de lista + reglas propias de cada subclase (depreciación, 4x4, patentamiento) |
| Persistencia | Un archivo `.ser` fuera del jar (`~/.concesionaria/`), escritura atómica, `.bak`, `serialVersionUID`, exportar/importar CSV |
| Estilo | Lambdas, streams, `Comparator` y referencias a métodos donde aclaren. Sin records, sealed ni pattern matching |
| Diagramas | Mermaid: capas, clases, secuencias, estados, persistencia, despliegue y rendimiento |

### Decisiones de implementación (2026-11-18)

| Tema | Decisión |
|---|---|
| Orden de trabajo | build.sh + runner → modelo/excepciones/comun → datos → negocio → presentación → docs y bench, un commit por etapa |
| Lint | `-Xlint:all` salvo `serial` y `this-escape` (los constructores JavaBean llaman a setters que validan; está comentado en `build.sh`) |
| Fecha | `Reloj` con un `Clock` estático; las pruebas lo fijan en 15/06/2026 |
| `ResultadoImportacion` | En `modelo`, no en `datos`: lo recorren las tres capas y la consola no puede importar `datos` |
| `Marca.clave` | Campo `transient` cacheado: el bench mostró 89 ms al ordenar 10.000 y bajó a 7 ms |
| Clave nueva | `Pantalla.leerClaveNueva()` la comparten el primer arranque, el alta de usuarios y el cambio de clave |
| Pantallas (2026-11-24) | Esqueleto común de SPEC §5: título y ruta, contexto, opciones con descripción y secciones, `0`/`C` alineados y "Volver a …", cada acción en su propia pantalla, prompt en negrita y avisos en amarillo |
| Formularios y resumen (2026-11-24) | Formularios por pasos con ayuda antes del campo, ficha en dos columnas, tarjetas de resumen en el inicio según el rol y stock agrupado por tipo en el orden natural |
| README e imágenes (2026-11-24) | Capturas reales de la consola (por pseudo-terminal) convertidas a SVG, más diagramas de capas, precio y persistencia en `docs/img/`. Copias PNG en `docs/img/png/` para las notas del release, donde un SVG relativo no se ve |
| Elegir un vehículo (2026-11-24) | Selector común en `Pantalla.elegirVehiculo`: búsqueda por partes y tabla numerada, en lugar de pedir el código. Sin autocompletado con Tab: exigiría la terminal en modo crudo, fuera del JDK (SPEC §5) |
| Tolerancia al error humano (2026-11-24) | Listas por número o nombre (`Textos.coincidencias`), números con unidad, patentes con puntos, sugerencia de marca parecida, y al agotar los intentos de un campo se pregunta si seguir en vez de descartar el formulario (SPEC §3). El login conserva su límite de 3 |
| Catálogo de modelos (2026-11-24) | Cada `Marca` tiene su catálogo de `Modelo` (nombre y tipo). El modelo se elige de la lista, nunca se escribe libre; el negocio lo vuelve a controlar en alta, modificación e importación. Búsqueda por comienzo y, si no hay, por clave fonética (`Textos.fonetica`: k/q→c, z→s, v→b, y→i, sin h ni dobles) o hasta 2 letras de diferencia: "korollà" → "¿Quisiste decir Corolla?". Solo el administrador agrega modelos (también desde el alta). Esquema 2: la migración arma el catálogo con los vehículos existentes. El vehículo guarda el modelo como texto, así el CSV no cambió |
| Catálogo de referencia (2026-11-24) | `catalogo-ar-2026.csv` dentro del `.jar` (recurso junto a `CatalogoReferencia`; `build.sh` copia todo lo que no es `.java`): 52 marcas y unos 490 modelos del mercado argentino, vigentes en 2026 y habituales en usados. Se incorpora en el primer arranque y en la migración al esquema 3, solo agregando lo que falta. `DatosDemo` ya no define marcas: toma las del catálogo y suma 10 vehículos. Apodos de marca para buscar ("vw", "chevy"). `nombrePropio` respeta siglas de hasta 4 letras, mayúsculas mezcladas ("CFMoto") y conectores ("Corea del Sur"). El catálogo sale del conocimiento del mercado a mediados de 2026: no es exhaustivo y el administrador completa lo que falte |
| Nada se elige sin verlo (2026-11-24) | En todo lugar donde se elige (también las listas numeradas de `Lector.elegir`: tipo, color, rol, usuario, marca…): solo el nombre completo (o código/patente exactos, o un apodo) se toma directo; una coincidencia parcial única se confirma con "(S/n)" donde Enter acepta (`Lector.confirmar(pregunta, porDefecto)`); sin coincidencias se muestra la lista completa antes de ofrecer agregar. `Textos.coincidencias` busca "dentro" del nombre solo con 3 letras o más. Origen: "t" elegía Toyota y Etios sin preguntar, porque la SPEC decía "una sola coincidencia se elige sola" |
| Escribir o listar (2026-11-24) | En Marca y Modelo del formulario se ve la opción `L  Listar modelos de Toyota · 7 automóviles` debajo de la ayuda: se escribe el nombre o se aprieta `L` (también `?` o Enter) para la lista numerada. `L` no choca con ningún nombre porque marcas y modelos tienen 2 letras o más |
| Aire entre campos (2026-11-24) | `Terminal` recuerda qué imprimió último y deja un renglón en blanco antes de cada campo nuevo, sin duplicarlo ni separar un campo de su ayuda o su lista. La ayuda y los avisos se parten al ancho de la consola |
| Interactividad | `Lector` recibe `interactivo` explícito: con entrada redirigida hace eco de lo leído y no usa `readPassword` |

## Estado actual

- Implementación completa: los CU-01 a CU-17 funcionan en la consola, con el rediseño de
  SPEC §5 (esqueleto común, formularios por pasos, resumen por rol).
- `./build.sh test`: 96 pruebas automáticas en verde. Plan manual PM-01 a PM-20 en SPEC §8.
- `./build.sh bench`: resultados reales en ARQUITECTURA §7 (Apple M4, JVM 27).
- `./build.sh docs`: JavaDoc sin errores.
- Releases en GitHub con el `.jar` y su SHA-256: `v1.0.0` (entrega inicial) y `v1.1.0`
  (selección de vehículos por búsqueda y tolerancia al error humano).
- Informe de entrega: `docs/informe/informe.html`. El PDF se arma con
  `ALUMNO_DNI=... ALUMNO_LEGAJO=... docs/informe/generar.sh` en `dist/entrega/`; el DNI y el
  legajo no se versionan porque el repositorio es público.
- Pendiente: recorrer el plan manual en una terminal real.
