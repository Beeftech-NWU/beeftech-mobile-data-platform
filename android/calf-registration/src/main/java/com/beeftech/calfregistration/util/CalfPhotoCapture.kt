package com.beeftech.calfregistration.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Camera hand-off for the calf photo: the system camera writes a full-size picture into a
 * cache file we share through [FileProvider]; we then shrink it into app storage and drop
 * the original. The saved calf keeps only the small file's path.
 */
object CalfPhotoCapture {

    private const val CAPTURE_DIR = "calf-captures"
    private const val PHOTO_DIR = "calf-photos"

    /** A new empty file for the camera to fill, and the content URI to give the camera. */
    fun newCaptureTarget(context: Context): Pair<File, Uri> {
        val directory = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
        val file = File(directory, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        return file to uri
    }

    /**
     * Shrinks the camera's picture into app storage and deletes the original.
     * Returns the saved file's path, or null if the picture could not be read.
     */
    fun finalizeCapture(context: Context, capture: File, tagNumber: String): String? {
        val directory = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
        val target = File(directory, "calf_${safeName(tagNumber)}_${System.currentTimeMillis()}.jpg")

        return try {
            ImageCompressionUtils.compressImageFile(capture.absolutePath, target)
        } finally {
            capture.delete()
        }
    }

    /** Deletes a photo this app saved; any other path (a stale or foreign value) is left alone. */
    fun deleteSavedPhoto(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        val directory = File(context.filesDir, PHOTO_DIR)
        if (file.parentFile?.canonicalPath == directory.canonicalPath) {
            file.delete()
        }
    }

    internal fun safeName(tagNumber: String): String =
        tagNumber.filter { it.isLetterOrDigit() }.ifEmpty { "calf" }

    private fun authority(context: Context) = "${context.packageName}.calfregistration.fileprovider"
}
