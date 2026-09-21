package com.beeftech.calfregistration.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Utility functions for local image resizing and quality compression
 * prior to saving photo attachments to storage and uploading to server.
 */
object ImageCompressionUtils {

    /**
     * Resizes a [Bitmap] preserving aspect ratio so that its maximum dimension does not exceed [maxDimension].
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1080): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val maxAspect = max(width, height).toFloat()
        val scale = maxDimension / maxAspect

        val targetWidth = max(1, (width * scale).toInt())
        val targetHeight = max(1, (height * scale).toInt())

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Compresses a source [Bitmap] to JPEG format with [quality] (1-100) and saves it to [outputFile].
     * Returns the absolute path of the compressed file.
     */
    fun compressAndSaveBitmap(
        bitmap: Bitmap,
        outputFile: File,
        quality: Int = 80,
        maxDimension: Int = 1080
    ): String {
        val resized = resizeBitmap(bitmap, maxDimension)
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return outputFile.absolutePath
    }

    /**
     * Decodes and compresses an image file at [inputPath] to [outputFile].
     */
    fun compressImageFile(
        inputPath: String,
        outputFile: File,
        quality: Int = 80,
        maxDimension: Int = 1080
    ): String? {
        val bitmap = BitmapFactory.decodeFile(inputPath) ?: return null
        return compressAndSaveBitmap(bitmap, outputFile, quality, maxDimension)
    }
}
