package com.beeftech.tagscanner

import com.beeftech.database.util.TagColour
import com.beeftech.database.util.TagNamingUtils
import com.beeftech.tagscanner.model.EarTagScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EarTagScanResultTest {

    @Test
    fun `yellow 000993 gives Yel0000993`() {
        assertEquals("Yel0000993", EarTagScanResult("000993", TagColour.YELLOW, 0.9f).tagId)
    }

    @Test
    fun `single digit is padded to seven`() {
        assertEquals("Blu0000001", EarTagScanResult("1", TagColour.BLUE, 0.9f).tagId)
    }

    @Test
    fun `null colour gives null tagId`() {
        assertNull(EarTagScanResult("000993", null, 0f).tagId)
    }

    @Test
    fun `every non-null tagId is valid`() {
        for (colour in TagColour.entries) {
            for (sequence in listOf("0", "1", "42", "000993", "1234567", "9999999")) {
                val id = EarTagScanResult(sequence, colour, 1f).tagId
                assertTrue(id, TagNamingUtils.validateTag(id))
            }
        }
    }
}
