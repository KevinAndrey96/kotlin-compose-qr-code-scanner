package com.kevinandrey.qrscanner.ui.generator

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.kevinandrey.qrscanner.core.QrEncoder
import com.kevinandrey.qrscanner.core.QrMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.floor

/** Black on white QR code with its quiet zone, drawn on whole pixels so modules never show seams. */
@Composable
fun QrCodeImage(
    matrix: QrMatrix,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Image
            },
    ) {
        val modules = matrix.size + QrEncoder.QUIET_ZONE_MODULES * 2
        val cell = floor(size.minDimension / modules)
        val origin = (size.minDimension - cell * modules) / 2 + cell * QrEncoder.QUIET_ZONE_MODULES

        drawRect(Color.White)
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix[x, y]) {
                    drawRect(Color.Black, Offset(origin + x * cell, origin + y * cell), Size(cell, cell))
                }
            }
        }
    }
}

object QrImageExporter {
    private const val MODULE_PIXELS = 16
    private const val DARK = 0xFF000000.toInt()
    private const val LIGHT = 0xFFFFFFFF.toInt()
    private const val PNG_QUALITY = 100

    fun render(
        matrix: QrMatrix,
        modulePixels: Int = MODULE_PIXELS,
    ): Bitmap {
        val quietZone = QrEncoder.QUIET_ZONE_MODULES
        val side = (matrix.size + quietZone * 2) * modulePixels
        val pixels =
            IntArray(side * side) { index ->
                val x = index % side / modulePixels - quietZone
                val y = index / side / modulePixels - quietZone
                if (x in 0 until matrix.size && y in 0 until matrix.size && matrix[x, y]) DARK else LIGHT
            }

        return createBitmap(side, side).apply { setPixels(pixels, 0, side, 0, 0, side, side) }
    }

    /** Writes the code as a PNG in the cache and returns a chooser to share it. */
    suspend fun shareIntent(
        context: Context,
        matrix: QrMatrix,
        title: String,
    ): Intent {
        val file =
            withContext(Dispatchers.IO) {
                val directory = File(context.cacheDir, "shared").apply { mkdirs() }
                File(directory, "codigo-qr.png").also { file ->
                    file.outputStream().use { render(matrix).compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
                }
            }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .apply { clipData = ClipData.newRawUri(null, uri) }

        return Intent.createChooser(send, title)
    }
}
