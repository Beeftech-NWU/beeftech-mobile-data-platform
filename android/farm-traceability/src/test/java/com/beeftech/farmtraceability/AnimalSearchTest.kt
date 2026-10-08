package com.beeftech.farmtraceability

import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.util.TagColour
import com.beeftech.farmtraceability.repository.AnimalSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalSearchTest {

    private val animals = listOf(
        animal("Blu0000001", "Bonsmara", "Female", hideColour = "Red"),
        animal("Blu0000064", "Brangus", "Male", damTag = "Blu0000001"),
        animal("Blu0000641", "Nguni", "Female", brandMark = "BT"),
        animal("Red0000064", "Bonsmara", "Male"),
        animal("Grn0000100", "Brangus", "Female", oldTag = "OLD-778")
    )

    private fun tags(result: List<CalfRegistrationView>) = result.map { it.tagNumber }

    @Test
    fun `blank query returns everything in order`() {
        assertEquals(animals, AnimalSearch.filter(animals, "  "))
    }

    @Test
    fun `shorthand puts the exact tag first`() {
        assertEquals("Blu0000064", AnimalSearch.filter(animals, "B64").first().tagNumber)
        assertEquals("Red0000064", AnimalSearch.filter(animals, "red 64").first().tagNumber)
    }

    @Test
    fun `bare number finds every colour, exact numbers before prefixes`() {
        assertEquals(
            listOf("Blu0000064", "Red0000064", "Blu0000641"),
            tags(AnimalSearch.filter(animals, "64"))
        )
    }

    @Test
    fun `colour filter narrows the list and completes bare numbers`() {
        assertEquals(listOf("Red0000064"), tags(AnimalSearch.filter(animals, "", TagColour.RED)))
        assertEquals("Blu0000064", AnimalSearch.filter(animals, "64", TagColour.BLUE).first().tagNumber)
    }

    @Test
    fun `partial tag text matches`() {
        assertEquals(listOf("Grn0000100"), tags(AnimalSearch.filter(animals, "grn00001")))
    }

    @Test
    fun `breed brand old tag and parent all match`() {
        assertEquals(listOf("Blu0000001", "Red0000064"), tags(AnimalSearch.filter(animals, "bonsm")))
        assertEquals(listOf("Blu0000641"), tags(AnimalSearch.filter(animals, "BT")))
        assertEquals(listOf("Grn0000100"), tags(AnimalSearch.filter(animals, "old-778")))
        assertEquals(
            listOf("Blu0000001", "Blu0000064"),
            tags(AnimalSearch.filter(animals, "Blu0000001")).sorted()
        )
    }

    @Test
    fun `male does not match female`() {
        assertEquals(listOf("Blu0000064", "Red0000064"), tags(AnimalSearch.filter(animals, "male")))
    }

    @Test
    fun `every word must match`() {
        // A red tag outranks a red hide.
        assertEquals(listOf("Red0000064", "Blu0000001"), tags(AnimalSearch.filter(animals, "red bonsmara")))
        assertEquals(listOf("Blu0000064", "Grn0000100"), tags(AnimalSearch.filter(animals, "brangus")))
        assertEquals(listOf("Grn0000100"), tags(AnimalSearch.filter(animals, "brangus female")))
    }

    @Test
    fun `no match returns an empty list`() {
        assertEquals(emptyList<CalfRegistrationView>(), AnimalSearch.filter(animals, "zzz"))
    }

    @Test
    fun `best match is the exact tag or the only result`() {
        val numberResults = AnimalSearch.filter(animals, "64")
        assertNull(AnimalSearch.bestMatch(numberResults, "64"))
        assertEquals("Blu0000064", AnimalSearch.bestMatch(AnimalSearch.filter(animals, "B64"), "B64")?.tagNumber)
        assertEquals("Blu0000641", AnimalSearch.bestMatch(AnimalSearch.filter(animals, "nguni"), "nguni")?.tagNumber)
    }

    private fun animal(
        tag: String,
        breed: String,
        gender: String,
        hideColour: String? = null,
        brandMark: String? = null,
        damTag: String? = null,
        oldTag: String? = null
    ) = CalfRegistrationView(
        registrationId = "reg-$tag",
        animalId = "animal-$tag",
        tagNumber = tag,
        breed = breed,
        gender = gender,
        hideColour = hideColour,
        brandMark = brandMark,
        birthdate = 0L,
        damAnimalId = null,
        damTagNumber = damTag,
        sireAnimalId = null,
        sireTagNumber = null,
        birthWeightKg = null,
        calvingEase = null,
        ageClass = null,
        bodyCondition = null,
        conformity = null,
        processProof = null,
        implantProof = null,
        oldTagNumber = oldTag,
        referenceNumber = null,
        registrationDate = 0L,
        gpsLat = 0.0,
        gpsLng = 0.0,
        deviceId = "device",
        captureAt = 0L,
        photoPath = null,
        recordGuid = "guid-$tag",
        syncStatus = "PENDING",
        syncedAt = null,
        syncError = null
    )
}
