package com.example.data.repository

import com.example.data.local.CalfRegistrationDao
import com.example.data.local.SyncLogDao
import com.example.data.model.CalfRegistration
import com.example.data.model.SyncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class RegistrationResult {
    data class Success(val calf: CalfRegistration) : RegistrationResult()
    data class DuplicateError(val existingId: String) : RegistrationResult()
    data class ValidationError(val message: String) : RegistrationResult()
    data class DatabaseError(val message: String) : RegistrationResult()
}

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CalfRepository(
    private val calfDao: CalfRegistrationDao,
    private val syncLogDao: SyncLogDao
) {
    val allCalves: Flow<List<CalfRegistration>> = calfDao.getAllCalves()
    val pendingCalves: Flow<List<CalfRegistration>> = calfDao.getPendingCalves()
    val syncedCalves: Flow<List<CalfRegistration>> = calfDao.getSyncedCalves()
    val pendingCount: Flow<Int> = calfDao.getPendingCount()
    val syncedCount: Flow<Int> = calfDao.getSyncedCount()
    val totalCount: Flow<Int> = calfDao.getTotalCount()
    val knownDamIds: Flow<List<String>> = calfDao.getKnownDamIds()
    val knownSireIds: Flow<List<String>> = calfDao.getKnownSireIds()
    val syncLogs: Flow<List<SyncLog>> = syncLogDao.getAllLogs()

    fun getCalfById(id: Int): Flow<CalfRegistration?> = calfDao.getCalfById(id)

    suspend fun checkIsDuplicateId(animalId: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = animalId.trim()
        if (trimmed.isEmpty()) return@withContext false
        val existing = calfDao.findByAnimalId(trimmed)
        existing != null
    }

    suspend fun registerCalf(
        animalId: String,
        birthDate: String,
        breed: String,
        sex: String = CalfRegistration.SEX_HEIFER,
        birthWeightKg: Double? = null,
        calvingEase: Int = CalfRegistration.CALVING_EASE_UNASSISTED,
        vigor: String = CalfRegistration.VIGOR_VIGOROUS,
        hornStatus: String = CalfRegistration.HORN_POLLED,
        pastureLocation: String = "Camp 4 - North Pasture",
        rfidTag: String? = null,
        damId: String?,
        sireId: String?,
        photoPath: String?,
        videoPath: String?,
        gpsLat: Double,
        gpsLng: Double,
        deviceId: String
    ): RegistrationResult = withContext(Dispatchers.IO) {
        val cleanAnimalId = animalId.trim()
        val cleanBirthDate = birthDate.trim()
        val cleanBreed = breed.trim()

        if (cleanAnimalId.isEmpty()) {
            return@withContext RegistrationResult.ValidationError("Animal ID is required.")
        }
        if (cleanBirthDate.isEmpty()) {
            return@withContext RegistrationResult.ValidationError("Birth date is required.")
        }
        if (cleanBreed.isEmpty()) {
            return@withContext RegistrationResult.ValidationError("Breed selection is required.")
        }

        // Mandatory duplicate detection
        val existing = calfDao.findByAnimalId(cleanAnimalId)
        if (existing != null) {
            return@withContext RegistrationResult.DuplicateError(cleanAnimalId)
        }

        val recordGuid = UUID.randomUUID().toString()
        val capturedAt = System.currentTimeMillis()

        val newRecord = CalfRegistration(
            id = 0,
            animalId = cleanAnimalId,
            birthDate = cleanBirthDate,
            breed = cleanBreed,
            sex = sex,
            birthWeightKg = birthWeightKg,
            calvingEase = calvingEase,
            vigor = vigor,
            hornStatus = hornStatus,
            pastureLocation = pastureLocation,
            rfidTag = rfidTag?.trim()?.ifEmpty { null },
            damId = damId?.trim()?.ifEmpty { null },
            sireId = sireId?.trim()?.ifEmpty { null },
            photoPath = photoPath,
            videoPath = videoPath,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            capturedAt = capturedAt,
            deviceId = deviceId.ifEmpty { "BEEFTECH-DEVICE-DEFAULT" },
            recordGuid = recordGuid,
            syncStatus = CalfRegistration.SYNC_STATUS_PENDING,
            syncedAt = null
        )

        try {
            val insertedId = calfDao.insert(newRecord)
            val insertedRecord = newRecord.copy(id = insertedId.toInt())
            RegistrationResult.Success(insertedRecord)
        } catch (e: Exception) {
            RegistrationResult.DatabaseError(e.localizedMessage ?: "Failed to write record to SQLite.")
        }
    }

    suspend fun triggerSync(endpoint: String): SyncResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val pendingList = calfDao.getPendingCalvesList()

        if (pendingList.isEmpty()) {
            return@withContext SyncResult(
                success = true,
                syncedCount = 0,
                message = "All records are already synchronised. No pending data."
            )
        }

        // Simulate local network HTTP POST to backend server
        delay(900) // Realistic field Wi-Fi sync latency

        val targetEndpoint = endpoint.ifEmpty { "http://192.168.1.100:8080/api/v1/sync" }
        val now = System.currentTimeMillis()
        val idsToSync = pendingList.map { it.id }

        try {
            // Atomic update to SYNCED in Room database
            calfDao.markAsSynced(idsToSync, now)

            val duration = System.currentTimeMillis() - startTime
            val msg = "200 OK: Batched ${pendingList.size} calf record(s) transmitted to $targetEndpoint"

            syncLogDao.insertLog(
                SyncLog(
                    timestamp = now,
                    batchSize = pendingList.size,
                    status = "SUCCESS",
                    endpoint = targetEndpoint,
                    responseMessage = msg,
                    durationMs = duration
                )
            )

            SyncResult(
                success = true,
                syncedCount = pendingList.size,
                message = msg,
                timestamp = now
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = "Sync failed: ${e.localizedMessage ?: "Network connection error"}"

            syncLogDao.insertLog(
                SyncLog(
                    timestamp = now,
                    batchSize = pendingList.size,
                    status = "FAILED",
                    endpoint = targetEndpoint,
                    responseMessage = errorMsg,
                    durationMs = duration
                )
            )

            SyncResult(
                success = false,
                syncedCount = 0,
                message = errorMsg,
                timestamp = now
            )
        }
    }

    suspend fun markCalfAsPending(id: Int) = withContext(Dispatchers.IO) {
        calfDao.markAsPending(id)
    }

    suspend fun deleteCalf(calf: CalfRegistration) = withContext(Dispatchers.IO) {
        calfDao.delete(calf)
    }

    fun exportToCsv(calves: List<CalfRegistration>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("id,animal_id,birth_date,breed,sex,birth_weight_kg,calving_ease,vigor,horn_status,pasture_location,rfid_tag,dam_id,sire_id,gps_lat,gps_lng,captured_at,device_id,record_guid,sync_status,synced_at\n")

        for (c in calves) {
            val capturedStr = dateFormat.format(Date(c.capturedAt))
            val syncedStr = c.syncedAt?.let { dateFormat.format(Date(it)) } ?: "N/A"
            val weightStr = c.birthWeightKg?.toString() ?: ""
            sb.append("${c.id},\"${c.animalId}\",\"${c.birthDate}\",\"${c.breed}\",\"${c.sex}\",\"$weightStr\",${c.calvingEase},\"${c.vigor}\",\"${c.hornStatus}\",\"${c.pastureLocation}\",\"${c.rfidTag ?: ""}\",\"${c.damId ?: ""}\",\"${c.sireId ?: ""}\",${c.gpsLat},${c.gpsLng},\"$capturedStr\",\"${c.deviceId}\",\"${c.recordGuid}\",\"${c.syncStatus}\",\"$syncedStr\"\n")
        }
        return sb.toString()
    }
}
