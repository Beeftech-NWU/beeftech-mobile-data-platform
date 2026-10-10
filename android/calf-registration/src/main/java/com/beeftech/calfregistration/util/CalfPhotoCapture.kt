package com.beeftech.calfregistration.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.beeftech.database.security.SyncIdentityRegistry
import com.beeftech.database.util.FileNamingUtils
import com.beeftech.database.util.ProjectCode
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
        val target = File(directory, photoFileName(tagNumber, System.currentTimeMillis(), directory))

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

    /**
     * [FarmCode]-CALF_REG-[YYYYMMDD]-[HHMMSS]-[DeviceID].jpg when this device knows its farm code,
     * otherwise the older calf_<tag>_<time>.jpg. A second photo in the same second keeps the older
     * form too, so a name is never reused.
     */
    internal fun photoFileName(tagNumber: String, nowMillis: Long, directory: File): String {
        val named = runCatching {
            FileNamingUtils.build(
                farmCode = SyncIdentityRegistry.farmCode().orEmpty(),
                project = ProjectCode.CALF_REG,
                epochMillis = nowMillis,
                deviceId = SyncIdentityRegistry.deviceId().orEmpty(),
                extension = "jpg"
            )
        }.getOrNull()

        return if (named != null && !File(directory, named).exists()) {
            named
        } else {
            "calf_${safeName(tagNumber)}_$nowMillis.jpg"
        }
    }

    internal fun safeName(tagNumber: String): String =
        tagNumber.filter { it.isLetterOrDigit() }.ifEmpty { "calf" }

    private fun authority(context: Context) = "${context.packageName}.calfregistration.fileprovider"
}
