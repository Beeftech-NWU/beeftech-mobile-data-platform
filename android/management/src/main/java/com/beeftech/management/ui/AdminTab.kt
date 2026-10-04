package com.beeftech.management.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beeftech.management.data.ManagementApiClient

/* One entry per admin screen. Later Phase 4 PRs add Sync policy and so on. */
enum class AdminSection(val label: String) {
    SITES("Sites"),
    DEVICES("Phones"),
    LOGIN_SECURITY("Login security"),
    REFERENCE_DATA("Reference data"),
    AUDIT_LOG("Audit log")
}

/** The admin-only tab: a section selector over the admin screens. */
@Composable
fun AdminTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    var section by rememberSaveable { mutableStateOf(AdminSection.SITES) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminSection.entries.forEach {
                FilterChip(
                    selected = section == it,
                    onClick = { section = it },
                    label = { Text(it.label) }
                )
            }
        }

        when (section) {
            AdminSection.SITES -> SitesTab(
                apiClient = apiClient,
                currentUserId = currentUserId,
                modifier = Modifier.weight(1f)
            )
            AdminSection.DEVICES -> DevicesTab(
                apiClient = apiClient,
                currentUserId = currentUserId,
                canManage = true,
                modifier = Modifier.weight(1f)
            )
            AdminSection.LOGIN_SECURITY -> LoginSecurityTab(
                apiClient = apiClient,
                currentUserId = currentUserId,
                modifier = Modifier.weight(1f)
            )
            AdminSection.REFERENCE_DATA -> ReferenceDataTab(
                apiClient = apiClient,
                currentUserId = currentUserId,
                modifier = Modifier.weight(1f)
            )
            AdminSection.AUDIT_LOG -> AuditLogTab(
                apiClient = apiClient,
                currentUserId = currentUserId,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
