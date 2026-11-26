package com.kevinandrey.qrscanner.core

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class QrEncoderTest {
    @Test
    fun `encoded matrices decode back to the original payload`() {
        listOf(
            "https://github.com/KevinAndrey96",
            QrDraft.Wifi(ssid = "Casa Kevin", password = "contraseña-segura").payload(),
            "Ñandú, café y 😀",
        ).forEach { payload ->
            assertEquals(payload, decode(QrEncoder.encode(payload)))
        }
    }

    @Test
    fun `small payloads use the smallest version and larger ones grow`() {
        assertEquals(21, QrEncoder.encode("Hola").size)
        assertEquals(true, QrEncoder.encode("x".repeat(QrDraft.MAX_PAYLOAD_BYTES)).size > 100)
    }

    @Test
    fun `empty and oversized payloads are rejected`() {
        assertFailsWith<IllegalArgumentException> { QrEncoder.encode("") }
        assertFailsWith<IllegalArgumentException> { QrEncoder.encode("x".repeat(5000)) }
    }

    @Test
    fun `matrices compare by content`() {
        assertEquals(QrEncoder.encode("a"), QrEncoder.encode("a"))
        assertEquals(QrEncoder.encode("a").hashCode(), QrEncoder.encode("a").hashCode())
        assertNotEquals(QrEncoder.encode("a"), QrEncoder.encode("b"))
        assertFailsWith<IllegalArgumentException> { QrMatrix(2, BooleanArray(3)) }
    }

    private fun decode(matrix: QrMatrix): String {
        val scale = 4
        val side = (matrix.size + QrEncoder.QUIET_ZONE_MODULES * 2) * scale
        val pixels =
            IntArray(side * side) { index ->
                val x = index % side / scale - QrEncoder.QUIET_ZONE_MODULES
                val y = index / side / scale - QrEncoder.QUIET_ZONE_MODULES
                val dark = x in 0 until matrix.size && y in 0 until matrix.size && matrix[x, y]
                if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))

        return QRCodeReader().decode(bitmap, mapOf(DecodeHintType.CHARACTER_SET to "UTF-8")).text
    }
}
