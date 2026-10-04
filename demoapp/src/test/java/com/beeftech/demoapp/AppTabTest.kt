package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class AppTabTest {

    private val captureTabs =
        listOf(AppTab.TRACEABILITY, AppTab.CALF_REGISTRATION, AppTab.FEED_CRIB)

    @Test
    fun `workers and unknown roles get only the three capture tabs`() {
        assertEquals(captureTabs, tabsFor(Role.WORKER))
        assertEquals(captureTabs, tabsFor(null))
    }

    @Test
    fun `managers and admins get Dashboard, Records and Team after the capture tabs`() {
        val expected = captureTabs + listOf(AppTab.DASHBOARD, AppTab.RECORDS, AppTab.TEAM)

        assertEquals(expected, tabsFor(Role.MANAGER))
        assertEquals(expected, tabsFor(Role.ADMIN))
    }

    @Test
    fun `labels match what the tab row showed before`() {
        assertEquals(
            listOf("Farm Traceability", "Calf Registration", "Feed Crib"),
            tabsFor(Role.WORKER).map { it.label }
        )
        assertEquals(
            listOf("Dashboard", "Records", "Team"),
            tabsFor(Role.MANAGER).drop(3).map { it.label }
        )
    }
}
