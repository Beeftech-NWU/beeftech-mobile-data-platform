package com.beeftech.tagscanner

import com.beeftech.database.util.TagColour
import com.beeftech.tagscanner.model.EarTagScanResult
import com.beeftech.tagscanner.scan.ScanStabilizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScanStabilizerTest {

    private val a = EarTagScanResult("000993", TagColour.YELLOW, 0.9f)
    private val b = EarTagScanResult("000994", TagColour.YELLOW, 0.9f)

    @Test
    fun `three identical results emit`() {
        val stabilizer = ScanStabilizer()
        assertNull(stabilizer.offer(a))
        assertNull(stabilizer.offer(a))
        assertEquals(a, stabilizer.offer(a))
    }

    @Test
    fun `alternating results do not emit before three of the same`() {
        val stabilizer = ScanStabilizer()
        assertNull(stabilizer.offer(a))
        assertNull(stabilizer.offer(b))
        assertNull(stabilizer.offer(a))
        assertNull(stabilizer.offer(b))
        assertNotNull(stabilizer.offer(a)) // third A within the last five
    }

    @Test
    fun `same number with a different colour is a different key`() {
        val stabilizer = ScanStabilizer()
        val blue = a.copy(colour = TagColour.BLUE)
        assertNull(stabilizer.offer(a))
        assertNull(stabilizer.offer(blue))
        assertNull(stabilizer.offer(a))
    }

    @Test
    fun `reset clears the history`() {
        val stabilizer = ScanStabilizer()
        stabilizer.offer(a)
        stabilizer.offer(a)
        stabilizer.reset()
        assertNull(stabilizer.offer(a))
    }
}
