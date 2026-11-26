package com.kevinandrey.qrscanner.core

/** Structured meaning of the text stored in a QR code. */
public sealed interface QrContent {
    public val kind: ContentKind

    /** Most relevant value to show as a title, independent of the UI language. */
    public val headline: String

    public data class Text(
        val text: String,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.TEXT
        override val headline: String get() =
            text
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
    }

    public data class Url(
        val url: String,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.URL
        override val headline: String get() = url.substringAfter("://").removePrefix("www.").trimEnd('/')
    }

    public data class Email(
        val address: String,
        val subject: String? = null,
        val body: String? = null,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.EMAIL
        override val headline: String get() = address
    }

    public data class Phone(
        val number: String,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.PHONE
        override val headline: String get() = number
    }

    public data class Sms(
        val number: String,
        val message: String? = null,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.SMS
        override val headline: String get() = number
    }

    public data class Wifi(
        val ssid: String,
        val password: String? = null,
        val security: WifiSecurity = WifiSecurity.WPA,
        val hidden: Boolean = false,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.WIFI
        override val headline: String get() = ssid
    }

    public data class Geo(
        val latitude: Double,
        val longitude: Double,
        val query: String? = null,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.GEO
        override val headline: String get() = query ?: "$latitude, $longitude"
    }

    public data class Contact(
        val name: String? = null,
        val organization: String? = null,
        val phones: List<String> = emptyList(),
        val emails: List<String> = emptyList(),
        val urls: List<String> = emptyList(),
        val address: String? = null,
    ) : QrContent {
        override val kind: ContentKind get() = ContentKind.CONTACT
        override val headline: String
            get() = name ?: organization ?: phones.firstOrNull() ?: emails.firstOrNull().orEmpty()
    }
}

public enum class ContentKind {
    URL,
    WIFI,
    CONTACT,
    EMAIL,
    PHONE,
    SMS,
    GEO,
    TEXT,
}

public enum class WifiSecurity {
    WPA,
    WEP,
    OPEN,
}
