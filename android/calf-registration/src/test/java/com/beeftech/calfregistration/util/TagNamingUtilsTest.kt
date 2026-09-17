package com.beeftech.calfregistration.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagNamingUtilsTest {

    @Test
    fun `formatTag formats colour and sequence to zero-padded 7 digit tag`() {
        assertEquals("Blu0000064", TagNamingUtils.formatTag(TagColour.BLUE, 64))
        assertEquals("Red0000123", TagNamingUtils.formatTag(TagColour.RED, 123))
        assertEquals("Grn0000045", TagNamingUtils.formatTag(TagColour.GREEN, 45))
        assertEquals("Yel0000078", TagNamingUtils.formatTag(TagColour.YELLOW, 78))
        assertEquals("Blu0000000", TagNamingUtils.formatTag(TagColour.BLUE, 0))
        assertEquals("Blu9999999", TagNamingUtils.formatTag(TagColour.BLUE, 9_999_999))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `formatTag throws when sequence number is negative`() {
        TagNamingUtils.formatTag(TagColour.BLUE, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `formatTag throws when sequence number exceeds 7 digits`() {
        TagNamingUtils.formatTag(TagColour.BLUE, 10_000_000)
    }

    @Test
    fun `validateTag correctly validates standard tag numbers`() {
        assertTrue(TagNamingUtils.validateTag("Blu0000064"))
        assertTrue(TagNamingUtils.validateTag("Red0000123"))
        assertTrue(TagNamingUtils.validateTag("Grn0000045"))
        assertTrue(TagNamingUtils.validateTag("Yel0000078"))

        assertFalse(TagNamingUtils.validateTag("RMB25423"))
        assertFalse(TagNamingUtils.validateTag("Blu0064"))
        assertFalse(TagNamingUtils.validateTag("Blu00000064"))
        assertFalse(TagNamingUtils.validateTag("Pur0000064"))
        assertFalse(TagNamingUtils.validateTag(""))
        assertFalse(TagNamingUtils.validateTag(null))
    }

    @Test
    fun `parseAndExpand correctly expands shorthand queries`() {
        assertEquals("Blu0000064", TagNamingUtils.parseAndExpand("B64"))
        assertEquals("Blu0000064", TagNamingUtils.parseAndExpand("b64"))
        assertEquals("Red0000123", TagNamingUtils.parseAndExpand("R123"))
        assertEquals("Grn0000045", TagNamingUtils.parseAndExpand("G45"))
        assertEquals("Yel0000078", TagNamingUtils.parseAndExpand("Y78"))

        assertEquals("Blu0000064", TagNamingUtils.parseAndExpand("Blu64"))
        assertEquals("Red0000123", TagNamingUtils.parseAndExpand("red123"))

        assertEquals("Blu0000064", TagNamingUtils.parseAndExpand("Blu0000064"))
        assertEquals("Red0000123", TagNamingUtils.parseAndExpand("RED0000123"))

        assertEquals("UnknownQuery", TagNamingUtils.parseAndExpand("UnknownQuery"))
        assertEquals("", TagNamingUtils.parseAndExpand(""))
        assertEquals("", TagNamingUtils.parseAndExpand(null))
    }

    @Test
    fun `extractComponents parses tag into colour and sequence string`() {
        val (blueColour, blueSeq) = TagNamingUtils.extractComponents("B64")!!
        assertEquals(TagColour.BLUE, blueColour)
        assertEquals("0000064", blueSeq)

        val (redColour, redSeq) = TagNamingUtils.extractComponents("Red0000123")!!
        assertEquals(TagColour.RED, redColour)
        assertEquals("0000123", redSeq)

        assertNull(TagNamingUtils.extractComponents("InvalidTag"))
    }
}
