package com.beeftech.tagscanner

import com.beeftech.database.util.TagColour
import com.beeftech.tagscanner.colour.TagColourClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagColourClassifierTest {

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun pixels(vararg parts: Pair<Int, Int>): IntArray =
        parts.flatMap { (colour, count) -> List(count) { colour } }.toIntArray()

    private val grey = rgb(128, 128, 128)
    private val black = rgb(10, 10, 10)

    @Test
    fun `yellow mixed with grey and black is yellow`() {
        val result = TagColourClassifier.classify(
            pixels(rgb(235, 220, 40) to 400, grey to 300, black to 300)
        )
        assertEquals(TagColour.YELLOW, result.colour)
    }

    @Test
    fun `blue and green are detected`() {
        assertEquals(TagColour.BLUE, TagColourClassifier.classify(pixels(rgb(30, 90, 200) to 500)).colour)
        assertEquals(TagColour.GREEN, TagColourClassifier.classify(pixels(rgb(40, 160, 60) to 500)).colour)
    }

    @Test
    fun `red is detected on both sides of the hue wrap`() {
        assertEquals(TagColour.RED, TagColourClassifier.classify(pixels(rgb(230, 40, 20) to 500)).colour) // ~5 deg
        assertEquals(TagColour.RED, TagColourClassifier.classify(pixels(rgb(230, 20, 60) to 500)).colour) // ~350 deg
    }

    @Test
    fun `only greys and blacks give null`() {
        assertNull(TagColourClassifier.classify(pixels(grey to 500, black to 500)).colour)
    }

    @Test
    fun `an even yellow and blue split gives null`() {
        assertNull(TagColourClassifier.classify(pixels(rgb(235, 220, 40) to 300, rgb(30, 90, 200) to 300)).colour)
    }

    @Test
    fun `orange gives null`() {
        assertNull(TagColourClassifier.classify(pixels(rgb(240, 140, 20) to 500)).colour)
    }

    @Test
    fun `too few coloured pixels gives null`() {
        assertNull(TagColourClassifier.classify(pixels(rgb(235, 220, 40) to 50, grey to 500)).colour)
    }
}
