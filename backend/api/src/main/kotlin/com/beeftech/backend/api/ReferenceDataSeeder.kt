package com.beeftech.backend.api

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Seeds the reference values on every start. insertIgnore never changes a row that exists, so a
 * value an admin deactivated stays deactivated. The version only moves when a row was actually
 * inserted, so a restart doesn't make every device re-download.
 */
object ReferenceDataSeeder {

    private val diseases =
        listOf(
            "Anthrax",
            "Blackquarter",
            "Botulism",
            "Bovine brucellosis",
            "Bovine respiratory disease",
            "Bovine tuberculosis",
            "Bovine viral diarrhoea",
            "Foot-and-mouth disease",
            "Gallsickness (Anaplasmosis)",
            "Johne's disease",
            "Lumpy skin disease",
            "Mastitis",
            "Rabies",
            "Redwater (Babesiosis)",
            "Rift Valley fever",
            "Trichomoniasis"
        )

    private val treatmentTypes =
        listOf(
            "Antibiotic treatment",
            "Anti-inflammatory treatment",
            "Antiparasitic treatment",
            "Deworming",
            "Dipping / external parasite treatment",
            "Fluid / supportive therapy",
            "Mineral / vitamin supplementation",
            "Topical / wound treatment",
            "Vaccination",
            "Veterinary procedure",
            "Other"
        )

    /* A copy of android CostTypeSeed.TYPES (code to display name, in display order). Keep them in step. */
    private val costTypes =
        listOf(
            "TRANSPORT" to "Transport",
            "PROCESSING" to "Processing",
            "HANDLING" to "Handling",
            "INTEREST" to "Interest",
            "TREATMENT" to "Treatment",
            "FEED" to "Feed / Ration",
            "DIRECT" to "Direct",
            "INDIRECT" to "Indirect",
            "FUEL_MAINTENANCE" to "Fuel & maintenance"
        )

    fun seed(now: Long = System.currentTimeMillis()) {

        transaction(DatabaseFactory.getDatabase()) {

            var inserted = 0

            diseases.forEach { diseaseName ->
                inserted += DiseaseTable.insertIgnore {
                    it[name] = diseaseName
                    it[active] = true
                }.insertedCount
            }

            treatmentTypes.forEach { treatmentTypeName ->
                inserted += TreatmentTypeTable.insertIgnore {
                    it[name] = treatmentTypeName
                    it[active] = true
                }.insertedCount
            }

            costTypes.forEachIndexed { index, (typeCode, typeName) ->
                inserted += CostTypeTable.insertIgnore {
                    it[code] = typeCode
                    it[displayName] = typeName
                    it[sortOrder] = index
                    it[active] = true
                    it[createdAt] = now
                }.insertedCount
            }

            val hasVersion = AppSettingsTable.selectAll()
                .where { AppSettingsTable.key eq AppSettingKeys.REFERENCE_DATA_VERSION }
                .any()

            if (!hasVersion) {
                AppSettingsTable.insert {
                    it[key] = AppSettingKeys.REFERENCE_DATA_VERSION
                    it[value] = "1"
                    it[updatedAt] = now
                }
            } else if (inserted > 0) {
                /* A value added by a newer build of the server: devices should pick it up. */
                bumpReferenceDataVersion(now, null)
            }
        }
    }
}

/* Call inside a transaction, in the same one as the change that needs the bump. Returns the new version. */
fun bumpReferenceDataVersion(now: Long, updatedByUserId: String?): Long {
    val current = AppSettingsTable.selectAll()
        .where { AppSettingsTable.key eq AppSettingKeys.REFERENCE_DATA_VERSION }
        .singleOrNull()
        ?.get(AppSettingsTable.value)
        ?.toLongOrNull() ?: 1L
    val next = current + 1

    val updated = AppSettingsTable.update({ AppSettingsTable.key eq AppSettingKeys.REFERENCE_DATA_VERSION }) {
        it[value] = next.toString()
        it[updatedAt] = now
        it[AppSettingsTable.updatedByUserId] = updatedByUserId
    }
    if (updated == 0) {
        AppSettingsTable.insert {
            it[key] = AppSettingKeys.REFERENCE_DATA_VERSION
            it[value] = next.toString()
            it[updatedAt] = now
            it[AppSettingsTable.updatedByUserId] = updatedByUserId
        }
    }
    return next
}
