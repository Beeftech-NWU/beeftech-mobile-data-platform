package com.beeftech.calfregistration.data

import com.beeftech.database.util.TagNamingUtils

/**
 * Filters the dam/sire choices ("Blu0000011 (Bonsmara)") for a typed query.
 *
 * Understands the same shorthand as the registered-calves search ("B11" finds Blu0000011),
 * bare numbers ("11" finds every tag whose number is 11 or starts with 11) and breed names.
 * Best matches come first; matches of equal quality keep their original order.
 */
object ParentSearch {

    fun filter(options: List<String>, query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return options

        val expanded = TagNamingUtils.parseAndExpand(q)
        val digits = q.takeIf { it.all(Char::isDigit) }?.trimStart('0')

        return options
            .mapNotNull { option -> rank(option, q, expanded, digits)?.let { it to option } }
            .sortedBy { it.first }
            .map { it.second }
    }

    private fun rank(option: String, query: String, expanded: String, digits: String?): Int? {
        val tag = option.substringBefore(" (")
        val breed = option.substringAfter(" (", "").removeSuffix(")")
        val number = tag.drop(3).trimStart('0')

        return when {
            tag.equals(expanded, ignoreCase = true) -> 0
            digits != null && digits.isNotEmpty() && number == digits -> 1
            digits != null && digits.isNotEmpty() && number.startsWith(digits) -> 2
            tag.contains(query, ignoreCase = true) || tag.contains(expanded, ignoreCase = true) -> 3
            breed.contains(query, ignoreCase = true) -> 4
            else -> null
        }
    }
}
