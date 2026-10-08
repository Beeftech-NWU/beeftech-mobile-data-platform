package com.beeftech.farmtraceability.repository

import com.beeftech.database.BeefTechDatabase
import com.beeftech.database.entity.AnimalWeightEntity
import com.beeftech.database.repository.PendingSyncRepository
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/** The Animal Record dialog edits the existing calf registration, never re-registers a calf. */
class AnimalMassEditorRepository(private val database: BeefTechDatabase) {
    suspend fun correctRegisteredMass(animalId: String, massKg: Double) =
        withContext(Dispatchers.IO) {
            validateMass(massKg)
            require(animalId.isNotBlank()) { "Select an animal first." }
            val registration = database.calfRegistrationDao()
                .getAllRegistrationViews().firstOrNull().orEmpty()
                .firstOrNull { it.animalId == animalId }
                ?: throw IllegalArgumentException("This animal has no calf registration to correct.")

            database.openHelper.writableDatabase.execSQL(
                """
                UPDATE calf_registrations
                SET birth_weight_kg = ?, sync_status = 'PENDING', synced_at = NULL
                WHERE registration_id = ? AND registered_animal_id = ?
                """.trimIndent(),
                arrayOf<Any>(massKg, registration.registrationId, animalId)
            )

            // Sync the same immutable registration GUID. Existing calf sync
            // also repairs missing queue entries if offline queuing fails.
            try {
                PendingSyncRepository(database.pendingSyncDao()).queueOperation(
                    entityType = "CALF_REGISTRATION",
                    entityId = registration.recordGuid,
                    operation = "UPSERT",
                    payload = ""
                )
            } catch (_: Exception) {
                // The PENDING registration is preserved for the existing sync worker.
            }
        }

    suspend fun recordWeighing(
        animalId: String,
        massKg: Double,
        weighDate: Long,
        note: String
    ) = withContext(Dispatchers.IO) {
        validateMass(massKg)
        require(animalId.isNotBlank()) { "Select an animal first." }
        require(weighDate > 0L && weighDate <= System.currentTimeMillis()) {
            "Choose a weighing date that is not in the future."
        }

        var deviceId: String? = null
        var birthDate: Long? = null
        database.openHelper.readableDatabase.query(
            "SELECT deviceId, birthdate FROM animals WHERE animalId = ? LIMIT 1",
            arrayOf<Any?>(animalId)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                deviceId = cursor.getString(0)
                birthDate = cursor.getLong(1)
            }
        }
        val validDeviceId = deviceId?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("The animal or its registered device is unavailable.")
        val earliestDate = birthDate
        if (earliestDate != null && startOfDay(weighDate) < startOfDay(earliestDate)) {
            throw IllegalArgumentException("Weighing date cannot be before the animal's birth date.")
        }

        val dayStart = startOfDay(weighDate)
        // Birth-mass corrections must update the existing calf registration,
        // not create an additional weight event for the same birth date.
        val existingRegistration = database.calfRegistrationDao()
            .getAllRegistrationViews().firstOrNull().orEmpty()
            .firstOrNull { it.animalId == animalId }
        if (existingRegistration?.birthWeightKg != null &&
            startOfDay(existingRegistration.birthdate) == dayStart
        ) {
            throw IllegalArgumentException(
                "Birth mass is already recorded for this date. Select Correct birth mass."
            )
        }
        val nextDayStart = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val count = database.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM animal_weights WHERE animal_id = ? AND weigh_date >= ? AND weigh_date < ?",
            arrayOf<Any?>(animalId, dayStart, nextDayStart)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        require(count == 0) {
            "A weighing already exists for that date. No duplicate mass record was created."
        }

        database.animalWeightDao().insertWeight(
            AnimalWeightEntity(
                animalId = animalId,
                weightKg = massKg,
                weighDate = weighDate,
                notes = note.trim().takeIf { it.isNotBlank() },
                deviceId = validDeviceId,
                capturedAt = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
        )
        // Weight records are stored in Room and remain PENDING until a
        // supported weight-sync route is available. No false SYNCED status.
    }

    private fun validateMass(value: Double) {
        require(value.isFinite() && value > 0.0) { "Enter a valid mass greater than zero." }
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
