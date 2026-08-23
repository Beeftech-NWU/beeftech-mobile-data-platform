package com.example.registernewcalf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = CalfDatabaseHelper(this)

        insertCalf(
            dbHelper,
            "CALF001",
            "2026-08-20",
            "Brahman",
            "DAM123",
            "SIRE456",
            photoPath = "/storage/emulated/0/DCIM/calf001.jpg",
            gpsLat = -26.7167,
            gpsLng = 27.1000
        )

        setContent {
            RegistrationForm(dbHelper)
        }
    }
}
@Composable
fun RegistrationForm(dbHelper: CalfDatabaseHelper) {
    var animalId by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var damId by remember { mutableStateOf("") }
    var sireId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var calves by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        calves = getAllCalves(dbHelper)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = animalId, onValueChange = { animalId = it }, label = { Text("Animal ID") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Birth Date (YYYY-MM-DD)") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = breed, onValueChange = { breed = it }, label = { Text("Breed") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = damId, onValueChange = { damId = it }, label = { Text("Dam ID") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = sireId, onValueChange = { sireId = it }, label = { Text("Sire ID") })
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val success = insertCalf(dbHelper, animalId, birthDate, breed, damId, sireId)
            message = if (success) "Calf saved successfully" else "Error: Validation or Duplicate"
        }) {
            Text("Save Calf")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Saved Calves:")
        calves.forEach{ calf ->
            Text(text = calf)
        }
    }
}


