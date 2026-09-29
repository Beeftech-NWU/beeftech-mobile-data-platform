package com.beeftech.tagscanner.parse

import com.beeftech.tagscanner.model.GuideRect
import com.beeftech.tagscanner.model.TextLineBox

data class TagNumberCandidate(val sequence: String, val box: TextLineBox)

object TagTextParser {
    /** Candidates shorter than this fraction of the guide height are treated as noise. */
    const val MIN_BOX_HEIGHT_FRACTION = 0.12f

    /** Share of characters that must already be digits before look-alikes are substituted. */
    const val MIN_DIGIT_RATIO = 0.5f

    private const val MAX_DIGITS = 7
    private val DIGITS = Regex("\\d{1,$MAX_DIGITS}")

    private val LOOK_ALIKES = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', '|' to '1', 'i' to '1',
        'S' to '5', 's' to '5',
        'B' to '8',
        'Z' to '2', 'z' to '2',
        'G' to '6'
    )

    fun parse(lines: List<TextLineBox>, guide: GuideRect): TagNumberCandidate? {
        val minHeight = guide.height * MIN_BOX_HEIGHT_FRACTION
        return lines
            .filter { guide.contains(it.centerX, it.centerY) && it.height >= minHeight }
            .flatMap { line ->
                line.text.split(Regex("\\s+"))
                    .mapNotNull { normalise(it) }
                    .map { TagNumberCandidate(it, line) }
            }
            .sortedWith(
                compareByDescending<TagNumberCandidate> { it.box.height }
                    .thenBy { distanceToCentre(it.box, guide) }
            )
            .firstOrNull()
    }

    private fun normalise(rawToken: String): String? {
        val token = rawToken.filterNot { it == '.' || it == '-' || it == ',' }
        if (token.isEmpty()) return null
        val digitCount = token.count { it.isDigit() }
        if (digitCount.toFloat() / token.length < MIN_DIGIT_RATIO) return null
        val mapped = token.map { if (it.isDigit()) it else LOOK_ALIKES[it] ?: return null }
            .joinToString("")
        return mapped.takeIf { DIGITS.matches(it) }
    }

    private fun distanceToCentre(box: TextLineBox, guide: GuideRect): Long {
        val dx = (box.centerX - guide.centerX).toLong()
        val dy = (box.centerY - guide.centerY).toLong()
        return dx * dx + dy * dy
    }
}
