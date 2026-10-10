package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.FarmerAnimalLink
import com.beeftech.farmtraceability.data.PendingAssignmentDiagnosis

/** Offline-first selection. Existing sync/recovery operations are intentionally unchanged. */
private enum class AnimalPickerFilter { ALL, UNASSIGNED, OTHER_FARMER, THIS_FARMER }
@Composable
fun FarmerAnimalAssignmentScreen(
    farmerName: String,
    farmerId: String,
    preselectedAnimalId: String? = null,
    animals: List<Animal>,
    animalTags: Map<String, String> = emptyMap(),
    farmerLabels: Map<String, String> = emptyMap(),
    activeLinks: List<FarmerAnimalLink>,
    isBusy: Boolean,
    isDownloading: Boolean = false,
    onDownloadSiteCalves: () -> Unit = {},
    pendingDiagnostics: List<PendingAssignmentDiagnosis> = emptyList(),
    pendingHistoricalRecordGuids: Set<String> = emptySet(),
    pendingDiagnosisMessage: String = "",
    checkingPending: Boolean = false,
    restoringParent: Boolean = false,
    onRestoreMissing: (String, String, String) -> Unit = { _, _, _ -> },
    onEndTestAssignment: (String) -> Unit = {},
    onCheckPending: () -> Unit = {},
    message: String,
    onAssign: (List<String>, (List<String>) -> Unit) -> Unit,
    onRegisterCalf: () -> Unit,
    onBack: () -> Unit
) {
    val selected = remember(farmerId) { mutableStateListOf<String>() }
    var searchText by remember(farmerId) { mutableStateOf("") }
    var filter by remember(farmerId) { mutableStateOf(AnimalPickerFilter.ALL) }
    var showTools by remember(farmerId) { mutableStateOf(false) }
    var transferAnimalId by remember(farmerId) { mutableStateOf<String?>(null) }
    var showPendingReport by remember(farmerId) { mutableStateOf(false) }
    var restoreTarget by remember(farmerId) { mutableStateOf<Pair<String, String>?>(null) }
    var endTestTarget by remember(farmerId) { mutableStateOf<PendingAssignmentDiagnosis?>(null) }
    var verificationReason by remember(farmerId) { mutableStateOf("") }

    val owners = remember(activeLinks) { activeLinks.associateBy { it.animalId } }
    val assignedHere = remember(activeLinks, farmerId) {
        activeLinks.filter { it.farmerId == farmerId }.map { it.animalId }.toSet()
    }
    val unassignedCount = animals.count { it.animalId !in owners }
    val otherFarmerCount = animals.count {
        owners[it.animalId]?.farmerId?.let { owner -> owner != farmerId } == true
    }
    val matchingAnimals = remember(animals, animalTags, searchText, filter, owners, farmerId) {
        val query = searchText.trim()
        animals.asSequence().filter { animal ->
            val owner = owners[animal.animalId]?.farmerId
            val matchesFilter = when (filter) {
                AnimalPickerFilter.ALL -> true
                AnimalPickerFilter.UNASSIGNED -> owner == null
                AnimalPickerFilter.OTHER_FARMER -> owner != null && owner != farmerId
                AnimalPickerFilter.THIS_FARMER -> owner == farmerId
            }
            matchesFilter && (query.isEmpty() ||
                animalTags[animal.animalId].orEmpty().contains(query, ignoreCase = true) ||
                animal.breed.contains(query, ignoreCase = true) ||
                animal.gender.orEmpty().contains(query, ignoreCase = true) ||
                animal.animalId.contains(query, ignoreCase = true))
        }.sortedWith(
            compareBy<Animal> { animal ->
                when (owners[animal.animalId]?.farmerId) {
                    null -> 0     // Unassigned animals first
                    farmerId -> 2 // Already on this farm last
                    else -> 1
                }
            }.thenBy { animalTags[it.animalId].orEmpty().lowercase() }.thenBy { it.animalId }
        ).toList()
    }

    LaunchedEffect(farmerId, preselectedAnimalId, animals, activeLinks) {
        if (preselectedAnimalId != null &&
            animals.any { it.animalId == preselectedAnimalId } &&
            preselectedAnimalId !in selected && owners[preselectedAnimalId] == null
        ) selected.add(preselectedAnimalId)
    }

    // Bulk assignment is strictly for unassigned animals. Moving an existing assignment
    // must always go through the separate transfer action and its confirmation dialog.
    LaunchedEffect(owners) { selected.removeAll { owners.containsKey(it) } }
    val submitSelection: () -> Unit = {
        if (!isBusy) {
            val requested = selected.filter { id -> owners[id] == null && animals.any { it.animalId == id } }
            if (requested.isNotEmpty()) {
                onAssign(requested) { savedIds ->
                    // Keep unsaved choices selected if the local operation partially fails.
                    savedIds.forEach { selected.remove(it) }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("← Back to farmer")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showTools = !showTools }) {
                Text(if (showTools) "Hide tools ↑" else "Tools & sync ↓")
            }
        }
        Text("Assign animals", style = MaterialTheme.typography.headlineSmall)
        Text(
            farmerName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${assignedHere.size} assigned here  •  $unassignedCount unassigned  •  $otherFarmerCount with other farmers",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        if (showTools) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Farm tools", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(
                        onClick = onDownloadSiteCalves,
                        enabled = !isBusy && !isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isDownloading) "Downloading…" else "Download farm animals for offline use") }
                    OutlinedButton(
                        onClick = { showPendingReport = true; onCheckPending() },
                        enabled = !isBusy && !checkingPending,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (checkingPending) "Checking issues…" else "Check pending assignment issues") }
                }
            }
        }

        if (showPendingReport) {
            AlertDialog(
                onDismissRequest = { showPendingReport = false },
                title = { Text("Pending assignment report") },
                text = {
                    Column(
                        Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (checkingPending) CircularProgressIndicator()
                        else if (pendingDiagnosisMessage.isNotBlank()) Text(pendingDiagnosisMessage)
                        if (!checkingPending && pendingDiagnostics.isEmpty() && pendingDiagnosisMessage.isBlank()) {
                            Text("There are no pending farmer–animal assignments.")
                        }
                        pendingDiagnostics.forEachIndexed { index, result ->
                            val isHistorical = result.recordGuid in pendingHistoricalRecordGuids
                            val tag = animalTags[result.animalId]
                            Text("${index + 1}. Animal ${tag ?: result.animalId.take(8)} · Farmer ${farmerLabels[result.farmerId] ?: result.farmerId.take(8)}",
                                style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (isHistorical) "ENDED · Previous farmer (history)"
                                else "CURRENT · Active farmer assignment",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isHistorical) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.primary
                            )
                            Text("Farmer: ${result.farmerStatus}")
                            Text("Calf: ${result.calfStatus}")
                            if (isHistorical) {
                                Text(
                                    "This farmer is a previous owner, not the current one. Do not restore " +
                                        "their registration just to clear history. Sync will request a " +
                                        "closure or retain the ended link locally as unsent history.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(result.advice, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!isHistorical && result.farmerStatus.contains("Missing or inaccessible", ignoreCase = true)) {
                                OutlinedButton(
                                    onClick = { endTestTarget = result },
                                    enabled = !isBusy && !checkingPending && !restoringParent
                                ) { Text("End invalid test assignment") }
                                Text(
                                    "For test data only. Closes the active link without deleting " +
                                        "the farmer, calf or ownership history. The server will " +
                                        "verify the closure during synchronization.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (result.canRestore && !isHistorical) {
                                Text("Only restore verified originals belonging to this farm. " +
                                    "Existing server records will never be overwritten.", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(
                                    onClick = { restoreTarget = result.recordGuid to "FARMER"; verificationReason = "" },
                                    enabled = !restoringParent
                                ) { Text("Check / restore original farmer") }
                                OutlinedButton(
                                    onClick = { restoreTarget = result.recordGuid to "CALF"; verificationReason = "" },
                                    enabled = !restoringParent
                                ) { Text("Check / restore original calf") }
                            }
                            HorizontalDivider()
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showPendingReport = false }) { Text("Close") } }
            )
        }

        endTestTarget?.let { target ->
            val tag = animalTags[target.animalId]?.takeIf { it.isNotBlank() }
                ?: "Animal ${target.animalId.take(8)}"
            val ownerName = farmerLabels[target.farmerId]?.takeIf { it.isNotBlank() }
                ?: "the test farmer"
            AlertDialog(
                onDismissRequest = { if (!isBusy) endTestTarget = null },
                title = { Text("End test assignment?") },
                text = {
                    Text(
                        "End the CURRENT assignment of $tag to $ownerName? " +
                            "The calf and farmer registrations will not be deleted. " +
                            "Previous assignments remain in history. The closure will " +
                            "remain pending until the server acknowledges it or confirms " +
                            "that this test link was never uploaded. The animal will then " +
                            "be unassigned and available for a future assignment."
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !isBusy && !checkingPending && !restoringParent,
                        onClick = {
                            endTestTarget = null
                            onEndTestAssignment(target.recordGuid)
                        }
                    ) { Text("End assignment") }
                },
                dismissButton = {
                    TextButton(onClick = { endTestTarget = null }, enabled = !isBusy) {
                        Text("Keep assignment")
                    }
                }
            )
        }

        if (restoreTarget != null) {
            val target = restoreTarget!!
            AlertDialog(
                onDismissRequest = { if (!restoringParent) restoreTarget = null },
                title = { Text("Verify ${if (target.second == "FARMER") "farmer" else "calf"} restoration") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Only continue if you've checked the original registration belongs to your assigned farm. " +
                            "This creates a missing server record using its original ID; it will never overwrite an existing one.")
                        OutlinedTextField(
                            value = verificationReason,
                            onValueChange = { verificationReason = it },
                            label = { Text("Verification reason (required)") },
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !restoringParent && verificationReason.trim().length in 15..500,
                        onClick = {
                            onRestoreMissing(target.first, target.second, verificationReason.trim())
                            restoreTarget = null
                        }
                    ) { Text("Restore only if missing") }
                },
                dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("Cancel") } }
            )
        }

        transferAnimalId?.let { animalId ->
            val fromFarmerId = owners[animalId]?.farmerId
            val fromFarmerName = fromFarmerId?.let { farmerLabels[it] }
                ?.takeIf { it.isNotBlank() } ?: "another farmer"
            val tag = animalTags[animalId]?.takeIf { it.isNotBlank() }
                ?: "Animal ${animalId.take(8)}"
            val canTransfer = !isBusy && fromFarmerId != null && fromFarmerId != farmerId &&
                animals.any { it.animalId == animalId }
            AlertDialog(
                onDismissRequest = { if (!isBusy) transferAnimalId = null },
                title = { Text("Transfer $tag?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current farmer: $fromFarmerName")
                        Text("New farmer: $farmerName")
                        Text(
                            "This will end the current assignment and save a new one on this " +
                                "device for synchronization. Only continue if this transfer is correct."
                        )
                        if (!canTransfer) {
                            Text("This animal is no longer available to transfer.",
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = canTransfer,
                        onClick = {
                            // Keep transfer separate from the bulk unassigned-animal selection.
                            if (owners[animalId]?.farmerId == fromFarmerId && canTransfer) {
                                transferAnimalId = null
                                onAssign(listOf(animalId)) { /* The flow updates the record and message. */ }
                            }
                        }
                    ) { Text("Confirm transfer") }
                },
                dismissButton = {
                    TextButton(onClick = { transferAnimalId = null }, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search tag or breed") },
            leadingIcon = { Text("⌕", style = MaterialTheme.typography.titleLarge) },
            trailingIcon = if (searchText.isNotBlank()) {
                { TextButton(onClick = { searchText = "" }) { Text("Clear") } }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(AnimalPickerFilter.ALL, "All", animals.size),
                Triple(AnimalPickerFilter.UNASSIGNED, "Unassigned", unassignedCount),
                Triple(AnimalPickerFilter.OTHER_FARMER, "Assigned elsewhere", otherFarmerCount),
                Triple(AnimalPickerFilter.THIS_FARMER, "Assigned here", assignedHere.size)
            ).forEach { (option, label, count) ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text("$label ($count)", maxLines = 1) }
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${matchingAnimals.size} animals",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (selected.isNotEmpty()) {
                TextButton(onClick = { selected.clear() }, enabled = !isBusy) { Text("Clear selected") }
            } else {
                val unassignedVisible = matchingAnimals.filter { it.animalId !in owners }
                if (unassignedVisible.isNotEmpty()) {
                    TextButton(
                        onClick = { unassignedVisible.forEach { selected.add(it.animalId) } },
                        enabled = !isBusy
                    ) { Text("Select unassigned") }
                }
            }
        }

        if (matchingAnimals.isEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (animals.isEmpty()) "No animals saved on this device"
                        else "No animals found",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (animals.isEmpty()) "Use Tools & sync to download farm calves, or register a new calf."
                        else "Try a different search or change the filter.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (animals.isEmpty()) {
                        OutlinedButton(onClick = onDownloadSiteCalves, enabled = !isDownloading && !isBusy) {
                            Text(if (isDownloading) "Downloading…" else "Download farm animals")
                        }
                        TextButton(onClick = onRegisterCalf) { Text("Register new calf") }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matchingAnimals, key = { it.animalId }) { animal ->
                    val ownerId = owners[animal.animalId]?.farmerId
                    val alreadyHere = ownerId == farmerId
                    val assignedElsewhere = ownerId != null && ownerId != farmerId
                    val isSelected = animal.animalId in selected
                    val tag = animalTags[animal.animalId]?.takeIf { it.isNotBlank() }
                    val detail = listOfNotNull(
                        animal.breed.takeIf { it.isNotBlank() },
                        animal.gender?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ").ifBlank { "Registered animal" }
                    val status = when {
                        alreadyHere -> "Assigned to this farmer"
                        assignedElsewhere -> "Assigned to ${ownerId?.let { farmerLabels[it] }?.takeIf { it.isNotBlank() } ?: "another farmer"}"
                        else -> "Unassigned · Available to add"
                    }
                    val toggle: () -> Unit = {
                        if (!alreadyHere && !assignedElsewhere && !isBusy) {
                            if (isSelected) selected.remove(animal.animalId)
                            else selected.add(animal.animalId)
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().then(
                            if (!alreadyHere && !assignedElsewhere) {
                                Modifier.clickable(enabled = !isBusy, onClick = toggle)
                            } else Modifier
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (alreadyHere) {
                                Text("✓", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleLarge)
                            } else if (!assignedElsewhere) {
                                // Only unassigned animals may be included in bulk selection.
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    tag ?: "Animal ${animal.animalId.take(8)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (assignedElsewhere) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (assignedElsewhere) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { transferAnimalId = animal.animalId },
                                    enabled = !isBusy,
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) { Text("Transfer animal") }
                            }
                        }
                    }
                }
            }
        }

        // The action stays visible while the list scrolls; never hide the selected count.
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = submitSelection,
                    enabled = selected.isNotEmpty() && !isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isBusy) "Saving assignment…"
                        else if (selected.isEmpty()) "Select animals to assign"
                        else "Assign ${selected.size} unassigned animal${if (selected.size == 1) "" else "s"}"
                    )
                }
            }
        }
    }
}
