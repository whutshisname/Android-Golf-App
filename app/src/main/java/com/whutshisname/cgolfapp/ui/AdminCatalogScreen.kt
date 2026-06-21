package com.whutshisname.cgolfapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whutshisname.cgolfapp.model.ClubType
import com.whutshisname.cgolfapp.model.parseCallawayUrl

/**
 * Hidden, functional admin screen for managing the local catalog override.
 * Reached via long-press on the brand header. Lets the operator add clubs from a
 * Callaway Preowned URL and hide/restore bundled clubs — all persisted locally,
 * never touching the bundled club_types.json asset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCatalogScreen(
    effectiveClubs: List<ClubType>,
    hiddenClubs: List<ClubType>,
    onAddClub: (ClubType) -> Unit,
    onHideClub: (pid: String) -> Unit,
    onRestoreClub: (pid: String) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Admin · Catalog") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { AddClubSection(onAddClub = onAddClub) }

            item { SectionTitle("Visible Clubs (${effectiveClubs.size})") }
            items(effectiveClubs, key = { it.selectionKey }) { club ->
                ClubAdminRow(
                    title = club.displayValue,
                    subtitle = "${club.cgid} · ${club.pid}",
                    actionLabel = "Hide",
                    onAction = { onHideClub(club.pid) }
                )
            }

            item { SectionTitle("Hidden Clubs (${hiddenClubs.size})") }
            if (hiddenClubs.isEmpty()) {
                item {
                    Text(
                        "No hidden clubs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(hiddenClubs, key = { "hidden-${it.pid}" }) { club ->
                ClubAdminRow(
                    title = club.displayValue,
                    subtitle = "${club.cgid} · ${club.pid}",
                    actionLabel = "Restore",
                    onAction = { onRestoreClub(club.pid) }
                )
            }
        }
    }
}

@Composable
private fun AddClubSection(onAddClub: (ClubType) -> Unit) {
    var url by remember { mutableStateOf("") }
    var cgid by remember { mutableStateOf("") }
    var displayValue by remember { mutableStateOf("") }
    var pid by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Add Club From URL")

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Callaway Preowned URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    val result = parseCallawayUrl(url)
                    if (result == null) {
                        message = "Could not parse that URL."
                        parsed = false
                    } else {
                        cgid = result.cgid
                        pid = result.pid
                        displayValue = result.displayValue
                        parsed = true
                        message = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Parse URL") }

            if (parsed) {
                OutlinedTextField(
                    value = cgid,
                    onValueChange = { cgid = it },
                    label = { Text("cgid (category)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayValue,
                    onValueChange = { displayValue = it },
                    label = { Text("displayValue (name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pid,
                    onValueChange = { pid = it },
                    label = { Text("pid (product id)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (cgid.isBlank() || pid.isBlank() || displayValue.isBlank()) {
                            message = "All three fields are required."
                        } else {
                            onAddClub(
                                ClubType(
                                    cgid = cgid.trim(),
                                    displayValue = displayValue.trim(),
                                    pid = pid.trim()
                                )
                            )
                            message = "Added \"${displayValue.trim()}\"."
                            url = ""; cgid = ""; displayValue = ""; pid = ""; parsed = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add Club") }
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ClubAdminRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
