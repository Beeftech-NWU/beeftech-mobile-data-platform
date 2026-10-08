package com.beeftech.database.util

import com.beeftech.database.security.SyncIdentityRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchNamingTest {

    @After
    fun tearDown() = SyncIdentityRegistry.clear()

    @Test
    fun `names the batch from the signed-in farm code and device`() {
        SyncIdentityRegistry.set("BF01", "MOB_DEV_a1b2c3d4")

        val name = BatchNaming.nameFor(ProjectCode.COST, nowMillis = 1_791_468_309_000L)!!

        assertTrue(name, FileNamingUtils.validate(name))
        val parts = FileNamingUtils.parse(name)!!
        assertEquals("BF01", parts.farmCode)
        assertEquals("COST", parts.project)
        assertEquals("MOB_DEV_a1b2c3d4", parts.deviceId)
    }

    @Test
    fun `gives no name until both the farm code and the device are known`() {
        SyncIdentityRegistry.clear()
        assertNull(BatchNaming.nameFor(ProjectCode.COST))

        SyncIdentityRegistry.set("BF01", null)
        assertNull(BatchNaming.nameFor(ProjectCode.COST))

        SyncIdentityRegistry.set(null, "MOB_DEV_1")
        assertNull(BatchNaming.nameFor(ProjectCode.COST))
    }

    @Test
    fun `a malformed farm code gives no name instead of failing the sync`() {
        SyncIdentityRegistry.set("bad", "MOB_DEV_1")

        assertNull(BatchNaming.nameFor(ProjectCode.COST))
    }
}
