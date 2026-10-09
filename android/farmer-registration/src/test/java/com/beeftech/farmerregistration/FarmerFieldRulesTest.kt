package com.beeftech.farmerregistration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmerFieldRulesTest {

    @Test
    fun `contact number is optional`() {
        assertTrue(FarmerFieldRules.isValidContactNumber(""))
        assertTrue(FarmerFieldRules.isValidContactNumber("   "))
    }

    @Test
    fun `contact number accepts plus, digits and spaces with 9 to 15 digits`() {
        assertTrue(FarmerFieldRules.isValidContactNumber("+27 82 555 0101"))
        assertTrue(FarmerFieldRules.isValidContactNumber("0825550101"))
        assertTrue(FarmerFieldRules.isValidContactNumber("082555010"))
        assertTrue(FarmerFieldRules.isValidContactNumber("123456789012345"))
    }

    @Test
    fun `contact number rejects other characters and wrong lengths`() {
        assertFalse(FarmerFieldRules.isValidContactNumber("08255501"))
        assertFalse(FarmerFieldRules.isValidContactNumber("1234567890123456"))
        assertFalse(FarmerFieldRules.isValidContactNumber("082-555-0101"))
        assertFalse(FarmerFieldRules.isValidContactNumber("27+825550101"))
        assertFalse(FarmerFieldRules.isValidContactNumber("call me"))
    }

    @Test
    fun `cleanDecimal keeps one point and two decimals`() {
        assertEquals("1250.55", FarmerFieldRules.cleanDecimal("1,250.559"))
        assertEquals("12.34", FarmerFieldRules.cleanDecimal("12.3.4"))
        assertEquals("12.", FarmerFieldRules.cleanDecimal("12."))
        assertEquals("", FarmerFieldRules.cleanDecimal("ha"))
    }

    @Test
    fun `parseFarmSizeHa treats blank and a lone point as empty`() {
        assertEquals(12.0, FarmerFieldRules.parseFarmSizeHa("12.")!!, 0.0)
        assertEquals(1250.5, FarmerFieldRules.parseFarmSizeHa(" 1250.5 ")!!, 0.0)
        assertNull(FarmerFieldRules.parseFarmSizeHa(""))
        assertNull(FarmerFieldRules.parseFarmSizeHa("."))
    }

    @Test
    fun `reviewLines lists only the filled in fields`() {
        val lines =
            FarmerFieldRules.reviewLines(
                ClientRegistrationData(contactName = " Jan Botha ", contactNumber = ""),
                AddressAndLocationData(farmSizeHa = "1250.5", headCount = "380", primaryBreed = "")
            )

        assertEquals(
            listOf("Contact: Jan Botha", "Farm size: 1250.5 ha", "Head count: 380"),
            lines
        )
    }

    @Test
    fun `reviewLines is empty when nothing new was entered`() {
        assertTrue(
            FarmerFieldRules.reviewLines(ClientRegistrationData(), AddressAndLocationData()).isEmpty()
        )
    }
}
