package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfRegistrationMappers
import com.beeftech.calfregistration.data.SYNC_STATUS_PENDING
import com.beeftech.calfregistration.data.SYNC_STATUS_SYNCED
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.entity.CalfRegistration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalfRegistrationMappersTest {

    private val deviceId = "TEST-DEVICE"
    private val captureAt = 1_735_689_600_000L // 2025-01-01T00:00:00Z

    @Test
    fun `toEntity maps tagNumber to animalId and animalType to breed`() {
        val formData = CalfRegistrationData(
            tagNumber = "RMB12345",
            animalType = "BRN — Brangus"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt
        )

        assertEquals("RMB12345", entity.animalId)
        assertEquals("BRN — Brangus", entity.breed)
        assertEquals(deviceId, entity.deviceId)
        assertEquals(captureAt, entity.captureAt)
        assertEquals(captureAt, entity.birthdate)
        assertEquals(0.0, entity.gpsLat, 0.0)
        assertEquals(0.0, entity.gpsLng, 0.0)
        assertNull(entity.photoPath)
        assertNull(entity.videoPath)
        assertEquals(SYNC_STATUS_PENDING, entity.syncStatus)
        assertNull(entity.syncedat)
    }

    @Test
    fun `toEntity maps placeholder dame and sire selections to null`() {
        val formData = CalfRegistrationData(
            dameTagNumber = "Select dame",
            sireTagNumber = "Select sire"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt
        )

        assertNull(entity.damId)
        assertNull(entity.sireId)
    }

    @Test
    fun `toEntity preserves real dame and sire selections`() {
        val formData = CalfRegistrationData(
            dameTagNumber = "RMB-DAM-011 (Bonsmara)",
            sireTagNumber = "BULL-BNM-902 (Bonsmara Stud)"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt
        )

        assertEquals("RMB-DAM-011 (Bonsmara)", entity.damId)
        assertEquals("BULL-BNM-902 (Bonsmara Stud)", entity.sireId)
    }

    @Test
    fun `toEntity generates a fresh recordguid for a brand new record`() {
        val formData = CalfRegistrationData(tagNumber = "RMB99999")

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt,
            existing = null
        )

        assertTrue(entity.recordguid.isNotBlank())
    }

    @Test
    fun `toEntity preserves recordguid and syncStatus of an existing record`() {
        val existing = CalfRegistration(
            id = 7,
            animalId = "RMB99999",
            birthdate = captureAt,
            breed = "Brangus",
            gpsLat = 0.0,
            gpsLng = 0.0,
            captureAt = captureAt,
            deviceId = deviceId,
            recordguid = "existing-guid-123",
            syncStatus = SYNC_STATUS_SYNCED,
            syncedat = 123L
        )

        val formData = CalfRegistrationData(
            tagNumber = "RMB99999",
            animalType = "Updated breed"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt + 1_000,
            existing = existing
        )

        assertEquals(7L, entity.id)
        assertEquals("existing-guid-123", entity.recordguid)
        assertEquals(SYNC_STATUS_SYNCED, entity.syncStatus)
        assertEquals(123L, entity.syncedat)
        assertEquals("Updated breed", entity.breed)
    }

    @Test
    fun `toFormData maps entity fields back to the UI form model`() {
        val entity = CalfRegistration(
            id = 1,
            animalId = "RMB12345",
            birthdate = captureAt,
            breed = "Bonsmara",
            damId = "RMB-DAM-011",
            sireId = "BULL-BNM-902",
            gpsLat = 0.0,
            gpsLng = 0.0,
            captureAt = captureAt,
            deviceId = deviceId,
            recordguid = "guid-1",
            syncStatus = SYNC_STATUS_SYNCED,
            syncedat = captureAt
        )

        val formData = CalfRegistrationMappers.toFormData(entity)

        assertEquals("RMB12345", formData.tagNumber)
        assertEquals("Bonsmara", formData.animalType)
        assertEquals("RMB-DAM-011", formData.dameTagNumber)
        assertEquals("BULL-BNM-902", formData.sireTagNumber)
        assertTrue(formData.synced)
    }

    @Test
    fun `toFormData maps null damId and sireId to select placeholders`() {
        val entity = CalfRegistration(
            id = 1,
            animalId = "RMB12345",
            birthdate = captureAt,
            breed = "Bonsmara",
            damId = null,
            sireId = null,
            gpsLat = 0.0,
            gpsLng = 0.0,
            captureAt = captureAt,
            deviceId = deviceId,
            recordguid = "guid-1",
            syncStatus = SYNC_STATUS_PENDING,
            syncedat = null
        )

        val formData = CalfRegistrationMappers.toFormData(entity)

        assertEquals("Select dame", formData.dameTagNumber)
        assertEquals("Select sire", formData.sireTagNumber)
        assertEquals(false, formData.synced)
    }

    @Test
    fun `round trip form to entity to form preserves the key identifying fields`() {
        val original = CalfRegistrationData(
            tagNumber = "RMB54321",
            animalType = "Nguni",
            dameTagNumber = "RMB-DAM-052 (Nguni)",
            sireTagNumber = "BULL-NGN-301 (Nguni Stud)"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = original,
            deviceId = deviceId,
            captureAt = captureAt
        )

        val roundTripped = CalfRegistrationMappers.toFormData(entity)

        assertEquals(original.tagNumber, roundTripped.tagNumber)
        assertEquals(original.animalType, roundTripped.animalType)
        assertEquals(original.dameTagNumber, roundTripped.dameTagNumber)
        assertEquals(original.sireTagNumber, roundTripped.sireTagNumber)
    }

    @Test
    fun `toEntity and toFormData correctly preserve photoPath`() {
        val formData = CalfRegistrationData(
            tagNumber = "Blu0000064",
            photoPath = "/photos/calf_Blu0000064.jpg"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt
        )

        assertEquals("/photos/calf_Blu0000064.jpg", entity.photoPath)

        val roundTripped = CalfRegistrationMappers.toFormData(entity)
        assertEquals("/photos/calf_Blu0000064.jpg", roundTripped.photoPath)
    }

    @Test
    fun `toDto maps every entity field 1-to-1`() {
        val entity = CalfRegistration(
            id = 1,
            animalId = "RMB12345",
            birthdate = 111L,
            breed = "Bonsmara",
            damId = "DAM-1",
            sireId = "SIRE-1",
            photoPath = "photo.jpg",
            videoPath = "video.mp4",
            gpsLat = -26.2,
            gpsLng = 28.0,
            captureAt = 222L,
            deviceId = deviceId,
            recordguid = "guid-1",
            syncStatus = SYNC_STATUS_PENDING,
            syncedat = null
        )

        val dto = CalfRegistrationMappers.toDto(entity)

        assertEquals(entity.animalId, dto.animalId)
        assertEquals(entity.birthdate, dto.birthdate)
        assertEquals(entity.breed, dto.breed)
        assertEquals(entity.damId, dto.damId)
        assertEquals(entity.sireId, dto.sireId)
        assertEquals(entity.photoPath, dto.photoPath)
        assertEquals(entity.videoPath, dto.videoPath)
        assertEquals(entity.gpsLat, dto.gpsLat, 0.0)
        assertEquals(entity.gpsLng, dto.gpsLng, 0.0)
        assertEquals(entity.captureAt, dto.captureAt)
        assertEquals(entity.deviceId, dto.deviceId)
        assertEquals(entity.recordguid, dto.recordguid)
        assertEquals(entity.syncStatus, dto.syncStatus)
        assertEquals(entity.syncedat, dto.syncedAt)
        assertNotEquals(SYNC_STATUS_SYNCED, dto.syncStatus)
    }
}
