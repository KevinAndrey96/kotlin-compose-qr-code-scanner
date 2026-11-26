package com.kevinandrey.qrscanner.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object ScanDates {
    private val locale: Locale = Locale.forLanguageTag("es-CO")

    fun long(
        instant: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(zone)
            .format(instant)

    fun short(
        instant: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(zone)
            .format(instant)
}

/** Starts [intent] and returns `false` when no installed app can handle it. */
fun Context.startActivitySafely(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

/** Copies plain text; [sensitive] hides it from the clipboard preview on Android 13+. */
suspend fun Clipboard.copyText(
    text: String,
    sensitive: Boolean = false,
) {
    val clip = ClipData.newPlainText(null, text)
    if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    setClipEntry(ClipEntry(clip))
}
