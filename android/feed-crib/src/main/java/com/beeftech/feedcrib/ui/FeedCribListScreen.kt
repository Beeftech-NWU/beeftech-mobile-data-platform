package com.beeftech.feedcrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FeedCribListScreen(
    sessions: List<SessionItem>,
    onPenClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val visibleSessions = remember(search, sessions) {
        sessions.filter {
            search.isBlank() ||
                it.name.contains(search.trim(), ignoreCase = true) ||
                it.details.contains(search.trim(), ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FeedCribColors.LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FeedCribColors.Rust)
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = "BEEFTECH",
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Feed Crib",
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Feed readings and crib activity",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search feed records…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FeedCribColors.Rust,
                        unfocusedBorderColor = FeedCribColors.Line,
                        focusedContainerColor = FeedCribColors.LightSurface,
                        unfocusedContainerColor = FeedCribColors.LightSurface
                    )
                )
            }

            items(visibleSessions, key = { it.name }) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPenClick(session.name) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FeedCribColors.LightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = Color(0xFFE7F0FA)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF2F6FAE),
                                modifier = Modifier.padding(10.dp).size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.size(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.name,
                                color = FeedCribColors.DarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Groups,
                                    contentDescription = null,
                                    tint = FeedCribColors.MutedText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.size(5.dp))
                                Text(
                                    text = session.details,
                                    color = FeedCribColors.MutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (session.isComplete) Color(0xFFDDF4E4) else Color(0xFFFFEDD4)
                        ) {
                            Text(
                                text = if (session.isComplete) "Complete" else "In progress",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                color = if (session.isComplete) Color(0xFF2D774C) else Color(0xFF9A6517),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.size(8.dp))
                        Text("›", fontSize = 25.sp, color = FeedCribColors.RustDeep)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val target = visibleSessions.firstOrNull()?.name ?: "Pen A06"
                        onPenClick(target)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FeedCribColors.Rust)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add Reading", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FeedCribColors.InkLight)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restaurant,
                            contentDescription = null,
                            tint = FeedCribColors.RustDeep,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "Open a crib to review readings, animals and A.D.I. information.",
                            color = FeedCribColors.MutedText,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
