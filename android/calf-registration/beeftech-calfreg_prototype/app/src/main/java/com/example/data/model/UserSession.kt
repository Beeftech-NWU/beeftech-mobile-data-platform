package com.example.data.model

enum class UserRole(val displayName: String, val description: String) {
    FIELD_WORKER("Field Worker", "Capture new calf registrations in the field (full capture & local sync)"),
    OFFICE_ADMIN("Office Administrator", "Review synchronised records, lineage reports & POPIA audit (view-only)"),
    SYSTEM_ADMIN("System Administrator", "Manage backend configurations, device enrolment & platform sync")
}

data class UserSession(
    val username: String,
    val fullName: String,
    val role: UserRole,
    val deviceId: String,
    val isLoggedIn: Boolean = false
) {
    companion object {
        val DEFAULT_USERS = listOf(
            UserSession(
                username = "field_worker",
                fullName = "J. Khumalo (Field Team)",
                role = UserRole.FIELD_WORKER,
                deviceId = "BEEFTECH-TAB-042",
                isLoggedIn = true
            ),
            UserSession(
                username = "office_admin",
                fullName = "S. Van der Merwe (Office)",
                role = UserRole.OFFICE_ADMIN,
                deviceId = "BEEFTECH-HQ-01",
                isLoggedIn = false
            ),
            UserSession(
                username = "sys_admin",
                fullName = "Karl E. Edoun (Admin)",
                role = UserRole.SYSTEM_ADMIN,
                deviceId = "BEEFTECH-SRV-99",
                isLoggedIn = false
            )
        )
    }
}
