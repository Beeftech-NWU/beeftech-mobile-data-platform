package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.ParentSearch
import org.junit.Assert.assertEquals
import org.junit.Test

class ParentSearchTest {

    private val options = listOf(
        "Blu0000001 (Bonsmara)",
        "Blu0000064 (Brangus)",
        "Blu0000641 (Nguni)",
        "Red0000064 (Bonsmara)",
        "Grn0000100 (Brangus)"
    )

    @Test
    fun `blank query returns everything in order`() {
        assertEquals(options, ParentSearch.filter(options, "  "))
    }

    @Test
    fun `shorthand finds the exact tag`() {
        assertEquals(listOf("Blu0000064 (Brangus)"), ParentSearch.filter(options, "B64").take(1))
    }

    @Test
    fun `bare number ranks the exact number first, then numbers starting with it`() {
        val result = ParentSearch.filter(options, "64")

        assertEquals(
            listOf("Blu0000064 (Brangus)", "Red0000064 (Bonsmara)", "Blu0000641 (Nguni)"),
            result
        )
    }

    @Test
    fun `leading zeros in the query are ignored`() {
        assertEquals("Blu0000001 (Bonsmara)", ParentSearch.filter(options, "0001").first())
    }

    @Test
    fun `breed name matches case-insensitively`() {
        assertEquals(
            listOf("Blu0000001 (Bonsmara)", "Red0000064 (Bonsmara)"),
            ParentSearch.filter(options, "bonsm")
        )
    }

    @Test
    fun `a full tag matches only that animal`() {
        assertEquals(listOf("Red0000064 (Bonsmara)"), ParentSearch.filter(options, "Red0000064"))
    }

    @Test
    fun `no match returns an empty list`() {
        assertEquals(emptyList<String>(), ParentSearch.filter(options, "zzz"))
    }
}
