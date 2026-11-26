package com.kevinandrey.qrscanner.ui.generator

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevinandrey.qrscanner.R
import com.kevinandrey.qrscanner.core.QrDraft
import com.kevinandrey.qrscanner.core.QrMatrix
import com.kevinandrey.qrscanner.core.WifiSecurity
import com.kevinandrey.qrscanner.ui.components.copyText
import com.kevinandrey.qrscanner.ui.components.message
import com.kevinandrey.qrscanner.ui.components.startActivitySafely
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val copied = stringResource(R.string.copied)
    val shareTitle = stringResource(R.string.generator_share_title)

    GeneratorContent(
        state = viewModel.state,
        snackbarHostState = snackbarHostState,
        onTypeSelected = viewModel::onTypeSelected,
        onTextChanged = viewModel::onTextChanged,
        onLinkChanged = viewModel::onLinkChanged,
        onWifiChanged = viewModel::onWifiChanged,
        onContactChanged = viewModel::onContactChanged,
        onShareImage = { matrix ->
            scope.launch { context.startActivitySafely(QrImageExporter.shareIntent(context, matrix, shareTitle)) }
        },
        onCopyContent = { payload ->
            scope.launch {
                clipboard.copyText(payload)
                snackbarHostState.showSnackbar(copied)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorContent(
    state: GeneratorUiState,
    snackbarHostState: SnackbarHostState,
    onTypeSelected: (GeneratorType) -> Unit,
    onTextChanged: (String) -> Unit,
    onLinkChanged: (String) -> Unit,
    onWifiChanged: (QrDraft.Wifi) -> Unit,
    onContactChanged: (QrDraft.Contact) -> Unit,
    onShareImage: (QrMatrix) -> Unit,
    onCopyContent: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.generator_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SegmentedChoice(
                options = GeneratorType.entries,
                selected = state.type,
                label = { it.label },
                onSelected = onTypeSelected,
            )

            when (state.type) {
                GeneratorType.TEXT -> {
                    OutlinedTextField(
                        value = state.text.text,
                        onValueChange = onTextChanged,
                        label = { Text(stringResource(R.string.generator_text_label)) },
                        modifier = Modifier.fillMaxWidth().testTag("generatorText"),
                        minLines = 3,
                    )
                }

                GeneratorType.LINK -> {
                    FormField(
                        value = state.link.url,
                        onValueChange = onLinkChanged,
                        label = R.string.generator_link_label,
                        keyboardType = KeyboardType.Uri,
                    )
                }

                GeneratorType.WIFI -> {
                    WifiForm(state.wifi, onWifiChanged)
                }

                GeneratorType.CONTACT -> {
                    ContactForm(state.contact, onContactChanged)
                }
            }

            if (state.problems.isNotEmpty()) {
                Column(Modifier.testTag("problems"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.problems.forEach { problem ->
                        Text(
                            text = stringResource(problem.message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Preview(state, onShareImage, onCopyContent)
        }
    }
}

@Composable
private fun Preview(
    state: GeneratorUiState,
    onShareImage: (QrMatrix) -> Unit,
    onCopyContent: (String) -> Unit,
) {
    val matrix = state.matrix

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (matrix != null) {
                QrCodeImage(
                    matrix = matrix,
                    contentDescription = stringResource(R.string.generator_preview_description),
                    modifier = Modifier.widthIn(max = 260.dp).fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onShareImage(matrix) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.generator_share_image))
                    }
                    FilledTonalButton(onClick = { onCopyContent(state.draft.payload()) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.generator_copy_text))
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.generator_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WifiForm(
    wifi: QrDraft.Wifi,
    onChanged: (QrDraft.Wifi) -> Unit,
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val password = rememberTextFieldState(wifi.password)
    val currentWifi by rememberUpdatedState(wifi)
    val currentOnChanged by rememberUpdatedState(onChanged)

    LaunchedEffect(password) {
        snapshotFlow { password.text.toString() }
            .drop(1)
            .collect { currentOnChanged(currentWifi.copy(password = it)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(value = wifi.ssid, onValueChange = { onChanged(wifi.copy(ssid = it)) }, label = R.string.generator_ssid_label)
        SegmentedChoice(
            options = WifiSecurity.entries,
            selected = wifi.security,
            label = { it.shortLabel },
            onSelected = { onChanged(wifi.copy(security = it)) },
        )
        if (wifi.security != WifiSecurity.OPEN) {
            OutlinedSecureTextField(
                state = password,
                label = { Text(stringResource(R.string.generator_password_label)) },
                modifier = Modifier.fillMaxWidth().testTag("wifiPassword"),
                textObfuscationMode = if (showPassword) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription =
                                stringResource(if (showPassword) R.string.generator_hide_password else R.string.generator_show_password),
                        )
                    }
                },
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.generator_hidden_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(checked = wifi.hidden, onCheckedChange = { onChanged(wifi.copy(hidden = it)) })
        }
    }
}

@Composable
private fun ContactForm(
    contact: QrDraft.Contact,
    onChanged: (QrDraft.Contact) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(contact.name, { onChanged(contact.copy(name = it)) }, R.string.generator_name_label)
        FormField(contact.phone, { onChanged(contact.copy(phone = it)) }, R.string.generator_phone_label, KeyboardType.Phone)
        FormField(contact.email, { onChanged(contact.copy(email = it)) }, R.string.generator_email_label, KeyboardType.Email)
        FormField(contact.organization, { onChanged(contact.copy(organization = it)) }, R.string.generator_organization_label)
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelected: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(stringResource(label(option)), maxLines = 1) },
            )
        }
    }
}

@get:StringRes
private val GeneratorType.label: Int
    get() =
        when (this) {
            GeneratorType.TEXT -> R.string.generator_type_text
            GeneratorType.LINK -> R.string.generator_type_link
            GeneratorType.WIFI -> R.string.generator_type_wifi
            GeneratorType.CONTACT -> R.string.generator_type_contact
        }

@get:StringRes
private val WifiSecurity.shortLabel: Int
    get() =
        when (this) {
            WifiSecurity.WPA -> R.string.generator_security_wpa
            WifiSecurity.WEP -> R.string.generator_security_wep
            WifiSecurity.OPEN -> R.string.generator_security_open
        }
