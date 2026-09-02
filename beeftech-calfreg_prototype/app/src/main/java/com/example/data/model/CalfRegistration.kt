package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calf_registration",
    indices = [
        Index(value = ["animal_id"], unique = true),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class CalfRegistration(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "birth_date")
    val birthDate: String,

    @ColumnInfo(name = "breed")
    val breed: String,

    @ColumnInfo(name = "sex")
    val sex: String = SEX_HEIFER,

    @ColumnInfo(name = "birth_weight_kg")
    val birthWeightKg: Double? = null,

    @ColumnInfo(name = "calving_ease")
    val calvingEase: Int = CALVING_EASE_UNASSISTED,

    @ColumnInfo(name = "vigor")
    val vigor: String = VIGOR_VIGOROUS,

    @ColumnInfo(name = "horn_status")
    val hornStatus: String = HORN_POLLED,

    @ColumnInfo(name = "pasture_location")
    val pastureLocation: String = "Camp 4 - North Pasture",

    @ColumnInfo(name = "rfid_tag")
    val rfidTag: String? = null,

    @ColumnInfo(name = "dam_id")
    val damId: String? = null,

    @ColumnInfo(name = "sire_id")
    val sireId: String? = null,

    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,

    @ColumnInfo(name = "video_path")
    val videoPath: String? = null,

    @ColumnInfo(name = "gps_lat")
    val gpsLat: Double = 0.0,

    @ColumnInfo(name = "gps_lng")
    val gpsLng: Double = 0.0,

    @ColumnInfo(name = "captured_at")
    val capturedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "device_id")
    val deviceId: String = "",

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = "",

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SYNC_STATUS_PENDING,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
) {
    companion object {
        const val SYNC_STATUS_PENDING = "PENDING"
        const val SYNC_STATUS_SYNCED = "SYNCED"

        const val SEX_HEIFER = "Heifer"
        const val SEX_BULL = "Bull"
        const val SEX_STEER = "Steer"

        const val CALVING_EASE_UNASSISTED = 1 // 1: Normal / Unassisted
        const val CALVING_EASE_EASY_PULL = 2  // 2: Easy Pull / Minor Assist
        const val CALVING_EASE_HARD_PULL = 3  // 3: Hard Pull / Calving Jack
        const val CALVING_EASE_SURGICAL = 4   // 4: Vet / Caesarean

        const val VIGOR_VIGOROUS = "Vigorous / Nursing"
        const val VIGOR_MODERATE = "Moderate Vigor"
        const val VIGOR_WEAK = "Weak / Assisted"

        const val HORN_POLLED = "Polled (Natural)"
        const val HORN_HORNED = "Horned"
        const val HORN_DEHORNED = "Dehorned"

        val STANDARD_BREEDS = listOf(
            "Bonsmara",
            "Brahman",
            "Nguni",
            "Angus",
            "Simmentaler",
            "Afrikaner",
            "Drakensberger",
            "Charolais",
            "Limousin",
            "Hereford",
            "Boran",
            "Beefmaster",
            "Santa Gertrudis",
            "Sussex",
            "Crossbreed"
        )

        val PASTURE_LOCATIONS = listOf(
            "Camp 1 - River Meadow",
            "Camp 2 - West Hill Paddock",
            "Camp 3 - South Ridge",
            "Camp 4 - North Pasture",
            "Maternity Kraal",
            "Nursery Paddock A",
            "Klipkop Camp"
        )
    }
}
