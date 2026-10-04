package com.beeftech.farmtraceability.repository

import com.beeftech.database.BeefTechDatabase
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class AnimalRecordSummary(

    val breed: String = "",

    val gender: String = "",

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


    private companion object {

        const val DAY_MILLIS =
            24L *
                60L *
                60L *
                1000L
    }
}
