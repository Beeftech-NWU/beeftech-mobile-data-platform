package com.beeftech.calfregistration.data

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.entity.CalfRegistration
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/** Sync status values persisted on [CalfRegistration.syncStatus]. */
const val SYNC_STATUS_PENDING = "PENDING"
const val SYNC_STATUS_SYNCED = "SYNCED"

private const val SELECT_DAME_PLACEHOLDER = "Select dame"
private const val SELECT_SIRE_PLACEHOLDER = "Select sire"

/**
 * Pure mapping functions between:
 *  - [CalfRegistrationData]: the UI form model used by the Tag Identity /
 *    Appearance & Parentage / Calves Registered screens.
 *  - [CalfRegistration]: the Room entity persisted locally.
 *  - [CalfRegistrationDto]: the network DTO sent to `backend:api`.
 *
 * All functions here are pure (no I/O, no `android.os.Build`, no
 * `System.currentTimeMillis()` reads) so they can be unit tested with plain
 * JUnit. Values that would normally come from the platform (device id,
 * "now" timestamp) are passed in as parameters by the caller
 * ([com.beeftech.calfregistration.data.CalfRegistrationRepository]).
 */
object CalfRegistrationMappers {

    /**
     * Maps the UI form model into a persistable Room entity.
     *
     * KNOWN LIMITATIONS / APPROXIMATIONS (documented; the Tag Identity and
     * Appearance & Parentage screens do not currently capture this data):
     *  - [CalfRegistration.animalId] = [CalfRegistrationData.tagNumber]. The
     *    tag number is the natural unique key the UI already treats as
     *    unique, and matches the `findByAnimalId`/`existsByAnimalId` DAO
     *    usage pattern used elsewhere in the codebase where tag-like
     *    strings are used as IDs.
     *  - [CalfRegistration.breed] = [CalfRegistrationData.animalType].
     *  - [CalfRegistration.birthdate] = [captureAt]. TODO: no real
     *    birthdate is captured by these screens yet.
     *  - [CalfRegistration.gpsLat]/[CalfRegistration.gpsLng] = `0.0`.
     *    TODO: no location utility exists anywhere in this codebase yet.
     *  - [CalfRegistration.photoPath]/[CalfRegistration.videoPath] = `null`.
     *    Not captured by these screens.
     *  - [CalfRegistration.recordguid] is generated once (via
     *    [UUID.randomUUID]) the first time an `animalId` is saved, and is
     *    preserved across subsequent saves/edits of the same `animalId` (by
     *    passing the previously persisted row in as [existing]) so that
     *    re-saving/editing an already-registered calf does not create a
     *    duplicate record server-side.
     *  - [CalfRegistration.syncStatus]/[CalfRegistration.syncedat] default
     *    to [SYNC_STATUS_PENDING]/`null` for brand-new records, and are
     *    otherwise carried over from [existing] (sync state is only ever
     *    mutated by the repository after a real sync attempt).
     */
    fun toEntity(
        formData: CalfRegistrationData,
        deviceId: String,
        captureAt: Long,
        existing: CalfRegistration? = null
    ): CalfRegistration {
        val damId = formData.dameTagNumber.takeUnless { it.startsWith("Select") }
        val sireId = formData.sireTagNumber.takeUnless { it.startsWith("Select") }

        return CalfRegistration(
            id = existing?.id ?: 0,
            animalId = formData.tagNumber,
            birthdate = captureAt,
            breed = formData.animalType,
            damId = damId,
            sireId = sireId,
            photoPath = formData.photoPath,
            videoPath = null,
            gpsLat = 0.0,
            gpsLng = 0.0,
            captureAt = captureAt,
            deviceId = deviceId,
            recordguid = existing?.recordguid ?: UUID.randomUUID().toString(),
            syncStatus = existing?.syncStatus ?: SYNC_STATUS_PENDING,
            syncedat = existing?.syncedat
        )
    }

    /**
     * Reverse mapping used to populate the "Registered calves" list
     * ([CalvesRegisteredScreen][com.beeftech.calfregistration.ui.CalvesRegisteredScreen]).
     *
     * KNOWN LIMITATION: only the fields actually persisted by
     * [CalfRegistration] are restored (`tagNumber`, `animalType`,
     * `dameTagNumber`, `sireTagNumber`, `synced`, `dateRegistered`). All
     * other UI-only fields (gender/age/condition/hideColour/etc.) fall back
     * to [CalfRegistrationData]'s defaults since the Room entity does not
     * store them yet — extending the entity schema to capture them is out
     * of scope for this change.
     */
    fun toFormData(entity: CalfRegistration): CalfRegistrationData {
        val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())

        return CalfRegistrationData(
            tagNumber = entity.animalId,
            animalType = entity.breed,
            dameTagNumber = entity.damId ?: SELECT_DAME_PLACEHOLDER,
            sireTagNumber = entity.sireId ?: SELECT_SIRE_PLACEHOLDER,
            photoPath = entity.photoPath,
            synced = entity.syncStatus == SYNC_STATUS_SYNCED,
            dateRegistered = dateFormat.format(entity.captureAt)
        )
    }

    /** Straightforward 1:1 mapping from the Room entity to the network DTO. */
    fun toDto(entity: CalfRegistration): CalfRegistrationDto {
        return CalfRegistrationDto(
            animalId = entity.animalId,
            birthdate = entity.birthdate,
            breed = entity.breed,
            damId = entity.damId,
            sireId = entity.sireId,
            photoPath = entity.photoPath,
            videoPath = entity.videoPath,
            gpsLat = entity.gpsLat,
            gpsLng = entity.gpsLng,
            captureAt = entity.captureAt,
            deviceId = entity.deviceId,
            recordguid = entity.recordguid,
            syncStatus = entity.syncStatus,
            syncedAt = entity.syncedat
        )
    }
}
