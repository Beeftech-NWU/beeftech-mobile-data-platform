package com.beeftech.calfregistration.ui

data class CalfRegistrationData(
    val tagNumber: String = "",
    val oldTagNumber: String = "",
    val referenceNumber: String = "",
    val animalType: String = "BRN — Brangus",
    val gender: String = "Female",
    val age: String = "Newborn",
    val condition: String = "Good",
    val hideColour: String = "RED",
    val conformity: String = "F — Fair",
    val mark: String = "",
    val birthDate: String = "",
    val birthWeightKg: String = "",
    val dameTagNumber: String = CalfRegistrationLookups.DAME_PLACEHOLDER,
    val sireTagNumber: String = CalfRegistrationLookups.SIRE_PLACEHOLDER,
    val processProof: String = "",
    val implantProof: String = "",
    val photoPath: String? = null,
    val synced: Boolean = false,
    /** The server rejected this record and automatic retries stopped. */
    val needsAttention: Boolean = false,
    val syncError: String? = null,
    val dateRegistered: String = "26 Aug"
)

object CalfRegistrationLookups {
    val animalTypes = listOf(
        "BRN — Brangus",
        "BNM — Bonsmara",
        "BRH — Brahman",
        "NGN — Nguni",
        "ANG — Angus",
        "SMT — Simmentaler",
        "AFR — Afrikaner"
    )

    val genders = listOf(
        "Female",
        "Male",
        "Steer"
    )

    val ages = listOf(
        "Newborn",
        "< 1 Week",
        "1-2 Weeks",
        "> 2 Weeks"
    )

    val conditions = listOf(
        "Good",
        "Fair",
        "Poor",
        "Excellent"
    )

    val hideColours = listOf(
        "RED",
        "BLACK",
        "DUN",
        "WHITE",
        "BRINDLE",
        "ROAN"
    )

    val conformities = listOf(
        "E — Excellent",
        "G — Good",
        "F — Fair",
        "P — Poor"
    )

    const val DAME_PLACEHOLDER = "Select dame"
    const val SIRE_PLACEHOLDER = "Select sire"

    val initialRegisteredCalves = listOf(
        CalfRegistrationData(
            tagNumber = "Blu0000064",
            animalType = "Brangus",
            gender = "Female",
            dateRegistered = "26 Aug",
            synced = false
        ),
        CalfRegistrationData(
            tagNumber = "Blu0000065",
            animalType = "Brangus",
            gender = "Male",
            dateRegistered = "25 Aug",
            synced = false
        ),
        CalfRegistrationData(
            tagNumber = "Blu0000066",
            animalType = "Bonsmara",
            gender = "Female",
            dateRegistered = "24 Aug",
            synced = true
        )
    )
}
