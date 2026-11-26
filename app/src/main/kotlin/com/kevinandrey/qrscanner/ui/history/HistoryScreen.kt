package com.kevinandrey.qrscanner.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevinandrey.qrscanner.R
import com.kevinandrey.qrscanner.core.Scan
import com.kevinandrey.qrscanner.core.ScanFilter
import com.kevinandrey.qrscanner.ui.components.ContentKindBadge
import com.kevinandrey.qrscanner.ui.components.ScanDates
import com.kevinandrey.qrscanner.ui.components.label

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenScan: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HistoryContent(
        state = state,
        query = viewModel.query,
        onQueryChanged = viewModel::onQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onFavoriteToggled = viewModel::onFavoriteToggled,
        onClearConfirmed = viewModel::onClearConfirmed,
        onOpenScan = onOpenScan,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    state: HistoryUiState,
    query: String,
    onQueryChanged: (String) -> Unit,
    onFilterSelected: (ScanFilter) -> Unit,
    onFavoriteToggled: (Scan) -> Unit,
    onClearConfirmed: () -> Unit,
    onOpenScan: (Long) -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    if (state.hasHistory) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.history_clear))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.hasHistory) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text(stringResource(R.string.history_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.history_clear_search))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ScanFilter.entries) { filter ->
                        FilterChip(
                            selected = filter == state.filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(stringResource(filter.label)) },
                        )
                    }
                }
            }

            when {
                state.loading -> {}

                !state.hasHistory -> {
                    EmptyState(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                    )
                }

                state.scans.isEmpty() -> {
                    EmptyState(icon = Icons.Outlined.Search, title = stringResource(R.string.history_no_results))
                }

                else -> {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(state.scans, key = Scan::id) { scan ->
                            HistoryItem(scan, onClick = { onOpenScan(scan.id) }, onFavoriteToggled = { onFavoriteToggled(scan) })
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearConfirmed()
                    },
                ) { Text(stringResource(R.string.history_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryItem(
    scan: Scan,
    onClick: () -> Unit,
    onFavoriteToggled: () -> Unit,
) {
    ListItem(
        onClick = onClick,
        leadingContent = { ContentKindBadge(scan.content.kind) },
        supportingContent = {
            Text(
                stringResource(R.string.history_item_supporting, stringResource(scan.content.kind.label), ScanDates.short(scan.scannedAt)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            IconButton(onClick = onFavoriteToggled) {
                Icon(
                    imageVector = if (scan.favorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(if (scan.favorite) R.string.action_unfavorite else R.string.action_favorite),
                    tint = if (scan.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(scan.content.headline.ifEmpty { scan.raw }, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String? = null,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (body != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
