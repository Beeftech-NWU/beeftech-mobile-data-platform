package com.beeftech.management.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.database.DatabaseProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.REVIEW_TYPES
import com.beeftech.management.data.ReviewRecord
import com.beeftech.management.viewmodel.RecordsReviewViewModel
import com.beeftech.management.viewmodel.RecordsReviewViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.firstOrNull

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

/**
 * One REAL animal row shown in the Animals list.
 *
 * This deliberately contains no mock/sample fallback values.
 */
private data class AnimalListItem(
    val animalId: String,
    val tag: String,
    val breed: String,
    val sex: String,
    val age: String,
    val weight: String,
    val location: String,
    val status: String,
    val photoPath: String
)

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun RecordsReviewTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val viewModel: RecordsReviewViewModel = viewModel(
        key = "records-review-$currentUserId",
        factory = RecordsReviewViewModelFactory(apiClient)
    )

    RecordsReviewScreen(
        viewModel = viewModel,
        isAdmin = isAdmin,
        modifier = modifier
    )
}

/**
 * Records has two views:
 *
 * 1. Animals - loaded from the encrypted local BeefTech database.
 * 2. Captured Records - the existing online review/void workflow.
 *
 * No example animal rows are used at runtime.
 */
@Composable
fun RecordsReviewScreen(
    viewModel: RecordsReviewViewModel,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var selectedSection by remember { mutableStateOf(RecordsSection.ANIMALS) }
    var voidTarget by remember { mutableStateOf<ReviewRecord?>(null) }
    var siteTarget by remember { mutableStateOf<ReviewRecord?>(null) }
    LaunchedEffect(isAdmin) { if (isAdmin) viewModel.loadSites() }
    var reviewSearchQuery by remember { mutableStateOf("") }
    var animalSearchQuery by remember { mutableStateOf("") }
    var animalFilter by remember { mutableStateOf(AnimalFilter.ALL) }

    var liveAnimals by remember {
        mutableStateOf<List<AnimalListItem>>(emptyList())
    }

    var animalLoading by remember {
        mutableStateOf(true)
    }

    var animalError by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    /*
     * Use the same encrypted Room database as Calf Registration and
     * Farm Traceability. The list therefore reflects actual local records,
     * including records captured while offline.
     */
    LaunchedEffect(selectedSection) {
        if (
            selectedSection !=
            RecordsSection.ANIMALS
        ) {
            return@LaunchedEffect
        }

        animalLoading = true
        animalError = ""

        val database = DatabaseProvider.getDatabase()

        if (database == null) {
            liveAnimals = emptyList()
            animalError = "Animal data is unavailable because the local database is not initialised."
            animalLoading = false
            return@LaunchedEffect
        }

        try {
            database
                .calfRegistrationDao()
                .getAllRegistrationViews()
                .collect { registrations ->
                    val rows =
                        registrations.map { registration ->
                            val latestWeight =
                                try {
                                    database
                                        .animalWeightDao()
                                        .getLatestWeightForAnimal(
                                            registration.animalId
                                        )
                                        .firstOrNull()
                                        ?.weightKg
                                } catch (_: Exception) {
                                    null
                                }

                            val movements =
                                try {
                                    database
                                        .animalMovementDao()
                                        .getByAnimalId(
                                            registration.animalId
                                        )
                                        .filter {
                                            it.feedLocationType
                                                .isNullOrBlank()
                                        }
                                } catch (_: Exception) {
                                    emptyList()
                                }

                            val latestMovement =
                                movements.maxByOrNull {
                                    it.movementDate
                                }

                            val mortalityExists =
                                try {
                                    database
                                        .mortalityDao()
                                        .getByAnimalId(
                                            registration.animalId
                                        )
                                        .isNotEmpty()
                                } catch (_: Exception) {
                                    false
                                }

                            val destination =
                                latestMovement
                                    ?.destinationFarmId
                                    .orEmpty()
                                    .trim()

                            val status =
                                deriveAnimalStatus(
                                    mortalityExists = mortalityExists,
                                    destination = destination
                                )

                            val location =
                                cleanDestination(
                                    destination
                                )

                            val displayWeight =
                                latestWeight
                                    ?: registration.birthWeightKg

                            AnimalListItem(
                                animalId =
                                    registration.animalId,

                                tag =
                                    registration.tagNumber
                                        .trim()
                                        .ifBlank {
                                            registration.animalId
                                        },

                                breed =
                                    registration.breed
                                        .trim(),

                                sex =
                                    registration.gender
                                        .orEmpty()
                                        .trim(),

                                age =
                                    formatAnimalAge(
                                        registration.birthdate
                                    ),

                                weight =
                                    formatAnimalWeight(
                                        displayWeight
                                    ),

                                location =
                                    location,

                                status =
                                    status,

                                photoPath =
                                    registration.photoPath
                                        .orEmpty()
                                        .trim()
                            )
                        }

                    liveAnimals =
                        rows.sortedBy {
                            it.tag.lowercase(
                                Locale.ROOT
                            )
                        }

                    animalError = ""
                    animalLoading = false
                }

        } catch (exception: Exception) {
            liveAnimals = emptyList()
            animalError =
                exception.message
                    ?: "Unable to load animals from the local database."
            animalLoading = false
        }
    }

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

    val filteredAnimals =
        remember(
            liveAnimals,
            animalSearchQuery,
            animalFilter
        ) {
            val query =
                animalSearchQuery.trim()

            liveAnimals.filter { animal ->
                val matchesQuery =
                    query.isBlank() ||
                        animal.tag.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        animal.breed.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        animal.sex.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        animal.location.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        animal.status.contains(
                            query,
                            ignoreCase = true
                        )

                val matchesFilter =
                    animalFilter == AnimalFilter.ALL ||
                        animal.status.equals(
                            animalFilter.label,
                            ignoreCase = true
                        )

                matchesQuery &&
                    matchesFilter
            }
        }

    siteTarget?.let { record ->
        AssignSiteDialog(
            record = record,
            sites = state.sites.filter { it.active },
            onDismiss = { siteTarget = null },
            onAssign = { siteId, reason ->
                viewModel.assignSite(record, siteId, reason) { siteTarget = null }
            }
        )
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
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            RecordsSection
                .values()
                .forEach { section ->
                    FilterChip(
                        selected =
                            selectedSection == section,
                        onClick = {
                            selectedSection = section
                        },
                        label = {
                            Text(
                                text = section.label,
                                fontWeight =
                                    if (
                                        selectedSection ==
                                        section
                                    ) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    }
                            )
                        },
                        colors =
                            FilterChipDefaults
                                .filterChipColors(
                                    selectedContainerColor =
                                        RecordsSage,
                                    selectedLabelColor =
                                        Color.White
                                )
                    )
                }
        }

        when (selectedSection) {
            RecordsSection.ANIMALS ->
                AnimalsContent(
                    searchQuery =
                        animalSearchQuery,

                    onSearchQueryChange = {
                        animalSearchQuery = it
                    },

                    selectedFilter =
                        animalFilter,

                    onFilterChange = {
                        animalFilter = it
                    },

                    totalAnimalCount =
                        liveAnimals.size,

                    animals =
                        filteredAnimals,

                    isLoading =
                        animalLoading,

                    errorMessage =
                        animalError
                )

            RecordsSection.REVIEW ->
                RecordsReviewContent(
                    state = state,
                    filteredRecords = filteredRecords,
                    searchQuery = reviewSearchQuery,
                    onSearchQueryChange = {
                        reviewSearchQuery = it
                    },
                    onRefresh =
                        viewModel::refresh,
                    onTypeSelected =
                        viewModel::selectType,
                    onShowVoidedChange =
                        viewModel::setShowVoided,
                    onVoid = {
                        voidTarget = it
                    },
                    canAssignSite = isAdmin,
                    onAssignSite = { siteTarget = it }
                )
        }
    }
}

@Composable
private fun ColumnScope.AnimalsContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: AnimalFilter,
    onFilterChange: (AnimalFilter) -> Unit,
    totalAnimalCount: Int,
    animals: List<AnimalListItem>,
    isLoading: Boolean,
    errorMessage: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.Bottom
    ) {
        Column {
            Text(
                text = "Animals",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold,
                color =
                    RecordsText
            )

            Text(
                text =
                    "$totalAnimalCount " +
                        if (totalAnimalCount == 1) {
                            "animal"
                        } else {
                            "animals"
                        },
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    RecordsMuted
            )
        }

        Surface(
            shape =
                RoundedCornerShape(999.dp),
            color =
                RecordsSoft
        ) {
            val shownLabel =
                when {
                    searchQuery.isNotBlank() ->
                        "${animals.size} matching"

                    selectedFilter ==
                        AnimalFilter.ALL ->
                        "${animals.size} shown"

                    selectedFilter ==
                        AnimalFilter.AT_SITE ->
                        "${animals.size} at site"

                    else ->
                        "${animals.size} " +
                            selectedFilter
                                .label
                                .lowercase(
                                    Locale.ROOT
                                )
                }

            Text(
                text =
                    shownLabel,
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
                    RecordsSageStrong,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }

    OutlinedTextField(
        value =
            searchQuery,
        onValueChange =
            onSearchQueryChange,
        modifier =
            Modifier.fillMaxWidth(),
        singleLine =
            true,
        label = {
            Text("Search animals")
        },
        placeholder = {
            Text(
                "Search by tag, breed, sex, location or status"
            )
        },
        leadingIcon = {
            Icon(
                imageVector =
                    Icons.Outlined.Search,
                contentDescription =
                    null
            )
        },
        shape =
            RoundedCornerShape(14.dp)
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                ),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        AnimalFilter
            .values()
            .forEach { filter ->
                FilterChip(
                    selected =
                        selectedFilter ==
                            filter,
                    onClick = {
                        onFilterChange(
                            filter
                        )
                    },
                    label = {
                        Text(
                            filter.label
                        )
                    },
                    colors =
                        FilterChipDefaults
                            .filterChipColors(
                                selectedContainerColor =
                                    RecordsSage,
                                selectedLabelColor =
                                    Color.White
                            )
                )
            }
    }

    when {
        isLoading -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage.isNotBlank() -> {
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                RecordsSurface
                        ),
                shape =
                    RoundedCornerShape(16.dp),
                border =
                    BorderStroke(
                        1.dp,
                        RecordsBorder
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
                            "Unable to load animals",
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            RecordsText
                    )

                    Text(
                        text =
                            errorMessage,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            RecordsMuted,
                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            )
                    )
                }
            }
        }

        animals.isEmpty() -> {
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                RecordsSurface
                        ),
                shape =
                    RoundedCornerShape(16.dp),
                border =
                    BorderStroke(
                        1.dp,
                        RecordsBorder
                    )
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            22.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.Pets,
                        contentDescription =
                            null,
                        tint =
                            RecordsSage,
                        modifier =
                            Modifier.size(
                                34.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.size(
                                8.dp
                            )
                    )

                    Text(
                        text =
                            if (
                                totalAnimalCount ==
                                0
                            ) {
                                "No animals recorded yet"
                            } else {
                                "No animals match this filter"
                            },
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            RecordsText
                    )

                    Text(
                        text =
                            if (
                                totalAnimalCount ==
                                0
                            ) {
                                "Registered animals will appear here automatically."
                            } else {
                                "Try another status or search term."
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            RecordsMuted
                    )
                }
            }
        }

        else -> {
            LazyColumn(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(
                        9.dp
                    )
            ) {
                items(
                    items = animals,
                    key = {
                        it.animalId
                    }
                ) { animal ->
                    AnimalCard(
                        animal
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimalCard(
    animal: AnimalListItem
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    RecordsSurface
            ),
        border =
            BorderStroke(
                1.dp,
                RecordsBorder
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            AnimalThumbnail(
                photoPath =
                    animal.photoPath
            )

            Spacer(
                modifier =
                    Modifier.size(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        animal.tag,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        RecordsText
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    if (
                        animal.breed
                            .isNotBlank()
                    ) {
                        AnimalMetaChip(
                            animal.breed
                        )
                    }

                    if (
                        animal.sex
                            .isNotBlank()
                    ) {
                        AnimalMetaChip(
                            animal.sex
                        )
                    }
                }

                val detail =
                    listOf(
                        animal.age,
                        animal.weight,
                        animal.location
                    )
                        .filter {
                            it.isNotBlank()
                        }
                        .joinToString(" · ")

                Text(
                    text =
                        detail.ifBlank {
                            "Additional details unavailable"
                        },
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        RecordsMuted,
                    modifier =
                        Modifier.padding(
                            top = 5.dp
                        ),
                    maxLines =
                        1
                )
            }

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                AnimalStatusBadge(
                    animal.status
                )

                Spacer(
                    modifier =
                        Modifier.size(
                            10.dp
                        )
                )

                Icon(
                    imageVector =
                        Icons.Outlined
                            .ChevronRight,
                    contentDescription =
                        "Animal details",
                    tint =
                        RecordsSageStrong,
                    modifier =
                        Modifier.size(
                            20.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun AnimalThumbnail(
    photoPath: String
) {
    val bitmap =
        remember(photoPath) {
            photoPath
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    runCatching {
                        BitmapFactory
                            .decodeFile(it)
                    }
                        .getOrNull()
                }
        }

    Box(
        modifier =
            Modifier
                .size(58.dp)
                .clip(
                    RoundedCornerShape(
                        13.dp
                    )
                )
                .background(
                    RecordsSoft
                ),
        contentAlignment =
            Alignment.Center
    ) {
        if (
            bitmap != null
        ) {
            Image(
                bitmap =
                    bitmap.asImageBitmap(),
                contentDescription =
                    "Animal photo",
                modifier =
                    Modifier.fillMaxSize(),
                contentScale =
                    ContentScale.Crop
            )
        } else {
            Icon(
                imageVector =
                    Icons.Outlined.Pets,
                contentDescription =
                    null,
                tint =
                    RecordsSageStrong,
                modifier =
                    Modifier.size(
                        30.dp
                    )
            )
        }
    }
}

@Composable
private fun AnimalMetaChip(
    text: String
) {
    Surface(
        shape =
            RoundedCornerShape(6.dp),
        color =
            Color(0xFFF1F3F1)
    ) {
        Text(
            text =
                text,
            modifier =
                Modifier.padding(
                    horizontal = 7.dp,
                    vertical = 3.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                RecordsMuted
        )
    }
}

@Composable
private fun AnimalStatusBadge(
    status: String
) {
    val background =
        when (status) {
            "At Site" ->
                Color(0xFFDFF3E5)

            "Moved" ->
                Color(0xFFDCEEFF)

            "Sold" ->
                Color(0xFFFFEED3)

            "Deceased" ->
                Color(0xFFF9DAD7)

            else ->
                Color(0xFFEDEFEA)
        }

    val foreground =
        when (status) {
            "At Site" ->
                Color(0xFF2F7A4C)

            "Moved" ->
                Color(0xFF2D6F98)

            "Sold" ->
                Color(0xFF936000)

            "Deceased" ->
                Color(0xFFB23A35)

            else ->
                RecordsSageStrong
        }

    Surface(
        shape =
            RoundedCornerShape(999.dp),
        color =
            background
    ) {
        Text(
            text =
                status,
            modifier =
                Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 5.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            fontWeight =
                FontWeight.SemiBold,
            color =
                foreground
        )
    }
}

private fun deriveAnimalStatus(
    mortalityExists: Boolean,
    destination: String
): String =
    when {
        mortalityExists ->
            "Deceased"

        destination.startsWith(
            "Sold:",
            ignoreCase = true
        ) ->
            "Sold"

        destination.startsWith(
            "Another site:",
            ignoreCase = true
        ) ||
            destination.startsWith(
                "Other:",
                ignoreCase = true
            ) ->
            "Moved"

        else ->
            "At Site"
    }

private fun cleanDestination(
    rawDestination: String
): String {
    val value =
        rawDestination
            .substringBefore(
                " · "
            )
            .trim()

    return when {
        value.startsWith(
            "Sold:",
            ignoreCase = true
        ) ->
            value.substringAfter(":")
                .trim()

        value.startsWith(
            "Another site:",
            ignoreCase = true
        ) ->
            value.substringAfter(":")
                .trim()

        value.startsWith(
            "Other:",
            ignoreCase = true
        ) ->
            value.substringAfter(":")
                .trim()

        else ->
            value
    }
}

private fun formatAnimalAge(
    birthDate: Long
): String {
    if (
        birthDate <= 0L
    ) {
        return ""
    }

    val now =
        System.currentTimeMillis()

    if (
        birthDate > now
    ) {
        return ""
    }

    val elapsed =
        now - birthDate

    val months =
        (
            elapsed /
                (
                    30.4375 *
                        24.0 *
                        60.0 *
                        60.0 *
                        1000.0
                )
        )
            .toInt()
            .coerceAtLeast(0)

    return if (
        months < 24
    ) {
        "$months " +
            if (months == 1) {
                "month"
            } else {
                "months"
            }
    } else {
        val years =
            months / 12

        "$years " +
            if (years == 1) {
                "year"
            } else {
                "years"
            }
    }
}

private fun formatAnimalWeight(
    weight: Double?
): String {
    val value =
        weight
            ?.takeIf {
                it >= 0.0
            }
            ?: return ""

    return if (
        value % 1.0 == 0.0
    ) {
        "${value.toInt()} kg"
    } else {
        String.format(
            Locale.US,
            "%.1f kg",
            value
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
    onVoid: (ReviewRecord) -> Unit,
    canAssignSite: Boolean,
    onAssignSite: (ReviewRecord) -> Unit
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
                onVoid = { onVoid(record) },
                onAssignSite = if (canAssignSite && record.siteId == null && !record.isVoided &&
                    (record.type == "farmers" || record.type == "calf-registrations")) {
                    { onAssignSite(record) }
                } else null
            )
        }
    }
}

@Composable
private fun RecordCard(
    record: ReviewRecord,
    onVoid: () -> Unit,
    onAssignSite: (() -> Unit)? = null
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
                onAssignSite?.let { action ->
                    TextButton(onClick = action) { Text("Assign site") }
                }
                TextButton(onClick = onVoid) {
                    Text("Void")
                }
            }
        }
    }
}

@Composable
private fun AssignSiteDialog(
    record: ReviewRecord,
    sites: List<com.beeftech.management.data.Site>,
    onDismiss: () -> Unit,
    onAssign: (String, String) -> Unit
) {
    var selectedId by remember(record.id) { mutableStateOf("") }
    var reason by remember(record.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign ${record.label} to a site") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assign only after verifying the record belongs to the selected site. This is audited and cannot transfer records already assigned elsewhere.")
                if (sites.isEmpty()) Text("No active sites available")
                sites.forEach { site ->
                    FilterChip(
                        selected = selectedId == site.siteId,
                        onClick = { selectedId = site.siteId },
                        label = { Text(site.name) }
                    )
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= MAX_VOID_REASON_LENGTH) reason = it },
                    label = { Text("Verification reason (required)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedId.isNotBlank() && reason.isNotBlank(),
                onClick = { onAssign(selectedId, reason) }
            ) { Text("Confirm assignment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
