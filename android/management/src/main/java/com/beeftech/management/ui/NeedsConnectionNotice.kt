package com.beeftech.management.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/* The online-only screens all say the same thing when a call fails for lack of signal. */
@Composable
fun NeedsConnectionNotice(subject: String) {
    Text(
        "$subject needs a connection. Check your signal and tap Refresh.",
        color = MaterialTheme.colorScheme.error
    )
}
