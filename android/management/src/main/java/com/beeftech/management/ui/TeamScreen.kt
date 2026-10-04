package com.beeftech.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.ManagementApiClient
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

private val TeamWarningBackground =
    Color(
        0xFFF3E9DD
    )


const val PIN_LENGTH =
    5


/*
 * A PIN is exactly PIN_LENGTH digits,
 * matching login and the backend.
 */
fun isValidPin(
    pin: String
): Boolean =
    pin.length ==
        PIN_LENGTH &&
        pin.all {
            it in '0'..'9'
        }


/*
 * Keyed by the signed-in user so another user on the same
 * device never sees the previous user's Team state.
 */
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


    TeamScreen(
        viewModel =
            viewModel,
        currentUserId =
            currentUserId,
        isAdmin =
            isAdmin,
        modifier =
            modifier
    )
}


/**
 * Online Team Management.
 *
 * Managers see workers on their site.
 * Admins can manage users across sites.
 */
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
    }


    if (
        showCreate
    ) {

        CreateUserDialog(
            askForSite =
                isAdmin,
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


    state
        .issuedPin
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
                                "Give this PIN to the worker now. For security, it will not be shown again.",
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

        /*
         * Header
         */
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


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
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

                        "Manage users, access and linked devices"

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
                        horizontal = 14.dp,
                        vertical = 14.dp
                    )
        ) {

            /*
             * Actions
             */
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(),
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
                                            state.members.size == 1
                                        ) {

                                            "member"

                                        } else {

                                            "members"
                                        }
                                    }",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }


                        if (
                            state.loading
                        ) {

                            CircularProgressIndicator(
                                color =
                                    TeamAccent
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


            /*
             * Connectivity / notices
             */
            if (
                state.needsConnection
            ) {

                MessageCard(
                    message =
                        "Team management needs a connection. Check your signal and tap Refresh.",
                    background =
                        TeamWarningBackground,
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
                state.members.isEmpty()
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
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    24.dp
                                ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {

                        CircularProgressIndicator(
                            color =
                                TeamSage
                        )


                        Text(
                            text =
                                "Loading team…",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }

            } else if (
                state.members.isEmpty() &&
                !state.needsConnection &&
                state.error == null
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


                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )


                        Text(
                            text =
                                "Use Add worker to create the first worker account for this site.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier =
                        Modifier
                            .weight(
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
    isSelf: Boolean,
    onToggleActive: () -> Unit,
    onResetPin: () -> Unit,
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
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            member.username +
                                if (
                                    isSelf
                                ) {

                                    "  ·  You"

                                } else {

                                    ""
                                },
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    Text(
                        text =
                            buildString {

                                append(
                                    roleLabel(
                                        member.role
                                    )
                                )

                                member
                                    .siteId
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {
                                        append(
                                            "  ·  $it"
                                        )
                                    }
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                StatusBadge(
                    text =
                        if (
                            member.active
                        ) {

                            "Active"

                        } else {

                            "Inactive"
                        },
                    active =
                        member.active
                )
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

                Column {

                    Text(
                        text =
                            "DEVICE",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            TeamAccent
                    )


                    Text(
                        text =
                            if (
                                member.deviceAssignedId !=
                                null
                            ) {

                                "Phone linked"

                            } else {

                                "No phone linked"
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        fontWeight =
                            FontWeight.Medium
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


            /*
             * Actions
             */
            if (
                !isSelf
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
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

            } else {

                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),
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
    text: String,
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
                text,
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


@Composable
private fun CreateUserDialog(
    askForSite: Boolean,
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


    var siteId by
        remember {
            mutableStateOf(
                ""
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
                    siteId
                        .isNotBlank()
                )


    AlertDialog(
        onDismissRequest =
            onDismiss,
        shape =
            RoundedCornerShape(
                20.dp
            ),
        title = {

            Column {

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


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        if (
                            askForSite
                        ) {

                            "Create a user and assign a site."

                        } else {

                            "Create a worker account for your site."
                        },
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
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

                        if (
                            it.length <=
                            PIN_LENGTH
                        ) {

                            pin =
                                it.filter {
                                        character ->

                                    character
                                        .isDigit()
                                }
                        }
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

                    OutlinedTextField(
                        modifier =
                            Modifier.fillMaxWidth(),
                        value =
                            siteId,
                        onValueChange = {
                            siteId =
                                it
                        },
                        label = {

                            Text(
                                "Site ID"
                            )
                        },
                        singleLine =
                            true,
                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    )
                }


                Text(
                    text =
                        "The PIN must contain exactly $PIN_LENGTH digits.",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
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

                        siteId
                            .trim()
                            .takeIf {
                                askForSite
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
