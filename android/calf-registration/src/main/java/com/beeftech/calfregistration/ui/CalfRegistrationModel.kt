package com.beeftech.calfregistration.ui

data class CalfRegistrationData(
    val tagNumber: String = "",
    val oldTagNumber: String = "",
    val referenceNumber: String = "",
    val animalType: String = "BRN — Brangus",
    val gender: String = "Female",
    val age: String = "Newborn",
    val condition: String = CalfRegistrationLookups.DEFAULT_CONDITION,
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

    /** Body condition is a 1-5 score, stored as the digit text "1".."5". */
    const val DEFAULT_CONDITION = "3"

    val conditionScores = listOf("1", "2", "3", "4", "5")

    private val conditionLabels = mapOf(
        "1" to "Poor",
        "2" to "Fair",
        "3" to "Good",
        "4" to "Very good",
        "5" to "Excellent"
    )

    fun conditionLabel(score: String): String = conditionLabels[score].orEmpty()

    /** "3 – Good" for a score, or the text unchanged if it is not a score. */
    fun conditionDisplay(score: String): String {
        val label = conditionLabels[score] ?: return score
        return "$score – $label"
    }

    /**
     * Turns a stored condition into a score. Registrations saved before the
     * 1-5 scale hold text; those are mapped, and anything unrecognised falls
     * back to the default. Only the form value is converted: the stored text is
     * left as it is until the user saves the calf again.
     */
    fun legacyConditionToScore(stored: String?): String {
        val value = stored?.trim().orEmpty()
        if (value in conditionLabels) return value
        return when (value.lowercase()) {
            "poor" -> "1"
            "fair" -> "2"
            "good" -> "3"
            "excellent" -> "5"
            else -> DEFAULT_CONDITION
        }
    }

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
