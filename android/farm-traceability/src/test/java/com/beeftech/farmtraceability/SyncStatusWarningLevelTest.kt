package com.beeftech.farmtraceability

import com.beeftech.database.repository.SyncWarningPolicy
import com.beeftech.farmtraceability.viewmodel.SyncStatusViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

/* The status card and the lock screen share one set of levels, so they can never disagree. */
class SyncStatusWarningLevelTest {

    private fun level(pending: Int, ageDays: Long, policy: SyncWarningPolicy = SyncWarningPolicy.DEFAULT) =
        SyncStatusViewModel.calculateWarningLevel(pending, ageDays, policy)

    @Test
    fun `with the default policy the levels are what they always were`() {
        assertEquals(
            listOf(0, 0, 1, 1, 2, 2, 3, 4),
            (0L..7L).map { level(3, it) }
        )
    }

    @Test
    fun `nothing pending means no warning at any age`() {
        assertEquals(listOf(0, 0, 0, 0), listOf(0L, 3L, 6L, 30L).map { level(0, it) })
    }

    @Test
    fun `a custom policy moves the warnings and the card follows`() {
        val custom = SyncWarningPolicy.sanitize(listOf(1, 3, 5))

        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4), (0L..7L).map { level(2, it, custom) })
    }

    @Test
    fun `an invalid policy from the server shows the default levels`() {
        val broken = SyncWarningPolicy.sanitize(listOf(9, 9, 9))

        assertEquals((0L..7L).map { level(2, it) }, (0L..7L).map { level(2, it, broken) })
    }

    @Test
    fun `the wipe level is day 7 whatever the policy`() {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(2, 4, 6)).forEach {
            val policy = SyncWarningPolicy.sanitize(it)
            assertEquals("$it", 4, level(1, 7, policy))
            assertEquals("$it", 4, level(1, 30, policy))
            assertEquals("$it at day 6", true, level(1, 6, policy) <= 3)
        }
    }
}
