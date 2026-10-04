package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role

enum class AppTab(val label: String) {
    TRACEABILITY("Farm Traceability"),
    CALF_REGISTRATION("Calf Registration"),
    FEED_CRIB("Feed Crib"),
    DASHBOARD("Dashboard"),
    RECORDS("Records"),
    TEAM("Team"),
    ADMIN("Admin")
}

/* ADMIN is the manager's tabs plus Admin; Admin holds the screens only admins get. */
fun tabsFor(role: Role?): List<AppTab> {
    val capture = listOf(
        AppTab.TRACEABILITY,
        AppTab.CALF_REGISTRATION,
        AppTab.FEED_CRIB
    )

    /* An unknown or missing role gets the least privilege. */
    return when (role) {
        Role.ADMIN -> capture + listOf(AppTab.DASHBOARD, AppTab.RECORDS, AppTab.TEAM, AppTab.ADMIN)
        Role.MANAGER -> capture + listOf(AppTab.DASHBOARD, AppTab.RECORDS, AppTab.TEAM)
        else -> capture
    }
}
