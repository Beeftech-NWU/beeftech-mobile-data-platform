package com.beeftech.database

import androidx.sqlite.db.SupportSQLiteDatabase

object BreedSeed {
    val BREEDS = listOf(
        "Angus",
        "Bonsmara",
        "Brahman",
        "Charolais",
        "Hereford",
        "Limousin",
        "Simmentaler",
        "Beefmaster",
        "Afrikaner",
        "Nguni",
        "Sussex",
        "Drakensberger",
        "Bovelder",
        "Santa Gertrudis",
        "Mixed / Crossbreed",
        "Unknown"
    )

    fun execute(db: SupportSQLiteDatabase) {
        BREEDS.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO `breeds` (`breedId`, `name`) VALUES (?, ?)",
                arrayOf<Any>(name, name)
            )
        }
    }
}

object HideColourSeed {
    val COLOURS = listOf(
        "Black",
        "Red",
        "Brown",
        "White",
        "Tan",
        "Brindle",
        "Grey",
        "Roan",
        "Black & White",
        "Red & White",
        "Spotted / Speckled"
    )

    fun execute(db: SupportSQLiteDatabase) {
        COLOURS.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO `hide_colours` (`colourId`, `name`) VALUES (?, ?)",
                arrayOf<Any>(name, name)
            )
        }
    }
}

object DiseaseSeed {
    val DISEASES = listOf(
        "Bovine respiratory disease",
        "Foot-and-mouth disease",
        "Johne's disease",
        "Lumpy skin disease",
        "Foot Rot",
        "Anaplasmosis",
        "Mastitis",
        "Pinkeye",
        "Parasites",
        "Pneumonia",
        "Blackleg",
        "Heartwater",
        "Anthrax"
    )

    fun execute(db: SupportSQLiteDatabase) {
        DISEASES.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO `diseases` (`diseaseId`, `name`) VALUES (?, ?)",
                arrayOf<Any>(name, name)
            )
        }
    }
}

object MedicationSeed {
    // (medicationId, name, withdrawalPeriodDays)
    val MEDICATIONS = listOf(
        Triple("ANTIBIOTIC", "Antibiotic", 28),
        Triple("ANTI_INFLAMMATORY", "Anti-inflammatory", 14),
        Triple("ANTIPARASITIC", "Antiparasitic", 21),
        Triple("DIPPING", "Dipping / external parasite treatment", 7),
        Triple("TOPICAL", "Topical / wound treatment", 0),
        Triple("VACCINE", "Vaccine", 0)
    )

    fun execute(db: SupportSQLiteDatabase) {
        MEDICATIONS.forEach { (id, name, withdrawalDays) ->
            db.execSQL(
                "INSERT OR IGNORE INTO `medications` (`medicationId`, `name`, `withdrawal_period_days`) VALUES (?, ?, ?)",
                arrayOf<Any>(id, name, withdrawalDays)
            )
        }
    }
}

object CountryProvinceSeed {
    val COUNTRIES = listOf(
        Triple("ZAF", "ZA", "South Africa")
    )

    val PROVINCES = listOf(
        "Eastern Cape",
        "Free State",
        "Gauteng",
        "KwaZulu-Natal",
        "Limpopo",
        "Mpumalanga",
        "Northern Cape",
        "North West",
        "Western Cape"
    )

    fun execute(db: SupportSQLiteDatabase) {
        COUNTRIES.forEach { (id, iso, name) ->
            db.execSQL(
                "INSERT OR IGNORE INTO `countries` (`countryId`, `iso_code`, `name`) VALUES (?, ?, ?)",
                arrayOf<Any>(id, iso, name)
            )
        }
        PROVINCES.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO `provinces` (`provinceId`, `countryId`, `name`) VALUES (?, 'ZAF', ?)",
                arrayOf<Any>(name, name)
            )
        }
    }
}

object NecropsyCodeSeed {
    val CODES = listOf(
        Triple("N01", "RESPIRATORY", "Respiratory Failure / Pneumonia"),
        Triple("N02", "GASTROINTESTINAL", "Bloat / Gastrointestinal Distress"),
        Triple("N03", "TRAUMA", "Injury / Trauma / Fracture"),
        Triple("N04", "INFECTIOUS", "Infectious Disease / Septicemia"),
        Triple("N05", "TOXICITY", "Poisoning / Toxicity"),
        Triple("N06", "DYSTOCIA", "Calving Complication / Dystocia"),
        Triple("N07", "WEATHER", "Heat Stress / Weather Exposure"),
        Triple("N08", "NATURAL", "Old Age / Natural Causes"),
        Triple("N99", "OTHER", "Undetermined / Other")
    )

    fun execute(db: SupportSQLiteDatabase) {
        CODES.forEach { (id, code, description) ->
            db.execSQL(
                "INSERT OR IGNORE INTO `necropsy_codes` (`necropsyCodeId`, `code`, `description`) VALUES (?, ?, ?)",
                arrayOf<Any>(id, code, description)
            )
        }
    }
}
