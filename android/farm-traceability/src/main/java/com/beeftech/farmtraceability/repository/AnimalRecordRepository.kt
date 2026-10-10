package com.beeftech.farmtraceability.repository

import com.beeftech.database.BeefTechDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class AnimalRecordSummary(

    val breed: String = "",

    val gender: String = "",

    val birthDate: String = "",

    val age: String = "",

    val photoPath: String = "",

    val entryMass: String = "",

    val registeredBirthMass: String = "",

    val lastMass: String = "",

    val daysAtFacility: String = "",

    val averageDailyGain: String = "",
    val hasCalfRegistration: Boolean = false,
    val massHistory: List<AnimalMassReading> = emptyList()
)

data class AnimalMassReading(
    val massKg: Double,
    val dateMillis: Long,
    val source: String
)


class AnimalRecordRepository(
    private val database:
        BeefTechDatabase
) {

    suspend fun load(
        animalId: String,
        tagNumber: String
    ): AnimalRecordSummary =
        withContext(
            Dispatchers.IO
        ) {

            if (
                animalId.isBlank()
            ) {

                return@withContext AnimalRecordSummary()
            }


            // Resolve by canonical animal ID, not display tag text.
            val registration = try {
                database.calfRegistrationDao()
                    .getAllRegistrationViews()
                    .firstOrNull()
                    .orEmpty()
                    .firstOrNull { it.animalId == animalId }
            } catch (_: Exception) {
                null
            }


            var databaseBreed =
                ""

            var databaseGender =
                ""

            var animalBirthdate:
                    Long? =
                null

            var animalCaptureAt:
                    Long? =
                null


            /*
             * Read directly from the canonical animals row.
             *
             * This keeps Animal Record useful even if a registration
             * projection is temporarily unavailable.
             */
            try {

                database
                    .openHelper
                    .readableDatabase
                    .query(
                        """
                        SELECT
                            breed,
                            gender,
                            birthdate,
                            captureAt
                        FROM animals
                        WHERE animalId = ?
                        LIMIT 1
                        """.trimIndent(),

                        arrayOf<Any?>(
                            animalId
                        )
                    )
                    .use {
                            cursor ->

                        if (
                            cursor.moveToFirst()
                        ) {

                            val breedIndex =
                                cursor
                                    .getColumnIndex(
                                        "breed"
                                    )

                            val genderIndex =
                                cursor
                                    .getColumnIndex(
                                        "gender"
                                    )

                            val birthdateIndex =
                                cursor
                                    .getColumnIndex(
                                        "birthdate"
                                    )

                            val captureIndex =
                                cursor
                                    .getColumnIndex(
                                        "captureAt"
                                    )


                            if (
                                breedIndex >= 0 &&
                                !cursor.isNull(
                                    breedIndex
                                )
                            ) {

                                databaseBreed =
                                    cursor
                                        .getString(
                                            breedIndex
                                        )
                                        .orEmpty()
                            }


                            if (
                                genderIndex >= 0 &&
                                !cursor.isNull(
                                    genderIndex
                                )
                            ) {

                                databaseGender =
                                    cursor
                                        .getString(
                                            genderIndex
                                        )
                                        .orEmpty()
                            }


                            if (
                                birthdateIndex >= 0 &&
                                !cursor.isNull(
                                    birthdateIndex
                                )
                            ) {

                                animalBirthdate =
                                    cursor
                                        .getLong(
                                            birthdateIndex
                                        )
                            }


                            if (
                                captureIndex >= 0 &&
                                !cursor.isNull(
                                    captureIndex
                                )
                            ) {

                                animalCaptureAt =
                                    cursor
                                        .getLong(
                                            captureIndex
                                        )
                            }
                        }
                    }

            } catch (
                _: Exception
            ) {
                // Registration data below remains usable.
            }


            // Registration is the single source of truth for birth mass.
            // Subsequent dated weighings live in animal_weights; corrections to
            // the birth mass never manufacture another weighing event.
            val readings = mutableListOf<AnimalMassReading>()
            val birthMass = registration?.birthWeightKg?.takeIf { it > 0.0 }
            val birthTimestamp = registration?.birthdate?.takeIf { it > 0L }
            if (birthMass != null && birthTimestamp != null) {
                readings += AnimalMassReading(birthMass, birthTimestamp, "Registered birth mass")
            }

            try {
                database.openHelper.readableDatabase.query(
                    """
                    SELECT weight_kg, weigh_date FROM animal_weights
                    WHERE animal_id = ? AND weight_kg > 0
                    ORDER BY weigh_date ASC
                    """.trimIndent(),
                    arrayOf<Any?>(animalId)
                ).use { cursor ->
                    val weightColumn = cursor.getColumnIndexOrThrow("weight_kg")
                    val dateColumn = cursor.getColumnIndexOrThrow("weigh_date")
                    while (cursor.moveToNext()) {
                        val weight = cursor.getDouble(weightColumn)
                        val recordedAt = cursor.getLong(dateColumn)
                        if (weight.isFinite() && weight > 0.0 && recordedAt > 0L) {
                            readings += AnimalMassReading(weight, recordedAt, "Weighing")
                        }
                    }
                }
            } catch (_: Exception) {
                // No weight measurements have been recorded yet.
            }

            val latestWeighing = readings.filter { it.source == "Weighing" }
                .maxByOrNull { it.dateMillis }
            val earliestWeighing = readings.filter { it.source == "Weighing" }
                .minByOrNull { it.dateMillis }
            val entryWeight = birthMass ?: earliestWeighing?.massKg
            val currentWeight = latestWeighing?.massKg ?: birthMass

            // Compare dated measurements, never elapsed time at the facility.
            // Collapsing repeated measurements on the same day avoids a
            // misleading divide-by-zero ADG.
            val latestDistinctDays = readings
                .filter { it.massKg.isFinite() && it.massKg > 0.0 && it.dateMillis > 0L }
                .sortedByDescending { it.dateMillis }
                .distinctBy { utcCalendarDay(it.dateMillis) }
                .take(2)
            val adg = if (latestDistinctDays.size == 2) {
                val newer = latestDistinctDays[0]
                val older = latestDistinctDays[1]
                val elapsedDays = utcCalendarDay(newer.dateMillis) - utcCalendarDay(older.dateMillis)
                if (elapsedDays > 0L) {
                    (newer.massKg - older.massKg) / elapsedDays.toDouble()
                } else null
            } else null

            val birthDateMillis =
                registration
                    ?.birthdate
                    ?.takeIf {
                        it > 0L
                    }
                    ?: animalBirthdate
                        ?.takeIf {
                            it > 0L
                        }


            val facilityStart =
                registration
                    ?.registrationDate
                    ?.takeIf {
                        it > 0L
                    }
                    ?: registration
                        ?.captureAt
                        ?.takeIf {
                            it > 0L
                        }
                    ?: animalCaptureAt
                        ?.takeIf {
                            it > 0L
                        }


            val days =
                facilityStart
                    ?.let {
                            startedAt ->

                        (
                            (
                                System.currentTimeMillis() -
                                    startedAt
                            )
                                .coerceAtLeast(
                                    0L
                                )
                            /
                            DAY_MILLIS
                        )
                            .toInt()
                    }


            val photoPath =
                try {

                    database
                        .animalMediaDao()
                        .getMediaForAnimalByType(
                            animalId,
                            "PHOTO"
                        )
                        .firstOrNull()
                        ?.firstOrNull()
                        ?.filePath
                        .orEmpty()

                } catch (
                    _: Exception
                ) {

                    ""
                }


            AnimalRecordSummary(

                breed =
                    registration
                        ?.breed
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: databaseBreed,

                gender =
                    registration
                        ?.gender
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: databaseGender,

                birthDate =
                    birthDateMillis
                        ?.let {
                            formatDate(it)
                        }
                        .orEmpty(),

                age =
                    birthDateMillis
                        ?.let {
                            formatAge(it)
                        }
                        .orEmpty(),

                photoPath =
                    photoPath,

                entryMass =
                    entryWeight
                        ?.let {
                            "%.1f kg".format(
                                Locale.US,
                                it
                            )
                        }
                        .orEmpty(),

                registeredBirthMass =
                    birthMass?.let { "%.1f kg".format(Locale.US, it) }.orEmpty(),

                lastMass =
                    currentWeight
                        ?.let {
                            "%.1f kg".format(
                                Locale.US,
                                it
                            )
                        }
                        .orEmpty(),

                daysAtFacility =
                    days
                        ?.let {
                            "$it days"
                        }
                        .orEmpty(),

                averageDailyGain =
                    adg?.let { "%.2f kg/day".format(Locale.US, it) }
                        ?: "Not enough data",
                hasCalfRegistration = registration != null,
                massHistory = readings.sortedByDescending { it.dateMillis }
            )
        }


    private fun utcCalendarDay(timestamp: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = timestamp }
        val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH)
            )
        }
        return utc.timeInMillis / DAY_MILLIS
    }

    private fun formatDate(
        epochMillis: Long
    ): String =
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )
            .format(
                Date(epochMillis)
            )


    private fun formatAge(
        birthDateMillis: Long
    ): String {

        val birth =
            Calendar.getInstance().apply {
                timeInMillis =
                    birthDateMillis
            }

        val today =
            Calendar.getInstance()

        var years =
            today.get(
                Calendar.YEAR
            ) -
                birth.get(
                    Calendar.YEAR
                )

        var months =
            today.get(
                Calendar.MONTH
            ) -
                birth.get(
                    Calendar.MONTH
                )

        if (
            today.get(
                Calendar.DAY_OF_MONTH
            ) <
            birth.get(
                Calendar.DAY_OF_MONTH
            )
        ) {
            months--
        }

        if (
            months < 0
        ) {
            years--
            months += 12
        }

        return when {
            years > 0 && months > 0 ->
                "$years y $months mo"

            years > 0 ->
                "$years y"

            months > 0 ->
                "$months mo"

            else ->
                "< 1 mo"
        }
    }


    private companion object {

        const val DAY_MILLIS =
            24L *
                60L *
                60L *
                1000L
    }
}
