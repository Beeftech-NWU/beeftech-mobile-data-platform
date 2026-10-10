package com.beeftech.backend.api

import java.io.File

/**
 * Keeps uploaded calf photos on local disk, one JPEG per registration, named by its
 * record GUID. The GUID comes from the database, never from the request, and is checked
 * again here so a name can never leave [directory].
 */
class CalfPhotoStore(private val directory: File) {

    private val safeName = Regex("^[A-Za-z0-9_-]{1,128}$")

    private fun fileFor(recordGuid: String): File {
        require(safeName.matches(recordGuid)) { "Unsafe record GUID for a photo file name." }
        return File(directory, "$recordGuid.jpg")
    }

    fun save(recordGuid: String, bytes: ByteArray) {
        val target = fileFor(recordGuid)
        directory.mkdirs()
        // Write beside the target and rename, so a reader never sees half a photo.
        val temp = File(directory, "$recordGuid.jpg.tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            target.writeBytes(bytes)
            temp.delete()
        }
    }

    fun read(recordGuid: String): ByteArray? =
        fileFor(recordGuid).takeIf { it.isFile }?.readBytes()

    companion object {

        const val MAX_BYTES = 5 * 1024 * 1024

        /** JPEG files start with FF D8 FF. */
        fun looksLikeJpeg(bytes: ByteArray): Boolean =
            bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()

        /**
         * BEEFTECH_MEDIA_DIR, then the beeftech.media.dir property, then a `media`
         * folder next to the SQLite database file.
         */
        fun configuredDirectory(jdbcUrl: String): File {
            val configured =
                System.getenv("BEEFTECH_MEDIA_DIR")?.trim()?.takeIf { it.isNotBlank() }
                    ?: System.getProperty("beeftech.media.dir")?.trim()?.takeIf { it.isNotBlank() }

            if (configured != null) return File(configured)

            val dbFile = File(jdbcUrl.removePrefix("jdbc:sqlite:"))
            return File(dbFile.absoluteFile.parentFile ?: File("."), "media")
        }
    }
}
