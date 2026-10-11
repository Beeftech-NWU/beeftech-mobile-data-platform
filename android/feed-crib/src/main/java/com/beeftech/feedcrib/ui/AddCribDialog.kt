package com.beeftech.feedcrib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.beeftech.feedcrib.data.AddCribForm
import com.beeftech.feedcrib.data.CribSetup
import com.beeftech.feedcrib.data.adjustInitialAdi

@Composable
fun AddCribDialog(
    setup: CribSetup, saving: Boolean, errors: Map<String, String>,
    setupMessage: String?, onSave: (AddCribForm) -> Unit, onDismiss: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    var site by rememberSaveable { mutableStateOf("") }
    var pen by rememberSaveable { mutableStateOf("") }
    var ration by rememberSaveable { mutableStateOf("") }
    var adi by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("") }
    var rfid by rememberSaveable { mutableStateOf("") }
    var edited by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(setup.sites) {
        if (site.isEmpty() && setup.sites.size == 1) site = setup.sites.single().id
    }
    fun cancel() { if (!saving) { if (edited) confirmDiscard = true else onDismiss() } }
    BackHandler { cancel() }
    val selectedPen = setup.pens.firstOrNull { it.id == pen && it.siteId == site }
    val setupMissing = setup.sites.isEmpty() || setup.rations.none { it.active } ||
        (site.isNotEmpty() && setup.pens.none { it.siteId == site && it.active })

    AlertDialog(
        onDismissRequest = ::cancel,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add crib")
                if (errors.isNotEmpty()) Text(
                    "Failed to save crib. " + errors.values.distinct().joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Register a physical crib. This does not create a reading.")
                if (setupMessage != null) Text(setupMessage)
                if (setupMissing) Text("Setup/synchronisation is needed: an existing site, pen and active ration are required.",
                    color = MaterialTheme.colorScheme.error)
                CribField("Crib number/code *", code, errors["code"], saving) { code = it; edited = true }
                CribChoice("Farm/feedlot *", site, setup.sites.map { it.id to it.name }, errors["site"], saving) {
                    if (it != site) { site = it; pen = "" }; edited = true
                }
                CribChoice("Pen *", pen, setup.pens.filter { it.siteId == site && it.active }.map { it.id to it.description },
                    errors["pen"], saving) { pen = it; edited = true }
                selectedPen?.let { p ->
                    Text("Pen: ${p.description}")
                    Text("Begin: ${p.animalsBegin ?: "Not available"} · In: ${p.animalsIn ?: "Not available"}\nOut: ${p.animalsOut ?: "Not available"} · Close: ${p.animalsClose ?: "Not available"}")
                    Text("Required feed: ${p.requiredKg?.let { formatKg(it) } ?: "Not available"}")
                }
                CribChoice("Ration/feed type *", ration, setup.rations.filter { it.active }.map { it.id to it.name },
                    errors["ration"], saving) { ration = it; edited = true }
                CribField("Description", description, errors["description"], saving) { description = it; edited = true }
                if (setup.methods.isNotEmpty()) CribChoice("Feeding method (optional)", method,
                    listOf("" to "Not specified") + setup.methods.map { it to it }, errors["method"], saving) {
                    method = it; edited = true
                }
                CribField("Initial ADI *", adi, errors["adi"], saving, KeyboardType.Decimal) { adi = it; edited = true }
                Text("Temporary ADI rule: minimum ${setup.adiRules.minimum}" +
                    (setup.adiRules.maximum?.let { ", maximum $it" } ?: ", no confirmed maximum") +
                    ". Units: ${setup.adiRules.units ?: "unconfirmed"}.")
                Row {
                    FeedCribTooltip("Decrease initial ADI by the configured step.") {
                        TextButton(enabled = !saving, onClick = {
                            adi = adjustInitialAdi(adi, -1, setup.adiRules)
                            edited = true
                        }) { Text("−") }
                    }
                    FeedCribTooltip("Increase initial ADI by the configured step.") {
                        TextButton(enabled = !saving, onClick = {
                            adi = adjustInitialAdi(adi, 1, setup.adiRules)
                            edited = true
                        }) { Text("+") }
                    }
                }
                CribField("UHF RFID tag (optional)", rfid, errors["rfid"], saving) { rfid = it; edited = true }
                Text("Status: Active. Creation time, creator and device are captured automatically. Location is unavailable if no recent permitted fix exists.")
                errors["form"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            FeedCribTooltip("Save this new crib on the phone and queue it for synchronisation.") {
                TextButton(enabled = !saving && !setupMissing, onClick = {
                    onSave(AddCribForm(code, site, pen, ration, adi, description, method, rfid))
                }) { Text(if (saving) "Saving…" else "Save") }
            }
        },
        dismissButton = { FeedCribTooltip("Close this form; you will be asked before discarding changes.") {
            TextButton(enabled = !saving, onClick = ::cancel) { Text("Cancel") }
        } }
    )
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard new crib?") },
        text = { Text("The crib has not been saved.") },
        confirmButton = { FeedCribTooltip("Discard this unsaved crib and return to the list.") {
            TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("Discard") }
        } },
        dismissButton = { FeedCribTooltip("Return to the form without losing your changes.") {
            TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
        } }
    )
}

@Composable
private fun CribField(
    label: String, value: String, error: String?, saving: Boolean,
    keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit
) {
    OutlinedTextField(value = value, onValueChange = onChange, enabled = !saving,
        label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        isError = error != null, supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}

@Composable
private fun CribChoice(
    label: String, value: String, options: List<Pair<String, String>>, error: String?,
    saving: Boolean, onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label)
        Box {
            FeedCribTooltip("Choose an existing option for $label.") {
                OutlinedButton(enabled = !saving && options.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(), onClick = { expanded = true }) {
                    Text(options.firstOrNull { it.first == value }?.second ?: "Select")
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; onSelect(id) })
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
    }
}

