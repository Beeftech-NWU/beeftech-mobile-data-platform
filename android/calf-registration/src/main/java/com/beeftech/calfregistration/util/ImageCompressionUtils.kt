package com.beeftech.calfregistration.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
     * Cameras store the sensor's orientation in EXIF rather than rotating the pixels, so the
     * picture is turned upright here; without that, portrait photos would come out sideways.
     * Large pictures are decoded at reduced size so a 12 MP photo does not need ~48 MB of memory.
     */
    fun compressImageFile(
        inputPath: String,
        outputFile: File,
        quality: Int = 80,
        maxDimension: Int = 1080
    ): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(inputPath, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val decoded = BitmapFactory.decodeFile(inputPath, options) ?: return null

        val orientation = try {
            ExifInterface(inputPath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val upright = rotateBitmap(decoded, rotationDegrees(orientation))
        return compressAndSaveBitmap(upright, outputFile, quality, maxDimension)
    }

    /**
     * Decodes a saved photo for showing on screen, subsampled so a large file does not
     * need full-size memory. Returns null if the file is missing or not a readable image.
     */
    fun decodeForDisplay(path: String, maxDimension: Int = 1080): Bitmap? {
        if (!File(path).isFile) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        return BitmapFactory.decodeFile(path, options)
    }

    /** Degrees to turn a picture clockwise to make it upright, from its EXIF orientation. */
    fun rotationDegrees(exifOrientation: Int): Int =
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    /**
     * Largest power-of-two subsampling that still leaves the longer side at least
     * [maxDimension], so the later resize has enough pixels to work with.
     */
    fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0) return 1

        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        return sample
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
