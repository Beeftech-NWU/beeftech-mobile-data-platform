#!/usr/bin/env bash
# Generates synthetic ear-tag photos for SampleEarTagTest from the real yellow sample.
#   - Recolour: hue-rotates the whole photo. The etched digits and the background are low-saturation,
#     so only the tag body changes colour.
#   - Renumber: covers the digits with a stretched strip of plain tag and draws a new number in the
#     etched-grey colour.
# Requires ImageMagick 7 (`magick`) and the Liberation Sans Narrow font.
set -euo pipefail

ASSETS="$(cd "$(dirname "$0")/../src/androidTest/assets" && pwd)"
SRC="$ASSETS/eartag_yellow_000993.jpg"
FONT="${FONT:-Liberation-Sans-Narrow}"

# Digit area in the 224x249 source (with a small margin), a plain strip of tag just above it, and
# the exact box the original digits occupy (new digits are squashed into it to match the tall font).
DIGITS_GEOM="124x56+46+116"
PLAIN_GEOM="124x12+46+102"
TEXT_GEOM="116x49+49+120"
# Etched digits are a dark outline around a lighter grey fill.
INK_FILL="#a4a48a"
INK_EDGE="#5a5a48"

# ImageMagick -modulate hue: 100 = unchanged, 1 unit = 1.8 degrees. The source tag sits at ~58 degrees.
hue_for() {
  case "$1" in
    yellow) echo 100 ;;
    red)    echo 68 ;;   # ~0 deg
    green)  echo 137 ;;  # ~125 deg
    blue)   echo 190 ;;  # ~220 deg
  esac
}

generate() {
  local colour="$1" number="$2"
  local out="$ASSETS/eartag_${colour}_${number}.jpg"
  local tmp
  tmp="$(mktemp --suffix=.png)"

  if [[ "$number" == "000993" ]]; then
    cp "$SRC" "$tmp"
  else
    magick "$SRC" \
      \( "$SRC" -crop "$PLAIN_GEOM" +repage -resize "${DIGITS_GEOM%%+*}!" -blur 0x1 \) \
      -geometry "+${DIGITS_GEOM#*+}" -composite \
      \( -background none -font "$FONT" -pointsize 120 -kerning 8 \
         -fill "$INK_FILL" -stroke "$INK_EDGE" -strokewidth 5 "label:$number" -trim +repage \
         -resize "${TEXT_GEOM%%+*}!" -blur 0x0.6 \) \
      -geometry "+${TEXT_GEOM#*+}" -composite "$tmp"
  fi

  magick "$tmp" -modulate "100,100,$(hue_for "$colour")" -quality 92 "$out"
  rm -f "$tmp"
  echo "wrote $out"
}

# Same number in every colour: isolates the colour classifier.
generate red 000993
generate green 000993
generate blue 000993
# New numbers, including OCR look-alikes (1/I, 8/B, 5/S, 2/Z, 6/G, 0/O).
generate yellow 004521
generate red 012876
generate green 000148
generate blue 035062
