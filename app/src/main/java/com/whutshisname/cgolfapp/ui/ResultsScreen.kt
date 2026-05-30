package com.whutshisname.cgolfapp.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whutshisname.cgolfapp.MainViewModel
import com.whutshisname.cgolfapp.SortOrder
import com.whutshisname.cgolfapp.UiState
import com.whutshisname.cgolfapp.VariantFilters
import com.whutshisname.cgolfapp.ViewMode
import com.whutshisname.cgolfapp.model.VariantRow

private val W_PRODUCT  = 130.dp
private val W_CLUB_SET =  80.dp
private val W_CLUB     =  60.dp
private val W_LOFT     =  50.dp
private val W_SHAFT    = 130.dp
private val W_FLEX     =  60.dp
private val W_PRICE    =  75.dp

@Composable
fun ResultsScreen(uiState: UiState, viewModel: MainViewModel) {
    if (uiState.variantRows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (uiState.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = uiState.fetchProgress.ifEmpty { "Fetching club data…" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(text = "⛳", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "No Results Yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select clubs on the Select tab and tap Fetch to see live pricing and availability.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val rows = uiState.variantRows
    val clubSetOpts = remember(rows) { rows.map { it.clubSet  }.filter { it.isNotBlank() }.distinct().sorted() }
    val clubOpts    = remember(rows) { rows.map { it.club     }.filter { it.isNotBlank() }.distinct().sorted() }
    val loftOpts    = remember(rows) { rows.map { it.loft     }.filter { it.isNotBlank() }.distinct()
        .sortedWith(compareBy { it.trimEnd('°').toDoubleOrNull() ?: 0.0 }) }
    val shaftOpts   = remember(rows) { rows.map { it.shaftType}.filter { it.isNotBlank() }.distinct().sorted() }
    val flexOpts    = remember(rows) { rows.map { it.shaftFlex}.filter { it.isNotBlank() }.distinct().sorted() }

    val hScroll = rememberScrollState()
    val filters = uiState.filters
    val activeFilterCount = listOfNotNull(
        filters.clubSet, filters.club, filters.loft, filters.shaftType, filters.shaftFlex
    ).size

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Controls row: filter label + sort + clear ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeFilterCount > 0) "Filters · $activeFilterCount active"
                       else "Filters",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (activeFilterCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            val nextMode = if (uiState.viewMode == ViewMode.TABLE) ViewMode.CARDS else ViewMode.TABLE
            IconButton(onClick = { viewModel.setViewMode(nextMode) }) {
                Icon(
                    imageVector = if (uiState.viewMode == ViewMode.TABLE) Icons.Filled.ViewAgenda
                                  else Icons.Filled.TableRows,
                    contentDescription = if (uiState.viewMode == ViewMode.TABLE) "Switch to card view"
                                         else "Switch to table view",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            SortDropdown(
                current = uiState.sortOrder,
                onSelect = viewModel::setSortOrder
            )
            if (filters.isActive) {
                TextButton(
                    onClick = viewModel::clearFilters,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Clear All", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // ── Filter dropdowns ─────────────────────────────────────────────────
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterDropdown("Club/Set",   clubSetOpts, filters.clubSet)   { viewModel.setFilter("clubSet",   it) } }
            item { FilterDropdown("Club",       clubOpts,    filters.club)      { viewModel.setFilter("club",      it) } }
            item { FilterDropdown("Loft",       loftOpts,    filters.loft)      { viewModel.setFilter("loft",      it) } }
            item { FilterDropdown("Shaft Type", shaftOpts,   filters.shaftType) { viewModel.setFilter("shaftType", it) } }
            item { FilterDropdown("Flex",       flexOpts,    filters.shaftFlex) { viewModel.setFilter("shaftFlex", it) } }
        }

        // ── Active filter chips (dismissible individually) ────────────────────
        if (filters.isActive) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.clubSet?.let { value ->
                    item {
                        ActiveFilterChip("Club/Set: $value") { viewModel.setFilter("clubSet", null) }
                    }
                }
                filters.club?.let { value ->
                    item {
                        ActiveFilterChip("Club: $value") { viewModel.setFilter("club", null) }
                    }
                }
                filters.loft?.let { value ->
                    item {
                        ActiveFilterChip("Loft: $value") { viewModel.setFilter("loft", null) }
                    }
                }
                filters.shaftType?.let { value ->
                    item {
                        ActiveFilterChip("Shaft: $value") { viewModel.setFilter("shaftType", null) }
                    }
                }
                filters.shaftFlex?.let { value ->
                    item {
                        ActiveFilterChip("Flex: $value") { viewModel.setFilter("shaftFlex", null) }
                    }
                }
            }
        }

        // ── Result count ──────────────────────────────────────────────────────
        val countText = when {
            filters.isActive ->
                "${uiState.filteredRows.size} of ${uiState.variantRows.size} variants · $activeFilterCount filter${if (activeFilterCount > 1) "s" else ""} active"
            else ->
                "${uiState.variantRows.size} variants"
        }
        Text(
            text = countText,
            style = MaterialTheme.typography.labelSmall,
            color = if (filters.isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )

        // ── Table header (table view only) ────────────────────────────────────
        if (uiState.viewMode == ViewMode.TABLE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp)
            ) {
                HeaderCell("Product",    W_PRODUCT)
                HeaderCell("Club/Set",   W_CLUB_SET)
                HeaderCell("Club",       W_CLUB)
                HeaderCell("Loft",       W_LOFT)
                HeaderCell("Shaft Type", W_SHAFT)
                HeaderCell("Flex",       W_FLEX)
                HeaderCell("Outlet",     W_PRICE)
                HeaderCell("Like New",   W_PRICE)
                HeaderCell("Very Good",  W_PRICE)
                HeaderCell("Good",       W_PRICE)
                HeaderCell("Average",    W_PRICE)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        }

        // ── Filter empty state / data rows ────────────────────────────────────
        if (uiState.filteredRows.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "No variants match the active filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (filters.isActive) {
                        TextButton(onClick = viewModel::clearFilters) {
                            Text("Clear All Filters")
                        }
                    }
                }
            }
        } else if (uiState.viewMode == ViewMode.TABLE) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(uiState.filteredRows, key = { _, row -> row.id }) { index, row ->
                    TableRow(row, hScroll, isEven = index % 2 == 0) { viewModel.selectRow(row) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(uiState.filteredRows, key = { _, row -> row.id }) { _, row ->
                    VariantCard(row) { viewModel.selectRow(row) }
                }
            }
        }

        // ── JSON viewer ───────────────────────────────────────────────────────
        if (uiState.fetchedResults.isNotEmpty()) {
            val jsonContent = uiState.fetchedResults.joinToString("\n\n") { result ->
                "=== ${result.club.displayValue} ===\n${result.rawJson.substringAfter("\n\n")}"
            }
            JsonViewerSection(
                content = jsonContent,
                expanded = uiState.jsonExpanded,
                onToggle = viewModel::toggleJsonExpanded
            )
        }
    }

    // ── Product detail bottom sheet ───────────────────────────────────────────
    uiState.selectedRow?.let { row ->
        VariantDetailSheet(
            row = row,
            onDismiss = { viewModel.selectRow(null) }
        )
    }
}

// ── Product detail bottom sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantDetailSheet(
    row: VariantRow,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

            Text(
                text = row.productName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Specs
            DetailRow("Club/Set",  row.clubSet)
            DetailRow("Club",      row.club)
            DetailRow("Loft",      row.loft)
            DetailRow("Shaft Type", row.shaftType)
            DetailRow("Shaft Flex", row.shaftFlex)
            DetailRow("Length",    row.length)

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Pricing by Condition",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            PriceLine("Outlet",    row.outletPrice,   row.outletUrl)   { uriHandler.openUri(it) }
            PriceLine("Like New",  row.likeNewPrice,  row.likeNewUrl)  { uriHandler.openUri(it) }
            PriceLine("Very Good", row.veryGoodPrice, row.veryGoodUrl) { uriHandler.openUri(it) }
            PriceLine("Good",      row.goodPrice,     row.goodUrl)     { uriHandler.openUri(it) }
            PriceLine("Average",   row.averagePrice,  row.averageUrl)  { uriHandler.openUri(it) }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap a price to open that listing in your browser.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Card view — phone-optimized result card ──────────────────────────────────

@Composable
private fun VariantCard(row: VariantRow, onClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = row.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))

            // Spec line: only non-blank fields, joined with dots
            val specs = listOfNotNull(
                row.club.takeIf { it.isNotBlank() },
                row.clubSet.takeIf { it.isNotBlank() },
                row.loft.takeIf { it.isNotBlank() }?.let { "Loft $it" },
                row.shaftType.takeIf { it.isNotBlank() },
                row.shaftFlex.takeIf { it.isNotBlank() }?.let { "Flex $it" }
            )
            if (specs.isNotEmpty()) {
                Text(
                    text = specs.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            // Condition prices — each clickable when a link exists
            PriceLine("Outlet",    row.outletPrice,   row.outletUrl)   { uriHandler.openUri(it) }
            PriceLine("Like New",  row.likeNewPrice,  row.likeNewUrl)  { uriHandler.openUri(it) }
            PriceLine("Very Good", row.veryGoodPrice, row.veryGoodUrl) { uriHandler.openUri(it) }
            PriceLine("Good",      row.goodPrice,     row.goodUrl)     { uriHandler.openUri(it) }
            PriceLine("Average",   row.averagePrice,  row.averageUrl)  { uriHandler.openUri(it) }
        }
    }
}

@Composable
private fun PriceLine(condition: String, price: String, url: String?, onTap: (String) -> Unit) {
    if (price == "-" || price.isBlank()) return
    val hasLink = url != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasLink) Modifier.clickable { onTap(url!!) } else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = condition,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = price,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (hasLink) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = if (hasLink) TextDecoration.Underline else TextDecoration.None
        )
    }
}

// ── Active filter chip — selected appearance, tap to dismiss ─────────────────

@Composable
private fun ActiveFilterChip(label: String, onDismiss: () -> Unit) {
    FilterChip(
        selected = true,
        onClick = onDismiss,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}

// ── Table composables (unchanged) ────────────────────────────────────────────

@Composable
private fun TableRow(row: VariantRow, hScroll: ScrollState, isEven: Boolean, onClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val bgColor = if (isEven) MaterialTheme.colorScheme.surface
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onClick() }
            .horizontalScroll(hScroll)
            .padding(vertical = 6.dp)
    ) {
        DataCell(row.productName, W_PRODUCT)
        DataCell(row.clubSet,     W_CLUB_SET)
        DataCell(row.club,        W_CLUB)
        DataCell(row.loft,        W_LOFT)
        DataCell(row.shaftType,   W_SHAFT)
        DataCell(row.shaftFlex,   W_FLEX)
        PriceCell(row.outletPrice,   row.outletUrl,   W_PRICE) { uriHandler.openUri(it) }
        PriceCell(row.likeNewPrice,  row.likeNewUrl,  W_PRICE) { uriHandler.openUri(it) }
        PriceCell(row.veryGoodPrice, row.veryGoodUrl, W_PRICE) { uriHandler.openUri(it) }
        PriceCell(row.goodPrice,     row.goodUrl,     W_PRICE) { uriHandler.openUri(it) }
        PriceCell(row.averagePrice,  row.averageUrl,  W_PRICE) { uriHandler.openUri(it) }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width).padding(horizontal = 6.dp)
    )
}

@Composable
private fun DataCell(text: String, width: Dp) {
    Text(
        text = text.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width).padding(horizontal = 6.dp)
    )
}

@Composable
private fun PriceCell(price: String, url: String?, width: Dp, onTap: (String) -> Unit) {
    val hasLink = url != null && price != "-"
    Text(
        text = price,
        style = MaterialTheme.typography.bodySmall.copy(
            color = if (hasLink) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = if (hasLink) TextDecoration.Underline else TextDecoration.None,
            fontWeight = if (hasLink) FontWeight.Medium else FontWeight.Normal
        ),
        maxLines = 1,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
            .then(if (hasLink) Modifier.clickable { onTap(url!!) } else Modifier)
    )
}

@Composable
private fun SortDropdown(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = current != SortOrder.NONE
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isActive) "Sort: ${current.label}" else "Sort",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = order.label,
                            fontWeight = if (order == current) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (order == current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { onSelect(order); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = {
                Text(
                    if (selected != null) "$label: $selected" else label,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = { onSelect(null); expanded = false }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
