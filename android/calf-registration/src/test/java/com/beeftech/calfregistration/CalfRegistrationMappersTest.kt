package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfRegistrationMappers
import com.beeftech.calfregistration.data.SYNC_STATUS_SYNCED
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.entity.CalfRegistrationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalfRegistrationMappersTest {

    private val deviceId = "TEST-DEVICE"
    private val captureAt = 1_735_689_600_000L // 2025-01-01T00:00:00Z

    @Test
    fun `toEntity maps tagNumber to registeredAnimalId`() {
        val formData = CalfRegistrationData(
            tagNumber = "RMB12345",
            animalType = "BRN — Brangus"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt
        )

        assertEquals("RMB12345", entity.registeredAnimalId)
        assertTrue(entity.registrationId.isNotBlank())
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
            dameTagNumber = "RMB-DAM-011",
            sireTagNumber = "BULL-BNM-902"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt
        )

        assertEquals("RMB-DAM-011", entity.damId)
        assertEquals("BULL-BNM-902", entity.sireId)
    }

    @Test
    fun `toEntity generates a fresh registrationId for a brand new record`() {
        val formData = CalfRegistrationData(tagNumber = "RMB99999")

        val entity = CalfRegistrationMappers.toEntity(
            formData = formData,
            deviceId = deviceId,
            captureAt = captureAt,
            existing = null
        )

        assertTrue(entity.registrationId.isNotBlank())
    }

    @Test
    fun `toEntity preserves registrationId of an existing record`() {
        val existing = CalfRegistrationEntity(
            registrationId = "existing-guid-123",
            registeredAnimalId = "RMB99999",
            damId = null,
            sireId = null,
            birthWeightKg = null,
            calvingEase = null,
            registrationDate = "2025-01-01"
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

        assertEquals("existing-guid-123", entity.registrationId)
        assertEquals("RMB99999", entity.registeredAnimalId)
    }

    @Test
    fun `toFormData maps entity fields back to the UI form model`() {
        val entity = CalfRegistrationEntity(
            registrationId = "guid-1",
            registeredAnimalId = "RMB12345",
            damId = "RMB-DAM-011",
            sireId = "BULL-BNM-902",
            birthWeightKg = 35.0,
            calvingEase = "Normal",
            registrationDate = "2025-01-01"
        )

        val formData = CalfRegistrationMappers.toFormData(entity)

        assertEquals("RMB12345", formData.tagNumber)
        assertEquals("RMB-DAM-011", formData.dameTagNumber)
        assertEquals("BULL-BNM-902", formData.sireTagNumber)
        assertTrue(formData.synced)
    }

    @Test
    fun `toFormData maps null damId and sireId to select placeholders`() {
        val entity = CalfRegistrationEntity(
            registrationId = "guid-1",
            registeredAnimalId = "RMB12345",
            damId = null,
            sireId = null,
            birthWeightKg = null,
            calvingEase = null,
            registrationDate = "2025-01-01"
        )

        val formData = CalfRegistrationMappers.toFormData(entity)

        assertEquals("Select dame", formData.dameTagNumber)
        assertEquals("Select sire", formData.sireTagNumber)
    }

    @Test
    fun `round trip form to entity to form preserves key fields`() {
        val original = CalfRegistrationData(
            tagNumber = "RMB54321",
            dameTagNumber = "RMB-DAM-052",
            sireTagNumber = "BULL-NGN-301"
        )

        val entity = CalfRegistrationMappers.toEntity(
            formData = original,
            deviceId = deviceId,
            captureAt = captureAt
        )

        val roundTripped = CalfRegistrationMappers.toFormData(entity)

        assertEquals(original.tagNumber, roundTripped.tagNumber)
        assertEquals(original.dameTagNumber, roundTripped.dameTagNumber)
        assertEquals(original.sireTagNumber, roundTripped.sireTagNumber)
    }

    @Test
    fun `toDto maps entity fields to Dto`() {
        val entity = CalfRegistrationEntity(
            registrationId = "guid-1",
            registeredAnimalId = "RMB12345",
            damId = "DAM-1",
            sireId = "SIRE-1",
            birthWeightKg = 32.5,
            calvingEase = "Easy",
            registrationDate = "2025-01-01"
        )

        val dto = CalfRegistrationMappers.toDto(entity)

        assertEquals(entity.registeredAnimalId, dto.animalId)
        assertEquals(entity.damId, dto.damId)
        assertEquals(entity.sireId, dto.sireId)
        assertEquals(entity.registrationId, dto.recordguid)
        assertEquals(SYNC_STATUS_SYNCED, dto.syncStatus)
    }
}
