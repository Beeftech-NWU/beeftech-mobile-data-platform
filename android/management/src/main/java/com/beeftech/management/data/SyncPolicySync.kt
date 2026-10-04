package com.beeftech.management.data

import com.beeftech.database.repository.SyncPolicyStore

sealed interface PolicySyncOutcome {
    /* The device already has this version of the policy. */
    data class UpToDate(val version: Long) : PolicySyncOutcome

    data class Updated(val version: Long) : PolicySyncOutcome

    /* No connection, no token, or the server refused: the stored policy stays as it was. */
    data class Failed(val reason: String) : PolicySyncOutcome
}

/**
 * Pulls the sync policy and stores its warning days on the device. The days are sanitized before
 * they are stored (see SyncWarningPolicy), and nothing here can change when data is wiped: the
 * server's wipe day is informational and is never read.
 */
class SyncPolicySync(
    private val apiClient: ManagementApiClient,
    private val store: SyncPolicyStore,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun pull(): PolicySyncOutcome =
        when (val result = apiClient.syncPolicy()) {
            is ManagementResult.Success -> {
                val policy = result.value
                if (store.version() == policy.version) {
                    PolicySyncOutcome.UpToDate(policy.version)
                } else {
                    store.save(policy.warningDays, policy.version, now())
                    PolicySyncOutcome.Updated(policy.version)
                }
            }
            is ManagementResult.NoConnection -> PolicySyncOutcome.Failed("No connection")
            is ManagementResult.Unauthorized -> PolicySyncOutcome.Failed("Signed out")
            is ManagementResult.Forbidden -> PolicySyncOutcome.Failed(result.message)
            is ManagementResult.NotFound -> PolicySyncOutcome.Failed("The server has no sync policy yet")
            is ManagementResult.Rejected -> PolicySyncOutcome.Failed(result.message)
            is ManagementResult.Error -> PolicySyncOutcome.Failed(result.message)
        }
}
