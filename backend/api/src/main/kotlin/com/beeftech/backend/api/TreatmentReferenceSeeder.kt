package com.beeftech.backend.api

import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

object TreatmentReferenceSeeder {

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

    fun seed() {

        transaction {

            diseases.forEach { diseaseName ->

                DiseaseTable.insertIgnore {

                    it[name] =
                        diseaseName

                    it[active] =
                        true
                }
            }

            treatmentTypes.forEach { treatmentTypeName ->

                TreatmentTypeTable.insertIgnore {

                    it[name] =
                        treatmentTypeName

                    it[active] =
                        true
                }
            }
        }
    }
}
