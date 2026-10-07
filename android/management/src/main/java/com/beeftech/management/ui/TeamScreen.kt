package com.beeftech.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.Site
import com.beeftech.management.data.TeamMember
import com.beeftech.management.data.roleLabel
import com.beeftech.management.viewmodel.TeamViewModel
import com.beeftech.management.viewmodel.TeamViewModelFactory

private val TeamSage =
    Color(
        0xFF4F6256
    )

private val TeamAccent =
    Color(
        0xFF667A6C
    )

private val TeamBackground =
    Color(
        0xFFFAF9F2
    )

private val TeamCard =
    Color(
        0xFFF4F3E8
    )

private val TeamSoftGreen =
    Color(
        0xFFE3E8E2
    )

private val TeamSuccess =
    Color(
        0xFF3F6A50
    )

private val TeamDanger =
    Color(
        0xFF8A4F4F
    )


const val PIN_LENGTH =
    5


fun isValidPin(
    pin: String
): Boolean =
    pin.length ==
        PIN_LENGTH &&
        pin.all {
            it in '0'..'9'
        }


private enum class TeamSection(
    val label: String
) {

    PEOPLE(
        "People"
    ),

    DEVICES(
        "Phones"
    ),

    ACTIVITY(
        "Activity"
    )
}


@Composable
fun TeamTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {

    val viewModel:
            TeamViewModel =
        viewModel(
            key =
                "team-$currentUserId",

            factory =
                TeamViewModelFactory(
                    apiClient
                )
        )


    var section by
        rememberSaveable {
            mutableStateOf(
                TeamSection.PEOPLE
            )
        }


    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    TeamBackground
                )
    ) {

        if (
            !isAdmin
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White
                        )
                        .horizontalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                TeamSection.entries
                    .forEach {
                            option ->

                        FilterChip(
                            selected =
                                section ==
                                    option,

                            onClick = {
                                section =
                                    option
                            },

                            label = {

                                Text(
                                    option.label
                                )
                            }
                        )
                    }
            }
        }


        when {

            isAdmin ||
                section ==
                TeamSection.PEOPLE -> {

                TeamScreen(
                    viewModel =
                        viewModel,

                    currentUserId =
                        currentUserId,

                    isAdmin =
                        isAdmin,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }


            section ==
                TeamSection.DEVICES -> {

                DevicesTab(
                    apiClient =
                        apiClient,

                    currentUserId =
                        currentUserId,

                    canManage =
                        false,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }


            else -> {

                AuditLogTab(
                    apiClient =
                        apiClient,

                    currentUserId =
                        currentUserId,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }
        }
    }
}


@Composable
fun TeamScreen(
    viewModel: TeamViewModel,
    currentUserId: String,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {

    val state by
        viewModel
            .uiState
            .collectAsState()


    var showCreate by
        remember {
            mutableStateOf(
                false
            )
        }


    LaunchedEffect(
        Unit
    ) {

        viewModel.refresh()


        if (
            isAdmin
        ) {

            viewModel.loadSites()
        }
    }


    if (
        showCreate
    ) {

        CreateUserDialog(
            askForSite =
                isAdmin,

            sites =
                state.sites
                    .filter {
                        it.active
                    },

            onDismiss = {

                showCreate =
                    false
            },

            onCreate = {
                    username,
                    pin,
                    siteId ->

                viewModel
                    .createWorker(
                        username,
                        pin,
                        siteId
                    ) {

                        showCreate =
                            false
                    }
            }
        )
    }


    state.issuedPin
        ?.let {
                issued ->

            AlertDialog(
                onDismissRequest =
                    viewModel::dismissIssuedPin,

                shape =
                    RoundedCornerShape(
                        20.dp
                    ),

                title = {

                    Text(
                        text =
                            "New PIN for ${issued.username}",
                        fontWeight =
                            FontWeight.SemiBold
                    )
                },

                text = {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {

                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            color =
                                TeamSoftGreen,
                            shape =
                                RoundedCornerShape(
                                    14.dp
                                )
                        ) {

                            Text(
                                text =
                                    issued.pin,
                                modifier =
                                    Modifier.padding(
                                        18.dp
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineMedium,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    TeamSage
                            )
                        }


                        Text(
                            text =
                                "Give this PIN to the worker now. It will not be shown again.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                },

                confirmButton = {

                    Button(
                        onClick =
                            viewModel::dismissIssuedPin,
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        TeamSage
                                )
                    ) {

                        Text(
                            "Done"
                        )
                    }
                }
            )
        }


    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    TeamBackground
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        TeamSage
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 18.dp
                    )
        ) {

            Text(
                text =
                    if (
                        isAdmin
                    ) {

                        "USER MANAGEMENT"

                    } else {

                        "TEAM MANAGEMENT"
                    },
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    Color.White
                        .copy(
                            alpha = 0.72f
                        )
            )


            Text(
                text =
                    if (
                        isAdmin
                    ) {

                        "Users"

                    } else {

                        "Team"
                    },
                color =
                    Color.White,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    if (
                        isAdmin
                    ) {

                        "Manage users, sites, access and linked devices"

                    } else {

                        "Manage workers assigned to your site"
                    },
                color =
                    Color.White
                        .copy(
                            alpha = 0.82f
                        ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }


        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        14.dp
                    )
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                TeamCard
                        )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            14.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column {

                            Text(
                                text =
                                    "Team members",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.SemiBold
                            )


                            Text(
                                text =
                                    "${state.members.size} ${
                                        if (
                                            state.members.size ==
                                            1
                                        ) {

                                            "member"

                                        } else {

                                            "members"
                                        }
                                    }",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }


                        if (
                            state.loading
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        26.dp
                                    ),
                                color =
                                    TeamSage
                            )
                        }
                    }


                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {

                        OutlinedButton(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            onClick =
                                viewModel::refresh,
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        ) {

                            Text(
                                "Refresh"
                            )
                        }


                        Button(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            onClick = {

                                if (
                                    isAdmin
                                ) {

                                    viewModel
                                        .loadSites()
                                }


                                showCreate =
                                    true
                            },
                            enabled =
                                !state.needsConnection,
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                ),
                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            TeamSage
                                    )
                        ) {

                            Text(
                                if (
                                    isAdmin
                                ) {

                                    "Add user"

                                } else {

                                    "Add worker"
                                }
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            if (
                state.needsConnection
            ) {

                MessageCard(
                    message =
                        "Team management needs a connection. Check your signal and tap Refresh.",
                    background =
                        Color(
                            0xFFF3E9DD
                        ),
                    textColor =
                        TeamDanger
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )
            }


            state.error
                ?.let {
                        error ->

                    MessageCard(
                        message =
                            error,
                        background =
                            Color(
                                0xFFF4E3E1
                            ),
                        textColor =
                            TeamDanger
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }


            state.notice
                ?.let {
                        notice ->

                    MessageCard(
                        message =
                            notice,
                        background =
                            TeamSoftGreen,
                        textColor =
                            TeamSuccess
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }


            if (
                state.loading &&
                state.members
                    .isEmpty()
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    24.dp
                                ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator(
                            color =
                                TeamSage
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )


                        Text(
                            "Loading team..."
                        )
                    }
                }

            } else if (
                state.members
                    .isEmpty() &&
                !state.needsConnection &&
                state.error ==
                null
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    ) {

                        Text(
                            text =
                                "No team members yet",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.SemiBold
                        )


                        Text(
                            text =
                                "Use Add worker to create the first worker account for this site.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    items(
                        items =
                            state.members,
                        key = {
                            it.userId
                        }
                    ) {
                            member ->

                        MemberCard(
                            member =
                                member,

                            siteName =
                                state.sites
                                    .firstOrNull {
                                        it.siteId ==
                                            member.siteId
                                    }
                                    ?.name,

                            isSelf =
                                member.userId ==
                                    currentUserId,

                            onToggleActive = {

                                viewModel
                                    .setActive(
                                        member,
                                        !member.active
                                    )
                            },

                            onResetPin = {

                                viewModel
                                    .resetPin(
                                        member
                                    )
                            },

                            onUnlockLogin = {

                                viewModel
                                    .unlockLogin(
                                        member
                                    )
                            },

                            onUnbind = {

                                viewModel
                                    .unbindDevice(
                                        member
                                    )
                            }
                        )
                    }
                }
            }
        }
    }
}


private fun formatMemberSync(timestamp: Long): String =
    DateFormat
        .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestamp))


@Composable
private fun MessageCard(
    message: String,
    background: Color,
    textColor: Color
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            background,
        shape =
            RoundedCornerShape(
                14.dp
            )
    ) {

        Text(
            text =
                message,
            modifier =
                Modifier.padding(
                    14.dp
                ),
            color =
                textColor,
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            fontWeight =
                FontWeight.Medium
        )
    }
}


@Composable
private fun MemberCard(
    member: TeamMember,
    siteName: String?,
    isSelf: Boolean,
    onToggleActive: () -> Unit,
    onResetPin: () -> Unit,
    onUnlockLogin: () -> Unit,
    onUnbind: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        Color.White
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE7F0FA)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color(0xFF2F6FAE),
                        modifier = Modifier.padding(10.dp).size(25.dp)
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text =
                            member.username +
                                if (isSelf) "  ·  You" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = buildString {
                            append(roleLabel(member.role))
                            val site = siteName ?: member.siteId
                            if (!site.isNullOrBlank()) append("  ·  $site")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = member.deviceLastSync
                            ?.let { "Last sync: ${formatMemberSync(it)}" }
                            ?: "Last sync: not available",
                        style = MaterialTheme.typography.bodySmall,
                        color = TeamAccent
                    )
                }

                StatusBadge(active = member.active)
            }


            HorizontalDivider(
                color =
                    TeamSage
                        .copy(
                            alpha = 0.10f
                        )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = null,
                        tint = if (member.deviceAssignedId != null) TeamSuccess else TeamAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (member.deviceAssignedId != null) "Phone linked" else "No phone linked",
                        fontWeight = FontWeight.Medium
                    )
                }


                Surface(
                    color =
                        if (
                            member.deviceAssignedId !=
                            null
                        ) {

                            TeamSoftGreen

                        } else {

                            TeamCard
                        },
                    shape =
                        RoundedCornerShape(
                            50.dp
                        )
                ) {

                    Text(
                        text =
                            if (
                                member.deviceAssignedId !=
                                null
                            ) {

                                "Linked"

                            } else {

                                "Not linked"
                            },
                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            TeamSage
                    )
                }
            }


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                if (
                    !isSelf
                ) {

                    OutlinedButton(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        onClick =
                            onToggleActive,
                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    ) {

                        Text(
                            if (
                                member.active
                            ) {

                                "Deactivate"

                            } else {

                                "Reactivate"
                            }
                        )
                    }
                }


                OutlinedButton(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    onClick =
                        onResetPin,
                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                ) {

                    Text(
                        "Reset PIN"
                    )
                }
            }


            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick =
                    onUnlockLogin,
                shape =
                    RoundedCornerShape(
                        12.dp
                    )
            ) {

                Text(
                    "Unlock sign-in"
                )
            }


            if (
                member.deviceAssignedId !=
                null
            ) {

                TextButton(
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick =
                        onUnbind
                ) {

                    Text(
                        text =
                            "Unlink phone",
                        color =
                            TeamDanger
                    )
                }
            }
        }
    }
}


@Composable
private fun StatusBadge(
    active: Boolean
) {

    Surface(
        color =
            if (
                active
            ) {

                TeamSoftGreen

            } else {

                Color(
                    0xFFF0E3E2
                )
            },
        shape =
            RoundedCornerShape(
                50.dp
            )
    ) {

        Text(
            text =
                if (
                    active
                ) {

                    "Active"

                } else {

                    "Inactive"
                },
            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
            color =
                if (
                    active
                ) {

                    TeamSuccess

                } else {

                    TeamDanger
                },
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}


@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
private fun CreateUserDialog(
    askForSite: Boolean,
    sites: List<Site>,
    onDismiss: () -> Unit,
    onCreate: (
        username: String,
        pin: String,
        siteId: String?
    ) -> Unit
) {

    var username by
        remember {
            mutableStateOf(
                ""
            )
        }


    var pin by
        remember {
            mutableStateOf(
                ""
            )
        }


    var site by
        remember {
            mutableStateOf<Site?>(
                null
            )
        }


    var menuOpen by
        remember {
            mutableStateOf(
                false
            )
        }


    val valid =
        username
            .trim()
            .length >=
            3 &&
            isValidPin(
                pin
            ) &&
            (
                !askForSite ||
                    site !=
                    null
                )


    AlertDialog(
        onDismissRequest =
            onDismiss,
        shape =
            RoundedCornerShape(
                20.dp
            ),
        title = {

            Text(
                text =
                    if (
                        askForSite
                    ) {

                        "Add user"

                    } else {

                        "Add worker"
                    },
                fontWeight =
                    FontWeight.SemiBold
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                OutlinedTextField(
                    modifier =
                        Modifier.fillMaxWidth(),
                    value =
                        username,
                    onValueChange = {
                        username =
                            it
                    },
                    label = {

                        Text(
                            "Username"
                        )
                    },
                    singleLine =
                        true,
                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                )


                OutlinedTextField(
                    modifier =
                        Modifier.fillMaxWidth(),
                    value =
                        pin,
                    onValueChange = {
                            value ->

                        pin =
                            value
                                .filter {
                                        character ->

                                    character
                                        .isDigit()
                                }
                                .take(
                                    PIN_LENGTH
                                )
                    },
                    label = {

                        Text(
                            "$PIN_LENGTH-digit PIN"
                        )
                    },
                    singleLine =
                        true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType
                                    .NumberPassword
                        ),
                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                )


                if (
                    askForSite
                ) {

                    ExposedDropdownMenuBox(
                        expanded =
                            menuOpen,
                        onExpandedChange = {
                            menuOpen =
                                it
                        }
                    ) {

                        OutlinedTextField(
                            value =
                                site
                                    ?.name
                                    .orEmpty(),
                            onValueChange = {},
                            readOnly =
                                true,
                            label = {

                                Text(
                                    "Site"
                                )
                            },
                            placeholder = {

                                Text(
                                    if (
                                        sites.isEmpty()
                                    ) {

                                        "No active sites"

                                    } else {

                                        "Choose a site"
                                    }
                                )
                            },
                            trailingIcon = {

                                ExposedDropdownMenuDefaults
                                    .TrailingIcon(
                                        expanded =
                                            menuOpen
                                    )
                            },
                            modifier =
                                Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        )


                        ExposedDropdownMenu(
                            expanded =
                                menuOpen,
                            onDismissRequest = {
                                menuOpen =
                                    false
                            }
                        ) {

                            sites
                                .forEach {
                                        option ->

                                    DropdownMenuItem(
                                        text = {

                                            Text(
                                                option.name
                                            )
                                        },
                                        onClick = {

                                            site =
                                                option

                                            menuOpen =
                                                false
                                        }
                                    )
                                }
                        }
                    }
                }


                Text(
                    text =
                        "The PIN must contain exactly $PIN_LENGTH digits.",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )
            }
        },
        confirmButton = {

            Button(
                enabled =
                    valid,
                onClick = {

                    onCreate(
                        username
                            .trim(),

                        pin,

                        if (
                            askForSite
                        ) {

                            site
                                ?.siteId

                        } else {

                            null
                        }
                    )
                },
                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                TeamSage
                        )
            ) {

                Text(
                    "Create"
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )
}
