package com.beeftech.database

/**
 * Stable row-level cost categories. Adding a category means adding a value,
 * not adding a column or changing the database schema.
 */
object CostTypes {
    const val TRANSPORT = "TRANSPORT"
    const val PROCESSING = "PROCESSING"
    const val TREATMENT = "TREATMENT"
    const val FEED = "FEED"
    const val HANDLING = "HANDLING"
    const val INTEREST = "INTEREST"

    val ALL = setOf(
        TRANSPORT,
        PROCESSING,
        TREATMENT,
        FEED,
        HANDLING,
        INTEREST
    )
}
