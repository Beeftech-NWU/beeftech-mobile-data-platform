package com.beeftech.farmerregistration

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.farmerregistration.ui.theme.BeeftechTheme

val BeeftechBackground = Color(0xFFF4F3E8)
val BeeftechSurface = Color(0xFFFAF9F2)
val BeeftechPrimary = Color(0xFFA8B8AD)
val BeeftechPrimaryDark = Color(0xFF728578)
val BeeftechPrimaryDeep = Color(0xFF4F6256)
val BeeftechText = Color(0xFF2F3632)
val BeeftechMutedText = Color(0xFF6F756F)
val BeeftechBorder = Color(0xFFD6D9D1)
val BeeftechSoftAccent = Color(0xFFE3E8E2)
val BeeftechWhite = Color(0xFFFFFFFF)

data class ClientRegistrationData(
    val clientCode: String = "",
    val coRegIdNo: String = "",
    val organisationName: String = "",
    val emailAddress: String = "",
    val vatNumber: String = "",
    val selectedRoles: Set<String> = emptySet()
)

object ClientRegistrationLookups {

    val roles =
        listOf(
            "Agent",
            "Buyer",
            "Client",
            "Location",
            "Feedlot",
            "Owner",
            "Supplier",
            "Transporter"
        )

    val existingClientCodes =
        listOf(
            "CLI001",
            "CLI002",
            "BEEF77"
        )
}

class ClientDetailsScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            BeeftechTheme {

                var formData by remember {
                    mutableStateOf(
                        FarmerRegistrationSession.clientDetails
                    )
                }

                ClientDetailsContent(

                    formData = formData,

                    onFormDataChange = { updatedData ->

                        formData =
                            updatedData

                        FarmerRegistrationSession.clientDetails =
                            updatedData
                    },

                    onBackClick = {

                        finish()
                    },

                    onDiscardClick = {

                        FarmerRegistrationSession.clear()

                        formData =
                            ClientRegistrationData()
                    },

                    onContinueClick = {

                        FarmerRegistrationSession.clientDetails =
                            formData

                        val intent =
                            Intent(
                                this@ClientDetailsScreen,
                                AddressAndLocationScreen::class.java
                            )

                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun ClientDetailsContent(
    formData: ClientRegistrationData,
    onFormDataChange: (ClientRegistrationData) -> Unit,
    onBackClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onContinueClick: () -> Unit
) {

    var codeFieldHasBeenFocused by remember {
        mutableStateOf(false)
    }

    var showUniquenessError by remember {
        mutableStateOf(false)
    }

    val isDuplicate =
        ClientRegistrationLookups
            .existingClientCodes
            .contains(
                formData
                    .clientCode
                    .trim()
                    .uppercase()
            )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BeeftechBackground)
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BeeftechPrimaryDeep)
                    .padding(
                        start = 14.dp,
                        end = 22.dp,
                        top = 44.dp,
                        bottom = 20.dp
                    )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(42.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = BeeftechWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .background(
                                BeeftechPrimary.copy(
                                    alpha = 0.18f
                                ),
                                RoundedCornerShape(11.dp)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Person,
                        contentDescription = null,
                        tint = BeeftechPrimary,
                        modifier =
                            Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "ORGANISATION REGISTRATION",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color = BeeftechPrimary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            "Client Details",
                        fontSize = 25.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color = BeeftechWhite
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(9.dp)
            )

            Text(
                text =
                    "Complete identity profile records and verify business roles.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color =
                    BeeftechWhite.copy(
                        alpha = 0.7f
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(17.dp)
            )

            HorizontalDivider(
                thickness = 2.dp,
                color = BeeftechPrimary
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
        ) {

            FarmerSectionTitle(
                "Identity Profile"
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            FarmerCard {

                FarmerTextField(
                    label =
                        "Client Code",
                    value =
                        formData.clientCode,
                    onValueChange = {

                        onFormDataChange(
                            formData.copy(
                                clientCode = it
                            )
                        )

                        showUniquenessError =
                            false
                    },
                    supportingText =
                        if (showUniquenessError) {
                            "Error: Client Code already exists!"
                        } else {
                            "Unique identifier code"
                        },
                    isError =
                        showUniquenessError,
                    modifier =
                        Modifier.onFocusChanged { focusState ->

                            if (focusState.isFocused) {

                                codeFieldHasBeenFocused =
                                    true

                            } else if (
                                codeFieldHasBeenFocused
                            ) {

                                showUniquenessError =
                                    isDuplicate
                            }
                        }
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                FarmerTextField(
                    label =
                        "Co-Reg / ID No.",
                    value =
                        formData.coRegIdNo,
                    onValueChange = {

                        onFormDataChange(
                            formData.copy(
                                coRegIdNo = it
                            )
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                FarmerTextField(
                    label =
                        "Name of Organisation",
                    value =
                        formData.organisationName,
                    onValueChange = {

                        onFormDataChange(
                            formData.copy(
                                organisationName = it
                            )
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                FarmerTextField(
                    label =
                        "Email Address",
                    value =
                        formData.emailAddress,
                    onValueChange = {

                        onFormDataChange(
                            formData.copy(
                                emailAddress = it
                            )
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                FarmerTextField(
                    label =
                        "VAT Number (if registered)",
                    value =
                        formData.vatNumber,
                    onValueChange = {

                        onFormDataChange(
                            formData.copy(
                                vatNumber = it
                            )
                        )
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            FarmerSectionTitle(
                "Roles (select all that apply)"
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            FarmerCard {

                ClientRegistrationLookups
                    .roles
                    .forEachIndexed { index, role ->

                        val isChecked =
                            formData
                                .selectedRoles
                                .contains(role)

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        val updated =
                                            if (isChecked) {

                                                formData
                                                    .selectedRoles -
                                                        role

                                            } else {

                                                formData
                                                    .selectedRoles +
                                                        role
                                            }

                                        onFormDataChange(
                                            formData.copy(
                                                selectedRoles =
                                                    updated
                                            )
                                        )
                                    }
                                    .padding(
                                        vertical = 4.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            RadioButton(
                                selected =
                                    isChecked,
                                onClick = {

                                    val updated =
                                        if (isChecked) {

                                            formData
                                                .selectedRoles -
                                                    role

                                        } else {

                                            formData
                                                .selectedRoles +
                                                    role
                                        }

                                    onFormDataChange(
                                        formData.copy(
                                            selectedRoles =
                                                updated
                                        )
                                    )
                                },
                                colors =
                                    RadioButtonDefaults
                                        .colors(
                                            selectedColor =
                                                BeeftechPrimaryDeep,
                                            unselectedColor =
                                                BeeftechBorder
                                        )
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(4.dp)
                            )

                            Text(
                                text = role,
                                fontSize = 14.sp,
                                fontWeight =
                                    if (isChecked) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                color =
                                    BeeftechText
                            )
                        }

                        if (
                            index <
                            ClientRegistrationLookups
                                .roles
                                .lastIndex
                        ) {

                            HorizontalDivider(
                                thickness =
                                    0.5.dp,
                                color =
                                    BeeftechBorder.copy(
                                        alpha = 0.5f
                                    )
                            )
                        }
                    }
            }

            Spacer(
                modifier =
                    Modifier.height(26.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Box(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    FarmerSecondaryButton(
                        text = "Discard",
                        onClick =
                            onDiscardClick
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Box(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    FarmerPrimaryButton(
                        text = "Continue",
                        onClick =
                            onContinueClick,
                        enabled =
                            !isDuplicate &&
                                    formData
                                        .clientCode
                                        .isNotBlank()
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )
        }
    }
}

@Composable
fun FarmerSectionTitle(
    title: String
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        width = 4.dp,
                        height = 18.dp
                    )
                    .background(
                        BeeftechPrimaryDark,
                        RoundedCornerShape(3.dp)
                    )
        )

        Spacer(
            modifier =
                Modifier.width(9.dp)
        )

        Text(
            text =
                title.uppercase(),
            fontSize = 12.sp,
            fontWeight =
                FontWeight.Bold,
            letterSpacing =
                0.8.sp,
            color =
                BeeftechPrimaryDark
        )
    }
}

@Composable
fun FarmerCard(
    content:
    @Composable
    ColumnScope.() -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    BeeftechSurface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(17.dp),
            content = content
        )
    }
}

@Composable
fun FarmerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "—",
    supportingText: String? = null,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    icon: ImageVector =
        Icons.Outlined.EditNote
) {

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {

        Text(
            text =
                label.uppercase(),
            fontSize = 10.sp,
            fontWeight =
                FontWeight.Bold,
            letterSpacing =
                0.6.sp,
            color =
                BeeftechPrimaryDark
        )

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange =
                onValueChange,
            placeholder = {

                Text(
                    text = placeholder,
                    color =
                        BeeftechMutedText,
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            isError = isError,
            leadingIcon = {

                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .background(
                                BeeftechSoftAccent,
                                RoundedCornerShape(
                                    8.dp
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            icon,
                        contentDescription =
                            null,
                        tint =
                            BeeftechPrimaryDark,
                        modifier =
                            Modifier.size(
                                19.dp
                            )
                    )
                }
            },
            modifier =
                modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(11.dp),
            colors =
                OutlinedTextFieldDefaults
                    .colors(
                        focusedBorderColor =
                            if (isError) {
                                Color.Red
                            } else {
                                BeeftechPrimaryDark
                            },
                        unfocusedBorderColor =
                            if (isError) {
                                Color.Red
                            } else {
                                BeeftechBorder
                            },
                        cursorColor =
                            BeeftechPrimaryDark,
                        focusedContainerColor =
                            BeeftechWhite,
                        unfocusedContainerColor =
                            BeeftechWhite
                    )
        )

        if (
            !supportingText.isNullOrBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    supportingText,
                fontSize = 11.sp,
                color =
                    if (isError) {
                        Color.Red
                    } else {
                        BeeftechMutedText
                    }
            )
        }
    }
}

@Composable
fun FarmerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp),
        colors =
            ButtonDefaults
                .buttonColors(
                    containerColor =
                        BeeftechPrimaryDeep,
                    contentColor =
                        BeeftechWhite,
                    disabledContainerColor =
                        BeeftechBorder,
                    disabledContentColor =
                        BeeftechMutedText
                ),
        shape =
            RoundedCornerShape(11.dp)
    ) {

        Text(
            text = text,
            fontWeight =
                FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun FarmerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    OutlinedButton(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(50.dp),
        shape =
            RoundedCornerShape(11.dp),
        colors =
            ButtonDefaults
                .outlinedButtonColors(
                    contentColor =
                        BeeftechPrimaryDeep
                )
    ) {

        Text(
            text = text,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
fun ClientDetailsScreenPreview() {

    BeeftechTheme {

        ClientDetailsContent(
            formData =
                ClientRegistrationData(
                    clientCode =
                        "CLI009",
                    organisationName =
                        "Alpha Cattle Farms",
                    selectedRoles =
                        setOf(
                            "Client",
                            "Owner"
                        )
                ),
            onFormDataChange = {},
            onBackClick = {},
            onDiscardClick = {},
            onContinueClick = {}
        )
    }
}

@Composable
private fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color =
        Color.Unspecified,
    fontSize: TextUnit =
        TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit =
        TextUnit.Unspecified,
    lineHeight: TextUnit =
        TextUnit.Unspecified
) {

    val style =
        LocalTextStyle.current.merge(
            TextStyle(
                color =
                    color,
                fontSize =
                    fontSize,
                fontWeight =
                    fontWeight,
                letterSpacing =
                    letterSpacing,
                lineHeight =
                    lineHeight
            )
        )

    BasicText(
        text = text,
        modifier = modifier,
        style = style
    )
}


