package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTabTest {

    private val bottomTabs =
        listOf(
            AppTab.HOME,
            AppTab.CALF_REGISTRATION,
            AppTab.TRACEABILITY,
            AppTab.FEED_CRIB,
            AppTab.MORE
        )

    @Test
    fun `every role gets the same five primary destinations`() {
        assertEquals(bottomTabs, tabsFor(Role.WORKER))
        assertEquals(bottomTabs, tabsFor(Role.MANAGER))
        assertEquals(bottomTabs, tabsFor(Role.ADMIN))
        assertEquals(bottomTabs, tabsFor(null))
    }

    @Test
    fun `worker management tools stay under More and remain hidden`() {
        assertTrue(moreTabsFor(Role.WORKER).isEmpty())
        assertTrue(moreTabsFor(null).isEmpty())
    }

    @Test
    fun `manager gets management destinations under More`() {
        assertEquals(
            listOf(AppTab.DASHBOARD, AppTab.REPORTS, AppTab.RECORDS, AppTab.TEAM),
            moreTabsFor(Role.MANAGER)
        )
    }

    @Test
    fun `admin gets manager destinations plus Admin`() {
        assertEquals(moreTabsFor(Role.MANAGER) + AppTab.ADMIN, moreTabsFor(Role.ADMIN))
        assertFalse(AppTab.ADMIN in moreTabsFor(Role.MANAGER))
    }

    @Test
    fun `bottom labels stay short enough for five item navigation`() {
        assertEquals(
            listOf("Home", "Calves", "Traceability", "Feed", "More"),
            tabsFor(Role.WORKER).map { it.label }
        )
    }
}
