package com.beeftech.calfregistration.data

import android.util.Log

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.util.TagNamingUtils
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.dao.DuplicateTagException
import com.beeftech.database.dao.ParentCandidate
import com.beeftech.database.entity.IdentifierTypes
import com.beeftech.database.repository.PendingSyncRepository
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class CalfRegistrationRepository(
    private val calfRegistrationDao: CalfRegistrationDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: CalfRegistrationApiClient,
    private val captureContextProvider: () -> CalfCaptureContext
) {

    suspend fun loadAll(): List<CalfRegistrationData> {
        return try {
            val views = calfRegistrationDao.getAllRegistrationViews().firstOrNull() ?: emptyList()
            views.map { CalfRegistrationMappers.toFormData(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Registered animals that can be chosen as dam (female) or sire (male), as "Tag (Breed)". */
    suspend fun loadParentOptions(): ParentOptions {
        return try {
            fun label(candidate: ParentCandidate) = "${candidate.tagNumber} (${candidate.breed})"

            ParentOptions(
                dams = calfRegistrationDao.getParentCandidates(GENDER_FEMALE).map(::label),
                sires = calfRegistrationDao.getParentCandidates(GENDER_MALE).map(::label)
            )
        } catch (_: Exception) {
            ParentOptions()
        }
    }

    suspend fun isTagRegistered(tagNumber: String): Boolean {
        if (tagNumber.isBlank()) return false

        return try {
            calfRegistrationDao.findAnimalIdByTag(TagNamingUtils.parseAndExpand(tagNumber)) != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun saveCalf(formData: CalfRegistrationData): SaveCalfOutcome {
        var savedLocally = false
        return try {
            val warnings = mutableListOf<String>()

            suspend fun resolveParent(label: String, value: String): String? {
                val tag = CalfRegistrationMappers.parentTag(value) ?: return null
                return calfRegistrationDao.findAnimalIdByTag(tag)
                    ?: null.also { warnings += "$label $tag is not registered; saved without $label." }
            }

            val damId = resolveParent("Dam", formData.dameTagNumber)
            val sireId = resolveParent("Sire", formData.sireTagNumber)

            val newCalf = try {
                CalfRegistrationMappers.toNewCalf(formData, captureContextProvider(), damId, sireId)
            } catch (e: IllegalArgumentException) {
                return SaveCalfOutcome(formData.copy(synced = false), validationError = e.message)
            }

            try {
                calfRegistrationDao.registerCalf(
                    newCalf.animal,
                    newCalf.identifiers,
                    newCalf.media,
                    newCalf.registration
                )
            } catch (e: DuplicateTagException) {
                return SaveCalfOutcome(formData.copy(synced = false), validationError = e.message)
            }
            savedLocally = true

            val tag = newCalf.identifiers.first { it.identifierType == IdentifierTypes.TAG }.identifierValue
            val view = calfRegistrationDao.getRegistrationByTag(tag).first()
                ?: error("Calf $tag was saved but could not be read back")

            val sync =
                apiClient.syncCalves(
                    listOf(view),
                    deviceId = view.deviceId
                )

            if (sync.isSuccess) {

                val serverResult =
                    sync
                        .getOrThrow()
                        .results
                        .firstOrNull {
                            it.recordguid == view.recordGuid
                        }

                if (serverResult?.status == SYNC_STATUS_SYNCED) {

                    calfRegistrationDao.markSynced(
                        listOf(view.recordGuid),
                        System.currentTimeMillis()
                    )

                    val photos = uploadPendingPhotos()

                    SaveCalfOutcome(
                        data =
                            CalfRegistrationMappers
                                .toFormData(view)
                                .copy(synced = true),
                        warnings = warnings,
                        photoPending = photos.pending > 0
                    )

                } else {

                    val message =
                        serverResult?.message
                            ?: "Server did not confirm calf synchronization."

                    /*
                     * The server answered and said no. Count it, and stop retrying
                     * automatically once the cap is reached.
                     */
                    if (serverResult != null) {
                        calfRegistrationDao.recordRejection(
                            view.recordGuid,
                            message,
                            MAX_SERVER_REJECTIONS
                        )

                        val stillPending =
                            calfRegistrationDao
                                .getPendingRegistrationViews()
                                .any { it.recordGuid == view.recordGuid }

                        if (!stillPending) {
                            return SaveCalfOutcome(
                                data =
                                    CalfRegistrationMappers
                                        .toFormData(view)
                                        .copy(needsAttention = true, syncError = message),
                                syncErrorMessage = message,
                                warnings = warnings,
                                needsAttention = true
                            )
                        }
                    }

                    pendingSyncRepository.queueOperation(
                        entityType = ENTITY_TYPE,
                        entityId = view.recordGuid,
                        operation = "UPSERT",
                        payload = ""
                    )

                    SaveCalfOutcome(
                        data =
                            CalfRegistrationMappers
                                .toFormData(view),

                        syncErrorMessage = message,

                        warnings = warnings
                    )
                }

            } else {

                pendingSyncRepository.queueOperation(
                    entityType = ENTITY_TYPE,
                    entityId = view.recordGuid,
                    operation = "UPSERT",
                    payload = ""
                )

                SaveCalfOutcome(
                    data =
                        CalfRegistrationMappers
                            .toFormData(view),

                    syncErrorMessage =
                        sync.exceptionOrNull()?.message
                            ?: "Sync failed",

                    warnings = warnings
                )
            }
        } catch (exception: Exception) {
            if (savedLocally) {
                // The calf is stored; only the read-back or sync step failed. It stays pending.
                SaveCalfOutcome(
                    data = formData.copy(synced = false),
                    syncErrorMessage = exception.message
                )
            } else {
                SaveCalfOutcome(
                    data = formData.copy(synced = false),
                    saveError = exception.message ?: "Unable to save calf registration."
                )
            }
        }
    }

    /**
     * Each request is labelled with the device that captured its records, so a batch holding
     * records from more than one device is sent as one request per device. A device whose
     * request fails simply returns no results, which leaves its records pending; the whole
     * call fails only when every request failed.
     */
    private suspend fun syncByDevice(views: List<CalfRegistrationView>): Result<CalfRegistrationSyncResponse> {
        val byDevice = views.groupBy { it.deviceId }

        if (byDevice.size == 1) {
            return apiClient.syncCalves(views, deviceId = byDevice.keys.first())
        }

        val results = mutableListOf<CalfRegistrationSyncResult>()
        var firstFailure: Throwable? = null

        for ((deviceId, records) in byDevice) {
            apiClient.syncCalves(records, deviceId).fold(
                onSuccess = { results += it.results },
                onFailure = { firstFailure = firstFailure ?: it }
            )
        }

        val failure = firstFailure
        return if (results.isEmpty() && failure != null) {
            Result.failure(failure)
        } else {
            Result.success(CalfRegistrationSyncResponse(results))
        }
    }

    /**
     * Syncs pending registrations, then uploads photos for calves the server already has.
     * Photos never change a record's sync status: a calf is SYNCED once the server has it.
     *
     * [retryRejected] is the user's manual retry; see [syncPendingRecords].
     */
    suspend fun syncPending(retryRejected: Boolean = false): SyncPendingOutcome {
        val records = syncPendingRecords(retryRejected)
        val photos = uploadPendingPhotos()
        return records.copy(photosUploaded = photos.uploaded, photosPending = photos.pending)
    }

    /**
     * Uploads each waiting photo file. A file that no longer exists (or never did) is skipped.
     * Stops at the first temporary failure, since the rest would fail the same way.
     * A refusal by the server marks that photo FAILED so it is not retried forever.
     */
    suspend fun uploadPendingPhotos(): PhotoUploadSummary {
        var uploaded = 0

        try {
            for (photo in calfRegistrationDao.getPhotosAwaitingUpload()) {
                val file = File(photo.filePath)
                if (!file.isFile) continue

                val bytes = file.readBytes()

                when (val result = apiClient.uploadPhoto(photo.tagNumber, bytes)) {
                    PhotoUploadResult.Uploaded -> {
                        calfRegistrationDao.markPhotoUploaded(photo.mediaId)
                        uploaded++
                    }

                    is PhotoUploadResult.Rejected -> {
                        Log.w("CalfRegistrationRepository", "Photo for ${photo.tagNumber} refused: ${result.message}")
                        calfRegistrationDao.markPhotoUploadFailed(photo.mediaId, result.message)
                    }

                    is PhotoUploadResult.RetryLater -> {
                        Log.w("CalfRegistrationRepository", "Photo upload will retry: ${result.message}")
                        break
                    }
                }
            }
        } catch (exception: Exception) {
            Log.w("CalfRegistrationRepository", "Photo upload stopped unexpectedly.", exception)
        }

        return PhotoUploadSummary(uploaded = uploaded, pending = pendingPhotoCount())
    }

    /** Photos still waiting that can actually be sent (their file exists). */
    suspend fun pendingPhotoCount(): Int =
        try {
            calfRegistrationDao.getPhotosAwaitingUpload().count { File(it.filePath).isFile }
        } catch (_: Exception) {
            0
        }

    /*
     * BEEFTECH_CALF_PENDING_SOURCE_OF_TRUTH
     *
     * The calf-registration table is the source of truth.
     *
     * pending_sync is supporting bookkeeping only. A missing,
     * stale, or differently scoped queue row must never prevent
     * an actual PENDING calf from being synchronized.
     */
    /**
     * [retryRejected] is the user's manual retry: records the server rejected too many
     * times get a fresh set of attempts first. The background worker never sets it.
     */
    private suspend fun syncPendingRecords(retryRejected: Boolean): SyncPendingOutcome {

        return try {

            if (retryRejected) {
                calfRegistrationDao.requeueRejected()
            }

            val pendingOperations =
                pendingSyncRepository
                    .getPendingOperations()
                    .filter {
                        it.entityType == ENTITY_TYPE
                    }

            /*
             * Read actual unsynchronized calf registrations FIRST.
             */
            val views =
                calfRegistrationDao
                    .getPendingRegistrationViews()

            /*
             * Nothing genuinely pending:
             * remove stale queue entries and finish successfully.
             */
            if (views.isEmpty()) {

                pendingOperations.forEach {
                    pendingSyncRepository
                        .markSyncSuccessful(it.id)
                }

                return SyncPendingOutcome(
                    syncedCount = 0
                )
            }

            val pendingGuids =
                views
                    .map { it.recordGuid }
                    .toSet()

            /*
             * Remove queue rows whose underlying calf is already
             * synced or no longer exists.
             */
            pendingOperations
                .filter {
                    it.entityId !in pendingGuids
                }
                .forEach {
                    pendingSyncRepository
                        .markSyncSuccessful(it.id)
                }

            val queuedEntityIds =
                pendingOperations
                    .map { it.entityId }
                    .toMutableSet()

            /*
             * Repair missing queue rows automatically.
             */
            views.forEach { view ->

                if (
                    queuedEntityIds.add(
                        view.recordGuid
                    )
                ) {

                    pendingSyncRepository
                        .queueOperation(
                            entityType = ENTITY_TYPE,
                            entityId = view.recordGuid,
                            operation = "UPSERT",
                            payload = ""
                        )
                }
            }

            Log.i(
                "CalfRegistrationRepository",
                "Attempting synchronization for " +
                    "${views.size} pending calf registration(s)."
            )

            val syncResult = syncByDevice(views)

            if (syncResult.isSuccess) {

                val results =
                    syncResult
                        .getOrThrow()
                        .results

                val syncedGuids =
                    results
                        .filter {
                            it.status ==
                                SYNC_STATUS_SYNCED
                        }
                        .map {
                            it.recordguid
                        }
                        .toSet()

                if (syncedGuids.isNotEmpty()) {

                    calfRegistrationDao
                        .markSynced(
                            syncedGuids.toList(),
                            System.currentTimeMillis()
                        )

                    /*
                     * Re-read queue because missing entries may
                     * have been repaired above.
                     */
                    pendingSyncRepository
                        .getPendingOperations()
                        .filter {
                            it.entityType ==
                                ENTITY_TYPE &&
                                it.entityId in syncedGuids
                        }
                        .forEach {

                            pendingSyncRepository
                                .markSyncSuccessful(
                                    it.id
                                )
                        }
                }

                val resultByGuid =
                    results.associateBy {
                        it.recordguid
                    }

                val unsynced =
                    views.filter {
                        it.recordGuid !in syncedGuids
                    }

                /*
                 * Only an explicit server rejection counts toward the cap. A missing
                 * result or a transport failure is treated as temporary.
                 */
                unsynced.forEach { view ->
                    resultByGuid[view.recordGuid]?.let { result ->
                        calfRegistrationDao.recordRejection(
                            view.recordGuid,
                            result.message ?: "Rejected by the server.",
                            MAX_SERVER_REJECTIONS
                        )
                    }
                }

                val stillPendingGuids =
                    calfRegistrationDao
                        .getPendingRegistrationViews()
                        .map { it.recordGuid }
                        .toSet()

                val rejectedViews =
                    unsynced.filter {
                        it.recordGuid !in stillPendingGuids
                    }

                /* A rejected record leaves the queue; the calf table keeps the truth. */
                if (rejectedViews.isNotEmpty()) {
                    val rejectedGuids = rejectedViews.map { it.recordGuid }.toSet()

                    pendingSyncRepository
                        .getPendingOperations()
                        .filter {
                            it.entityType == ENTITY_TYPE &&
                                it.entityId in rejectedGuids
                        }
                        .forEach {
                            pendingSyncRepository.markSyncSuccessful(it.id)
                        }
                }

                val errors =
                    unsynced
                        .filter { it.recordGuid in stillPendingGuids }
                        .associate { view ->
                            view.tagNumber to (
                                resultByGuid[view.recordGuid]?.message
                                    ?: "Server did not confirm calf synchronization."
                            )
                        }

                val rejected =
                    rejectedViews.associate { view ->
                        view.tagNumber to (
                            resultByGuid[view.recordGuid]?.message
                                ?: "Rejected by the server."
                        )
                    }

                Log.i(
                    "CalfRegistrationRepository",
                    "Calf synchronization result: " +
                        "${syncedGuids.size} synced, " +
                        "${errors.size} still pending, " +
                        "${rejected.size} rejected."
                )

                SyncPendingOutcome(
                    syncedCount =
                        syncedGuids.size,

                    errorMessagesByTagNumber =
                        errors,

                    rejectedByTagNumber =
                        rejected
                )

            } else {

                val message =
                    syncResult
                        .exceptionOrNull()
                        ?.message
                        ?: "Unable to reach the server"

                Log.w(
                    "CalfRegistrationRepository",
                    "Calf synchronization request failed: $message"
                )

                SyncPendingOutcome(
                    syncedCount = 0,

                    errorMessagesByTagNumber =
                        views.associate {
                            it.tagNumber to message
                        }
                )
            }

        } catch (exception: Exception) {

            /*
             * Never silently convert a real synchronization
             * exception into a successful empty result.
             */
            Log.e(
                "CalfRegistrationRepository",
                "Pending calf synchronization failed unexpectedly.",
                exception
            )

            val remainingViews =
                try {
                    calfRegistrationDao
                        .getPendingRegistrationViews()
                } catch (_: Exception) {
                    emptyList()
                }

            val message =
                exception.message
                    ?: "Unexpected calf synchronization error."

            val errors =
                if (remainingViews.isNotEmpty()) {

                    remainingViews.associate {
                        it.tagNumber to message
                    }

                } else {

                    mapOf(
                        "_sync" to message
                    )
                }

            SyncPendingOutcome(
                syncedCount = 0,
                errorMessagesByTagNumber = errors
            )
        }
    }

    companion object {
        private const val ENTITY_TYPE = "CALF_REGISTRATION"

        /** Explicit server rejections after which a record stops retrying automatically. */
        const val MAX_SERVER_REJECTIONS = 3
        private const val GENDER_FEMALE = "Female"
        private const val GENDER_MALE = "Male"
    }
}

data class SaveCalfOutcome(
    val data: CalfRegistrationData,
    val syncErrorMessage: String? = null,
    val warnings: List<String> = emptyList(),
    val validationError: String? = null,
    /** The calf is synced but its photo has not reached the server yet. */
    val photoPending: Boolean = false,
    /** The server rejected the record enough times that automatic retries stopped. */
    val needsAttention: Boolean = false,
    /** The calf was NOT stored locally because of an unexpected failure. */
    val saveError: String? = null
)

data class ParentOptions(
    val dams: List<String> = emptyList(),
    val sires: List<String> = emptyList()
)

data class SyncPendingOutcome(
    val syncedCount: Int,
    /** Still pending: will be retried automatically. */
    val errorMessagesByTagNumber: Map<String, String?> = emptyMap(),
    /** Newly stopped after repeated server rejections: needs the user's attention. */
    val rejectedByTagNumber: Map<String, String> = emptyMap(),
    val photosUploaded: Int = 0,
    /** Photos for synced calves that still need uploading. */
    val photosPending: Int = 0
)

data class PhotoUploadSummary(
    val uploaded: Int = 0,
    val pending: Int = 0
)
