package com.beeftech.farmerregistration

/**
 * Input rules and review text for the optional farmer contact and herd fields. Kept free of
 * Compose so they can be unit tested.
 */
object FarmerFieldRules {

    private const val MIN_PHONE_DIGITS = 9
    private const val MAX_PHONE_DIGITS = 15

    private val phoneCharacters = Regex("""^\+?[0-9 ]+$""")

    /** Blank is allowed. Otherwise `+`, digits and spaces with 9 to 15 digits. */
    fun isValidContactNumber(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return true
        if (!phoneCharacters.matches(trimmed)) return false
        return trimmed.count { it.isDigit() } in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    }

    /** Keeps digits and the first decimal point, with at most two decimals. */
    fun cleanDecimal(rawValue: String): String {
        val kept = StringBuilder()
        var seenPoint = false
        var decimals = 0
        for (character in rawValue) {
            when {
                character == '.' && !seenPoint -> {
                    seenPoint = true
                    kept.append(character)
                }
                character.isDigit() && !seenPoint -> kept.append(character)
                character.isDigit() && decimals < 2 -> {
                    decimals++
                    kept.append(character)
                }
            }
        }
        return kept.toString()
    }

    /** Null for blank or unparseable input, so "12." saves as 12.0 and "." saves as null. */
    fun parseFarmSizeHa(value: String): Double? =
        value.trim().trimEnd('.').toDoubleOrNull()

    /** Lines for the save screen summary. Only fields that were filled in are listed. */
    fun reviewLines(
        client: ClientRegistrationData,
        address: AddressAndLocationData
    ): List<String> =
        listOfNotNull(
            client.contactName.trim().ifBlank { null }?.let { "Contact: $it" },
            client.contactNumber.trim().ifBlank { null }?.let { "Phone: $it" },
            parseFarmSizeHa(address.farmSizeHa)?.let { "Farm size: ${address.farmSizeHa.trim().trimEnd('.')} ha" },
            address.headCount.trim().toIntOrNull()?.let { "Head count: $it" },
            address.primaryBreed.trim().ifBlank { null }?.let { "Primary breed: $it" }
        )
}
