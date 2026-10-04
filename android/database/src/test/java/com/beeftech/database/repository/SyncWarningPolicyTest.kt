package com.beeftech.database.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SyncWarningPolicyTest {

    @Test
    fun `the default is 2, 4 and 6 and gives the levels the app always had`() {
        val policy = SyncWarningPolicy.DEFAULT

        assertEquals(listOf(2, 4, 6), policy.warningDays)
        assertEquals(
            listOf(0, 0, 1, 1, 2, 2, 3, 4, 4),
            listOf(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 30L).map { policy.levelFor(it) }
        )
    }

    @Test
    fun `custom days move the levels, and the wipe level always starts at day 7`() {
        val early = SyncWarningPolicy.sanitize(listOf(1, 2, 3))
        val late = SyncWarningPolicy.sanitize(listOf(4, 5, 6))

        assertEquals(listOf(0, 1, 2, 3, 3, 3, 3, 4), (0L..7L).map { early.levelFor(it) })
        assertEquals(listOf(0, 0, 0, 0, 1, 2, 3, 4), (0L..7L).map { late.levelFor(it) })
        assertEquals(4, SyncWarningPolicy.WIPE_LEVEL)
        assertEquals(7, SyncWarningPolicy.WIPE_DAY)
    }

    @Test
    fun `nothing a server sends can make the wipe level appear before day 7`() {
        val extremes = listOf(
            listOf(1, 2, 3), listOf(4, 5, 6), listOf(6, 6, 6), listOf(0, 1, 2), listOf(5, 6, 7),
            listOf(7, 8, 9), listOf(-1, 2, 3), listOf(1, 2), listOf(1, 2, 3, 4), emptyList(), listOf(100, 200, 300)
        )

        extremes.forEach { days ->
            val policy = SyncWarningPolicy.sanitize(days)
            (0L..6L).forEach { age ->
                assertEquals("$days at day $age", true, policy.levelFor(age) in 0..3)
            }
            assertEquals("$days at day 7", 4, policy.levelFor(7))
        }
    }

    @Test
    fun `an invalid set is replaced by the default, never partly used`() {
        listOf(
            null, emptyList(), listOf(2), listOf(2, 4), listOf(2, 4, 6, 7),
            listOf(4, 2, 6), listOf(2, 2, 6), listOf(6, 6, 6), listOf(3, 2, 1),
            listOf(0, 4, 6), listOf(2, 4, 7), listOf(2, 4, 8), listOf(-1, 4, 6)
        ).forEach { days ->
            assertSame("$days", SyncWarningPolicy.DEFAULT, SyncWarningPolicy.sanitize(days))
        }
    }

    @Test
    fun `a valid set is kept, and the edges are valid`() {
        assertEquals(listOf(1, 2, 3), SyncWarningPolicy.sanitize(listOf(1, 2, 3)).warningDays)
        assertEquals(listOf(4, 5, 6), SyncWarningPolicy.sanitize(listOf(4, 5, 6)).warningDays)
        assertEquals(listOf(1, 3, 5), SyncWarningPolicy.sanitize(listOf(1, 3, 5)).warningDays)
    }

    @Test
    fun `the stored form round trips and anything unreadable gives the default`() {
        assertEquals("1,3,5", SyncWarningPolicy.sanitize(listOf(1, 3, 5)).serialize())
        assertEquals(listOf(1, 3, 5), SyncWarningPolicy.parse("1,3,5").warningDays)
        assertEquals(listOf(1, 3, 5), SyncWarningPolicy.parse(" 1 , 3 , 5 ").warningDays)

        listOf(null, "", "x", "1,2", "1,x,3", "9,9,9", "2,4,7", "2,4,6,7", "[1,2,3]").forEach {
            assertSame("'$it'", SyncWarningPolicy.DEFAULT, SyncWarningPolicy.parse(it))
        }
    }

    @Test
    fun `two policies with the same days are equal`() {
        assertEquals(SyncWarningPolicy.DEFAULT, SyncWarningPolicy.sanitize(listOf(2, 4, 6)))
        assertEquals(SyncWarningPolicy.DEFAULT.hashCode(), SyncWarningPolicy.parse("2,4,6").hashCode())
    }
}
