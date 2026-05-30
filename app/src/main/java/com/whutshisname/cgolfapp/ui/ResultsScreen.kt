package com.whutshisname.cgolfapp.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whutshisname.cgolfapp.UiState
import com.whutshisname.cgolfapp.MainViewModel
import com.whutshisname.cgolfapp.model.VariantRow

private val W_PRODUCT   = 130.dp
private val W_CLUB_SET  =  80.dp
private val W_CLUB      =  60.dp
private val W_LOFT      =  50.dp
private val W_SHAFT     = 130.dp
private val W_FLEX      =  60.dp
private val W_PRICE     =  75.dp

@Composable
fun ResultsScreen(uiState: UiState, viewModel: MainViewModel) {
    if (uiState.variantRows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results yet — select clubs and tap Fetch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val rows = uiState.variantRows
    val clubSetOpts  = remember(rows) { rows.map { it.clubSet  }.filter { it.isNotBlank() }.distinct().sorted() }
    val clubOpts     = remember(rows) { rows.map { it.club     }.filter { it.isNotBlank() }.distinct().sorted() }
    val loftOpts     = remember(rows) { rows.map { it.loft     }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy { it.trimEnd('°').toDoubleOrNull() ?: 0.0 }) }
    val shaftOpts    = remember(rows) { rows.map { it.shaftType}.filter { it.isNotBlank() }.distinct().sorted() }
    val flexOpts     = remember(rows) { rows.map { it.shaftFlex}.filter { it.isNotBlank() }.distinct().sorted() }

    val hScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterDropdown("Club/Set",   clubSetOpts, uiState.filters.clubSet)  { viewModel.setFilter("clubSet",   it) } }
            item { FilterDropdown("Club",       clubOpts,    uiState.filters.club)     { viewModel.setFilter("club",      it) } }
            item { FilterDropdown("Loft",       loftOpts,    uiState.filters.loft)     { viewModel.setFilter("loft",      it) } }
            item { FilterDropdown("Shaft Type", shaftOpts,   uiState.filters.shaftType){ viewModel.setFilter("shaftType", it) } }
            item { FilterDropdown("Flex",       flexOpts,    uiState.filters.shaftFlex){ viewModel.setFilter("shaftFlex", it) } }
            if (uiState.filters.isActive) {
                item {
                    TextButton(onClick = viewModel::clearFilters) { Text("Clear") }
                }
            }
        }

        Text(
            text = "${uiState.filteredRows.size} of ${uiState.variantRows.size} variants",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScroll)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 6.dp)
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
        HorizontalDivider()

        // Data rows
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.filteredRows, key = { it.id }) { row ->
                TableRow(row, hScroll)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TableRow(row: VariantRow, hScroll: ScrollState) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .padding(vertical = 4.dp)
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width).padding(horizontal = 4.dp)
    )
}

@Composable
private fun DataCell(text: String, width: Dp) {
    Text(
        text = text.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width).padding(horizontal = 4.dp)
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
            textDecoration = if (hasLink) TextDecoration.Underline else TextDecoration.None
        ),
        maxLines = 1,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
            .then(if (hasLink) Modifier.clickable { onTap(url!!) } else Modifier)
    )
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
            label = { Text(if (selected != null) "$label: $selected" else label,
                          style = MaterialTheme.typography.labelSmall) }
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
