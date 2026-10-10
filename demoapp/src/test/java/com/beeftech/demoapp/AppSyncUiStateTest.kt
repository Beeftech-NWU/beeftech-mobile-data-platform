package com.beeftech.demoapp

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSyncUiStateTest {

    @Test
    fun `offline takes priority so the user knows why sync is waiting`() {
        assertEquals(
            SyncTone.OFFLINE,
            appSyncUiState(isOnline = false, pendingCount = 3, failedCount = 1).tone
        )
    }

    @Test
    fun `retry limit becomes needs attention when online`() {
        assertEquals(
            SyncTone.FAILED,
            appSyncUiState(isOnline = true, pendingCount = 3, failedCount = 1).tone
        )
    }

    @Test
    fun `pending rows show waiting`() {
        assertEquals(
            SyncTone.WAITING,
            appSyncUiState(isOnline = true, pendingCount = 2, failedCount = 0).tone
        )
    }

    @Test
    fun `empty online queue shows synced`() {
        assertEquals(
            SyncTone.SYNCED,
            appSyncUiState(isOnline = true, pendingCount = 0, failedCount = 0).tone
        )
    }
}
