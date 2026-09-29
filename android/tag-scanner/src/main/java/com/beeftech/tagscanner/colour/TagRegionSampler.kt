package com.beeftech.tagscanner.colour

import android.graphics.Bitmap
import com.beeftech.tagscanner.model.TextLineBox

object TagRegionSampler {
    /**
     * Samples ARGB pixels from a ring around [box]: the box expanded by [expand] × its width/height
     * on each side (clamped to the bitmap), minus the box itself. The etched digits are grey, so the
     * box interior would measure the wrong colour.
     */
    fun sampleRing(bitmap: Bitmap, box: TextLineBox, expand: Float = 0.5f, stride: Int = 4): IntArray {
        val dx = (box.width * expand).toInt()
        val dy = (box.height * expand).toInt()
        val left = (box.left - dx).coerceIn(0, bitmap.width)
        val right = (box.right + dx).coerceIn(0, bitmap.width)
        val top = (box.top - dy).coerceIn(0, bitmap.height)
        val bottom = (box.bottom + dy).coerceIn(0, bitmap.height)
        if (right <= left || bottom <= top) return IntArray(0)

        val out = ArrayList<Int>()
        val row = IntArray(right - left)
        var y = top
        while (y < bottom) {
            bitmap.getPixels(row, 0, row.size, left, y, row.size, 1)
            var x = left
            while (x < right) {
                val insideBox = x in box.left until box.right && y in box.top until box.bottom
                if (!insideBox) out.add(row[x - left])
                x += stride
            }
            y += stride
        }
        return out.toIntArray()
    }
}
