package com.beeftech.backend.api.common

import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileNamingTest {

    private val instant = Instant.parse("2026-10-08T14:05:09Z")

    @Test
    fun `builds the name from farm code, project, local time and device`() {
        val name = FileNaming.build("BF01", FileNaming.ProjectCode.CALF_REG, instant, "MOB_DEV_a1b2c3d4", zone = ZoneOffset.UTC)

        assertEquals("BF01-CALF_REG-20261008-140509-MOB_DEV_a1b2c3d4", name)
        assertTrue(FileNaming.validate(name))
    }

    @Test
    fun `uses the given zone for the date and time`() {
        val name = FileNaming.build("BF01", FileNaming.ProjectCode.REPORT, instant, "SERVER", "csv", ZoneOffset.ofHours(2))

        assertEquals("BF01-REPORT-20261008-160509-SERVER.csv", name)
    }

    @Test
    fun `cleans the device id and rejects a bad farm code`() {
        assertEquals(
            "S001-COST-20261008-140509-dev_1",
            FileNaming.build("S001", FileNaming.ProjectCode.COST, instant, "dev-1!", zone = ZoneOffset.UTC)
        )
        assertFailsWith<IllegalArgumentException> {
            FileNaming.build("bf01", FileNaming.ProjectCode.COST, instant, "dev", zone = ZoneOffset.UTC)
        }
        assertFailsWith<IllegalArgumentException> {
            FileNaming.build("BF01", FileNaming.ProjectCode.COST, instant, "---", zone = ZoneOffset.UTC)
        }
    }

    @Test
    fun `validate accepts only the agreed pattern`() {
        assertTrue(FileNaming.validate("BF01-MORTALITY-20261008-140509-MOB_DEV_1"))
        assertTrue(FileNaming.validate("BF01-BREED_2026-20261008-140509-MOB_DEV_1"))
        assertFalse(FileNaming.validate("BF01-breed_2026-20261008-140509-MOB_DEV_1"))
        assertFalse(FileNaming.validate("bf01-MORTALITY-20261008-140509-MOB_DEV_1"))
        assertFalse(FileNaming.validate("BF01-MORTALITY-2026108-140509-MOB_DEV_1"))
        assertFalse(FileNaming.validate("BF01-MORTALITY-20261008-140509"))
        assertFalse(FileNaming.validate("BF0-MORTALITY-20261008-140509-DEV"))
    }

    @Test
    fun `parse splits a valid name and returns null for an invalid one`() {
        val parts = FileNaming.parse("BF01-TRACE_EVENT-20261008-140509-MOB_DEV_9.jpg")!!

        assertEquals("BF01", parts.farmCode)
        assertEquals("TRACE_EVENT", parts.project)
        assertEquals("20261008", parts.date)
        assertEquals("140509", parts.time)
        assertEquals("MOB_DEV_9", parts.deviceId)
        assertEquals("jpg", parts.extension)
        assertNull(FileNaming.parse("nonsense"))
        assertNull(FileNaming.parse("BF01-COST-20261008-140509-DEV")!!.extension)
    }
}
