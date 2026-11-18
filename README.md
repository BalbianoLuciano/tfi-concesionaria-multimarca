# tfi-concesionaria-multimarca

Trabajo Final Integrador de Programación II (TUP, UTN FRRe, 2026): sistema de consola en
**Java SE 25 puro** para una concesionaria multimarca. Gestiona el stock de automóviles,
camionetas y motocicletas (0 km y usados), las ventas con su precio final, los reportes
para directivos y los usuarios con roles.

## Requisitos

JDK 25 o posterior. No usa nada fuera del JDK: ni Maven, ni Gradle, ni JUnit.

## Uso

```
./build.sh               # compila y arma dist/concesionaria.jar
./build.sh run           # compila y ejecuta
./build.sh test          # pruebas automáticas (runner propio)
./build.sh test PR-PRECIO  # un solo grupo
./build.sh docs          # JavaDoc en dist/javadoc/
./build.sh bench         # mediciones de rendimiento

java -jar dist/concesionaria.jar                 # datos en ~/.concesionaria/
java -Dconcesionaria.dir=/tmp/demo -jar dist/concesionaria.jar --sin-color
```

En el primer arranque se crea el usuario administrador y se ofrecen datos de demostración.

## Documentación

- [CONTEXTO.md](CONTEXTO.md): decisiones y estado del proyecto.
- [docs/SPEC.md](docs/SPEC.md): casos de uso, validaciones, reglas de precio y plan de pruebas.
- [docs/MODELO.md](docs/MODELO.md): modelo de clases.
- [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md): capas, persistencia, seguridad y rendimiento.
- [docs/TRAZABILIDAD.md](docs/TRAZABILIDAD.md): requisito → clase → prueba.
