package com.kevinandrey.qrscanner.core

import java.text.Normalizer
import java.time.Instant

/** A code read by the camera or from an image, as stored in the history. */
public data class Scan(
    val id: Long,
    val raw: String,
    val scannedAt: Instant,
    val favorite: Boolean = false,
) {
    val content: QrContent = QrContentParser.parse(raw)
}

public enum class ScanFilter {
    ALL,
    FAVORITES,
    LINKS,
    WIFI,
    CONTACTS,
    OTHER,
    ;

    public fun accepts(scan: Scan): Boolean =
        when (this) {
            ALL -> true
            FAVORITES -> scan.favorite
            LINKS -> scan.content.kind == ContentKind.URL
            WIFI -> scan.content.kind == ContentKind.WIFI
            CONTACTS -> scan.content.kind == ContentKind.CONTACT
            OTHER -> scan.content.kind !in setOf(ContentKind.URL, ContentKind.WIFI, ContentKind.CONTACT)
        }
}

public object ScanSearch {
    private val diacritics = Regex("\\p{Mn}+")

    /** Keeps the scans accepted by [filter] whose text contains every word of [query], ignoring case and accents. */
    public fun apply(
        scans: List<Scan>,
        query: String,
        filter: ScanFilter,
    ): List<Scan> {
        val terms = normalize(query).split(' ').filter { it.isNotEmpty() }

        return scans.filter { scan ->
            filter.accepts(scan) && normalize("${scan.content.headline} ${scan.raw}").let { text -> terms.all { it in text } }
        }
    }

    internal fun normalize(value: String): String =
        Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
}
