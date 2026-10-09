package com.beeftech.farmerregistration

import org.junit.Assert.assertEquals
import org.junit.Test

class FarmerSyncPillTest {

    @Test
    fun `each sync status maps to its pill`() {
        assertEquals(FarmerSyncPill.PENDING_SYNC, FarmerSyncPill.forStatus("PENDING"))
        assertEquals(FarmerSyncPill.PROCESSING, FarmerSyncPill.forStatus("PROCESSING"))
        assertEquals(FarmerSyncPill.REGISTERED, FarmerSyncPill.forStatus("SYNCED"))
    }

    @Test
    fun `unknown or missing statuses show as pending sync`() {
        assertEquals(FarmerSyncPill.PENDING_SYNC, FarmerSyncPill.forStatus("FAILED"))
        assertEquals(FarmerSyncPill.PENDING_SYNC, FarmerSyncPill.forStatus(null))
        assertEquals(FarmerSyncPill.PENDING_SYNC, FarmerSyncPill.forStatus(""))
        assertEquals(FarmerSyncPill.REGISTERED, FarmerSyncPill.forStatus(" synced "))
    }

    @Test
    fun `pill labels match the requirement`() {
        assertEquals(
            listOf("Pending Sync", "Processing", "Registered"),
            FarmerSyncPill.entries.map { it.label }
        )
    }
}
