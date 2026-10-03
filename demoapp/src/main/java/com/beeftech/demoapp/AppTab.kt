package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role

enum class AppTab(val label: String) {
    TRACEABILITY("Farm Traceability"),
    CALF_REGISTRATION("Calf Registration"),
    FEED_CRIB("Feed Crib"),
    DASHBOARD("Dashboard"),
    TEAM("Team")
}

/* The Admin tab is added for ADMIN in Phase 4. */
fun tabsFor(role: Role?): List<AppTab> {
    val capture = listOf(
        AppTab.TRACEABILITY,
        AppTab.CALF_REGISTRATION,
        AppTab.FEED_CRIB
    )

    /* An unknown or missing role gets the least privilege. */
    return when (role) {
        Role.ADMIN, Role.MANAGER -> capture + listOf(AppTab.DASHBOARD, AppTab.TEAM)
        else -> capture
    }
}
