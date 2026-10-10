package com.beeftech.calfregistration.data

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.calfregistration.ui.CalfRegistrationLookups
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
const val SYNC_STATUS_REJECTED = "REJECTED"

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

        val birthDate =
            parseUiDate(
                formData.birthDate
            )
                ?: startOfDay(
                    capture.captureAt
                )

        val birthWeightKg =
            formData.birthWeightKg
                .trim()
                .replace(",", ".")
                .takeIf {
                    it.isNotBlank()
                }
                ?.toDoubleOrNull()

        require(
            formData.birthWeightKg.isBlank() ||
                birthWeightKg != null
        ) {
            "Birth mass must be a valid number."
        }

        require(
            birthWeightKg == null ||
                birthWeightKg in 1.0..150.0
        ) {
            "Birth mass must be between 1 and 150 kg."
        }

        val animal = Animal(
            animalId = animalId,
            birthdate = birthDate,
            breed = breedName(formData.animalType),
            gender = formData.gender,
            hideColour = formData.hideColour,
            brandMark = formData.mark.ifBlank { null },
            damId = damAnimalId,
            sireId = sireAnimalId,
            gpsLat = capture.gpsLat,
            gpsLng = capture.gpsLng,
            captureAt = capture.captureAt,
            deviceId = capture.deviceId
        )

        val identifiers = buildList {
            add(identifier(animalId, IdentifierTypes.TAG, tag, capture.captureAt))
            formData.oldTagNumber.takeIf { it.isNotBlank() }
                ?.let { add(identifier(animalId, IdentifierTypes.OLD_TAG, it.trim(), capture.captureAt)) }
            formData.referenceNumber.takeIf { it.isNotBlank() }
                ?.let { add(identifier(animalId, IdentifierTypes.REFERENCE, it.trim(), capture.captureAt)) }
        }

        val media = listOfNotNull(
            formData.photoPath?.let {
                AnimalMediaEntity(animalId = animalId, filePath = it, mediaType = "PHOTO", createdAt = capture.captureAt)
            }
        )

        val registration = CalfRegistrationEntity(
            registeredAnimalId = animalId,
            damId = damAnimalId,
            sireId = sireAnimalId,
            birthWeightKg = birthWeightKg,
            ageClass = formData.age,
            bodyCondition = formData.condition,
            conformity = formData.conformity,
            processProof = formData.processProof.trim().ifEmpty { null },
            implantProof = formData.implantProof.trim().ifEmpty { null },
            registrationDate = capture.captureAt
        )

        return NewCalf(animal, identifiers, media, registration)
    }

    fun toFormData(view: CalfRegistrationView): CalfRegistrationData = CalfRegistrationData(
        tagNumber = view.tagNumber,
        oldTagNumber = view.oldTagNumber.orEmpty(),
        referenceNumber = view.referenceNumber.orEmpty(),
        processProof = view.processProof.orEmpty(),
        implantProof = view.implantProof.orEmpty(),
        animalType = view.breed,
        gender = view.gender ?: "",
        age = view.ageClass ?: CalfRegistrationData().age,
        condition = CalfRegistrationLookups.legacyConditionToScore(view.bodyCondition),
        conformity = view.conformity ?: CalfRegistrationData().conformity,
        hideColour = view.hideColour ?: CalfRegistrationData().hideColour,
        mark = view.brandMark.orEmpty(),
        birthDate = displayFullDate(view.birthdate),
        birthWeightKg =
            view.birthWeightKg
                ?.let {
                    String.format(
                        Locale.US,
                        "%.1f",
                        it
                    )
                }
                .orEmpty(),
        dameTagNumber = view.damTagNumber ?: SELECT_DAME_PLACEHOLDER,
        sireTagNumber = view.sireTagNumber ?: SELECT_SIRE_PLACEHOLDER,
        photoPath = view.photoPath,
        synced = view.syncStatus == SYNC_STATUS_SYNCED,
        needsAttention = view.syncStatus == SYNC_STATUS_REJECTED,
        syncError = view.syncError,
        dateRegistered = displayDate(view.registrationDate)
    )

    fun toDto(view: CalfRegistrationView): CalfRegistrationDto = CalfRegistrationDto(
        tagNumber = view.tagNumber,
        animalUuid = view.animalId,
        birthdate = view.birthdate,
        breed = view.breed,
        gender = view.gender,
        hideColour = view.hideColour,
        brandMark = view.brandMark,
        birthWeightKg = view.birthWeightKg,
        ageClass = view.ageClass,
        bodyCondition = view.bodyCondition,
        conformity = view.conformity,
        oldTagNumber = view.oldTagNumber,
        referenceNumber = view.referenceNumber,
        processProof = view.processProof,
        implantProof = view.implantProof,
        damTagNumber = view.damTagNumber,
        sireTagNumber = view.sireTagNumber,
        damAnimalUuid = view.damAnimalId,
        sireAnimalUuid = view.sireAnimalId,
        // The local file path means nothing to the server; the photo is uploaded separately.
        photoPath = null,
        videoPath = null,
        gpsLat = view.gpsLat,
        gpsLng = view.gpsLng,
        captureAt = view.captureAt,
        deviceId = view.deviceId,
        recordguid = view.recordGuid,
        syncStatus = view.syncStatus,
        syncedAt = view.syncedAt
    )

    private fun identifier(animalId: String, type: String, value: String, validFrom: Long) =
        AnimalIdentifierEntity(
            animalId = animalId,
            identifierType = type,
            identifierValue = value,
            validFrom = validFrom
        )

    private fun parseUiDate(
        value: String
    ): Long? {
        if (value.isBlank()) {
            return null
        }

        return try {
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }
                .parse(value)
                ?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun displayFullDate(
        epochMillis: Long
    ): String =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(
            Date(epochMillis)
        )

    /** "26 Aug" -- matches CalfRegistrationData.dateRegistered's placeholder style. */
    private fun displayDate(epochMillis: Long): String =
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochMillis))

    private fun startOfDay(epochMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
