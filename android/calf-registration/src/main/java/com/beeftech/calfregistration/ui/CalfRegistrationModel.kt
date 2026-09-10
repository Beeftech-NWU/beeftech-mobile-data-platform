package com.beeftech.calfregistration.ui

data class CalfRegistrationData(
    val tagNumber: String = "RMB25423",
    val oldTagNumber: String = "",
    val transponderNumber: String = "40",
    val referenceNumber: String = "",
    val animalType: String = "BRN — Brangus",
    val gender: String = "Female",
    val age: String = "Newborn",
    val condition: String = "Good",
    val hideColour: String = "RED",
    val conformity: String = "F — Fair",
    val mark: String = "",
    val dameTagNumber: String = "Select dame",
    val sireTagNumber: String = "Select sire",
    val processProof: String = "",
    val implantProof: String = "",
    val group: String = "",
    val synced: Boolean = false,
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

    val dameTagList = listOf(
        "RMB-DAM-011 (Bonsmara)",
        "RMB-DAM-024 (Brangus)",
        "RMB-DAM-039 (Brahman)",
        "RMB-DAM-052 (Nguni)",
        "RMB-DAM-088 (Angus)"
    )

    val sireTagList = listOf(
        "BULL-BNM-902 (Bonsmara Stud)",
        "BULL-BRG-550 (Brangus Stud)",
        "BULL-BRH-110 (Brahman Stud)",
        "BULL-NGN-301 (Nguni Stud)"
    )

    val initialRegisteredCalves = listOf(
        CalfRegistrationData(
            tagNumber = "RMB25423",
            animalType = "Brangus",
            gender = "Female",
            dateRegistered = "26 Aug",
            synced = false
        ),
        CalfRegistrationData(
            tagNumber = "RMB25424",
            animalType = "Brangus",
            gender = "Male",
            dateRegistered = "25 Aug",
            synced = false
        ),
        CalfRegistrationData(
            tagNumber = "RMB25425",
            animalType = "Bonsmara",
            gender = "Female",
            dateRegistered = "24 Aug",
            synced = true
        )
    )
}
