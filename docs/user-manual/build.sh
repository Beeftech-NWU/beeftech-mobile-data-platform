#!/usr/bin/env bash
# Builds BeefTech-User-Manual.pdf from cover.html + manual.html with headless Chromium.
# Requires: chromium (or google-chrome) and pdfunite (poppler-utils).
set -euo pipefail

cd "$(dirname "$0")"

CHROME="${CHROME:-$(command -v chromium || command -v chromium-browser || command -v google-chrome)}"
OUT="BeefTech-User-Manual.pdf"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

render() {
  "$CHROME" --headless --disable-gpu --no-sandbox \
    --allow-file-access-from-files \
    --no-pdf-header-footer \
    --print-to-pdf="$2" "file://$PWD/$1" 2>/dev/null
}

render cover.html "$TMP/cover.pdf"
render manual.html "$TMP/body.pdf"
pdfunite "$TMP/cover.pdf" "$TMP/body.pdf" "$OUT"

echo "Wrote $OUT ($(du -h "$OUT" | cut -f1), $(pdfinfo "$OUT" | awk '/^Pages:/ {print $2}') pages)"
