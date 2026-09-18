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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.farmerregistration.ui.theme.BeeftechTheme



data class AddressAndLocationData(
    val streetAddress: String = "",
    val streetCode: String = "",
    val postalCode: String = "",
    val postalAddress: String = "",
    val province: String = "",
    val country: String = "",
    val landOwnership: String = "",
    val faCodeRmis: String = "",
    val glnNumber: String = ""
)

object AddressAndLocationLookups {
    val provinces = listOf("Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape")
    val countries = listOf("South Africa", "Namibia", "Botswana", "Zimbabwe", "Mozambique", "Lesotho", "Eswatini")
    val ownershipTypes = listOf("Owned", "Leased", "Communal", "State-owned", "Trust")
    val faCodes = listOf("FA-RMIS-01", "FA-RMIS-02", "FA-RMIS-03", "FA-RMIS-04")
}

class AddressAndLocationScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeeftechTheme {
                var formData by remember { mutableStateOf(AddressAndLocationData()) }
                AddressAndLocationContent(
                    formData = formData,
                    onFormDataChange = { formData = it },
                    onBackClick = {
                        val intent = Intent(this, ClientDetailsScreen::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onDiscardClick = { formData = AddressAndLocationData() },
                    onContinueClick = {
                        val intent = Intent(this, CoordinatesAndSaveScreen::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun AddressAndLocationContent(
    formData: AddressAndLocationData,
    onFormDataChange: (AddressAndLocationData) -> Unit,
    onBackClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BeeftechPrimaryDeep)
                .padding(start = 14.dp, end = 22.dp, top = 44.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = BeeftechWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(BeeftechPrimary.copy(alpha = 0.18f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = BeeftechPrimary, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FARMER REGISTRATION",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BeeftechPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = "Address & Location", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = BeeftechWhite)
                }
            }

            Spacer(modifier = Modifier.height(9.dp))
            Text(text = "Provide the physical operation address, statutory postal boundaries, and land asset identifiers of the farming enterprise.", fontSize = 12.sp, lineHeight = 17.sp, color = BeeftechWhite.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(17.dp))
            HorizontalDivider(thickness = 2.dp, color = BeeftechPrimary)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            FarmerSectionTitle("Street Address")
            Spacer(modifier = Modifier.height(12.dp))
            FarmerCard {
                FarmerTextField("Street Address", formData.streetAddress, { onFormDataChange(formData.copy(streetAddress = it)) })
                Spacer(modifier = Modifier.height(16.dp))
                FarmerTextField("Street Code", formData.streetCode, { onFormDataChange(formData.copy(streetCode = it)) })
                Spacer(modifier = Modifier.height(16.dp))
                FarmerTextField("Postal Code", formData.postalCode, { onFormDataChange(formData.copy(postalCode = it)) })
                Spacer(modifier = Modifier.height(16.dp))
                
                FarmerMultilineTextField(
                    label = "Postal Address",
                    value = formData.postalAddress,
                    onValueChange = { onFormDataChange(formData.copy(postalAddress = it)) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                FarmerDropdownField(
                    label = "Province",
                    selectedOption = formData.province,
                    options = AddressAndLocationLookups.provinces,
                    onOptionSelected = { onFormDataChange(formData.copy(province = it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FarmerDropdownField(
                    label = "Country",
                    selectedOption = formData.country,
                    options = AddressAndLocationLookups.countries,
                    onOptionSelected = { onFormDataChange(formData.copy(country = it)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            FarmerSectionTitle("Land")
            Spacer(modifier = Modifier.height(12.dp))
            FarmerCard {
                FarmerDropdownField(
                    label = "Land Ownership",
                    selectedOption = formData.landOwnership,
                    options = AddressAndLocationLookups.ownershipTypes,
                    onOptionSelected = { onFormDataChange(formData.copy(landOwnership = it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FarmerDropdownField(
                    label = "FA Code (RMIS)",
                    selectedOption = formData.faCodeRmis,
                    options = AddressAndLocationLookups.faCodes,
                    onOptionSelected = { onFormDataChange(formData.copy(faCodeRmis = it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FarmerTextField("GLN Number (global land parcel ID)", formData.glnNumber, { onFormDataChange(formData.copy(glnNumber = it)) })
            }

            Spacer(modifier = Modifier.height(26.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    FarmerSecondaryButton(text = "Discard details", onClick = onDiscardClick)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    FarmerPrimaryButton(text = "Continue", onClick = onContinueClick)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun FarmerMultilineTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "—",
    maxLines: Int = 5
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = BeeftechPrimaryDark)
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = BeeftechMutedText, fontSize = 14.sp) },
            singleLine = false,
            minLines = 1,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BeeftechPrimaryDark,
                unfocusedBorderColor = BeeftechBorder,
                cursorColor = BeeftechPrimaryDark,
                focusedContainerColor = BeeftechWhite,
                unfocusedContainerColor = BeeftechWhite
            )
        )
    }
}

@Composable
fun FarmerDropdownField(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    placeholder: String = "Select option"
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = BeeftechPrimaryDark)
        Spacer(modifier = Modifier.height(7.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholder, color = BeeftechMutedText, fontSize = 14.sp) },
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = BeeftechPrimaryDark,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(11.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BeeftechPrimaryDark,
                    unfocusedBorderColor = BeeftechBorder,
                    cursorColor = BeeftechPrimaryDark,
                    focusedContainerColor = BeeftechWhite,
                    unfocusedContainerColor = BeeftechWhite
                )
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option, color = BeeftechText, fontSize = 14.sp) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddressAndLocationScreenPreview() {
    BeeftechTheme {
        AddressAndLocationContent(
            formData = AddressAndLocationData(
                streetAddress = "123 Cattle Lane",
                streetCode = "ST-990",
                postalCode = "0002",
                postalAddress = "P.O. Box 456\nBeeftech Center\nLevel 3\nPretoria",
                province = "Gauteng",
                country = "South Africa"
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
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val style = LocalTextStyle.current.merge(
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight
        )
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = style
    )
}

