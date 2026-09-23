package com.beeftech.calfregistration.data

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.entity.CalfRegistrationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

const val SYNC_STATUS_PENDING = "PENDING"
const val SYNC_STATUS_SYNCED = "SYNCED"

private const val SELECT_DAME_PLACEHOLDER = "Select dame"
private const val SELECT_SIRE_PLACEHOLDER = "Select sire"

object CalfRegistrationMappers {

    fun toEntity(
        formData: CalfRegistrationData,
        deviceId: String = "",
        captureAt: Long = System.currentTimeMillis(),
        existing: CalfRegistrationEntity? = null
    ): CalfRegistrationEntity {
        val damId = formData.dameTagNumber.takeUnless { it.startsWith("Select") }
        val sireId = formData.sireTagNumber.takeUnless { it.startsWith("Select") }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return CalfRegistrationEntity(
            registrationId = existing?.registrationId ?: UUID.randomUUID().toString(),
            registeredAnimalId = formData.tagNumber,
            damId = damId,
            sireId = sireId,
            birthWeightKg = null,
            calvingEase = null,
            registrationDate = dateFormat.format(Date(captureAt))
        )
    }

    fun toFormData(entity: CalfRegistrationEntity): CalfRegistrationData {
        return CalfRegistrationData(
            tagNumber = entity.registeredAnimalId,
            dameTagNumber = entity.damId ?: SELECT_DAME_PLACEHOLDER,
            sireTagNumber = entity.sireId ?: SELECT_SIRE_PLACEHOLDER,
            photoPath = null,
            synced = true,
            dateRegistered = entity.registrationDate
        )
    }

    fun toDto(entity: CalfRegistrationEntity): CalfRegistrationDto {
        return CalfRegistrationDto(
            animalId = entity.registeredAnimalId,
            birthdate = System.currentTimeMillis(),
            breed = "",
            damId = entity.damId,
            sireId = entity.sireId,
            photoPath = null,
            videoPath = null,
            gpsLat = 0.0,
            gpsLng = 0.0,
            captureAt = System.currentTimeMillis(),
            deviceId = "",
            recordguid = entity.registrationId,
            syncStatus = SYNC_STATUS_SYNCED,
            syncedAt = System.currentTimeMillis()
        )
    }
}
