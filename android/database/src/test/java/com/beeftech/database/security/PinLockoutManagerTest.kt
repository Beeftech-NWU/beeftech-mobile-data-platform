package com.beeftech.database.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinLockoutManagerTest {

    private lateinit var lockoutManager: PinLockoutManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lockoutManager = PinLockoutManager(context)
        lockoutManager.resetAttempts()
    }

    @Test
    fun testLockoutTriggeredAfterMaxFailedAttempts() {
        repeat(5) { lockoutManager.recordFailedAttempt() }
        assertTrue(lockoutManager.isLockedOut())
    }

    @Test
    fun testCooldownTimeRemaining() {
        repeat(5) { lockoutManager.recordFailedAttempt() }
        val secondsLeft = lockoutManager.getRemainingCooldownSeconds()
        assertTrue(secondsLeft in 1..60)
    }

    @Test
    fun testResetClearsLockoutState() {
        repeat(5) { lockoutManager.recordFailedAttempt() }
        lockoutManager.resetAttempts()
        assertFalse(lockoutManager.isLockedOut())
    }
}