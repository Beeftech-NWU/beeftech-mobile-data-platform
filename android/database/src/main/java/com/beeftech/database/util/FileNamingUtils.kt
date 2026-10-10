package com.beeftech.database.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** What a name is about. The backend has the same list in `common/FileNaming`. */
enum class ProjectCode {
    CALF_REG, FARMER_REG, TREATMENT, MOVEMENT, MORTALITY, COST, TRACE_EVENT, FEED_CRIB, REPORT
}

/**
 * Names for sync batches, photos and exports: `[FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]`,
 * for example `BF01-CALF_REG-20261008-140509-MOB_DEV_a1b2c3d4.jpg`.
 *
 * A copy of the backend `FileNaming` (backend:api, common). Keep the regex and the rules in step.
 */
object FileNamingUtils {

    val REGEX = Regex("^[A-Z0-9]{4}-[A-Z0-9_]+-\\d{8}-\\d{6}-[A-Za-z0-9_]+(\\.[a-z]+)?$")

    private val FARM_CODE = Regex("^[A-Z0-9]{4}$")

    /** The parts of a valid name. [extension] has no dot and is null when there is none. */
    data class Parts(
        val farmCode: String,
        val project: String,
        val date: String,
        val time: String,
        val deviceId: String,
        val extension: String?
    )

    /**
     * Builds a name for [epochMillis], shown in [timeZone] (the device's own time by default).
     * The device id is cleaned to letters, digits and underscores.
     * Throws [IllegalArgumentException] for a bad farm code or an empty device id.
     */
    fun build(
        farmCode: String,
        project: ProjectCode,
        epochMillis: Long,
        deviceId: String,
        extension: String? = null,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        require(FARM_CODE.matches(farmCode)) { "Farm code must be 4 characters, A-Z and 0-9: '$farmCode'" }
        val device = deviceId.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
        require(device.isNotEmpty()) { "Device id is empty after cleaning: '$deviceId'" }

        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply { this.timeZone = timeZone }
            .format(epochMillis)
        val base = "$farmCode-${project.name}-$stamp-$device"
        return if (extension.isNullOrBlank()) base else "$base.${extension.lowercase().removePrefix(".")}"
    }

    fun validate(name: String): Boolean = REGEX.matches(name)

    fun parse(name: String): Parts? {
        if (!validate(name)) return null
        val dot = name.indexOf('.')
        val extension = if (dot >= 0) name.substring(dot + 1) else null
        val stem = if (dot >= 0) name.substring(0, dot) else name
        // Project names contain underscores but never hyphens, so splitting on '-' is safe.
        val pieces = stem.split('-', limit = 5)
        return Parts(pieces[0], pieces[1], pieces[2], pieces[3], pieces[4], extension)
    }
}
