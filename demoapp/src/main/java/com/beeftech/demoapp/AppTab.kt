package com.beeftech.demoapp

import com.beeftech.authentication.domain.Role

/**
 * Five permanent bottom-navigation destinations.
 *
 * Management-only destinations live under More so the main navigation stays
 * predictable for workers, managers and administrators.
 */
enum class AppTab(val label: String) {
    HOME("Home"),
    CALF_REGISTRATION("Calves"),
    TRACEABILITY("Traceability"),
    FEED_CRIB("Feed"),
    MORE("More"),

    // Secondary destinations opened from More.
    DASHBOARD("Dashboard"),
    REPORTS("Reports"),
    RECORDS("Records"),
    TEAM("Team"),
    ADMIN("Admin")
}

fun tabsFor(role: Role?): List<AppTab> =
    listOf(
        AppTab.HOME,
        AppTab.CALF_REGISTRATION,
        AppTab.TRACEABILITY,
        AppTab.FEED_CRIB,
        AppTab.MORE
    )

fun moreTabsFor(role: Role?): List<AppTab> =
    when (role) {
        Role.ADMIN ->
            listOf(
                AppTab.DASHBOARD,
                AppTab.REPORTS,
                AppTab.RECORDS,
                AppTab.TEAM,
                AppTab.ADMIN
            )

        Role.MANAGER ->
            listOf(
                AppTab.DASHBOARD,
                AppTab.REPORTS,
                AppTab.RECORDS,
                AppTab.TEAM
            )

        else -> emptyList()
    }
