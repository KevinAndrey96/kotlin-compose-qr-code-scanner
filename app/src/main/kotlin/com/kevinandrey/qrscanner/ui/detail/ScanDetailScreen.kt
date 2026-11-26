package com.kevinandrey.qrscanner.ui.detail

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevinandrey.qrscanner.R
import com.kevinandrey.qrscanner.core.QrContent
import com.kevinandrey.qrscanner.core.Scan
import com.kevinandrey.qrscanner.ui.components.ContentKindBadge
import com.kevinandrey.qrscanner.ui.components.ScanDates
import com.kevinandrey.qrscanner.ui.components.copyText
import com.kevinandrey.qrscanner.ui.components.label
import com.kevinandrey.qrscanner.ui.components.startActivitySafely
import kotlinx.coroutines.launch

@Composable
fun ScanDetailScreen(
    viewModel: ScanDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val copied = stringResource(R.string.copied)
    val passwordCopied = stringResource(R.string.password_copied)
    val noApp = stringResource(R.string.no_app_for_action)

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    ScanDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onFavoriteToggled = viewModel::onFavoriteToggled,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onAction = { action, content ->
            val intent = ContentIntents.forAction(action, content)
            val password = (content as? QrContent.Wifi)?.password
            val launched = intent != null && context.startActivitySafely(intent)
            val copyPassword =
                password != null && (action == ContentAction.COPY_PASSWORD || intent?.action == Settings.ACTION_WIFI_SETTINGS)
            scope.launch {
                when {
                    copyPassword -> {
                        clipboard.copyText(password, sensitive = true)
                        snackbarHostState.showSnackbar(passwordCopied)
                    }

                    !launched && action != ContentAction.COPY_PASSWORD -> {
                        snackbarHostState.showSnackbar(noApp)
                    }
                }
            }
        },
        onCopy = { text ->
            scope.launch {
                clipboard.copyText(text)
                snackbarHostState.showSnackbar(copied)
            }
        },
        onShare = { text -> context.startActivitySafely(ContentIntents.share(text)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailContent(
    state: ScanDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFavoriteToggled: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onAction: (ContentAction, QrContent) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val scan = (state as? ScanDetailUiState.Loaded)?.scan

    Scaffold(
        topBar = {
            TopAppBar(
                title = { scan?.let { Text(stringResource(it.content.kind.label)) } },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (scan != null) {
                        IconButton(onClick = onFavoriteToggled) {
                            Icon(
                                imageVector = if (scan.favorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                                contentDescription =
                                    stringResource(
                                        if (scan.favorite) R.string.action_unfavorite else R.string.action_favorite,
                                    ),
                                tint = if (scan.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ScanDetailUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                ScanDetailUiState.NotFound -> {
                    Text(
                        text = stringResource(R.string.detail_not_found),
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                is ScanDetailUiState.Loaded -> {
                    ScanDetailBody(state.scan, onAction, onCopy, onShare)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteConfirmed()
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ScanDetailBody(
    scan: Scan,
    onAction: (ContentAction, QrContent) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val content = scan.content
    val shareable = (content as? QrContent.Url)?.url ?: scan.raw

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContentKindBadge(content.kind, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                SelectionContainer {
                    Text(content.headline.ifEmpty { scan.raw }, style = MaterialTheme.typography.titleLarge, maxLines = 3)
                }
                Text(
                    text = stringResource(R.string.detail_scanned_at, ScanDates.long(scan.scannedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content.actions().forEachIndexed { index, action ->
                val buttonContent: @Composable () -> Unit = {
                    Icon(action.icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(action.label))
                }
                if (index == 0) {
                    Button(onClick = { onAction(action, content) }, modifier = Modifier.fillMaxWidth()) { buttonContent() }
                } else {
                    OutlinedButton(onClick = { onAction(action, content) }, modifier = Modifier.fillMaxWidth()) { buttonContent() }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onCopy(shareable) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.action_copy))
                }
                FilledTonalButton(onClick = { onShare(shareable) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.action_share))
                }
            }
        }

        val fields = content.fields()
        if (fields.isNotEmpty()) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                fields.forEachIndexed { index, field ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(field.label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SelectionContainer {
                            Text(
                                text = field.value ?: field.valueRes?.let { stringResource(it) }.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.detail_raw_content),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedCard(Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(
                        text = scan.raw,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}
