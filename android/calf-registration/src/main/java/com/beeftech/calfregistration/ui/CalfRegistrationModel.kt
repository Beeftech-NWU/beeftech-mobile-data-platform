package com.beeftech.calfregistration.ui

data class CalfRegistrationData(
    val tagNumber: String = "Blu0000064",
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
    val photoPath: String? = null,
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
        "Blu0000011 (Bonsmara)",
        "Red0000024 (Brangus)",
        "Grn0000039 (Brahman)",
        "Yel0000052 (Nguni)",
        "Blu0000088 (Angus)"
    )

    val sireTagList = listOf(
        "Blu0000902 (Bonsmara Stud)",
        "Red0000550 (Brangus Stud)",
        "Grn0000110 (Brahman Stud)",
        "Yel0000301 (Nguni Stud)"
    )

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
