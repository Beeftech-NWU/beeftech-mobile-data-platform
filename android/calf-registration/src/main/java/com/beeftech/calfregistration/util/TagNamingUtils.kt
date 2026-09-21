package com.beeftech.calfregistration.util

/**
 * Standard tag colours and associated prefix metadata for animal ear tags.
 * Format requirement: Colour Prefix (3 chars) + Zero-Padded 7-Digit Number.
 */
enum class TagColour(
    val displayName: String,
    val prefix: String,
    val shortCode: Char
) {
    BLUE("Blue", "Blu", 'B'),
    RED("Red", "Red", 'R'),
    GREEN("Green", "Grn", 'G'),
    YELLOW("Yellow", "Yel", 'Y');

    companion object {
        fun fromPrefix(prefix: String?): TagColour? {
            if (prefix.isNullOrBlank()) return null
            val normalized = prefix.trim()
            return entries.firstOrNull {
                it.prefix.equals(normalized, ignoreCase = true) ||
                it.displayName.equals(normalized, ignoreCase = true) ||
                (normalized.length == 1 && it.shortCode.equals(normalized[0], ignoreCase = true))
            }
        }
    }
}

/**
 * Utility functions for validating, formatting, and auto-expanding ear tag numbers
 * per the Beeftech Tag Naming Standard (`Colour + Zero-Padded 7-Digit Number`).
 */
object TagNamingUtils {
    private val STRICT_TAG_REGEX = Regex("^(Blu|Red|Grn|Yel)\\d{7}$")
    private val SHORTHAND_REGEX = Regex("^(B|R|G|Y|Blu|Red|Grn|Yel)\\s*(\\d{1,7})$", RegexOption.IGNORE_CASE)

    /**
     * Formats a colour and numeric sequence into a standardized tag string (e.g., `Blu0000064`).
     */
    fun formatTag(colour: TagColour, sequenceNumber: Long): String {
        require(sequenceNumber in 0..9_999_999L) { "Sequence number must be between 0 and 9,999,999" }
        return "${colour.prefix}${sequenceNumber.toString().padStart(7, '0')}"
    }

    /**
     * Validates whether a tag matches the strict `^(Blu|Red|Grn|Yel)\d{7}$` standard.
     */
    fun validateTag(tag: String?): Boolean {
        if (tag.isNullOrBlank()) return false
        return STRICT_TAG_REGEX.matches(tag.trim())
    }

    /**
     * Auto-expands quick search or shorthand user inputs into standard full tag numbers.
     * Examples:
     * - "B64" -> "Blu0000064"
     * - "R123" -> "Red0000123"
     * - "G45" -> "Grn0000045"
     * - "Y78" -> "Yel0000078"
     * - "Blu64" -> "Blu0000064"
     * - "Blu0000064" -> "Blu0000064"
     */
    fun parseAndExpand(query: String?): String {
        if (query.isNullOrBlank()) return ""
        val trimmed = query.trim()

        // 1. If already valid full tag, return normalized prefix casing
        if (validateTag(trimmed)) {
            val colour = TagColour.fromPrefix(trimmed.substring(0, 3))
            val number = trimmed.substring(3)
            return "${colour?.prefix ?: trimmed.substring(0, 3)}$number"
        }

        // 2. Try parsing shorthand matching pattern (e.g., B64, R123, G45, Y78)
        val match = SHORTHAND_REGEX.matchEntire(trimmed)
        if (match != null) {
            val rawPrefix = match.groupValues[1]
            val rawNumber = match.groupValues[2]
            val colour = TagColour.fromPrefix(rawPrefix)
            if (colour != null && rawNumber.isNotEmpty()) {
                val paddedNumber = rawNumber.padStart(7, '0')
                return "${colour.prefix}$paddedNumber"
            }
        }

        return trimmed
    }

    /**
     * Extracts tag colour and sequence number string from a full or shorthand tag input.
     * Returns `null` if the input cannot be resolved to a valid tag format.
     */
    fun extractComponents(tag: String?): Pair<TagColour, String>? {
        val expanded = parseAndExpand(tag)
        if (!validateTag(expanded)) return null
        val colour = TagColour.fromPrefix(expanded.substring(0, 3)) ?: return null
        val sequence = expanded.substring(3)
        return Pair(colour, sequence)
    }
}
