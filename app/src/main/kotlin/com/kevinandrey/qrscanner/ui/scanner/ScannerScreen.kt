package com.kevinandrey.qrscanner.ui.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevinandrey.qrscanner.R
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onOpenScan: (Long) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var cameraAllowed by remember { mutableStateOf(context.hasCameraPermission()) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraAllowed = granted
            permissionRequested = true
        }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch { viewModel.onImageRead(readQrFromImage(context, uri)) }
        }

    LifecycleResumeEffect(Unit) {
        cameraAllowed = context.hasCameraPermission()
        viewModel.onScannerVisible()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        if (!cameraAllowed && !permissionRequested) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ScannerEvent.OpenScan -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onOpenScan(event.id)
                }
            }
        }
    }

    val message =
        when (state.message) {
            ScannerMessage.NO_CODE_IN_IMAGE -> stringResource(R.string.scanner_no_code_in_image)
            ScannerMessage.IMAGE_UNREADABLE -> stringResource(R.string.scanner_image_unreadable)
            null -> null
        }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    ScannerContent(
        cameraAllowed = cameraAllowed,
        torchOn = state.torchOn,
        snackbarHostState = snackbarHostState,
        onCodeDetected = viewModel::onCodeDetected,
        onTorchToggled = viewModel::onTorchToggled,
        onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
            )
        },
    )
}

@Composable
fun ScannerContent(
    cameraAllowed: Boolean,
    torchOn: Boolean,
    snackbarHostState: SnackbarHostState,
    onCodeDetected: (String) -> Unit,
    onTorchToggled: () -> Unit,
    onPickImage: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    camera: @Composable (Modifier) -> Unit = { modifier ->
        CameraPreview(torchOn = torchOn, onCodeDetected = onCodeDetected, modifier = modifier)
    },
) {
    if (cameraAllowed) LightStatusBarIcons()

    Box(Modifier.fillMaxSize()) {
        if (cameraAllowed) {
            camera(Modifier.fillMaxSize())
            ScannerOverlay(Modifier.fillMaxSize())
        } else {
            PermissionRationale(onRequestPermission = onRequestPermission, onOpenSettings = onOpenSettings)
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (cameraAllowed) {
                Text(
                    text = stringResource(R.string.scanner_hint),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (cameraAllowed) {
                    FilledTonalIconButton(onClick = onTorchToggled, modifier = Modifier.size(52.dp).testTag("torch")) {
                        Icon(
                            imageVector = if (torchOn) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                            contentDescription = stringResource(if (torchOn) R.string.scanner_torch_off else R.string.scanner_torch_on),
                        )
                    }
                }
                FilledTonalButton(onClick = onPickImage, modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.scanner_pick_image))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding(),
        )
    }
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.NoPhotography,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.scanner_permission_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.scanner_permission_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scanner_permission_grant))
            }
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.scanner_permission_settings))
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val side = size.minDimension * FRAME_RATIO
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2 - size.height * FRAME_LIFT
        val radius = 28.dp.toPx()

        drawRect(Color.Black.copy(alpha = SCRIM_ALPHA))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(radius),
            blendMode = BlendMode.Clear,
        )

        val length = side * CORNER_RATIO
        val right = left + side
        val bottom = top + side
        val corners =
            Path().apply {
                moveTo(left, top + length)
                lineTo(left, top + radius)
                quadraticTo(left, top, left + radius, top)
                lineTo(left + length, top)
                moveTo(right - length, top)
                lineTo(right - radius, top)
                quadraticTo(right, top, right, top + radius)
                lineTo(right, top + length)
                moveTo(right, bottom - length)
                lineTo(right, bottom - radius)
                quadraticTo(right, bottom, right - radius, bottom)
                lineTo(right - length, bottom)
                moveTo(left + length, bottom)
                lineTo(left + radius, bottom)
                quadraticTo(left, bottom, left, bottom - radius)
                lineTo(left, bottom - length)
            }
        drawPath(corners, Color.White, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Keeps status bar icons readable over the dark camera feed while the scanner is visible. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false

        onDispose {
            if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private const val FRAME_RATIO = 0.68f
private const val FRAME_LIFT = 0.06f
private const val CORNER_RATIO = 0.18f
private const val SCRIM_ALPHA = 0.55f
