package com.whutshisname.cgolfapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.whutshisname.cgolfapp.model.ClubType

@Composable
fun ClubCategoryGroup(
    clubs: List<ClubType>,
    selectedKeys: Set<String>,
    favoritePids: Set<String>,
    onToggle: (key: String) -> Unit,
    onSelectAll: (selectAll: Boolean) -> Unit,
    onToggleFavorite: (pid: String) -> Unit
) {
    val selectedCount = clubs.count { it.selectionKey in selectedKeys }
    val selectAllState = when {
        selectedCount == clubs.size -> ToggleableState.On
        selectedCount == 0         -> ToggleableState.Off
        else                       -> ToggleableState.Indeterminate
    }
    val categoryLabel = clubs.first().categoryLabel

    Column(modifier = Modifier.fillMaxWidth()) {
        // Category header — tinted surface so it stands out from club rows
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TriStateCheckbox(
                    state = selectAllState,
                    onClick = { onSelectAll(selectAllState != ToggleableState.On) },
                    modifier = Modifier.semantics {
                        contentDescription = "Select all $categoryLabel"
                    }
                )
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Selection count badge — only shown when some clubs are selected
                if (selectedCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "$selectedCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // Club rows
        clubs.forEach { club ->
            val isSelected = club.selectionKey in selectedKeys
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .clickable(
                            onClickLabel = if (isSelected) "Deselect ${club.displayValue}"
                                           else "Select ${club.displayValue}"
                        ) { onToggle(club.selectionKey) }
                        .padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle(club.selectionKey) },
                        modifier = Modifier.semantics {
                            contentDescription = "${club.displayValue}, ${if (isSelected) "selected" else "not selected"}"
                        }
                    )
                    Text(
                        text = club.displayValue,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp).weight(1f)
                    )
                    val isFavorite = club.pid in favoritePids
                    IconButton(onClick = { onToggleFavorite(club.pid) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Remove ${club.displayValue} from favorites"
                                                 else "Add ${club.displayValue} to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
