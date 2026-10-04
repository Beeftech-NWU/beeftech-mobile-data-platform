package com.beeftech.management

import com.beeftech.database.entity.SyncPolicyState
import com.beeftech.management.data.SyncLockRelease
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncLockReleaseTest {

    private val dao = FakeSyncSecurityDao()
    private val release = SyncLockRelease(dao, "u1")

    private fun locked(at: Long = 1_000) {
        dao.states["u1"] = SyncPolicyState("u1", locked = true, lockedAt = at, lockReason = "Day 7")
    }

    @Test
    fun `a clearance after the lock lifts it`() = runTest {
        locked(at = 1_000)

        assertTrue(release.apply(clearedAt = 2_000))

        assertFalse(dao.isLocked("u1"))
    }

    @Test
    fun `a clearance older than the lock does not undo a newer lock`() = runTest {
        locked(at = 5_000)

        assertFalse(release.apply(clearedAt = 2_000))
        assertFalse(release.apply(clearedAt = 5_000))

        assertTrue(dao.isLocked("u1"))
    }

    @Test
    fun `never cleared leaves the lock alone`() = runTest {
        locked()

        assertFalse(release.apply(clearedAt = null))

        assertTrue(dao.isLocked("u1"))
    }

    @Test
    fun `an account that is not locked is left as it is`() = runTest {
        assertFalse(release.apply(clearedAt = 9_000))
        dao.states["u1"] = SyncPolicyState("u1", locked = false)
        assertFalse(release.apply(clearedAt = 9_000))

        assertEquals(SyncPolicyState("u1", locked = false), dao.states["u1"])
    }

    @Test
    fun `it only touches the signed-in user`() = runTest {
        locked()
        dao.states["u2"] = SyncPolicyState("u2", locked = true, lockedAt = 1_000, lockReason = "Day 7")

        release.apply(clearedAt = 2_000)

        assertTrue(dao.isLocked("u2"))
    }
}
