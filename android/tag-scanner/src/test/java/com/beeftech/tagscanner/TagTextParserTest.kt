package com.beeftech.tagscanner

import com.beeftech.tagscanner.model.GuideRect
import com.beeftech.tagscanner.model.TextLineBox
import com.beeftech.tagscanner.parse.TagTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagTextParserTest {

    private val guide = GuideRect(100, 100, 1100, 500) // 1000 x 400

    private fun line(text: String, height: Int, cx: Int = 600, cy: Int = 300) =
        TextLineBox(text, cx - 150, cy - height / 2, cx + 150, cy + height / 2)

    @Test
    fun `picks the tag number among other tag text`() {
        val lines = listOf(
            line("000993", height = 160),
            line("BEEFTECH.CO.ZA", height = 50, cy = 400),
            line("Patent Pending", height = 50, cy = 200)
        )
        assertEquals("000993", TagTextParser.parse(lines, guide)?.sequence)
    }

    @Test
    fun `maps look-alike letters inside mostly-digit tokens`() {
        assertEquals("000993", TagTextParser.parse(listOf(line("OOO993", 160)), guide)?.sequence)
    }

    @Test
    fun `text words never become digits`() {
        val lines = listOf(line("BEEFTECH", 160), line("Patent", 160), line("LAIRSON", 160))
        assertNull(TagTextParser.parse(lines, guide))
    }

    @Test
    fun `tallest candidate wins over a smaller digit string`() {
        val lines = listOf(line("12", height = 80, cy = 200), line("000993", height = 160))
        assertEquals("000993", TagTextParser.parse(lines, guide)?.sequence)
    }

    @Test
    fun `eight digits are rejected`() {
        assertNull(TagTextParser.parse(listOf(line("12345678", 160)), guide))
    }

    @Test
    fun `line centred outside the guide is ignored`() {
        assertNull(TagTextParser.parse(listOf(line("000993", 160, cx = 1500)), guide))
    }

    @Test
    fun `tiny digit is rejected but a large one is accepted`() {
        assertNull(TagTextParser.parse(listOf(line("3", height = 20)), guide))
        assertEquals("7", TagTextParser.parse(listOf(line("7", height = 160)), guide)?.sequence)
    }

    @Test
    fun `empty list gives null`() {
        assertNull(TagTextParser.parse(emptyList(), guide))
    }
}
