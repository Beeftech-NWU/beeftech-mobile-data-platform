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

    val lastMass: String = "",

    val daysAtFacility: String = "",

    val averageDailyGain: String = ""
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


            val registration =
                try {

                    if (
                        tagNumber.isBlank()
                    ) {

                        null

                    } else {

                        database
                            .calfRegistrationDao()
                            .getRegistrationByTag(
                                tagNumber
                            )
                            .firstOrNull()
                    }

                } catch (
                    _: Exception
                ) {

                    null
                }


            var databaseBreed =
                ""

            var databaseGender =
                ""

            var animalBirthdate:
                    Long? =
                null

            var animalMass:
                    Double? =
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
                            massKg,
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

                            val massIndex =
                                cursor
                                    .getColumnIndex(
                                        "massKg"
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
                                massIndex >= 0 &&
                                !cursor.isNull(
                                    massIndex
                                )
                            ) {

                                animalMass =
                                    cursor
                                        .getDouble(
                                            massIndex
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


            var firstWeight:
                    Double? =
                null

            var lastWeight:
                    Double? =
                null


            /*
             * animal_weights is the source of truth for later
             * weigh-ins.
             */
            try {

                database
                    .openHelper
                    .readableDatabase
                    .query(
                        """
                        SELECT
                            weight_kg,
                            weigh_date
                        FROM animal_weights
                        WHERE animal_id = ?
                        ORDER BY weigh_date ASC
                        """.trimIndent(),

                        arrayOf<Any?>(
                            animalId
                        )
                    )
                    .use {
                            cursor ->

                        val weightIndex =
                            cursor
                                .getColumnIndex(
                                    "weight_kg"
                                )


                        while (
                            cursor.moveToNext()
                        ) {

                            if (
                                weightIndex >= 0 &&
                                !cursor.isNull(
                                    weightIndex
                                )
                            ) {

                                val weight =
                                    cursor
                                        .getDouble(
                                            weightIndex
                                        )


                                if (
                                    firstWeight == null
                                ) {

                                    firstWeight =
                                        weight
                                }


                                lastWeight =
                                    weight
                            }
                        }
                    }

            } catch (
                _: Exception
            ) {
                // No recorded weigh-in yet.
            }


            /*
             * Entry mass priority:
             *
             * 1. animal mass captured at registration
             * 2. calf registration birth weight
             * 3. earliest recorded weight
             */
            val entryWeight =
                animalMass
                    ?: registration
                        ?.birthWeightKg
                    ?: firstWeight


            /*
             * Last mass:
             *
             * latest weigh-in first, then current animal mass,
             * then registration weight as a final fallback.
             */
            val currentWeight =
                lastWeight
                    ?: animalMass
                    ?: registration
                        ?.birthWeightKg


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


            val adg =
                if (
                    entryWeight != null &&
                    currentWeight != null &&
                    days != null &&
                    days > 0
                ) {

                    (
                        currentWeight -
                            entryWeight
                    ) /
                        days.toDouble()

                } else {

                    null
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
                    adg
                        ?.let {
                            "%.2f kg/day".format(
                                Locale.US,
                                it
                            )
                        }
                        .orEmpty()
            )
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
