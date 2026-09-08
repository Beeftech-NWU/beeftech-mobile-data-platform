package com.beeftech.feedcrib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

private enum class FeedCribScreen {
    LIST,
    DETAIL
}

@Composable
fun FeedCribFlow(
    onBackToHome: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(FeedCribScreen.LIST) }

    // State for Feed Crib
    var selectedPen by remember { mutableStateOf("A06") }
    var adiValue by remember { mutableStateOf(11.06f) }
    var readings by remember { mutableStateOf<List<CribReading>>(emptyList()) }

    val sessions = remember {
        listOf(
            SessionItem("Pen A02", "3 readings · ADI 10.8", true),
            SessionItem("Pen A04", "2 readings · ADI 11.2", true),
            SessionItem("Pen A06", "In progress", false)
        )
    }

    fun goBack() {
        if (currentScreen == FeedCribScreen.DETAIL) {
            currentScreen = FeedCribScreen.LIST
        } else {
            onBackToHome()
        }
    }

    BackHandler {
        goBack()
    }

    when (currentScreen) {
        FeedCribScreen.LIST -> {
            FeedCribListScreen(
                sessions = sessions,
                onPenClick = { pen ->
                    selectedPen = pen
                    currentScreen = FeedCribScreen.DETAIL
                },
                onBack = onBackToHome
            )
        }
        FeedCribScreen.DETAIL -> {
            CribDetailScreen(
                penName = selectedPen,
                adiValue = adiValue,
                onAdiChange = { adiValue = it },
                readings = readings,
                onReadingsChange = { readings = it },
                onBack = { currentScreen = FeedCribScreen.LIST }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedCribFlowPreview() {
    FeedCribFlow()
}
