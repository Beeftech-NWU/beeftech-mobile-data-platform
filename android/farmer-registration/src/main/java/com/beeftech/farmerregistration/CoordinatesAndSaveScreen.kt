package com.beeftech.farmerregistration


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.farmerregistration.ui.theme.BeeftechTheme

data class CoordinatesSaveData(
    val latitude: String = "",
    val longitude: String = "",
    val organisationName: String = "Alpha Cattle Farms"
)

class CoordinatesAndSaveScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeeftechTheme {
                var formData by remember { mutableStateOf(CoordinatesSaveData()) }
                CoordinatesAndSaveContent(
                    formData = formData,
                    onFormDataChange = { formData = it },
                    onBackClick = { 
                        val intent = Intent(this@CoordinatesAndSaveScreen, AddressAndLocationScreen::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onUseCurrentLocationClick = {
                        formData = formData.copy(latitude = "-25.7479", longitude = "28.2293")
                    },
                    onViewOnMapClick = { /* Handle map view */ },
                    onSaveClick = { /* Handle save */ }
                )
            }
        }
    }
}

@Composable
fun CoordinatesAndSaveContent(
    formData: CoordinatesSaveData,
    onFormDataChange: (CoordinatesSaveData) -> Unit,
    onBackClick: () -> Unit,
    onUseCurrentLocationClick: () -> Unit,
    onViewOnMapClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section matching ClientDetailsScreen style
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
                        text = "FARM LOCATION TRACKING",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BeeftechPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = "Coordinates & Save", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = BeeftechWhite)
                }
            }

            Spacer(modifier = Modifier.height(9.dp))
            Text(text = "Capture precise geographic coordinates for farm boundaries and save records securely.", fontSize = 12.sp, lineHeight = 17.sp, color = BeeftechWhite.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(17.dp))
            HorizontalDivider(thickness = 2.dp, color = BeeftechPrimary)
        }

        // Form Fields
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            SafeFarmerSectionTitle("Where I Am")
            Spacer(modifier = Modifier.height(12.dp))
            SafeFarmerCard {
                SafeFarmerTextField(
                    label = "Latitude",
                    value = formData.latitude,
                    onValueChange = { onFormDataChange(formData.copy(latitude = it)) },
                    placeholder = "e.g. -25.7479"
                )
                Spacer(modifier = Modifier.height(16.dp))
                SafeFarmerTextField(
                    label = "Longitude",
                    value = formData.longitude,
                    onValueChange = { onFormDataChange(formData.copy(longitude = it)) },
                    placeholder = "e.g. 28.2293"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "May differ from where data is captured — confirm before saving.",
                    fontSize = 12.sp,
                    color = BeeftechMutedText,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                SafeFarmerSecondaryButton(
                    text = "Use current location", 
                    onClick = onUseCurrentLocationClick
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                SafeFarmerSecondaryButton(
                    text = "View on map (if Google available)", 
                    onClick = onViewOnMapClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Queue & Context Info Display
            SafeFarmerCard {

                Text(
                    text = "Record 1 of 1 • ${formData.organisationName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechPrimaryDeep
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All required fields complete. Saving appends this farmer to the local queue for the next sync.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = BeeftechText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Primary Save Action
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BeeftechPrimaryDeep)
            ) {
                Text(text = "Save Location Data", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BeeftechWhite)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoordinatesAndSaveScreenPreview() {
    BeeftechTheme {
        CoordinatesAndSaveContent(
            formData = CoordinatesSaveData(
                latitude = "-25.8432",
                longitude = "28.1945",
                organisationName = "Alpha Cattle Farms"
            ),
            onFormDataChange = {},
            onBackClick = {},
            onUseCurrentLocationClick = {},
            onViewOnMapClick = {},
            onSaveClick = {}
        )
    }
}

@Composable
private fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            TextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                lineHeight = lineHeight,
                fontFamily = fontFamily,
                textDecoration = textDecoration,
                fontStyle = fontStyle,
                letterSpacing = letterSpacing
            )
        ),
        onTextLayout = onTextLayout ?: {},
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines
    )
}

@Composable
private fun SafeFarmerSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(BeeftechPrimaryDark, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = BeeftechPrimaryDark
        )
    }
}

@Composable
private fun SafeFarmerCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(17.dp), content = content)
    }
}

@Composable
private fun SafeFarmerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "—",
    supportingText: String? = null,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.EditNote
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = BeeftechPrimaryDark)
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = BeeftechMutedText, fontSize = 14.sp) },
            singleLine = true,
            isError = isError,
            leadingIcon = {
                Box(
                    modifier = Modifier.size(34.dp).background(BeeftechSoftAccent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(19.dp))
                }
            },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) Color.Red else BeeftechPrimaryDark,
                unfocusedBorderColor = if (isError) Color.Red else BeeftechBorder,
                cursorColor = BeeftechPrimaryDark,
                focusedContainerColor = BeeftechWhite,
                unfocusedContainerColor = BeeftechWhite
            )
        )
        if (!supportingText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportingText,
                fontSize = 11.sp,
                color = if (isError) Color.Red else BeeftechMutedText
            )
        }
    }
}

@Composable
private fun SafeFarmerSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BeeftechPrimaryDeep)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}


