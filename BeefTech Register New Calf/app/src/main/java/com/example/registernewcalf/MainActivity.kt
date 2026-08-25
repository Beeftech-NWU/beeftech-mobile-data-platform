package com.example.registernewcalf


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "login") {
                composable("login") { LoginScreen(navController, dbHelper) }
                composable("modules") { ModuleSelectionScreen(navController) }
                composable("calfRegistration") { CalfRegistrationScreen(navController, dbHelper) }
            }
        }

    }
}
@Composable
fun CalfRegistrationScreen(navController : NavController, dbHelper: CalfDatabaseHelper) {
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
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Save Calf")
        }

        if (message.isNotEmpty()) {
            Text(message, color = if (message.contains("success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Saved Calves:")
        calves.forEach{ calf ->
            Text(text = calf)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("modules") }) {
            Text("Back to Modules")
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, dbHelper: CalfDatabaseHelper) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("App Login", style = MaterialTheme.typography.headlineMedium)

        TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())

        Button(onClick = {
            if (username == "farmer" && password == "1234") {
                navController.navigate("modules")
            } else {
                errorMessage = "Invalid credentials"
            }
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Log In")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ModuleSelectionScreen(navController: NavController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Module Selection Dashboard", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = { navController.navigate("calfRegistration") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Calf Registration")
        }

        Button(onClick = { navController.navigate("feedCrib") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Feed Crib Management")
        }

        Button(onClick = { navController.navigate("traceability") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Farmer & Farm Traceability")
        }
    }
}




