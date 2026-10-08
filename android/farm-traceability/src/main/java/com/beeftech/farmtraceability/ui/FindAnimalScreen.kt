package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.util.TagColour
import com.beeftech.farmtraceability.repository.AnimalSearch
import com.beeftech.tagscanner.ui.EarTagScannerDialog
import java.util.Calendar

@Composable
fun FindAnimalScreen(
    animals: List<CalfRegistrationView>? = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBackClick: () -> Unit = {},
    onFindAnimal: (String) -> Unit = {},
    onAnimalSelected: (CalfRegistrationView) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var colourName by rememberSaveable { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    val colour = colourName?.let { TagColour.valueOf(it) }
    val focusManager = LocalFocusManager.current

    val results = remember(animals, query, colour) {
        AnimalSearch.filter(animals.orEmpty(), query, colour)
    }

    fun submit() {
        focusManager.clearFocus()
        val match = AnimalSearch.bestMatch(results, query, colour)
        when {
            match != null -> onAnimalSelected(match)
            // Nothing on the list matches: fall back to the exact-tag lookup so the
            // user still gets a clear "not found" message.
            results.isEmpty() && query.isNotBlank() && !isLoading -> onFindAnimal(query.trim())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
    ) {
        item {
            TraceabilityHeader(
                eyebrow = "FARM TRACEABILITY",
                title = "Find Animal",
                subtitle = "Search by tag, number, breed, brand or parent",
                icon = Icons.Outlined.Search,
                showBackButton = true,
                onBackClick = onBackClick
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            ) {
                AnimalSearchField(
                    value = query,
                    onValueChange = { query = it.trimStart() },
                    onSearch = { submit() }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ColourChips(
                    selected = colour,
                    onSelected = { colourName = it?.name }
                )

                Spacer(modifier = Modifier.height(10.dp))

                TraceabilitySecondaryButton(
                    text = "Scan ear tag",
                    icon = Icons.Outlined.PhotoCamera,
                    onClick = { showScanner = true }
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Searching local animal records...", color = BeeftechMutedText)
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = resultsLabel(animals, results.size, query.isNotBlank() || colour != null),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechMutedText
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(results, key = { it.animalId }) { animal ->
            AnimalResultRow(
                animal = animal,
                onClick = {
                    focusManager.clearFocus()
                    onAnimalSelected(animal)
                }
            )
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showScanner) {
        EarTagScannerDialog(
            onDismiss = { showScanner = false },
            onTagScanned = { id ->
                showScanner = false
                query = id
                onFindAnimal(id)
            }
        )
    }
}

private fun resultsLabel(animals: List<CalfRegistrationView>?, matches: Int, filtering: Boolean): String = when {
    animals == null -> "Loading animals…"
    animals.isEmpty() -> "No animals registered on this device yet."
    !filtering -> "All animals (${animals.size}) · newest first"
    matches == 0 -> "No matching animals. Try fewer letters or another colour."
    else -> "$matches matching animal${if (matches == 1) "" else "s"}"
}

@Composable
private fun AnimalSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("e.g. B64, 64, Bonsmara", color = BeeftechMutedText) },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = BeeftechPrimaryDark)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = BeeftechMutedText)
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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

@Composable
private fun ColourChips(
    selected: TagColour?,
    onSelected: (TagColour?) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("All colours") },
            colors = chipColors()
        )
        TagColour.entries.forEach { colour ->
            FilterChip(
                selected = selected == colour,
                // Tapping the active colour again clears it.
                onClick = { onSelected(if (selected == colour) null else colour) },
                label = { Text(colour.displayName) },
                leadingIcon = { ColourDot(colour) },
                colors = chipColors()
            )
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = BeeftechWhite,
    selectedContainerColor = BeeftechSoftAccent,
    selectedLabelColor = BeeftechPrimaryDeep
)

@Composable
private fun ColourDot(colour: TagColour?, size: Int = 10) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(tagColourSwatch(colour), CircleShape)
    )
}

private fun tagColourSwatch(colour: TagColour?): Color = when (colour) {
    TagColour.BLUE -> Color(0xFF2F6FB5)
    TagColour.RED -> Color(0xFFC8423B)
    TagColour.GREEN -> Color(0xFF2E8B57)
    TagColour.YELLOW -> Color(0xFFE0B024)
    null -> BeeftechBorder
}

@Composable
private fun AnimalResultRow(
    animal: CalfRegistrationView,
    onClick: () -> Unit
) {
    val summary = listOfNotNull(
        animal.breed.takeIf { it.isNotBlank() },
        animal.gender?.takeIf { it.isNotBlank() },
        birthYear(animal.birthdate)?.let { "born $it" }
    ).joinToString(" · ")

    val details = listOfNotNull(
        animal.hideColour?.takeIf { it.isNotBlank() },
        animal.brandMark?.takeIf { it.isNotBlank() }?.let { "Brand $it" },
        animal.damTagNumber?.takeIf { it.isNotBlank() }?.let { "Dam $it" },
        animal.sireTagNumber?.takeIf { it.isNotBlank() }?.let { "Sire $it" },
        animal.oldTagNumber?.takeIf { it.isNotBlank() }?.let { "Old tag $it" }
    ).joinToString(" · ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColourDot(AnimalSearch.colourOf(animal.tagNumber), size = 14)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animal.tagNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechText
                )
                if (summary.isNotBlank()) {
                    Text(summary, fontSize = 12.sp, color = BeeftechMutedText)
                }
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        fontSize = 11.sp,
                        color = BeeftechMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text("›", fontSize = 26.sp, color = BeeftechPrimaryDark)
        }
    }
}

private fun birthYear(birthdate: Long): Int? =
    birthdate.takeIf { it > 0L }?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR) }

@Preview(showBackground = true)
@Composable
private fun FindAnimalScreenPreview() {
    FindAnimalScreen()
}
