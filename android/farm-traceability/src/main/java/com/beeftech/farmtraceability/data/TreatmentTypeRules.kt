package com.beeftech.farmtraceability.data

/**
 * Treatment type is a pick-from-list field. The list normally comes from the
 * backend (cached on the device). A device that has never synced has no list,
 * so it falls back to [SEED_TYPES] and saving is never blocked offline.
 */
object TreatmentTypeRules {

    /** A copy of the backend ReferenceDataSeeder treatment types. Keep them in step. */
    val SEED_TYPES = listOf(
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

    /** The synced options, or the seed list when there are none yet. */
    fun effectiveOptions(options: List<String>): List<String> =
        options.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { SEED_TYPES }

    /** The option that [input] matches, ignoring case, or null if it is not on the list. */
    fun match(input: String, options: List<String>): String? {
        val wanted = input.trim()
        if (wanted.isEmpty()) return null
        return effectiveOptions(options).firstOrNull { it.equals(wanted, ignoreCase = true) }
    }
}
