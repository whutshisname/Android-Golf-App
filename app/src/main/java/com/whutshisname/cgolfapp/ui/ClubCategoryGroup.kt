package com.whutshisname.cgolfapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.whutshisname.cgolfapp.model.ClubType

@Composable
fun ClubCategoryGroup(
    clubs: List<ClubType>,
    selectedKeys: Set<String>,
    onToggle: (key: String) -> Unit,
    onSelectAll: (selectAll: Boolean) -> Unit
) {
    val selectedCount = clubs.count { it.selectionKey in selectedKeys }
    val selectAllState = when {
        selectedCount == clubs.size -> ToggleableState.On
        selectedCount == 0         -> ToggleableState.Off
        else                       -> ToggleableState.Indeterminate
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TriStateCheckbox(
                state = selectAllState,
                onClick = { onSelectAll(selectAllState != ToggleableState.On) }
            )
            Text(
                text = clubs.first().categoryLabel,
                style = MaterialTheme.typography.titleSmall
            )
        }

        clubs.forEach { club ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(club.selectionKey) }
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = club.selectionKey in selectedKeys,
                    onCheckedChange = { onToggle(club.selectionKey) }
                )
                Text(
                    text = club.displayValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
