# Especificación funcional

Sistema de consola para una concesionaria multimarca: gestiona el stock de automóviles,
camionetas y motocicletas, nuevos (0 km) y usados, registra ventas y da reportes a los
directivos.

Documentos relacionados: [MODELO.md](MODELO.md) (clases y reglas del dominio),
[ARQUITECTURA.md](ARQUITECTURA.md) (capas, flujo, persistencia, rendimiento) y
[TRAZABILIDAD.md](TRAZABILIDAD.md) (requisito del enunciado → dónde se cumple).

Las decisiones de diseño y su porqué están en [../CONTEXTO.md](../CONTEXTO.md).

---

## 1. Actores y permisos

| Rol | Para qué está |
|---|---|
| **Administrador** | Gestiona usuarios y el catálogo de marcas. Puede hacer todo lo demás |
| **Vendedor** | Carga y mantiene el stock, cotiza y vende |
| **Directivo** | Consulta el stock y los reportes. No modifica nada |

| Caso de uso | Admin | Vendedor | Directivo |
|---|:-:|:-:|:-:|
| Ver stock, buscar, ver ficha | ✓ | ✓ | ✓ |
| Registrar, modificar, dar de baja vehículos | ✓ | ✓ | |
| Cotizar y vender | ✓ | ✓ | |
| Historial de ventas | ✓ | solo las propias | ✓ |
| Reportes | ✓ | | ✓ |
| ABM de marcas | ✓ | | |
| ABM de usuarios | ✓ | | |
| Exportar / importar CSV | ✓ | | exportar |
| Cambiar la propia clave | ✓ | ✓ | ✓ |

El menú de cada rol muestra solo lo que puede hacer. Igual, la capa de negocio vuelve a
verificar el permiso en cada operación (`AccesoDenegadoException`): la consola no es la que
decide la seguridad.

---

## 2. Casos de uso

Cada caso tiene un identificador que se usa en la trazabilidad y en los nombres de las pruebas.

### CU-01 · Primer arranque
1. Si no existe `~/.concesionaria/datos.ser`, el sistema crea la carpeta.
2. Pide crear el usuario administrador (usuario, nombre y clave dos veces).
3. Ofrece cargar los datos de demostración: 8 marcas y 10 vehículos de ejemplo.

### CU-02 · Iniciar y cerrar sesión
1. Pide usuario y clave. La clave no se muestra mientras se escribe (`System.console().readPassword()`;
   si no hay consola real, como en un IDE, se avisa y se lee visible).
2. Hay 3 intentos. Al tercer error el programa termina con un mensaje.
3. El mensaje de error no dice si falló el usuario o la clave ("Usuario o clave incorrectos").
4. Cerrar sesión vuelve al login sin salir del programa.

### CU-03 · Registrar vehículo
1. Elegir el tipo: Automóvil, Camioneta o Motocicleta.
2. Elegir el estado: Nuevo (0 km) o Usado.
3. Elegir la marca del catálogo. Se escriben las primeras letras ("toy") y se elige de la lista
   de coincidencias. Solo aparecen las marcas activas que fabrican ese tipo de vehículo. Si
   la marca no existe, el administrador la puede crear en ese momento.
4. Modelo, año, color y precio de lista.
5. Si es usado: kilometraje y patente (obligatoria). Si es nuevo: patente opcional y km = 0,
   que no se pregunta.
6. Los datos propios del tipo (ver [MODELO.md §3](MODELO.md#3-reglas-del-dominio)).
7. Se muestra un resumen con el precio final calculado y se pide confirmar.
8. El sistema asigna el código (`C-0004`), guarda y lo informa.

### CU-04 · Listar stock
- Por defecto muestra los vehículos **disponibles**, ordenados por tipo y marca.
- Filtros combinables: tipo, estado, marca, rango de año, rango de precio final.
- Orden: tipo y marca, precio (asc/desc), año (asc/desc), kilometraje.
- Paginado de 15 filas (`S` siguiente, `A` anterior, `0` volver).
- Pie: cantidad total, cuántos 0 km y cuántos usados del resultado filtrado.

### CU-05 · Buscar
Texto libre que busca en código, patente, marca y modelo, sin distinguir mayúsculas ni
acentos ("peugeot 208", "ab123cd", "c-0002").

### CU-06 · Ver ficha
Todos los datos del vehículo y el **desglose del precio**: lista, depreciación por antigüedad,
depreciación por km, recargos, patentamiento y precio final. Es lo que se le muestra al cliente.

### CU-07 · Modificar vehículo
- Se pueden cambiar todos los datos salvo el código, el tipo y el estado.
- Enter deja el valor actual (se muestra entre corchetes: `Color [Blanco] ›`).
- Un vehículo vendido o dado de baja no se puede modificar.

### CU-08 · Dar de baja
- Baja lógica: pasa a situación `BAJA` con fecha y motivo (texto corto). No se borra, para no
  perder el historial.
- Pide confirmación escribiendo el código del vehículo (no alcanza con "s").
- Un vehículo vendido no se puede dar de baja.

### CU-09 · Cotizar
Muestra el desglose de precio de un vehículo disponible sin venderlo.

### CU-10 · Vender
1. Elegir un vehículo disponible por código o búsqueda.
2. Datos del comprador: nombre y apellido, DNI.
3. Resumen con el desglose de precio y confirmación.
4. Se registra la venta (número correlativo, fecha, vendedor, precio final congelado) y el
   vehículo pasa a `VENDIDO`. Las dos cosas se guardan juntas o no se guarda ninguna.
5. Se imprime un comprobante formateado.

El precio de la venta queda congelado: si después cambian las reglas de precio, las ventas
pasadas no cambian.

### CU-11 · Historial de ventas
Lista de ventas filtrable por rango de fechas y vendedor, con el total recaudado del período.

### CU-12 · ABM de marcas
- Alta: nombre (se normaliza: "  toyota " → "Toyota"), país de origen y tipos que fabrica.
- El nombre no se puede repetir, y se compara normalizado: "TOYOTA" = "Toyota".
- Antes de crear una marca se muestran las parecidas que ya existen, para evitar duplicados
  como "Tollota".
- Una marca con vehículos no se borra: se desactiva (deja de ofrecerse en las altas).

### CU-13 · ABM de usuarios
Alta, desactivar, reactivar, cambiar rol y blanquear clave. Un administrador no se puede
desactivar a sí mismo, y siempre tiene que quedar al menos un administrador activo.

### CU-14 · Reportes (directivos)
| Reporte | Contenido |
|---|---|
| Stock por tipo y estado | Cantidades con barras horizontales (0 km / usado) |
| Stock por marca | Cantidad y valor total en precio final, ordenado de mayor a menor |
| Usados | Km promedio, mayor km, unidad más antigua, antigüedad promedio |
| Ventas por período | Unidades y recaudación por mes, con barras |
| Ranking de vendedores | Unidades vendidas y monto por vendedor en el período |
| Valorización del stock | Suma de precios de lista y de precios finales del stock disponible |

Cada reporte se puede exportar a `.txt` (el mismo texto, sin códigos de color).

### CU-15 · Exportar e importar CSV
- Exportar: vehículos, marcas o ventas a un `.csv` en la carpeta de datos (separador `;`,
  UTF-8, encabezado).
- Importar vehículos: valida cada fila con las mismas reglas que el alta. Si alguna fila falla,
  no se importa nada y se muestra un informe con línea, campo y motivo de cada error.

### CU-16 · Cambiar la propia clave
Clave actual, nueva y repetición.

### CU-17 · Salir
Guarda si hay cambios pendientes y cierra. Ctrl+C también guarda (shutdown hook).

---

## 3. Validaciones del data-entry

Reglas generales:
- Cada campo tiene **3 intentos**. Al tercer error se cancela la operación completa, con el
  mensaje "Se canceló el registro: demasiados intentos en «Año»." y vuelve al menú.
- El texto se recorta (`trim`) y los espacios internos repetidos se colapsan.
- Escribir `0` en una selección de menú, o `:q` en un campo de texto, cancela y vuelve sin
  guardar nada.
- Cada error dice qué estuvo mal y cómo corregirlo, y muestra los intentos que quedan.

| Campo | Regla | Mensaje de ejemplo |
|---|---|---|
| Año | Número entero entre 1990 y el año actual + 1 | El año tiene que estar entre 1990 y 2027. |
| Año (0 km) | ≥ año actual − 1 | Un 0 km no puede ser anterior a 2025. |
| Año (texto) | Tiene que ser numérico | "dos mil" no es un número. Escribí el año con cuatro dígitos, por ejemplo 2019. |
| Kilometraje (usado) | Entero entre 1 y 999.999. Acepta puntos de miles ("84.500") | El kilometraje de un usado tiene que estar entre 1 y 999.999. |
| Patente auto/camioneta | `AAA999` (1995–2016) o `AA999AA` (Mercosur) | "AB12CD" no es una patente válida. Formatos: ABC123 o AB123CD. |
| Patente moto | `999AAA` (anterior) o `A999AAA` (Mercosur) | "AB123CD" no es una patente de moto. Formatos: 123ABC o A123BCD. |
| Patente | Única en todo el stock (incluye vendidos y bajas) | La patente AB123CD ya está registrada en C-0002. |
| Modelo | 1 a 40 caracteres: letras, números, espacio, `-` y `.` | El modelo admite letras, números, espacios, guiones y puntos. |
| Precio de lista | Mayor a 0 y hasta 2.000.000.000. Acepta "38.000.000" o "38000000" | El precio tiene que ser mayor a $ 0. |
| Puertas | 2 a 5 | Un automóvil tiene entre 2 y 5 puertas. |
| Carga útil | 300 a 3.500 kg | La carga útil va de 300 a 3.500 kg. |
| Cilindrada | 50 a 2.500 cc | La cilindrada va de 50 a 2.500 cc. |
| Opción de menú o enum | Número de la lista | Elegí un número entre 1 y 4. |
| DNI | 7 u 8 dígitos (acepta puntos) | El DNI tiene 7 u 8 dígitos. |
| Nombre de persona | 2 a 60 caracteres, letras y espacios | El nombre solo puede tener letras y espacios. |
| Usuario | 4 a 20 caracteres: minúsculas, números, `.` y `_`. Único | Ese usuario ya existe. |
| Clave | Mínimo 8 caracteres, con al menos una letra y un número | La clave necesita al menos 8 caracteres, con letras y números. |

Las reglas viven en la capa de negocio y en los setters del modelo, no en la consola. La
consola solo traduce el texto a número y repregunta: si el mismo dato llega desde el CSV,
se valida con las mismas reglas.

---

## 4. Reglas de precio

Montos en pesos argentinos, con `BigDecimal` y redondeo a pesos enteros (`HALF_UP`). La
antigüedad se calcula como año actual − año del modelo.

```
precio final = precio de lista
             × (1 − depreciación por antigüedad − depreciación por km)   ← solo usados
             × (1 + recargos)                                            ← según el tipo
             + patentamiento                                             ← solo 0 km
```

| Regla | Automóvil | Camioneta | Motocicleta |
|---|---|---|---|
| Depreciación por año (usado) | 6 % por año, tope 45 % | 5 % por año, tope 40 % | 8 % por año, tope 50 % |
| Depreciación por km (usado) | 1 % cada 10.000 km, tope 15 % | 0,8 % cada 10.000 km, tope 12 % | 1 % cada 5.000 km, tope 20 % |
| Recargo | — | 4x4: +7 % | — |
| Patentamiento (0 km) | 3 % del precio | 3 % del precio | 2 % del precio, mínimo $ 150.000 |

Los porcentajes son constantes con nombre en cada subclase y están documentados en el
JavaDoc. Son valores de ejemplo razonables, no una tabla oficial.

**Ejemplo 1.** Toyota Hilux 2019, usada, 112.900 km, 4x4, lista $ 38.000.000, en 2026:
- Antigüedad: 7 años × 5 % = 35 %.
- Km: 11 tramos completos de 10.000 × 0,8 % = 8,8 %.
- Depreciación total: 43,8 % → $ 38.000.000 × 0,562 = $ 21.356.000.
- Recargo 4x4: × 1,07 = **$ 22.850.920**.

**Ejemplo 2.** Honda CB 500F 0 km, lista $ 12.500.000:
- Patentamiento: 2 % = $ 250.000 (supera el mínimo).
- Precio final: **$ 12.750.000**.

Los dos ejemplos son casos de prueba fijos (PR-PRECIO-01 y PR-PRECIO-02).

---

## 5. La consola

La consigna pide énfasis en la interacción con el usuario, y el profesor advirtió contra la
"lazy console". Estos son los criterios concretos. La maqueta de referencia está en el
historial del proyecto (menú, stock, alta con errores y reporte).

### Estructura de cada pantalla

Todas las pantallas tienen el mismo esqueleto, así el usuario siempre sabe dónde mirar:

```
 CONCESIONARIA MULTIMARCA                          admin · Administrador · mié 18/11/2026
 ─────────────────────────────────────────────────────────────────────────────────────────
 VEHÍCULOS                                                          Inicio › Vehículos
 10 disponibles · 0 km: 4 · usados: 6

   Consultar
     1  Listar stock          tabla con filtros, orden y páginas
     2  Buscar                por código, patente, marca o modelo

   Gestionar
     5  Registrar vehículo    alta paso a paso

     0  Volver a Inicio
 ─────────────────────────────────────────────────────────────────────────────────────────
 Elegí una opción ›
```

1. **Encabezado fijo:** el sistema, el usuario, el rol y la fecha, y una línea debajo.
2. **Título de la pantalla** en mayúsculas a la izquierda y la **ruta** ("Inicio › Vehículos")
   a la derecha.
3. **Línea de contexto** opcional, en tenue, con datos de esa sección (cuántos vehículos hay,
   cuántas marcas activas).
4. **Opciones** con el número en negrita, la etiqueta y una descripción corta en tenue,
   agrupadas en **secciones** cuando hay más de cuatro.
5. **Las teclas de salida** (`0`, `C`) van en la misma columna que las demás opciones,
   separadas por una línea en blanco, y dicen a dónde llevan: "Volver a Vehículos",
   "Cerrar sesión", "Salir del programa".
6. **Pie fijo:** una línea y debajo el prompt o las teclas de esa pantalla.
7. **Cada acción abre su propia pantalla**, con su título y su ruta ("Inicio › Vehículos ›
   Buscar"); el resultado nunca se imprime debajo del menú anterior.
8. La pantalla se limpia entre menús. Si la terminal no lo soporta, se imprime un separador.

### Tipos de mensaje

| Tipo | Señal | Color | Uso |
|---|---|---|---|
| Éxito | `✓` | verde | La operación se hizo |
| Error | `✗` | rojo | Algo no se pudo hacer; el texto dice cómo corregirlo |
| Aviso | `!` | amarillo | Algo para tener en cuenta antes de seguir |
| Información | (ninguna) | tenue | Ayuda o dato secundario |
| Prompt | `›` | negrita | Lo que se le pide al usuario |

Cada mensaje de éxito, y cada error que termina una operación, va separado del contenido por
una línea en blanco. Los errores de un campo que se vuelve a preguntar van pegados al campo.

### Formularios y fichas

- **Formularios por pasos:** el alta de un vehículo (4 pasos), la modificación (3), la venta
  (3) y el alta de usuario (3) dicen al principio cuántos pasos son, y cada paso arranca con
  "Paso 2 de 4 · Datos generales".
- **Ayuda antes del campo:** en tenue y con sangría, se explica el formato esperado antes de
  pedirlo ("Formatos de patente: ABC123 o AB123CD."), no recién después del error.
- **Resumen antes de confirmar:** separado del formulario por una línea, con la ficha y el
  desglose del precio.
- **Ficha en dos columnas:** "Vehículo" (tipo, detalle, marca, año, color) a la izquierda y
  "Registro" (estado, kilometraje, patente, ingreso) a la derecha, con la situación al lado
  del título (verde si está disponible).

### Inicio y listados

- **Resumen en el inicio:** debajo del saludo, tarjetas con lo que el rol puede ver: stock
  disponible (todos), ventas del mes (las propias para el vendedor, todas para el resto) y
  valor del stock a precio final (quien ve reportes).
- **Stock agrupado por tipo:** en el orden natural, la tabla separa automóviles, camionetas y
  motocicletas con un subtítulo que dice cuántas unidades y cuánto valen. Con otro orden la
  tabla va sin grupos, porque el orden elegido manda.

### Detalles
- **Tablas:** columnas calculadas según el contenido, números alineados a la derecha,
  separador de miles argentino (`$ 22.850.920`, `112.900 km`) y una sola línea de borde.
- **Color con significado, nunca de adorno:** cian para los títulos, verde para el éxito y
  los 0 km, rojo para los errores, amarillo para los avisos y tenue para la información
  secundaria. Nada más (ver "Tipos de mensaje").
- **Sin color si no corresponde:** si está definida la variable `NO_COLOR`, si se pasa
  `--sin-color` o si la salida no es una terminal (por ejemplo, se redirige a un archivo).
  El sistema tiene que verse bien sin color: el color nunca es la única forma de transmitir
  algo (los errores llevan además `✗`, los éxitos `✓` y los avisos `!`).
- **Ancho:** usa la variable `COLUMNS` si existe, con un mínimo de 80 columnas. Las columnas
  de texto largas se recortan con `…`.
- **Gráficos de barras** con caracteres (`█` y `░`), siempre con el número al lado.
- **Comprobante de venta:** un bloque con número, fecha, comprador, vehículo, desglose y total.
- **Confirmaciones proporcionales:** "s/n" para las altas; escribir el código para las bajas.
- **Valor actual entre corchetes** en las modificaciones, y Enter lo mantiene.
- **Sin emojis ni recuadros dobles.**
- **Mensajes en español rioplatense, en segunda persona** ("Elegí", "Escribí").

---

## 6. Requisitos no funcionales

| Tema | Requisito |
|---|---|
| Plataforma | Java 25. Corre en macOS, Linux y Windows (Windows Terminal) con `java -jar` |
| Dependencias | Ninguna fuera del JDK |
| Datos | `~/.concesionaria/datos.ser`. Se puede cambiar con `-Dconcesionaria.dir=<ruta>` |
| Integridad | Escritura atómica, respaldo `.bak` del guardado anterior, nunca un archivo a medio escribir |
| Compatibilidad | `serialVersionUID` fijo y una versión de esquema guardada en el archivo |
| Seguridad | Claves con hash SHA-256 y sal aleatoria. Nunca se guarda ni se muestra una clave en texto plano |
| Rendimiento | Objetivos y forma de medirlos en [ARQUITECTURA.md §7](ARQUITECTURA.md#7-rendimiento) |
| Documentación | JavaDoc en todas las clases y métodos públicos, generado con `./build.sh docs` |

---

## 7. Contenido extra (no visto en clase)

La rúbrica pide explicar todo lo que no se haya dado en la materia. Esta es la lista
completa. Cada ítem tiene un comentario en el código que remite acá.

| Qué | Dónde | Por qué se usa |
|---|---|---|
| `BigDecimal` | Precios | `double` no representa bien los decimales (0,1 + 0,2 ≠ 0,3). Con plata no se aproxima |
| `java.time.LocalDate` y `Clock` | Ventas, antigüedad | Fechas sin hora ni zona. `Clock` permite fijar "hoy" en las pruebas |
| `MessageDigest` (SHA-256) + `SecureRandom` + `HexFormat` | Usuarios (`Claves`) | Guardar un hash con sal en lugar de la clave; `HexFormat` pasa los bytes a texto para guardarlos |
| Códigos de escape ANSI | Consola | Color, negrita y limpieza de pantalla sin librerías |
| `Files.move(..., ATOMIC_MOVE)` | Persistencia | Que un corte a mitad del guardado no deje el archivo corrupto |
| `Runtime.addShutdownHook` | Salida | Guardar si el usuario cierra con Ctrl+C |
| `System.console().readPassword()` | Login | Que la clave no se vea en pantalla |
| `Normalizer` | Búsquedas | Comparar sin acentos ("Peugeot" = "peugeot", "Citroën" = "citroen") |
| `TreeMap.subMap` | Catálogo de marcas | Buscar marcas por prefijo en O(log n) |

---

## 8. Plan de pruebas

Runner propio (`PruebasRunner`), sin JUnit: cada prueba es un método que lanza
`AssertionError` si falla. Se corre con `./build.sh test` y muestra un resumen.

| Grupo | Qué cubre |
|---|---|
| PR-COMUN | Utilidades de `comun`: limpieza y normalización de texto, formatos argentinos de moneda, km y fecha, lectura de números con puntos de miles |
| PR-PRECIO | Los dos ejemplos de §4, topes de depreciación, 0 km sin depreciación, mínimo de patentamiento |
| PR-VALID | Cada fila de la tabla de §3: el límite válido, el inválido y el formato |
| PR-PATENTE | Formatos de auto y de moto, unicidad entre vehículos vendidos y dados de baja |
| PR-VENTA | Venta exitosa, vender algo ya vendido, vender algo dado de baja, precio congelado |
| PR-MARCA | Normalización, duplicado con otra capitalización, desactivar marca en uso |
| PR-PERMISO | Cada rol contra cada operación de la matriz de §1 |
| PR-PERSIST | Guardar y cargar, archivo corrupto → se recupera del `.bak`, versión de esquema distinta |
| PR-CSV | Importación válida, importación con una fila mala → no se importa nada |
| PR-COLECCION | `equals`, `hashCode` y `compareTo` de cada entidad |

Más un **plan manual** (lista de pasos en la consola) para lo que no se automatiza: el
aspecto visual, `NO_COLOR`, Ctrl+C y la terminal angosta.

### Plan manual

Se corre en una terminal real con una carpeta de datos descartable:

```
./build.sh
java -Dconcesionaria.dir=/tmp/demo -jar dist/concesionaria.jar
```

| N.º | Pasos | Resultado esperado |
|---|---|---|
| PM-01 | Arrancar con `/tmp/demo` vacía. Crear `admin`, aceptar los datos de demostración. | Pide usuario, nombre y clave dos veces sin mostrarla; avisa con ✓ en verde y entra al menú. |
| PM-02 | En la clave, escribir `corta`. | ✗ en rojo con la regla (8 o más, letras y números) y "Te quedan 2 intentos". |
| PM-03 | Vehículos › Listar stock. | Encabezado con usuario, rol y fecha; tabla alineada, números a la derecha con puntos de miles; "0 km" en verde. |
| PM-04 | En el listado: F › Tipo › Camioneta; O › Precio de mayor a menor; L. | El filtro activo se describe arriba; L vuelve a "Disponibles". |
| PM-05 | Registrar una moto usada con año `dos mil`, luego `1989`, luego `2022`; patente `AB123CD`, luego `123ABC`. | Cada error dice qué se esperaba con un ejemplo; al tercer error del mismo campo se cancela el registro. |
| PM-06 | En cualquier campo del registro, escribir `:q`. | "Se canceló el registro." y vuelve al menú sin guardar nada. |
| PM-07 | Ventas › Vender › `corolla`, comprador válido, confirmar. | Desglose antes de confirmar; comprobante con número, fecha, vendedor y precio final. |
| PM-08 | Intentar vender otra vez el mismo vehículo. | No aparece entre los disponibles; con su código, el error explica que ya está vendido. |
| PM-09 | Reportes › Stock por tipo y estado › E. | Gráfico de barras con █ y ░; se crea el `.txt` en `/tmp/demo` sin códigos de color. |
| PM-10 | Usuarios › Nuevo usuario con rol Vendedor. Cerrar sesión (C) y entrar con él. | El menú solo muestra Vehículos, Ventas y Cambiar mi clave. |
| PM-11 | En el login, clave incorrecta tres veces. | "Te quedan N intentos" y, al tercero, vuelve al inicio sin entrar. |
| PM-12 | `NO_COLOR=1 java -jar dist/concesionaria.jar` y también con `--sin-color`. | Ningún código ANSI; los símbolos ✓ ✗ ! siguen indicando el significado. |
| PM-13 | `COLUMNS=60` antes de arrancar. | El ancho se fija en el mínimo de 80 y las columnas largas se recortan con "…" sin romper la tabla. |
| PM-14 | Registrar un vehículo y, sin salir, cerrar con Ctrl+C. Volver a abrir. | El vehículo sigue ahí (se guardó al confirmar; el shutdown hook cubre lo pendiente). |
| PM-15 | Con el programa cerrado, truncar `/tmp/demo/datos.ser` (`: > /tmp/demo/datos.ser`) y abrir. | Aviso en amarillo: se recuperó la copia anterior; queda `datos.ser.corrupto`. |
| PM-16 | `java -jar dist/concesionaria.jar < /dev/null`. | Sale con "Hasta luego." sin excepciones (fin de entrada). |
