package com.whutshisname.cgolfapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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

/**
 * Emits one collapsible category into a LazyColumn: a sticky header followed by
 * the club cards (only when [expanded]). Building this directly into LazyListScope
 * — rather than as a single composable item — is what enables sticky headers and
 * per-card laziness. The club card design itself is unchanged.
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.clubCategorySection(
    cgid: String,
    label: String,
    clubs: List<ClubType>,
    selectedKeys: Set<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (key: String) -> Unit,
    onSelectAll: (selectAll: Boolean) -> Unit
) {
    val selectedCount = clubs.count { it.selectionKey in selectedKeys }
    val selectAllState = when {
        selectedCount == clubs.size -> ToggleableState.On
        selectedCount == 0          -> ToggleableState.Off
        else                        -> ToggleableState.Indeterminate
    }

    stickyHeader(key = "header-$cgid") {
        CategoryHeader(
            label = label,
            clubCount = clubs.size,
            selectedCount = selectedCount,
            expanded = expanded,
            selectAllState = selectAllState,
            onToggleExpanded = onToggleExpanded,
            onSelectAll = { onSelectAll(selectAllState != ToggleableState.On) }
        )
    }

    if (expanded) {
        items(clubs, key = { it.selectionKey }) { club ->
            ClubCard(
                club = club,
                isSelected = club.selectionKey in selectedKeys,
                onToggle = { onToggle(club.selectionKey) },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    label: String,
    clubCount: Int,
    selectedCount: Int,
    expanded: Boolean,
    selectAllState: ToggleableState,
    onToggleExpanded: () -> Unit,
    onSelectAll: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )
    val subtitle = if (selectedCount > 0) "$selectedCount of $clubCount selected"
                   else "$clubCount clubs"

    // Opaque surface (tonalElevation) so pinned headers fully cover scrolling cards.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(
                    onClickLabel = if (expanded) "Collapse $label" else "Expand $label"
                ) { onToggleExpanded() }
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(chevronRotation)
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selectedCount > 0) FontWeight.Medium else FontWeight.Normal,
                    color = if (selectedCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Category-level select-all — its own control, doesn't toggle expansion
            TriStateCheckbox(
                state = selectAllState,
                onClick = onSelectAll,
                modifier = Modifier.semantics {
                    contentDescription = "Select all $label"
                }
            )
        }
    }
}

// ─── Club card (unchanged design) ─────────────────────────────────────────────

@Composable
private fun ClubCard(
    club: ClubType,
    isSelected: Boolean,
    onToggle: () -> Unit,
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
                .padding(start = 14.dp),
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
                    .padding(start = 12.dp, top = 14.dp, bottom = 14.dp, end = 14.dp)
            )
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
