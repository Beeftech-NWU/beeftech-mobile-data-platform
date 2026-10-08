package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfCaptureContext
import com.beeftech.calfregistration.data.CalfRegistrationMappers
import com.beeftech.calfregistration.data.SYNC_STATUS_PENDING
import com.beeftech.calfregistration.data.SYNC_STATUS_REJECTED
import com.beeftech.calfregistration.data.SYNC_STATUS_SYNCED
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.entity.IdentifierTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CalfRegistrationMappersTest {

    private val capture = CalfCaptureContext(
        deviceId = "TEST-DEVICE",
        captureAt = 1_735_732_800_000L // 2025-01-01T12:00:00Z
    )

    private fun newCalf(
        form: CalfRegistrationData = CalfRegistrationData(tagNumber = "Blu1234567"),
        dam: String? = null,
        sire: String? = null
    ) = CalfRegistrationMappers.toNewCalf(form, capture, dam, sire)

    private fun view(syncStatus: String = SYNC_STATUS_PENDING) = CalfRegistrationView(
        registrationId = "reg-1",
        animalId = "animal-uuid",
        tagNumber = "Blu1234567",
        breed = "Brangus",
        gender = "Female",
        hideColour = "RED",
        brandMark = "K7",
        birthdate = 1_000L,
        damAnimalId = "dam-uuid",
        damTagNumber = "Blu0000011",
        sireAnimalId = null,
        sireTagNumber = null,
        birthWeightKg = 34.5,
        calvingEase = null,
        ageClass = "< 1 Week",
        bodyCondition = "Excellent",
        conformity = "G — Good",
        processProof = null,
        implantProof = null,
        oldTagNumber = null,
        referenceNumber = null,
        registrationDate = 1_735_689_600_000L,
        gpsLat = -26.0,
        gpsLng = 28.0,
        deviceId = "dev-1",
        captureAt = 2_000L,
        photoPath = null,
        recordGuid = "guid-1",
        syncStatus = syncStatus,
        syncedAt = null,
        syncError = null
    )

    @Test
    fun `breedName strips the code prefix`() {
        assertEquals("Brangus", CalfRegistrationMappers.breedName("BRN — Brangus"))
        assertEquals("Brangus", CalfRegistrationMappers.breedName("Brangus"))
    }

    @Test
    fun `parentTag extracts the tag from a dropdown value`() {
        assertEquals("Blu0000011", CalfRegistrationMappers.parentTag("Blu0000011 (Bonsmara)"))
        assertNull(CalfRegistrationMappers.parentTag("Select dame"))
        assertNull(CalfRegistrationMappers.parentTag(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toNewCalf rejects a tag that breaks the naming standard`() {
        newCalf(CalfRegistrationData(tagNumber = "RMB12345"))
    }

    @Test
    fun `toNewCalf keys the registration by a UUID, not the tag`() {
        val calf = newCalf()

        assertEquals(calf.animal.animalId, calf.registration.registeredAnimalId)
        assertNotEquals("Blu1234567", calf.animal.animalId)
        UUID.fromString(calf.animal.animalId) // throws if not a UUID
    }

    @Test
    fun `toNewCalf writes the TAG identifier`() {
        val calf = newCalf()

        val tags = calf.identifiers.filter { it.identifierType == IdentifierTypes.TAG }
        assertEquals(1, tags.size)
        assertEquals("Blu1234567", tags.single().identifierValue)
        assertEquals(calf.animal.animalId, tags.single().animalId)
    }

    @Test
    fun `toNewCalf expands shorthand tags`() {
        val calf = newCalf(CalfRegistrationData(tagNumber = "B1234567"))

        val tags = calf.identifiers.filter { it.identifierType == IdentifierTypes.TAG }
        assertEquals("Blu1234567", tags.single().identifierValue)
    }

    @Test
    fun `toNewCalf adds the old tag and reference identifiers only when given`() {
        val calf = newCalf(
            CalfRegistrationData(tagNumber = "Blu1234567", oldTagNumber = " OLD-1 ", referenceNumber = "")
        )

        assertEquals(listOf(IdentifierTypes.TAG, IdentifierTypes.OLD_TAG), calf.identifiers.map { it.identifierType })
        assertEquals("OLD-1", calf.identifiers.last().identifierValue)

        val both = newCalf(
            CalfRegistrationData(tagNumber = "Blu1234567", oldTagNumber = "OLD-1", referenceNumber = "REF-9")
        )
        assertTrue(IdentifierTypes.REFERENCE in both.identifiers.map { it.identifierType })
    }

    @Test
    fun `toNewCalf stores the proofs trimmed, and null when left blank`() {
        val filled = newCalf(
            CalfRegistrationData(tagNumber = "Blu1234567", processProof = " P-77 ", implantProof = "I-12")
        ).registration
        assertEquals("P-77", filled.processProof)
        assertEquals("I-12", filled.implantProof)

        val blank = newCalf(CalfRegistrationData(tagNumber = "Blu1234567", processProof = "  ")).registration
        assertNull(blank.processProof)
        assertNull(blank.implantProof)
    }

    @Test
    fun `old tag, reference and proofs go to the server and back into the form`() {
        val v = view().copy(
            oldTagNumber = "OLD-1", referenceNumber = "REF-9", processProof = "P-77", implantProof = "I-12"
        )

        val dto = CalfRegistrationMappers.toDto(v)
        assertEquals("OLD-1", dto.oldTagNumber)
        assertEquals("REF-9", dto.referenceNumber)
        assertEquals("P-77", dto.processProof)
        assertEquals("I-12", dto.implantProof)

        val form = CalfRegistrationMappers.toFormData(v)
        assertEquals("OLD-1", form.oldTagNumber)
        assertEquals("REF-9", form.referenceNumber)
        assertEquals("P-77", form.processProof)
        assertEquals("I-12", form.implantProof)

        val none = CalfRegistrationMappers.toFormData(view())
        assertEquals("", none.oldTagNumber)
        assertEquals("", none.implantProof)
    }

    @Test
    fun `toNewCalf writes only the tag identifier`() {
        val calf = newCalf(CalfRegistrationData(tagNumber = "Blu1234567"))

        assertEquals(listOf(IdentifierTypes.TAG), calf.identifiers.map { it.identifierType })
    }

    @Test
    fun `toNewCalf takes breed and capture data from the form and context`() {
        val calf = newCalf(CalfRegistrationData(tagNumber = "Blu1234567", animalType = "BNM — Bonsmara"))

        assertEquals("Bonsmara", calf.animal.breed)
        assertEquals("TEST-DEVICE", calf.animal.deviceId)
        assertEquals(capture.captureAt, calf.animal.captureAt)
        assertTrue(calf.animal.birthdate <= capture.captureAt)
        assertEquals(0.0, calf.animal.gpsLat, 0.0)
    }

    @Test
    fun `toNewCalf attaches a photo only when there is one`() {
        assertTrue(newCalf().media.isEmpty())

        val withPhoto = newCalf(CalfRegistrationData(tagNumber = "Blu1234567", photoPath = "/p.jpg"))
        assertEquals("/p.jpg", withPhoto.media.single().filePath)
        assertEquals(withPhoto.animal.animalId, withPhoto.media.single().animalId)
    }

    @Test
    fun `toNewCalf stores the resolved parent animal ids and starts pending`() {
        val calf = newCalf(dam = "dam-uuid", sire = "sire-uuid")

        assertEquals("dam-uuid", calf.registration.damId)
        assertEquals("sire-uuid", calf.registration.sireId)
        assertEquals(SYNC_STATUS_PENDING, calf.registration.syncStatus)
    }

    @Test
    fun `toFormData reports the real sync state`() {
        assertFalse(CalfRegistrationMappers.toFormData(view(SYNC_STATUS_PENDING)).synced)
        assertTrue(CalfRegistrationMappers.toFormData(view(SYNC_STATUS_SYNCED)).synced)
    }

    @Test
    fun `toFormData maps tags and falls back to select placeholders`() {
        val form = CalfRegistrationMappers.toFormData(view())

        assertEquals("Blu1234567", form.tagNumber)
        assertEquals("Blu0000011", form.dameTagNumber)
        assertEquals("Select sire", form.sireTagNumber)
    }

    @Test
    fun `age, condition and conformity round-trip from the form to the registration and back`() {
        val form = CalfRegistrationData(
            tagNumber = "Blu1234567",
            age = "1-2 Weeks",
            condition = "Poor",
            conformity = "P — Poor"
        )

        val registration = newCalf(form).registration

        assertEquals("1-2 Weeks", registration.ageClass)
        assertEquals("Poor", registration.bodyCondition)
        assertEquals("P — Poor", registration.conformity)

        val restored = CalfRegistrationMappers.toFormData(
            view().copy(ageClass = registration.ageClass, bodyCondition = registration.bodyCondition, conformity = registration.conformity)
        )
        assertEquals("1-2 Weeks", restored.age)
        assertEquals("Poor", restored.condition)
        assertEquals("P — Poor", restored.conformity)
    }

    @Test
    fun `toFormData falls back to the wizard defaults for rows saved before these fields existed`() {
        val restored = CalfRegistrationMappers.toFormData(
            view().copy(ageClass = null, bodyCondition = null, conformity = null)
        )

        assertEquals("Newborn", restored.age)
        assertEquals("Good", restored.condition)
        assertEquals("F — Fair", restored.conformity)
    }

    @Test
    fun `toFormData flags a REJECTED registration and carries the server message`() {
        val data = CalfRegistrationMappers.toFormData(
            view(SYNC_STATUS_REJECTED).copy(syncError = "Tag already registered")
        )

        assertTrue(data.needsAttention)
        assertFalse(data.synced)
        assertEquals("Tag already registered", data.syncError)
    }

    @Test
    fun `toDto does not send the phone's local photo path`() {
        val dto = CalfRegistrationMappers.toDto(view().copy(photoPath = "/data/user/0/app/files/calf-photos/p.jpg"))

        assertNull(dto.photoPath)
    }

    @Test
    fun `toDto carries the captured gender, hide colour, brand mark and birth weight`() {
        val dto = CalfRegistrationMappers.toDto(view())

        assertEquals("Female", dto.gender)
        assertEquals("RED", dto.hideColour)
        assertEquals("K7", dto.brandMark)
        assertEquals(34.5, dto.birthWeightKg!!, 0.0)
        assertEquals("< 1 Week", dto.ageClass)
        assertEquals("Excellent", dto.bodyCondition)
        assertEquals("G — Good", dto.conformity)
    }

    @Test
    fun `toDto sends tag and uuid separately with real values`() {
        val dto = CalfRegistrationMappers.toDto(view())

        assertEquals("Blu1234567", dto.tagNumber)
        assertEquals("animal-uuid", dto.animalUuid)
        assertEquals("Brangus", dto.breed)
        assertEquals(1_000L, dto.birthdate)
        assertEquals("Blu0000011", dto.damTagNumber)
        assertEquals("dam-uuid", dto.damAnimalUuid)
        assertNull(dto.sireAnimalUuid)
        assertEquals("guid-1", dto.recordguid)
        assertEquals("dev-1", dto.deviceId)
        assertEquals(SYNC_STATUS_PENDING, dto.syncStatus)
    }
}
