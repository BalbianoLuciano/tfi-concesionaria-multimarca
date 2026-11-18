# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Qué es este repo

Trabajo Final Integrador (TFI) de Programación II (TUP, UTN FRRe, 2026): sistema de **consola**
en **Java SE 25 puro** para una concesionaria multimarca. Gestiona stock de automóviles,
camionetas y motocicletas (0 km y usados), ventas con precio, reportes para directivos y
usuarios con roles.

Antes de escribir código, leé en este orden:

1. [CONTEXTO.md](CONTEXTO.md): decisiones tomadas y su porqué. Es el archivo de traspaso;
   actualizalo cuando se tome una decisión nueva.
2. [docs/SPEC.md](docs/SPEC.md): casos de uso (CU-xx), validaciones, reglas de precio,
   criterios de la consola, contenido extra y plan de pruebas (PR-xx).
3. [docs/MODELO.md](docs/MODELO.md) y [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).
4. [docs/TRAZABILIDAD.md](docs/TRAZABILIDAD.md): requisito del enunciado → clase → prueba.

Metodología **SDD**: si algo no está en la spec, primero se agrega a la spec y después se
codifica. Toda clase nueva queda trazada en `TRAZABILIDAD.md`.

## Comandos

```
./build.sh               # javac --release 25 + jar → dist/concesionaria.jar
./build.sh run           # compila y ejecuta
./build.sh test          # todas las pruebas (runner propio, sin JUnit)
./build.sh test PR-PRECIO  # un solo grupo
./build.sh docs          # javadoc → dist/javadoc/
./build.sh bench         # mediciones de rendimiento
java -Dconcesionaria.dir=/tmp/demo -jar dist/concesionaria.jar --sin-color
```

## Restricciones que no son obvias

- **Nada fuera del JDK**: ni frameworks, ni Maven/Gradle, ni JUnit.
- **Rúbrica: usar lo visto en clase** (referencia: `~/Documents/PersonalRepos/CursoDeJava`).
  Todo lo que no se haya dado va listado y justificado en SPEC §7 ("Contenido extra"), con un
  comentario en el código que remita ahí.
- Sin records, sealed classes ni pattern matching. Lambdas, streams y `Comparator` sí.
- **Código, identificadores, JavaDoc y mensajes en español** (rioplatense, voseo, en los mensajes).
- Plata con `BigDecimal` en pesos; nunca `double`.
- Consola "no lazy": seguir SPEC §5 al pie de la letra (color con significado, alternativa sin
  color, tablas alineadas, errores que dicen cómo corregir, sin emojis).

## Arquitectura en una línea

`presentacion → negocio → interfaces de datos`, con `modelo`, `excepciones` y `comun`
transversales. El negocio no importa `java.io` ni imprime; la consola no toca `datos`. `App`
arma las dependencias a mano (inyección por constructor). Todo se persiste como un único
snapshot `Almacen` en `~/.concesionaria/datos.ser` con escritura atómica y `.bak`. Ver
ARQUITECTURA.md.

## Documentación de entrega

Carátula: plantilla oficial TUP UTN FRRe en `docs/assets/caratula/`. Datos del alumno en
CONTEXTO.md.
