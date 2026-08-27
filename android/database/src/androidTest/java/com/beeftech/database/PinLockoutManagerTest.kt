package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.beeftech.database.security.PinLockoutManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinLockoutManagerTest {

    private lateinit var lockoutManager: PinLockoutManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        lockoutManager = PinLockoutManager(context)
        lockoutManager.resetAttempts()
    }

    @After
    fun cleanup() {
        lockoutManager.resetAttempts()
    }

    @Test
    fun initialFailedAttempts_isZero() {
        assertEquals(0, lockoutManager.getFailedAttempts())
    }

    @Test
    fun recordFailedAttempt_increasesCount() {
        val attempts = lockoutManager.recordFailedAttempt()

        assertEquals(1, attempts)
        assertEquals(1, lockoutManager.getFailedAttempts())
    }

    @Test
    fun fewerThanFiveAttempts_doesNotLockUser() {
        repeat(4) {
            lockoutManager.recordFailedAttempt()
        }

        assertEquals(4, lockoutManager.getFailedAttempts())
        assertFalse(lockoutManager.isLockedOut())
    }

    @Test
    fun fiveFailedAttempts_locksUser() {
        repeat(5) {
            lockoutManager.recordFailedAttempt()
        }

        assertEquals(5, lockoutManager.getFailedAttempts())
        assertTrue(lockoutManager.isLockedOut())
    }

    @Test
    fun resetAttempts_clearsFailedAttempts() {
        repeat(5) {
            lockoutManager.recordFailedAttempt()
        }

        assertTrue(lockoutManager.isLockedOut())

        lockoutManager.resetAttempts()

        assertEquals(0, lockoutManager.getFailedAttempts())
        assertFalse(lockoutManager.isLockedOut())
    }

    @Test
    fun lockedUser_hasRemainingLockoutTime() {
        repeat(5) {
            lockoutManager.recordFailedAttempt()
        }

        val remaining = lockoutManager.getRemainingLockoutTimeMs()

        assertTrue(remaining > 0)
        assertTrue(remaining <= 60_000L)
    }

    @Test
    fun unlockedUser_hasNoRemainingLockoutTime() {
        assertFalse(lockoutManager.isLockedOut())
        assertEquals(0L, lockoutManager.getRemainingLockoutTimeMs())
    }
}