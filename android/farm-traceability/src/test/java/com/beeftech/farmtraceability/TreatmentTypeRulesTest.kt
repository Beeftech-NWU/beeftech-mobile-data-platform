package com.beeftech.farmtraceability

import com.beeftech.farmtraceability.data.TreatmentTypeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatmentTypeRulesTest {

    private val synced = listOf("Vaccination", "Deworming")

    @Test
    fun `a listed type matches ignoring case and returns the listed spelling`() {
        assertEquals("Vaccination", TreatmentTypeRules.match("  vaccination ", synced))
    }

    @Test
    fun `free text that is not on the list does not match`() {
        assertNull(TreatmentTypeRules.match("Homemade remedy", synced))
        assertNull(TreatmentTypeRules.match("", synced))
    }

    @Test
    fun `a seed type is not accepted once a synced list exists without it`() {
        assertNull(TreatmentTypeRules.match("Antibiotic treatment", synced))
    }

    @Test
    fun `a device that never synced falls back to the seed list including Other`() {
        assertEquals(TreatmentTypeRules.SEED_TYPES, TreatmentTypeRules.effectiveOptions(emptyList()))
        assertEquals("Other", TreatmentTypeRules.match("other", emptyList()))
        assertEquals("Antibiotic treatment", TreatmentTypeRules.match("Antibiotic treatment", listOf(" ", "")))
        assertTrue("Other" in TreatmentTypeRules.SEED_TYPES)
    }
}
