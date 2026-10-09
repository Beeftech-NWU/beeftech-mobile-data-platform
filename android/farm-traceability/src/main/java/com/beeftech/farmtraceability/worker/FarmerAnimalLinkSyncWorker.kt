package com.beeftech.farmtraceability.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.SyncBatchEntity
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.farmtraceability.data.FarmerAnimalLinkApiClient
import com.beeftech.farmtraceability.data.LinkUploadResult
import kotlinx.coroutines.CancellationException
import java.util.UUID

/** Uploads immutable link revisions and reassignment closures without losing offline records. */
class FarmerAnimalLinkSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = DatabaseProvider.getDatabase() ?: run {
            Log.w("FarmerAnimalLinkSync", "Database unavailable; retrying")
            return Result.retry()
        }
        val tokenProvider = TokenProviderRegistry.get() ?: run {
            Log.w("FarmerAnimalLinkSync", "Token provider unavailable; retrying")
            return Result.retry()
        }
        val dao = db.farmerAnimalLinkDao()
        val api = FarmerAnimalLinkApiClient(tokenProvider)
        return try {
            val pending = dao.pendingUploads()
            var deferredCount = 0
            var acknowledgedCount = 0
            var localHistoryCount = 0
            // DAO sorts earlier revisions before later assignments. Close old links first.
            for (link in pending) {
                val outcome = try {
                    api.upload(link)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    Log.w("FarmerAnimalLinkSync", "Assignment upload deferred; continuing queue", exception)
                    LinkUploadResult.DEFERRED
                }
                when (outcome) {
                    LinkUploadResult.STORED -> {
                        val updated = dao.markSyncedIfUnchanged(
                            link.linkId, link.recordGuid, link.effectiveFrom, link.effectiveTo
                        )
                        if (updated > 0) acknowledgedCount++
                    }
                    LinkUploadResult.LOCAL_HISTORY -> {
                        // Only an exact, unchanged, ended revision can leave the upload queue.
                        val endedAt = link.effectiveTo
                        if (endedAt != null && dao.markLocalHistoryIfUnchanged(
                                link.linkId, link.recordGuid, link.effectiveFrom, endedAt
                            ) > 0) {
                            localHistoryCount++
                        } else {
                            deferredCount++
                        }
                    }
                    LinkUploadResult.DEFERRED -> deferredCount++
                }
            }
            // Do not update 'last successful upload' for local-only history.
            // The shared device sync-batch stream drives Home's timestamp.
            if (acknowledgedCount > 0) {
                db.syncBatchDao().insert(
                    SyncBatchEntity(UUID.randomUUID().toString(), System.currentTimeMillis())
                )
            }
            val remaining = dao.pendingUploads().size
            Log.i(
                "FarmerAnimalLinkSync",
                "Queue processed: acknowledged=$acknowledgedCount, localHistory=$localHistoryCount, deferred=$deferredCount, remaining=$remaining"
            )
            if (remaining == 0) Result.success() else Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Log.w("FarmerAnimalLinkSync", "Assignment upload deferred", exception)
            Result.retry()
        }
    }
}
