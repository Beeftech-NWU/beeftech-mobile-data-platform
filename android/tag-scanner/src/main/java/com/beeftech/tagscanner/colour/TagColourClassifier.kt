package com.beeftech.tagscanner.colour

import com.beeftech.database.util.TagColour

data class ColourClassification(val colour: TagColour?, val confidence: Float, val keptPixels: Int)

object TagColourClassifier {
    object Thresholds {
        const val MIN_SAT = 0.35f
        const val MIN_VAL = 0.25f
        const val MIN_KEPT = 200
        const val MIN_CONFIDENCE = 0.5f
    }

    fun classify(pixels: IntArray): ColourClassification {
        val counts = HashMap<TagColour, Int>()
        var kept = 0
        for (argb in pixels) {
            val hsv = rgbToHsv(argb)
            if (hsv[1] < Thresholds.MIN_SAT || hsv[2] < Thresholds.MIN_VAL) continue
            kept++
            bucket(hsv[0])?.let { counts[it] = (counts[it] ?: 0) + 1 }
        }
        if (kept == 0) return ColourClassification(null, 0f, 0)

        val sorted = counts.entries.sortedByDescending { it.value }
        val best = sorted.firstOrNull()
        val confidence = if (best == null) 0f else best.value.toFloat() / kept
        val tied = sorted.size > 1 && sorted[1].value == best?.value
        val colour = best?.key?.takeIf {
            kept >= Thresholds.MIN_KEPT && confidence >= Thresholds.MIN_CONFIDENCE && !tied
        }
        return ColourClassification(colour, confidence, kept)
    }

    private fun bucket(hue: Float): TagColour? = when {
        hue < 15f || hue >= 340f -> TagColour.RED
        hue in 40f..70f -> TagColour.YELLOW
        hue in 80f..165f -> TagColour.GREEN
        hue in 190f..255f -> TagColour.BLUE
        else -> null
    }

    /** Returns [hue 0..360, saturation 0..1, value 0..1]. */
    private fun rgbToHsv(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val sat = if (max == 0f) 0f else delta / max
        return floatArrayOf(hue, sat, max)
    }
}
