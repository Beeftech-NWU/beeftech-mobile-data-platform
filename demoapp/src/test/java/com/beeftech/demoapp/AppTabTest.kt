package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class AppTabTest {

    private val captureTabs =
        listOf(AppTab.TRACEABILITY, AppTab.CALF_REGISTRATION, AppTab.FEED_CRIB)

    @Test
    fun `every role still gets the three capture tabs in the existing order`() {
        (Role.entries + null).forEach { role ->
            assertEquals(captureTabs, tabsFor(role))
        }
    }

    @Test
    fun `labels match what the tab row showed before`() {
        assertEquals(
            listOf("Farm Traceability", "Calf Registration", "Feed Crib"),
            tabsFor(Role.WORKER).map { it.label }
        )
    }
}
