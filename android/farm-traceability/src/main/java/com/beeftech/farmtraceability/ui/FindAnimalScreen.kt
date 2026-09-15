package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FindAnimalScreen(
    onBackClick: () -> Unit = {},
    onFindAnimal: (String) -> Unit = {}
) {
    var tagReference by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TraceabilityHeader(
            eyebrow = "FARM TRACEABILITY",
            title = "Find Animal",
            subtitle = "Search using the animal's tag reference",
            icon = Icons.Outlined.Search,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            TraceabilitySectionTitle(
                title = "Animal Lookup"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityTextField(
                    label = "Tag Reference",
                    value = tagReference,
                    onValueChange = {
                        tagReference = it.trimStart()
                    },
                    icon = Icons.Outlined.Tag
                )

                Spacer(modifier = Modifier.height(20.dp))

                TraceabilityPrimaryButton(
                    text = "Find Animal",
                    icon = Icons.Outlined.Search,
                    onClick = {
                        val reference = tagReference.trim()

                        if (reference.isNotBlank()) {
                            onFindAnimal(reference)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FindAnimalScreenPreview() {
    FindAnimalScreen()
}