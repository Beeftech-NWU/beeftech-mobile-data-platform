package com.beeftech.management.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.REVIEW_TYPES
import com.beeftech.management.data.ReviewRecord
import com.beeftech.management.viewmodel.RecordsReviewViewModel
import com.beeftech.management.viewmodel.RecordsReviewViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val MAX_VOID_REASON_LENGTH = 500

private val RecordsCream = Color(0xFFFAF9F2)
private val RecordsSurface = Color(0xFFFFFFFF)
private val RecordsSage = Color(0xFF3E5D4D)
private val RecordsSageStrong = Color(0xFF294436)
private val RecordsSoft = Color(0xFFE6F1EA)
private val RecordsBorder = Color(0xFFD9DDD8)
private val RecordsText = Color(0xFF1F2823)
private val RecordsMuted = Color(0xFF6D756F)

private enum class RecordsSection(val label: String) {
    ANIMALS("Animals"),
    REVIEW("Captured Records")
}

private enum class AnimalFilter(val label: String) {
    ALL("All"),
    AT_SITE("At Site"),
    MOVED("Moved"),
    SOLD("Sold"),
    DECEASED("Deceased")
}

private data class MockAnimal(
    val tag: String,
    val breed: String,
    val sex: String,
    val age: String,
    val weight: String,
    val status: String
)

private val mockupAnimals =
    listOf(
        MockAnimal(
            tag = "ZA100123",
            breed = "Angus",
            sex = "Heifer",
            age = "12 months",
            weight = "320 kg",
            status = "At Site"
        ),
        MockAnimal(
            tag = "ZA100124",
            breed = "Brahman",
            sex = "Steer",
            age = "14 months",
            weight = "410 kg",
            status = "At Site"
        ),
        MockAnimal(
            tag = "ZA100125",
            breed = "Angus",
            sex = "Heifer",
            age = "10 months",
            weight = "295 kg",
            status = "Moved"
        ),
        MockAnimal(
            tag = "ZA100126",
            breed = "Bonsmara",
            sex = "Steer",
            age = "18 months",
            weight = "480 kg",
            status = "At Site"
        ),
        MockAnimal(
            tag = "ZA100127",
            breed = "Hereford",
            sex = "Heifer",
            age = "16 months",
            weight = "360 kg",
            status = "At Site"
        )
    )

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun RecordsReviewTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: RecordsReviewViewModel = viewModel(
        key = "records-review-$currentUserId",
        factory = RecordsReviewViewModelFactory(apiClient)
    )

    RecordsReviewScreen(viewModel = viewModel, modifier = modifier)
}

/**
 * Keeps the existing online records-review workflow intact and adds the mockup Animals list
 * as the default Records view. The Animals list is visual/demo data from the signed-off mockup.
 */
@Composable
fun RecordsReviewScreen(
    viewModel: RecordsReviewViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var selectedSection by remember { mutableStateOf(RecordsSection.ANIMALS) }
    var voidTarget by remember { mutableStateOf<ReviewRecord?>(null) }
    var reviewSearchQuery by remember { mutableStateOf("") }
    var animalSearchQuery by remember { mutableStateOf("") }
    var animalFilter by remember { mutableStateOf(AnimalFilter.ALL) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    val filteredRecords = remember(state.visibleRecords, reviewSearchQuery) {
        val query = reviewSearchQuery.trim()
        if (query.isBlank()) {
            state.visibleRecords
        } else {
            state.visibleRecords.filter { record ->
                record.label.contains(query, ignoreCase = true) ||
                    record.submittedByUsername.orEmpty().contains(query, ignoreCase = true) ||
                    record.siteId.orEmpty().contains(query, ignoreCase = true)
            }
        }
    }

    val filteredAnimals = remember(animalSearchQuery, animalFilter) {
        val query = animalSearchQuery.trim()
        mockupAnimals.filter { animal ->
            val matchesQuery =
                query.isBlank() ||
                    animal.tag.contains(query, ignoreCase = true) ||
                    animal.breed.contains(query, ignoreCase = true) ||
                    animal.sex.contains(query, ignoreCase = true) ||
                    animal.status.contains(query, ignoreCase = true)

            val matchesFilter =
                animalFilter == AnimalFilter.ALL ||
                    animal.status.equals(animalFilter.label, ignoreCase = true)

            matchesQuery && matchesFilter
        }
    }

    voidTarget?.let { record ->
        VoidDialog(
            record = record,
            onDismiss = { voidTarget = null },
            onVoid = { reason ->
                viewModel.voidRecord(record, reason)
                voidTarget = null
            }
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RecordsCream)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecordsSection.values().forEach { section ->
                FilterChip(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    label = {
                        Text(
                            text = section.label,
                            fontWeight =
                                if (selectedSection == section) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                        )
                    },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RecordsSage,
                            selectedLabelColor = Color.White
                        )
                )
            }
        }

        when (selectedSection) {
            RecordsSection.ANIMALS ->
                AnimalsMockupContent(
                    searchQuery = animalSearchQuery,
                    onSearchQueryChange = { animalSearchQuery = it },
                    selectedFilter = animalFilter,
                    onFilterChange = { animalFilter = it },
                    animals = filteredAnimals
                )

            RecordsSection.REVIEW ->
                RecordsReviewContent(
                    state = state,
                    filteredRecords = filteredRecords,
                    searchQuery = reviewSearchQuery,
                    onSearchQueryChange = { reviewSearchQuery = it },
                    onRefresh = viewModel::refresh,
                    onTypeSelected = viewModel::selectType,
                    onShowVoidedChange = viewModel::setShowVoided,
                    onVoid = { voidTarget = it }
                )
        }
    }
}

@Composable
private fun ColumnScope.AnimalsMockupContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: AnimalFilter,
    onFilterChange: (AnimalFilter) -> Unit,
    animals: List<MockAnimal>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "Animals",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = RecordsText
            )
            Text(
                text = "156 animals",
                style = MaterialTheme.typography.bodySmall,
                color = RecordsMuted
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = RecordsSoft
        ) {
            Text(
                text = "${animals.count { it.status == "At Site" }} shown on site",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = RecordsSageStrong,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Search animals") },
        placeholder = { Text("Search by tag, breed or location") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        },
        shape = RoundedCornerShape(14.dp)
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimalFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RecordsSage,
                        selectedLabelColor = Color.White
                    )
            )
        }
    }

    if (animals.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RecordsSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, RecordsBorder)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    tint = RecordsSage,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "No animals match this filter",
                    fontWeight = FontWeight.SemiBold,
                    color = RecordsText
                )
                Text(
                    text = "Try another status or search term.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RecordsMuted
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(animals, key = { it.tag }) { animal ->
                AnimalMockupCard(animal)
            }
        }
    }
}

@Composable
private fun AnimalMockupCard(
    animal: MockAnimal
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RecordsSurface),
        border = BorderStroke(1.dp, RecordsBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(58.dp)
                        .background(
                            color = RecordsSoft,
                            shape = RoundedCornerShape(13.dp)
                        ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    tint = RecordsSageStrong,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animal.tag,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RecordsText
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimalMetaChip(animal.breed)
                    AnimalMetaChip(animal.sex)
                }

                Text(
                    text = "${animal.age} · ${animal.weight}",
                    style = MaterialTheme.typography.bodySmall,
                    color = RecordsMuted,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                AnimalStatusBadge(animal.status)
                Spacer(modifier = Modifier.size(10.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = RecordsSageStrong,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimalMetaChip(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F3F1)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = RecordsMuted
        )
    }
}

@Composable
private fun AnimalStatusBadge(
    status: String
) {
    val background =
        when (status) {
            "At Site" -> Color(0xFFDFF3E5)
            "Moved" -> Color(0xFFDCEEFF)
            "Sold" -> Color(0xFFFFEED3)
            else -> Color(0xFFF9DAD7)
        }

    val foreground =
        when (status) {
            "At Site" -> Color(0xFF2F7A4C)
            "Moved" -> Color(0xFF2D6F98)
            "Sold" -> Color(0xFF936000)
            else -> Color(0xFFB23A35)
        }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}

@Composable
private fun ColumnScope.RecordsReviewContent(
    state: com.beeftech.management.viewmodel.RecordsReviewUiState,
    filteredRecords: List<ReviewRecord>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onTypeSelected: (String) -> Unit,
    onShowVoidedChange: (Boolean) -> Unit,
    onVoid: (ReviewRecord) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Captured Records",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = RecordsText
            )
            Text(
                text = "Review records and corrections",
                style = MaterialTheme.typography.bodySmall,
                color = RecordsMuted
            )
        }
        TextButton(onClick = onRefresh) { Text("Refresh") }
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        REVIEW_TYPES.forEach { (slug, label) ->
            FilterChip(
                selected = state.type == slug,
                onClick = { onTypeSelected(slug) },
                label = { Text(label) }
            )
        }
    }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Search records") },
        placeholder = { Text("Animal, worker or site") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        },
        shape = RoundedCornerShape(14.dp)
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = state.showVoided,
            onCheckedChange = onShowVoidedChange
        )
        Text(
            text = "  Show voided",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (state.needsConnection) {
        NeedsConnectionNotice("Records review")
    }

    state.error?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error
        )
    }

    state.notice?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.primary
        )
    }

    if (state.loading && state.records.isEmpty()) {
        CircularProgressIndicator()
    } else if (
        filteredRecords.isEmpty() &&
        !state.needsConnection &&
        state.error == null
    ) {
        Text(
            text = "No records to review. Captured records will appear here when available.",
            color = RecordsMuted
        )
    }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            filteredRecords,
            key = { "${it.type}-${it.id}" }
        ) { record ->
            RecordCard(
                record = record,
                onVoid = { onVoid(record) }
            )
        }
    }
}

@Composable
private fun RecordCard(
    record: ReviewRecord,
    onVoid: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RecordsSurface),
        border = BorderStroke(1.dp, RecordsBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = record.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                buildString {
                    record.submittedByUsername?.let { append(it) }
                    record.siteId?.let {
                        append(if (isEmpty()) it else " · $it")
                    }
                    record.capturedAt?.let {
                        append(
                            if (isEmpty()) {
                                formatTime(it)
                            } else {
                                " · ${formatTime(it)}"
                            }
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = RecordsMuted
            )

            if (record.isVoided) {
                Text(
                    text = "Voided: ${record.voidReason.orEmpty()}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                TextButton(onClick = onVoid) {
                    Text("Void")
                }
            }
        }
    }
}

@Composable
private fun VoidDialog(
    record: ReviewRecord,
    onDismiss: () -> Unit,
    onVoid: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Void ${record.label}?")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "The record is kept but hidden. " +
                        "The worker will need to capture it again."
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        if (it.length <= MAX_VOID_REASON_LENGTH) {
                            reason = it
                        }
                    },
                    label = { Text("Reason") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = reason.isNotBlank(),
                onClick = { onVoid(reason) }
            ) {
                Text("Void")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat(
        "d MMM yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(millis))
