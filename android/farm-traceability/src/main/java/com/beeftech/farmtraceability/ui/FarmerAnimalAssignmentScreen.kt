package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.FarmerAnimalLink

/** Offline-first animal selection; the backend sync contract is a separate phase. */
@Composable
fun FarmerAnimalAssignmentScreen(
    farmerName: String,
    farmerId: String,
    animals: List<Animal>,
    activeLinks: List<FarmerAnimalLink>,
    isBusy: Boolean,
    message: String,
    onAssign: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val selected = remember(farmerName) { mutableStateListOf<String>() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("Back to farmer") }
        Text("Assign registered animals", style = MaterialTheme.typography.headlineSmall)
        Text(farmerName, style = MaterialTheme.typography.titleMedium)
        Text("Assignments are saved on this device. Server sync is not yet implemented.")
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        val ownedIds = activeLinks.filter { it.farmerId == farmerId }.map { it.animalId }.toSet()
        LazyColumn(Modifier.weight(1f)) {
            items(animals, key = { it.animalId }) { animal ->
                val owner = activeLinks.firstOrNull { it.animalId == animal.animalId }
                val alreadyAssigned = animal.animalId in ownedIds
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Checkbox(
                        checked = alreadyAssigned || selected.contains(animal.animalId),
                        onCheckedChange = { checked ->
                            if (!alreadyAssigned && !isBusy) {
                                if (checked) selected.add(animal.animalId) else selected.remove(animal.animalId)
                            }
                        },
                        enabled = !alreadyAssigned && !isBusy
                    )
                    Column {
                        Text(animal.animalId, style = MaterialTheme.typography.bodyMedium)
                        Text(when { alreadyAssigned -> "Already assigned"; owner != null -> "Currently assigned elsewhere — reassigning will end the previous link"; else -> "Breed: ${animal.breed}" })
                    }
                }
            }
        }
        Button(
            onClick = { onAssign(selected.toList()); selected.clear() },
            enabled = selected.isNotEmpty() && !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isBusy) "Saving…" else "Assign selected animals (${selected.size})") }
    }
}
