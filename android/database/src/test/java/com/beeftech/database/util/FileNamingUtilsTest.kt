package com.beeftech.database.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class FileNamingUtilsTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // 2026-10-08T14:05:09Z
    private val instant = 1_791_468_309_000L

    @Test
    fun `builds the name from farm code, project, local time and device`() {
        val name = FileNamingUtils.build("BF01", ProjectCode.CALF_REG, instant, "MOB_DEV_a1b2c3d4", timeZone = utc)

        assertEquals("BF01-CALF_REG-20261008-140509-MOB_DEV_a1b2c3d4", name)
        assertTrue(FileNamingUtils.validate(name))
    }

    @Test
    fun `uses the given time zone and lower-cases the extension`() {
        val name = FileNamingUtils.build(
            "BF01", ProjectCode.REPORT, instant, "SERVER", ".CSV", TimeZone.getTimeZone("GMT+2")
        )

        assertEquals("BF01-REPORT-20261008-160509-SERVER.csv", name)
    }

    @Test
    fun `cleans the device id and rejects a bad farm code or empty device`() {
        assertEquals(
            "S001-COST-20261008-140509-dev_1",
            FileNamingUtils.build("S001", ProjectCode.COST, instant, "dev-1!", timeZone = utc)
        )
        assertThrows(IllegalArgumentException::class.java) {
            FileNamingUtils.build("bf01", ProjectCode.COST, instant, "dev", timeZone = utc)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FileNamingUtils.build("BF01", ProjectCode.COST, instant, "---", timeZone = utc)
        }
    }

    @Test
    fun `validate accepts only the agreed pattern`() {
        assertTrue(FileNamingUtils.validate("BF01-MORTALITY-20261008-140509-MOB_DEV_1"))
        assertTrue(FileNamingUtils.validate("BF01-BREED_2026-20261008-140509-MOB_DEV_1"))
        assertFalse(FileNamingUtils.validate("BF01-breed_2026-20261008-140509-MOB_DEV_1"))
        assertFalse(FileNamingUtils.validate("bf01-MORTALITY-20261008-140509-MOB_DEV_1"))
        assertFalse(FileNamingUtils.validate("BF01-MORTALITY-2026108-140509-MOB_DEV_1"))
        assertFalse(FileNamingUtils.validate("BF01-MORTALITY-20261008-140509"))
    }

    @Test
    fun `parse splits a valid name and returns null for an invalid one`() {
        val parts = FileNamingUtils.parse("BF01-TRACE_EVENT-20261008-140509-MOB_DEV_9.jpg")!!

        assertEquals("BF01", parts.farmCode)
        assertEquals("TRACE_EVENT", parts.project)
        assertEquals("20261008", parts.date)
        assertEquals("140509", parts.time)
        assertEquals("MOB_DEV_9", parts.deviceId)
        assertEquals("jpg", parts.extension)
        assertNull(FileNamingUtils.parse("nonsense"))
        assertNull(FileNamingUtils.parse("BF01-COST-20261008-140509-DEV")!!.extension)
    }
}
