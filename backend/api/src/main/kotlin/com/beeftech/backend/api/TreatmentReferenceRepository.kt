package com.beeftech.backend.api

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class TreatmentReferenceData(
    val diseases: List<String>,
    val treatmentTypes: List<String>
)

class TreatmentReferenceRepository {

    private fun ResultRow.toDiseaseName(): String =
        this[DiseaseTable.name]

    private fun ResultRow.toTreatmentTypeName(): String =
        this[TreatmentTypeTable.name]

    fun getActiveDiseases(): List<String> =
        transaction {

            DiseaseTable
                .selectAll()
                .where {
                    DiseaseTable.active eq true
                }
                .map {
                    it.toDiseaseName()
                }
                .sortedBy {
                    it.lowercase()
                }
        }

    fun getActiveTreatmentTypes(): List<String> =
        transaction {

            TreatmentTypeTable
                .selectAll()
                .where {
                    TreatmentTypeTable.active eq true
                }
                .map {
                    it.toTreatmentTypeName()
                }
                .sortedBy {
                    it.lowercase()
                }
        }

    fun getReferenceData(): TreatmentReferenceData =
        TreatmentReferenceData(
            diseases =
                getActiveDiseases(),

            treatmentTypes =
                getActiveTreatmentTypes()
        )
}