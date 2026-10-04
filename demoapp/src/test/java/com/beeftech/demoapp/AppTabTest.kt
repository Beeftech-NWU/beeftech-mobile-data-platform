package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `managers get Dashboard, Reports, Records and Team after the capture tabs`() {
        assertEquals(
            captureTabs + listOf(AppTab.DASHBOARD, AppTab.REPORTS, AppTab.RECORDS, AppTab.TEAM),
            tabsFor(Role.MANAGER)
        )
    }

    @Test
    fun `admins get everything a manager has plus Admin, and nobody else gets Admin`() {
        assertEquals(tabsFor(Role.MANAGER) + AppTab.ADMIN, tabsFor(Role.ADMIN))
        assertFalse(AppTab.ADMIN in tabsFor(Role.MANAGER))
        assertFalse(AppTab.ADMIN in tabsFor(Role.WORKER))
        assertFalse(AppTab.ADMIN in tabsFor(null))
    }

    @Test
    fun `labels match what the tab row showed before`() {
        assertEquals(
            listOf("Farm Traceability", "Calf Registration", "Feed Crib"),
            tabsFor(Role.WORKER).map { it.label }
        )
        assertEquals(
            listOf("Dashboard", "Reports", "Records", "Team"),
            tabsFor(Role.MANAGER).drop(3).map { it.label }
        )
        assertEquals("Admin", tabsFor(Role.ADMIN).last().label)
    }
}
