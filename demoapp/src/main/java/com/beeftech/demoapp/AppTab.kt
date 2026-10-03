package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role

enum class AppTab(val label: String) {
    TRACEABILITY("Farm Traceability"),
    CALF_REGISTRATION("Calf Registration"),
    FEED_CRIB("Feed Crib")
}

/* Dashboard, Team and Admin tabs are added for MANAGER and ADMIN in later phases. */
@Suppress("UNUSED_PARAMETER")
fun tabsFor(role: Role?): List<AppTab> =
    listOf(
        AppTab.TRACEABILITY,
        AppTab.CALF_REGISTRATION,
        AppTab.FEED_CRIB
    )
