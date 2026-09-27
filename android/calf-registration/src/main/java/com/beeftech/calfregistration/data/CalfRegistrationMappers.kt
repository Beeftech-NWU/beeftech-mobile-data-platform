package com.beeftech.calfregistration.data

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.util.TagNamingUtils
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.AnimalMediaEntity
import com.beeftech.database.entity.CalfRegistrationEntity
import com.beeftech.database.entity.IdentifierTypes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

const val SYNC_STATUS_PENDING = "PENDING"
const val SYNC_STATUS_SYNCED = "SYNCED"

private const val SELECT_DAME_PLACEHOLDER = "Select dame"
private const val SELECT_SIRE_PLACEHOLDER = "Select sire"

/** Everything that must be written, atomically, to register one calf. */
data class NewCalf(
    val animal: Animal,
    val identifiers: List<AnimalIdentifierEntity>,
    val media: List<AnimalMediaEntity>,
    val registration: CalfRegistrationEntity
)

object CalfRegistrationMappers {

    /** "BRN — Brangus" -> "Brangus"; "Brangus" -> "Brangus" */
    fun breedName(animalType: String): String =
        animalType.substringAfter("—", animalType).trim()

    /** "Blu0000011 (Bonsmara)" -> "Blu0000011"; "Select dame" -> null */
    fun parentTag(dropdownValue: String): String? =
        dropdownValue.takeUnless { it.isBlank() || it.startsWith("Select") }
            ?.substringBefore(" (")
            ?.let { TagNamingUtils.parseAndExpand(it) }

    fun toNewCalf(
        formData: CalfRegistrationData,
        capture: CalfCaptureContext,
        damAnimalId: String?,
        sireAnimalId: String?
    ): NewCalf {
        val tag = TagNamingUtils.parseAndExpand(formData.tagNumber)
        require(TagNamingUtils.validateTag(tag)) { "Tag '$tag' does not match the tag naming standard" }

        val animalId = UUID.randomUUID().toString()
        val captureDate = isoDate(capture.captureAt)

        val animal = Animal(
            animalId = animalId,
            tagNumber = tag, // legacy column, dual-written until R4 drops it
            birthdate = startOfDay(capture.captureAt), // assumption: calf registered on day of birth
            breed = breedName(formData.animalType),
            gender = formData.gender,
            condition = formData.condition,
            hideColour = formData.hideColour,
            brandMark = formData.mark.ifBlank { null },
            gpsLat = capture.gpsLat,
            gpsLng = capture.gpsLng,
            captureAt = capture.captureAt,
            deviceId = capture.deviceId
        )

        val identifiers = buildList {
            add(identifier(animalId, IdentifierTypes.TAG, tag, captureDate))
            formData.oldTagNumber.takeIf { it.isNotBlank() }
                ?.let { add(identifier(animalId, IdentifierTypes.OLD_TAG, it.trim(), captureDate)) }
            formData.referenceNumber.takeIf { it.isNotBlank() }
                ?.let { add(identifier(animalId, IdentifierTypes.REFERENCE, it.trim(), captureDate)) }
            formData.transponderNumber.takeIf { it.isNotBlank() }
                ?.let { add(identifier(animalId, IdentifierTypes.TRANSPONDER, it.trim(), captureDate)) }
        }

        val media = listOfNotNull(
            formData.photoPath?.let {
                AnimalMediaEntity(animalId = animalId, filePath = it, mediaType = "PHOTO", createdAt = captureDate)
            }
        )

        val registration = CalfRegistrationEntity(
            registeredAnimalId = animalId,
            damId = damAnimalId,
            sireId = sireAnimalId,
            registrationDate = captureDate
        )

        return NewCalf(animal, identifiers, media, registration)
    }

    fun toFormData(view: CalfRegistrationView): CalfRegistrationData = CalfRegistrationData(
        tagNumber = view.tagNumber,
        animalType = view.breed,
        gender = view.gender ?: "",
        dameTagNumber = view.damTagNumber ?: SELECT_DAME_PLACEHOLDER,
        sireTagNumber = view.sireTagNumber ?: SELECT_SIRE_PLACEHOLDER,
        photoPath = view.photoPath,
        synced = view.syncStatus == SYNC_STATUS_SYNCED,
        dateRegistered = view.registrationDate
    )

    fun toDto(view: CalfRegistrationView): CalfRegistrationDto = CalfRegistrationDto(
        tagNumber = view.tagNumber,
        animalUuid = view.animalId,
        birthdate = view.birthdate,
        breed = view.breed,
        damTagNumber = view.damTagNumber,
        sireTagNumber = view.sireTagNumber,
        damAnimalUuid = view.damAnimalId,
        sireAnimalUuid = view.sireAnimalId,
        photoPath = view.photoPath,
        videoPath = null,
        gpsLat = view.gpsLat,
        gpsLng = view.gpsLng,
        captureAt = view.captureAt,
        deviceId = view.deviceId,
        recordguid = view.recordGuid,
        syncStatus = view.syncStatus,
        syncedAt = view.syncedAt
    )

    private fun identifier(animalId: String, type: String, value: String, validFrom: String) =
        AnimalIdentifierEntity(
            animalId = animalId,
            identifierType = type,
            identifierValue = value,
            validFrom = validFrom
        )

    private fun isoDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(epochMillis))

    private fun startOfDay(epochMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
