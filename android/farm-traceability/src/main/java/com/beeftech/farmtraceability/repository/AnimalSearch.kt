package com.beeftech.farmtraceability.repository

import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.util.TagColour
import com.beeftech.database.util.TagNamingUtils

/**
 * Filters the registered animals for Find Animal as the user types.
 *
 * Understands full tags ("Blu0000064"), shorthand ("B64", "red 64"), bare numbers
 * ("64" finds every colour's 64, then numbers starting with 64), partial tags, and
 * free text across breed, sex, hide colour, brand mark, dam/sire tags, old tag and
 * reference number. Several words must all match ("red bonsmara").
 * Best matches come first; matches of equal quality keep their original order.
 */
object AnimalSearch {

    fun filter(
        animals: List<CalfRegistrationView>,
        query: String,
        colour: TagColour? = null
    ): List<CalfRegistrationView> {
        val inColour = if (colour == null) animals else animals.filter { colourOf(it.tagNumber) == colour }
        val q = query.trim()
        if (q.isEmpty()) return inColour

        val expanded = TagNamingUtils.parseAndExpand(q, colour)
        val tokens = q.split(Regex("\\s+"))

        return inColour
            .mapNotNull { animal -> rank(animal, q, expanded, tokens)?.let { it to animal } }
            .sortedBy { it.first }
            .map { it.second }
    }

    /** The one animal a submitted query clearly means, or null when the user still has to pick. */
    fun bestMatch(results: List<CalfRegistrationView>, query: String, colour: TagColour? = null): CalfRegistrationView? {
        val expanded = TagNamingUtils.parseAndExpand(query.trim(), colour)
        return results.firstOrNull { it.tagNumber.equals(expanded, ignoreCase = true) }
            ?: results.singleOrNull()
    }

    fun colourOf(tagNumber: String): TagColour? =
        TagColour.fromPrefix(tagNumber.take(3).takeIf { it.length == 3 })

    private fun rank(animal: CalfRegistrationView, query: String, expanded: String, tokens: List<String>): Int? {
        val tag = animal.tagNumber
        if (tag.equals(expanded, ignoreCase = true)) return 0
        if (tokens.size == 1) return tokenRank(animal, query, TagNamingUtils.parseAndExpand(query))

        // Every word has to match something; stronger matches across all words rank higher.
        return tokens
            .sumOf { tokenRank(animal, it, TagNamingUtils.parseAndExpand(it)) ?: return null }
    }

    private fun tokenRank(animal: CalfRegistrationView, token: String, expanded: String): Int? {
        val tag = animal.tagNumber
        val number = tag.drop(3).trimStart('0')
        val digits = token.takeIf { it.all(Char::isDigit) }?.trimStart('0')?.takeIf { it.isNotEmpty() }

        return when {
            tag.equals(expanded, ignoreCase = true) -> 0
            digits != null && number == digits -> 1
            digits != null && number.startsWith(digits) -> 2
            tag.contains(token, ignoreCase = true) || tag.contains(expanded, ignoreCase = true) -> 3
            token.length >= 3 && TagColour.fromPrefix(token) == colourOf(tag) -> 4
            otherFields(animal).any { it.contains(token, ignoreCase = true) } -> 5
            animal.gender?.startsWith(token, ignoreCase = true) == true -> 5
            else -> null
        }
    }

    private fun otherFields(animal: CalfRegistrationView): List<String> = listOfNotNull(
        animal.breed,
        animal.hideColour,
        animal.brandMark,
        animal.damTagNumber,
        animal.sireTagNumber,
        animal.oldTagNumber,
        animal.referenceNumber
    )
}
