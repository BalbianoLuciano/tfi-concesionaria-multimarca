#!/usr/bin/env bash
# Compila, prueba y empaqueta la concesionaria sin Maven ni Gradle: solo javac, jar y javadoc.
# -Xlint:-this-escape: los constructores de los JavaBeans llaman a sus setters a propósito,
# para validar con una sola regla (MODELO §2).
#
#   ./build.sh                 compila y arma dist/concesionaria.jar
#   ./build.sh run [args]      compila y ejecuta (los args van al programa, por ej. --sin-color)
#   ./build.sh test [GRUPO]    compila y corre las pruebas (todas, o un grupo: PR-PRECIO)
#   ./build.sh docs            genera el JavaDoc en dist/javadoc/
#   ./build.sh bench           corre las mediciones de rendimiento
#   ./build.sh clean           borra out/ y dist/
set -euo pipefail

cd "$(dirname "$0")"

RELEASE=25
PAQUETE=ar.edu.utn.frre.concesionaria
MAIN="$PAQUETE.App"
OUT=out
DIST=dist

compilar_fuentes() {
  rm -rf "$OUT/classes"
  mkdir -p "$OUT/classes"
  find src -name '*.java' > "$OUT/fuentes.txt"
  javac --release "$RELEASE" -encoding UTF-8 -Xlint:all -Xlint:-serial -Xlint:-this-escape \
    -d "$OUT/classes" @"$OUT/fuentes.txt"
  # Recursos (el catálogo de referencia): todo lo que no es .java viaja junto a las clases.
  (cd src && find . -type f ! -name '*.java' | tar -cf - -T -) | tar -xf - -C "$OUT/classes"
}

compilar_pruebas() {
  compilar_fuentes
  rm -rf "$OUT/test-classes"
  mkdir -p "$OUT/test-classes"
  find test -name '*.java' > "$OUT/pruebas.txt"
  javac --release "$RELEASE" -encoding UTF-8 -Xlint:all -Xlint:-serial -Xlint:-this-escape \
    -cp "$OUT/classes" -d "$OUT/test-classes" @"$OUT/pruebas.txt"
}

empaquetar() {
  compilar_fuentes
  mkdir -p "$DIST"
  jar --create --file "$DIST/concesionaria.jar" --main-class "$MAIN" -C "$OUT/classes" .
  echo "✓ $DIST/concesionaria.jar"
}

case "${1:-jar}" in
  jar)
    empaquetar
    ;;
  run)
    shift
    empaquetar
    java -jar "$DIST/concesionaria.jar" "$@"
    ;;
  test)
    shift
    compilar_pruebas
    java -cp "$OUT/classes:$OUT/test-classes" "$PAQUETE.pruebas.PruebasRunner" "$@"
    ;;
  docs)
    rm -rf "$DIST/javadoc"
    mkdir -p "$OUT" "$DIST/javadoc"
    find src -name '*.java' > "$OUT/fuentes.txt"
    javadoc --release "$RELEASE" -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 \
      -quiet -Xdoclint:all,-missing -private -author \
      -windowtitle "Concesionaria multimarca" -doctitle "Concesionaria multimarca · TFI Programación II" \
      -d "$DIST/javadoc" @"$OUT/fuentes.txt"
    echo "✓ $DIST/javadoc/index.html"
    ;;
  bench)
    compilar_pruebas
    java -cp "$OUT/classes:$OUT/test-classes" "$PAQUETE.rendimiento.MedicionRendimiento"
    ;;
  clean)
    rm -rf "$OUT" "$DIST"
    ;;
  *)
    echo "Uso: ./build.sh [jar|run|test [GRUPO]|docs|bench|clean]" >&2
    exit 2
    ;;
esac
