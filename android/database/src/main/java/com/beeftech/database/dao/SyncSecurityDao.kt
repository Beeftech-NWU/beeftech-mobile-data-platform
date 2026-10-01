package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.entity.SyncPolicyState
import com.beeftech.database.entity.SyncSecurityEvent

@Dao
abstract class SyncSecurityDao {

    // ========================================================
    // Audit events
    // ========================================================

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    abstract suspend fun insertEvent(
        event: SyncSecurityEvent
    ): Long

    @Query(
        """
        SELECT *
        FROM sync_security_events
        ORDER BY event_time DESC
        """
    )
    abstract suspend fun getAllEvents():
            List<SyncSecurityEvent>


    // ========================================================
    // Persistent Day-7 lock
    // ========================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    abstract suspend fun savePolicyState(
        state: SyncPolicyState
    )

    @Query(
        """
        SELECT *
        FROM sync_policy_state
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    abstract suspend fun getPolicyState(
        userId: String
    ): SyncPolicyState?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM sync_policy_state
            WHERE user_id = :userId
              AND locked = 1
        )
        """
    )
    abstract suspend fun isLocked(
        userId: String
    ): Boolean

    /*
     * This must only be called by the administrator recovery
     * workflow. Day-7 enforcement never unlocks automatically.
     */
    @Query(
        """
        UPDATE sync_policy_state
        SET
            locked = 0,
            locked_at = NULL,
            lock_reason = NULL
        WHERE user_id = :userId
        """
    )
    abstract suspend fun unlockByAdministrator(
        userId: String
    ): Int


    // ========================================================
    // Animal Movement
    // ========================================================

    @Query(
        """
        DELETE FROM animal_movements
        WHERE record_guid = :recordGuid
          AND sync_status != 'SYNCED'
        """
    )
    protected abstract suspend fun deleteUnsyncedMovement(
        recordGuid: String
    ): Int


    // ========================================================
    // Treatment
    //
    // A Treatment can create a derived animal_costs row.
    // Never leave that derived cost behind after wiping its
    // unsynced treatment.
    // ========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM treatments
        WHERE record_guid = :recordGuid
          AND syncStatus != 'SYNCED'
        """
    )
    protected abstract suspend fun unsyncedTreatmentExists(
        recordGuid: String
    ): Int

    @Query(
        """
        DELETE FROM animal_costs
        WHERE source_entity = 'TREATMENT'
          AND source_record_id = :recordGuid
        """
    )
    protected abstract suspend fun deleteTreatmentDerivedCost(
        recordGuid: String
    ): Int

    @Query(
        """
        DELETE FROM treatments
        WHERE record_guid = :recordGuid
          AND syncStatus != 'SYNCED'
        """
    )
    protected abstract suspend fun deleteUnsyncedTreatment(
        recordGuid: String
    ): Int


    // ========================================================
    // Farmer Registration
    //
    // farmer_addresses and farmer_roles use CASCADE from
    // farmers, so deleting an unsynced farmer also removes
    // its local registration details.
    // ========================================================

    @Query(
        """
        DELETE FROM farmers
        WHERE farmer_id = :farmerId
          AND sync_status != 'SYNCED'
        """
    )
    protected abstract suspend fun deleteUnsyncedFarmer(
        farmerId: String
    ): Int


    // ========================================================
    // Calf Registration
    //
    // The pending-sync ID is calf_registrations.record_guid.
    // registered_animal_id points to the locally created animal.
    //
    // Deleting the unsynced Animal cascades its identifiers,
    // media and calf registration graph.
    // ========================================================

    @Query(
        """
        SELECT registered_animal_id
        FROM calf_registrations
        WHERE record_guid = :recordGuid
          AND sync_status != 'SYNCED'
        LIMIT 1
        """
    )
    protected abstract suspend fun findUnsyncedCalfAnimalId(
        recordGuid: String
    ): String?

    @Query(
        """
        DELETE FROM calf_registrations
        WHERE record_guid = :recordGuid
          AND sync_status != 'SYNCED'
        """
    )
    protected abstract suspend fun deleteUnsyncedCalfRegistration(
        recordGuid: String
    ): Int

    @Query(
        """
        DELETE FROM animals
        WHERE animalId = :animalId
          AND syncStatus != 'SYNCED'
        """
    )
    protected abstract suspend fun deleteUnsyncedCalfAnimal(
        animalId: String
    ): Int


    // ========================================================
    // Pending queue
    // ========================================================

    @Query(
        """
        DELETE FROM pending_sync
        WHERE id = :pendingId
        """
    )
    protected abstract suspend fun deletePendingQueueItem(
        pendingId: Long
    ): Int


    // ========================================================
    // Atomic Day-7 action
    // ========================================================

    @Transaction
    open suspend fun enforceDay7(
        userId: String,
        pendingRecords: List<PendingSync>,
        oldestPendingCreatedAt: Long,
        now: Long
    ): Int {

        /*
         * Do not run destructive work twice.
         */
        if (isLocked(userId)) {
            return 0
        }

        insertEvent(
            SyncSecurityEvent(
                eventKey =
                    "$userId:DAY7_TRIGGER:$oldestPendingCreatedAt",

                userId =
                    userId,

                eventType =
                    EVENT_DAY_7_TRIGGERED,

                eventTime =
                    now,

                warningDay =
                    7,

                pendingCount =
                    pendingRecords.size,

                oldestPendingCreatedAt =
                    oldestPendingCreatedAt,

                details =
                    "Day 7 unsynced-data policy triggered."
            )
        )

        var wipedOperations = 0

        for (pending in pendingRecords) {

            when (pending.entityType) {

                ENTITY_CALF_REGISTRATION -> {

                    val animalId =
                        findUnsyncedCalfAnimalId(
                            pending.entityId
                        )

                    if (animalId != null) {

                        deleteUnsyncedCalfRegistration(
                            pending.entityId
                        )

                        /*
                         * Only an unsynced Animal can be removed.
                         *
                         * Foreign-key CASCADE handles the calf's
                         * locally-created identifier/media graph.
                         */
                        deleteUnsyncedCalfAnimal(
                            animalId
                        )
                    }
                }

                ENTITY_ANIMAL_MOVEMENT -> {

                    deleteUnsyncedMovement(
                        pending.entityId
                    )
                }

                ENTITY_TREATMENT -> {

                    if (
                        unsyncedTreatmentExists(
                            pending.entityId
                        ) > 0
                    ) {

                        deleteTreatmentDerivedCost(
                            pending.entityId
                        )

                        deleteUnsyncedTreatment(
                            pending.entityId
                        )
                    }
                }

                ENTITY_FARMER_REGISTRATION -> {

                    deleteUnsyncedFarmer(
                        pending.entityId
                    )
                }

                else -> {

                    /*
                     * Never silently throw away an unknown queue
                     * operation. If another module adds a new sync
                     * type later, Day-7 code must explicitly learn
                     * how to wipe that record safely.
                     */
                    throw IllegalStateException(
                        "Unsupported pending-sync entity type: " +
                                pending.entityType
                    )
                }
            }

            /*
             * Delete the queue item only after its associated
             * local record has been handled.
             */
            deletePendingQueueItem(
                pending.id
            )

            wipedOperations++
        }

        /*
         * Persist the lock in the same Room transaction.
         */
        savePolicyState(
            SyncPolicyState(
                userId =
                    userId,

                locked =
                    true,

                lockedAt =
                    now,

                lockReason =
                    DAY_7_LOCK_REASON
            )
        )

        insertEvent(
            SyncSecurityEvent(
                eventKey =
                    "$userId:DAY7_WIPE:$oldestPendingCreatedAt",

                userId =
                    userId,

                eventType =
                    EVENT_DAY_7_WIPE,

                eventTime =
                    now,

                warningDay =
                    7,

                pendingCount =
                    wipedOperations,

                oldestPendingCreatedAt =
                    oldestPendingCreatedAt,

                details =
                    "$wipedOperations pending local operation(s) removed."
            )
        )

        insertEvent(
            SyncSecurityEvent(
                eventKey =
                    "$userId:DAY7_LOCK:$oldestPendingCreatedAt",

                userId =
                    userId,

                eventType =
                    EVENT_ACCOUNT_LOCKED,

                eventTime =
                    now,

                warningDay =
                    7,

                pendingCount =
                    0,

                oldestPendingCreatedAt =
                    oldestPendingCreatedAt,

                details =
                    DAY_7_LOCK_REASON
            )
        )

        return wipedOperations
    }


    companion object {

        const val ENTITY_CALF_REGISTRATION =
            "CALF_REGISTRATION"

        const val ENTITY_ANIMAL_MOVEMENT =
            "ANIMAL_MOVEMENT"

        const val ENTITY_TREATMENT =
            "TREATMENT"

        const val ENTITY_FARMER_REGISTRATION =
            "FARMER_REGISTRATION"

        const val EVENT_WARNING =
            "SYNC_WARNING"

        const val EVENT_DAY_7_TRIGGERED =
            "DAY_7_TRIGGERED"

        const val EVENT_DAY_7_WIPE =
            "DAY_7_WIPE"

        const val EVENT_ACCOUNT_LOCKED =
            "SYNC_POLICY_ACCOUNT_LOCKED"

        const val DAY_7_LOCK_REASON =
            "Unsynced local data exceeded the 7-day policy. Administrator intervention is required."
    }
}
