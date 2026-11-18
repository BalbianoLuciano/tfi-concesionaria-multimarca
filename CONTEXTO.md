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
| Interactividad | `Lector` recibe `interactivo` explícito: con entrada redirigida hace eco de lo leído y no usa `readPassword` |

## Estado actual

- Implementación completa: los CU-01 a CU-17 funcionan en la consola.
- `./build.sh test`: 87 pruebas automáticas en verde. Plan manual PM-01 a PM-16 en SPEC §8.
- `./build.sh bench`: resultados reales en ARQUITECTURA §7 (Apple M4, JVM 27).
- `./build.sh docs`: JavaDoc sin errores.
- Pendiente: correr el plan manual en una terminal real, carátula e informe de entrega,
  release del `.jar` en GitHub.

## Antecedentes en el curso

En `CursoDeJava/src/` hay referencia de estilo y de lo que se dio:

- `Practica/calculadorabase3/`: jerarquía de excepciones propias, `catch` de lo específico a lo
  general, reintentos de data-entry, traducción de `NumberFormatException` a excepción propia.
- `Practica/biblioteca/` y `Practica/gestioninventario/`: encapsulamiento, constructores
  encadenados, validación en setters.
- `CursoJava/Serializacion/`, `Colecciones*`, `ExpresionesLambda*`, `InterfaceEscritorio/`
  (Swing), `BasesDeDatos/` (JDBC + MySQL).
- `run.sh`: compila y ejecuta con `javac` y `java` sin IDE.

No aparecen en el curso, así que se explican como extra si se usan: `BigDecimal`, `java.time`,
`MessageDigest`, códigos ANSI, `Files.move` atómico.
