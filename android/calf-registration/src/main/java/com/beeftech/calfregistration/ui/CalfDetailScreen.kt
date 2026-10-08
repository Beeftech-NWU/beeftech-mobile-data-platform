package com.beeftech.calfregistration.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.beeftech.calfregistration.util.ImageCompressionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalfDetailScreen(
    calf: CalfRegistrationData,
    onBackClick: () -> Unit,
    onRetryClick: (() -> Unit)? = null
) {
    fun parent(value: String) = if (value.startsWith("Select")) "" else value

    Column(
        modifier = Modifier.fillMaxSize().background(BeeftechBackground).verticalScroll(rememberScrollState())
    ) {
        CalfHeader(
            eyebrow = "Calf registration",
            title = calf.tagNumber,
            subtitle = "Registered calf details (read-only).",
            icon = Icons.Outlined.Pets,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            DetailSection(
                "Identity",
                "Tag number" to calf.tagNumber,
                "Old tag number" to calf.oldTagNumber,
                "Reference" to calf.referenceNumber
            )
            if (!calf.photoPath.isNullOrBlank()) {
                PhotoSection(tagNumber = calf.tagNumber, photoPath = calf.photoPath)
            }
            DetailSection(
                "Animal",
                "Type" to calf.animalType,
                "Gender" to calf.gender,
                "Age" to calf.age,
                "Condition" to calf.condition
            )
            DetailSection(
                "Appearance",
                "Hide colour" to calf.hideColour,
                "Conformity" to calf.conformity,
                "Mark" to calf.mark
            )
            DetailSection(
                "Parentage",
                "Dam" to parent(calf.dameTagNumber),
                "Sire" to parent(calf.sireTagNumber)
            )
            DetailSection(
                "Verification",
                "Process proof" to calf.processProof,
                "Implant proof" to calf.implantProof
            )
            DetailSection(
                "Status",
                "Date registered" to calf.dateRegistered,
                "Sync status" to when {
                    calf.needsAttention -> "Rejected by the server"
                    calf.synced -> "Synced"
                    else -> "Pending"
                },
                *listOfNotNull(
                    calf.syncError?.takeIf { !calf.synced }?.let { "Server message" to it }
                ).toTypedArray()
            )
            if (calf.needsAttention && onRetryClick != null) {
                CalfPrimaryButton(text = "Retry sync", onClick = onRetryClick)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private sealed interface PhotoState {
    data object Loading : PhotoState
    data object Missing : PhotoState
    data class Loaded(val image: ImageBitmap) : PhotoState
}

/** The calf's photo from this device, or a note when the file is not here. */
@Composable
private fun PhotoSection(tagNumber: String, photoPath: String) {
    val state by produceState<PhotoState>(PhotoState.Loading, photoPath) {
        value = withContext(Dispatchers.IO) {
            ImageCompressionUtils.decodeForDisplay(photoPath)
                ?.let { PhotoState.Loaded(it.asImageBitmap()) }
                ?: PhotoState.Missing
        }
    }

    CalfSectionTitle("Photo")
    Spacer(modifier = Modifier.height(12.dp))
    CalfCard {
        when (val current = state) {
            PhotoState.Loading ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BeeftechPrimary)
                }

            PhotoState.Missing ->
                Text(
                    text = "The photo is not available on this device.",
                    fontSize = 14.sp,
                    color = BeeftechMutedText
                )

            is PhotoState.Loaded ->
                Image(
                    bitmap = current.image,
                    contentDescription = "Photo of calf $tagNumber",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DetailSection(title: String, vararg rows: Pair<String, String>) {
    CalfSectionTitle(title)
    Spacer(modifier = Modifier.height(12.dp))
    CalfCard {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) Spacer(modifier = Modifier.height(12.dp))
            DetailRow(label, value)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value.ifBlank { "—" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = BeeftechText
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalfDetailScreenPreview() {
    CalfDetailScreen(CalfRegistrationLookups.initialRegisteredCalves.first(), {})
}
