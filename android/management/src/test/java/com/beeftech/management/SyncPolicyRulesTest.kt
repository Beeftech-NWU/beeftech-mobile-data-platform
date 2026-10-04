package com.beeftech.management

import com.beeftech.management.data.SyncPolicyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPolicyRulesTest {

    @Test
    fun `three increasing days from 1 to 6 are valid`() {
        listOf(listOf(2, 4, 6), listOf(1, 2, 3), listOf(4, 5, 6), listOf(1, 3, 6)).forEach {
            assertNull("$it", SyncPolicyRules.warningDaysError(it))
        }
    }

    @Test
    fun `wrong counts, blanks, out-of-range and non-increasing days are refused`() {
        listOf(
            emptyList(), listOf(2, 4), listOf(2, 4, 6, 7), listOf(2, null, 6), listOf(null, null, null),
            listOf(0, 4, 6), listOf(2, 4, 7), listOf(2, 4, 8), listOf(-1, 4, 6),
            listOf(4, 2, 6), listOf(2, 2, 6), listOf(6, 6, 6), listOf(3, 2, 1)
        ).forEach {
            assertNotNull("$it", SyncPolicyRules.warningDaysError(it))
        }
    }

    @Test
    fun `day 7 is refused with a reminder that it is the wipe day`() {
        assertTrue(SyncPolicyRules.warningDaysError(listOf(2, 4, 7))!!.contains("wiped on day 7"))
        assertEquals(7, SyncPolicyRules.WIPE_DAY)
    }

    @Test
    fun `stale hours need to be 12 to 336`() {
        assertNull(SyncPolicyRules.staleHoursError(12))
        assertNull(SyncPolicyRules.staleHoursError(48))
        assertNull(SyncPolicyRules.staleHoursError(336))
        listOf(null, 0, 11, 337, 5000, -3).forEach { assertNotNull("$it", SyncPolicyRules.staleHoursError(it)) }
    }
}
