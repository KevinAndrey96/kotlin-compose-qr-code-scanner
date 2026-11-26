package com.kevinandrey.qrscanner.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Square grid of QR modules without quiet zone; `true` means a dark module. */
public class QrMatrix(
    public val size: Int,
    private val modules: BooleanArray,
) {
    init {
        require(modules.size == size * size) { "expected ${size * size} modules, got ${modules.size}" }
    }

    public operator fun get(
        x: Int,
        y: Int,
    ): Boolean = modules[y * size + x]

    override fun equals(other: Any?): Boolean = other is QrMatrix && other.size == size && other.modules.contentEquals(modules)

    override fun hashCode(): Int = 31 * size + modules.contentHashCode()
}

public object QrEncoder {
    /** Recommended quiet zone around the matrix, in modules. */
    public const val QUIET_ZONE_MODULES: Int = 4

    private val hints =
        mapOf(
            EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
        )

    /** @throws IllegalArgumentException when the payload is empty or does not fit in a QR code. */
    public fun encode(payload: String): QrMatrix {
        require(payload.isNotEmpty()) { "empty payload" }
        val bits =
            try {
                QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
            } catch (exception: WriterException) {
                throw IllegalArgumentException("payload does not fit in a QR code", exception)
            }
        val size = bits.width

        return QrMatrix(size, BooleanArray(size * size) { index -> bits[index % size, index / size] })
    }
}
