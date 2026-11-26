package com.kevinandrey.qrscanner.ui.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

sealed interface ImageReadResult {
    data class Found(
        val raw: String,
    ) : ImageReadResult

    data object NotFound : ImageReadResult

    data object Unreadable : ImageReadResult
}

/** On-device ML Kit client restricted to QR codes, shared by the live camera and gallery images. */
fun qrBarcodeScanner(): BarcodeScanner =
    BarcodeScanning.getClient(
        BarcodeScannerOptions
            .Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

suspend fun readQrFromImage(
    context: Context,
    uri: Uri,
): ImageReadResult {
    val image =
        withContext(Dispatchers.IO) {
            try {
                InputImage.fromFilePath(context, uri)
            } catch (_: IOException) {
                null
            }
        } ?: return ImageReadResult.Unreadable
    val scanner = qrBarcodeScanner()

    return try {
        suspendCancellableCoroutine { continuation ->
            scanner
                .process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                    continuation.resume(raw?.let(ImageReadResult::Found) ?: ImageReadResult.NotFound)
                }.addOnFailureListener { continuation.resume(ImageReadResult.Unreadable) }
        }
    } finally {
        scanner.close()
    }
}
