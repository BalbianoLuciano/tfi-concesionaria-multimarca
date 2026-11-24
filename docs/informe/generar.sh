#!/usr/bin/env bash
# Arma el PDF de entrega a partir de informe.html.
#
# El DNI y el legajo no se guardan en el repositorio (es público): se pasan por variables de
# entorno y solo quedan en el PDF, que se genera en dist/entrega/ (ignorada por git).
#
#   ALUMNO_DNI=... ALUMNO_LEGAJO=... docs/informe/generar.sh
#
# Usa Google Chrome sin interfaz para imprimir el HTML a PDF. No forma parte del sistema: es
# una herramienta para la entrega.
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
: "${ALUMNO_DNI:?Falta ALUMNO_DNI}"
: "${ALUMNO_LEGAJO:?Falta ALUMNO_LEGAJO}"

chrome="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
if [[ ! -x "$chrome" ]]; then
  echo "No encuentro Chrome en \"$chrome\". Indicá la ruta con CHROME=..." >&2
  exit 1
fi

# El HTML completo va junto al original para que las imágenes relativas sigan andando.
temporal="$raiz/docs/informe/.informe-completo.html"
trap 'rm -f "$temporal"' EXIT
sed -e "s/{{DNI}}/$ALUMNO_DNI/g" -e "s/{{LEGAJO}}/$ALUMNO_LEGAJO/g" \
  "$raiz/docs/informe/informe.html" > "$temporal"

mkdir -p "$raiz/dist/entrega"
salida="$raiz/dist/entrega/TFI-Programacion-II-Balbiano.pdf"
"$chrome" --headless --disable-gpu --no-pdf-header-footer \
  --print-to-pdf="$salida" "file://$temporal" 2>/dev/null
echo "✓ $salida"
