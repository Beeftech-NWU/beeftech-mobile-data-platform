package com.beeftech.backend.api.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Names for synced batches, photos and exports: [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID].
 *
 * A copy of the mobile `FileNamingUtils` (android:database). Keep the regex and the rules in step.
 */
object FileNaming {

    enum class ProjectCode {
        CALF_REG, FARMER_REG, TREATMENT, MOVEMENT, MORTALITY, COST, TRACE_EVENT, REPORT
    }

    val REGEX = Regex("^[A-Z0-9]{4}-[A-Z_]+-\\d{8}-\\d{6}-[A-Za-z0-9_]+(\\.[a-z]+)?$")

    private val FARM_CODE = Regex("^[A-Z0-9]{4}$")
    private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val TIME = DateTimeFormatter.ofPattern("HHmmss")

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
     * Builds a name from the given instant, shown in [zone] (the device's own time on a phone,
     * the server's on reports). The device id is cleaned to letters, digits and underscores.
     */
    fun build(
        farmCode: String,
        project: ProjectCode,
        instant: Instant,
        deviceId: String,
        extension: String? = null,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        require(FARM_CODE.matches(farmCode)) { "Farm code must be 4 characters, A-Z and 0-9: '$farmCode'" }
        val device = deviceId.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
        require(device.isNotEmpty()) { "Device id is empty after cleaning: '$deviceId'" }

        val local = instant.atZone(zone)
        val base = "$farmCode-${project.name}-${DATE.format(local)}-${TIME.format(local)}-$device"
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
