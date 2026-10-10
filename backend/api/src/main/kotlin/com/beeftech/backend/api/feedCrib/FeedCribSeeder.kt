package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.DatabaseFactory
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * insertIgnore never changes a row that exists, so a code or crib an admin deactivated stays
 * deactivated across restarts.
 */
object FeedCribSeeder {

    /*
     * The 0-5 bunk scores. The labels are placeholders until Beeftech confirms its own wording;
     * the numbers are what is stored and synced.
     */
    private val codes =
        listOf(
            Triple(0, "Empty", "Bunk is clean, no feed left"),
            Triple(1, "Trace", "Scattered crumbs"),
            Triple(2, "Light", "A thin layer of feed"),
            Triple(3, "Moderate", "A fair amount of feed left"),
            Triple(4, "Heavy", "Most of the ration is still there"),
            Triple(5, "Full", "Little or none of the ration was eaten")
        )

    /** Every environment needs the code table. */
    fun seedCodes() {
        transaction(DatabaseFactory.getDatabase()) {
            codes.forEach { (codeValue, codeLabel, codeDescription) ->
                CribReadingCodesTable.insertIgnore {
                    it[code] = codeValue
                    it[label] = codeLabel
                    it[description] = codeDescription
                    it[active] = true
                }
            }
        }
    }

    /** Dev cribs A01-A08, only when the dev users are seeded. A real parameter-file import replaces them. */
    fun seedDevCribs(siteId: String, now: Long = System.currentTimeMillis()) {
        transaction(DatabaseFactory.getDatabase()) {
            (1..8).forEach { number ->
                FeedCribsTable.insertIgnore {
                    it[cribNumber] = "A%02d".format(number)
                    it[FeedCribsTable.siteId] = siteId
                    it[penDescription] = "Pen $number"
                    it[ration] = if (number <= 4) "Finisher" else "Grower"
                    it[method] = "Total mixed ration"
                    it[description] = "Dev crib $number"
                    it[requiredKg] = 10.0 + number
                    it[animalsBegin] = 100 + number
                    it[animalsIn] = 0
                    it[animalsOut] = 0
                    it[animalsClose] = 100 + number
                    it[active] = true
                    it[updatedAt] = now
                }
            }
        }
    }
}
