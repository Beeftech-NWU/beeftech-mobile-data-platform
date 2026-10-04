package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.SyncPolicyState
import com.beeftech.database.entity.SyncSecurityEvent
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/*
 * The real queries behind the security-event upload (Phase 4f): which events are waiting, that
 * an upload only flags them, and that an administrator's unlock lifts the lock.
 */
@RunWith(AndroidJUnit4::class)
class SyncSecurityUploadDaoTest {

    private lateinit var context: Context

    @Before
    fun setUp() {

        context =
            ApplicationProvider.getApplicationContext()

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)

        val result =
            DatabaseProvider.initialize(
                context = context,
                passphrase = ByteArray(32) { (it + 1).toByte() }
            )

        check(result is DatabaseResult.Success) {
            "Database could not be initialized."
        }
    }

    @After
    fun tearDown() {

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)
    }

    private val dao get() = DatabaseProvider.getDatabase()!!.syncSecurityDao()

    private suspend fun record(key: String, user: String?, time: Long) {
        dao.insertEvent(
            SyncSecurityEvent(
                eventKey = key,
                userId = user,
                eventType = "SYNC_WARNING",
                eventTime = time,
                warningDay = 2,
                pendingCount = 3
            )
        )
    }

    @Test
    fun waitingEventsAreTheUsersOwnAndOldestFirst() = runBlocking {

        record("late", "u1", 3_000)
        record("early", "u1", 1_000)
        record("theirs", "u2", 2_000)
        record("nobody", null, 2_500)

        val waiting = dao.getNotUploaded("u1", 10)

        assertEquals(listOf("early", "nobody", "late"), waiting.map { it.eventKey })
        assertEquals(listOf("early"), dao.getNotUploaded("u1", 1).map { it.eventKey })
    }

    @Test
    fun uploadingOnlyFlagsTheEventsAndNeverChangesOrDeletesThem() = runBlocking {

        record("a", "u1", 1_000)
        record("b", "u1", 2_000)
        val ids = dao.getNotUploaded("u1", 10).map { it.id }

        assertEquals(2, dao.markUploaded(ids, 5_000))

        assertTrue(dao.getNotUploaded("u1", 10).isEmpty())
        val all = dao.getAllEvents()
        assertEquals(2, all.size)
        assertEquals(listOf(5_000L, 5_000L), all.map { it.uploadedAt })
        assertEquals(listOf("b", "a"), all.map { it.eventKey })
        assertEquals(3, all.first().pendingCount)

        /* Flagging again changes nothing: the first acknowledgement stands. */
        assertEquals(0, dao.markUploaded(ids, 9_000))
        assertEquals(listOf(5_000L, 5_000L), dao.getAllEvents().map { it.uploadedAt })
    }

    @Test
    fun anotherUsersEventStaysWaitingUntilTheyUploadIt() = runBlocking {

        record("mine", "u1", 1_000)
        record("theirs", "u2", 1_000)

        dao.markUploaded(dao.getNotUploaded("u1", 10).map { it.id }, 5_000)

        assertEquals(listOf("theirs"), dao.getNotUploaded("u2", 10).map { it.eventKey })
        assertNull(dao.getAllEvents().first { it.eventKey == "theirs" }.uploadedAt)
    }

    @Test
    fun anAdministratorUnlockLiftsTheLockAndKeepsTheWhenAndWhy() = runBlocking {

        dao.savePolicyState(SyncPolicyState("u1", locked = true, lockedAt = 1_000, lockReason = "Day 7"))
        dao.savePolicyState(SyncPolicyState("u2", locked = true, lockedAt = 1_000, lockReason = "Day 7"))

        assertEquals(1, dao.unlockByAdministrator("u1"))

        assertFalse(dao.isLocked("u1"))
        assertTrue(dao.isLocked("u2"))
        assertNull(dao.getPolicyState("u1")!!.lockedAt)
    }

    companion object {

        private const val DATABASE_NAME = "beeftech.db"
    }
}
