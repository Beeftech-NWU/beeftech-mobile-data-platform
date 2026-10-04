package com.beeftech.management.data

import com.beeftech.database.dao.CostTypeValue
import com.beeftech.database.dao.ReferenceDataDao
import com.beeftech.database.dao.ReferenceSnapshot
import com.beeftech.database.dao.ReferenceValue
import com.beeftech.database.entity.DeviceConfigEntry

sealed interface ReferenceSyncOutcome {
    /* The device already had the current version. */
    data class UpToDate(val version: Long) : ReferenceSyncOutcome

    data class Updated(val version: Long) : ReferenceSyncOutcome

    /* No connection, no token, or the server refused: leave the cache as it was and try again later. */
    data class Failed(val reason: String) : ReferenceSyncOutcome
}

/**
 * Pulls the server's reference data into the device cache. It asks for "anything newer than the
 * version I have", so an unchanged server costs one tiny request, and applies a new snapshot in a
 * single transaction (see [ReferenceDataDao.apply]), which never deletes.
 */
class ReferenceDataSync(
    private val apiClient: ManagementApiClient,
    private val dao: ReferenceDataDao,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun pull(): ReferenceSyncOutcome {
        val have = dao.getConfig(DeviceConfigEntry.REFERENCE_DATA_VERSION)?.toLongOrNull()

        return when (val result = apiClient.referenceData(have)) {
            is ManagementResult.Success -> {
                val snapshot = result.value
                if (snapshot.unchanged) {
                    ReferenceSyncOutcome.UpToDate(snapshot.version)
                } else {
                    dao.apply(snapshot.toDatabaseSnapshot(), now())
                    ReferenceSyncOutcome.Updated(snapshot.version)
                }
            }
            is ManagementResult.NoConnection -> ReferenceSyncOutcome.Failed("No connection")
            is ManagementResult.Unauthorized -> ReferenceSyncOutcome.Failed("Signed out")
            is ManagementResult.Forbidden -> ReferenceSyncOutcome.Failed(result.message)
            is ManagementResult.NotFound -> ReferenceSyncOutcome.Failed("The server has no reference data yet")
            is ManagementResult.Rejected -> ReferenceSyncOutcome.Failed(result.message)
            is ManagementResult.Error -> ReferenceSyncOutcome.Failed(result.message)
        }
    }
}

/* A list the server left out (an older server) is an empty one: nothing is added or changed. */
internal fun ReferenceSnapshotDto.toDatabaseSnapshot() = ReferenceSnapshot(
    version = version,
    diseases = diseases.orEmpty().map { ReferenceValue(it.id, it.name, it.active) },
    treatmentTypes = treatmentTypes.orEmpty().map { ReferenceValue(it.id, it.name, it.active) },
    costTypes = costTypes.orEmpty().map { CostTypeValue(it.code, it.displayName, it.sortOrder, it.active) }
)
