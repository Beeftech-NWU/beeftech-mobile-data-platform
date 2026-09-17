package com.beeftech.feedcrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FeedCribListScreen(
    sessions: List<SessionItem>,
    onPenClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var cribNumber by remember { mutableStateOf("A06") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FeedCribColors.LightBg)
            .padding(16.dp)
    ) {
        // --- Back button ---
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = FeedCribColors.DarkText
            )
        }

        // Title
        Text(
            "Feed Crib Management",
            color = FeedCribColors.DarkText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Text(
            "At the Feedlot",
            color = FeedCribColors.MutedText,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Pen icon
        Box(
            modifier = Modifier
                .size(64.dp, 44.dp)
                .clip(RoundedCornerShape(6.dp, 6.dp, 6.dp, 22.dp))
                .background(FeedCribColors.Rust)
                .clickable { /* scan */ },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "PEN",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Enter or scan crib number",
            color = FeedCribColors.DarkText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "UHF-capable devices can scan the transponder",
            color = FeedCribColors.MutedText,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Input field
        OutlinedTextField(
            value = cribNumber,
            onValueChange = { cribNumber = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = FeedCribColors.DarkText
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FeedCribColors.Rust,
                unfocusedBorderColor = FeedCribColors.Line,
                focusedTextColor = FeedCribColors.DarkText,
                unfocusedTextColor = FeedCribColors.DarkText,
                cursorColor = FeedCribColors.Rust
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { /* scan */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = FeedCribColors.Rust
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    FeedCribColors.Rust
                )
            ) {
                Text("Scan UHF Tag")
            }
            Button(
                onClick = { onPenClick(cribNumber) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FeedCribColors.Rust
                )
            ) {
                Text("Open Pen $cribNumber", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "THIS SESSION",
            color = FeedCribColors.RustDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(8.dp))

        // List of pens
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onPenClick(session.name) },
                    colors = CardDefaults.cardColors(
                        containerColor = FeedCribColors.LightSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                session.name,
                                color = FeedCribColors.DarkText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                session.details,
                                color = FeedCribColors.MutedText,
                                fontSize = 10.sp
                            )
                        }
                        if (session.isComplete) {
                            Surface(
                                color = FeedCribColors.Olive,
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    "Done",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
