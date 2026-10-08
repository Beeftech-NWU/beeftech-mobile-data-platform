package com.beeftech.calfregistration.util

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageCompressionUtilsTest {

    @Test
    fun `rotationDegrees maps EXIF orientations to clockwise turns`() {
        assertEquals(0, ImageCompressionUtils.rotationDegrees(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(90, ImageCompressionUtils.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, ImageCompressionUtils.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, ImageCompressionUtils.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0, ImageCompressionUtils.rotationDegrees(ExifInterface.ORIENTATION_UNDEFINED))
    }

    @Test
    fun `sampleSizeFor keeps enough pixels for the target size`() {
        // 4000 x 3000 -> 2x gives 2000 (>= 1080), 4x gives 1000 (< 1080)
        assertEquals(2, ImageCompressionUtils.sampleSizeFor(4000, 3000, 1080))
        // Already small enough: no subsampling
        assertEquals(1, ImageCompressionUtils.sampleSizeFor(1080, 800, 1080))
        assertEquals(1, ImageCompressionUtils.sampleSizeFor(1500, 1000, 1080))
        // Very large
        assertEquals(8, ImageCompressionUtils.sampleSizeFor(9000, 6000, 1080))
    }

    @Test
    fun `sampleSizeFor ignores unreadable dimensions`() {
        assertEquals(1, ImageCompressionUtils.sampleSizeFor(0, 0, 1080))
        assertEquals(1, ImageCompressionUtils.sampleSizeFor(-1, 500, 1080))
    }

    @Test
    fun `safeName keeps only letters and digits for file names`() {
        assertEquals("Blu1234567", CalfPhotoCapture.safeName("Blu1234567"))
        assertEquals("Blu12", CalfPhotoCapture.safeName("../Blu 12/"))
        assertEquals("calf", CalfPhotoCapture.safeName("///"))
    }
}
