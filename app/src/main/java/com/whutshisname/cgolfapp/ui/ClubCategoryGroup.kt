package com.whutshisname.cgolfapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
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
        selectedCount == 0          -> ToggleableState.Off
        else                        -> ToggleableState.Indeterminate
    }
    val categoryLabel = clubs.first().categoryLabel

    Column(modifier = Modifier.fillMaxWidth()) {
        CategoryHeader(
            label = categoryLabel,
            selectedCount = selectedCount,
            selectAllState = selectAllState,
            onSelectAll = { onSelectAll(selectAllState != ToggleableState.On) }
        )

        clubs.forEach { club ->
            ClubCard(
                club = club,
                isSelected = club.selectionKey in selectedKeys,
                isFavorite = club.pid in favoritePids,
                onToggle = { onToggle(club.selectionKey) },
                onToggleFavorite = { onToggleFavorite(club.pid) },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    label: String,
    selectedCount: Int,
    selectAllState: ToggleableState,
    onSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        // Selected-count pill — only when some clubs in this category are selected
        if (selectedCount > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }

        TriStateCheckbox(
            state = selectAllState,
            onClick = onSelectAll,
            modifier = Modifier.semantics {
                contentDescription = "Select all $label"
            }
        )
    }
}

@Composable
private fun ClubCard(
    club: ClubType,
    isSelected: Boolean,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Lightweight selection motion — color + border animate between states
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surface,
        label = "cardContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant,
        label = "cardBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        label = "cardBorderWidth"
    )

    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionIndicator(isSelected = isSelected)

            Text(
                text = club.displayValue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            )

            // Favorite is a separate, secondary action — its own button consumes the
            // tap so it never triggers card selection.
            IconButton(onClick = onToggleFavorite) {
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

// Circle that is empty when unselected and a filled check when selected.
// Shape change (not just color) keeps the state legible without relying on color.
@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    val fill by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "indicatorFill"
    )
    val ring by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline,
        label = "indicatorRing"
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, ring, CircleShape)
            .background(fill, CircleShape)
            .clearAndSetSemantics { }, // state announced at card level
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
