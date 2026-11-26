package com.kevinandrey.qrscanner.core

/**
 * Detects the structured formats most QR generators produce: links, Wi-Fi credentials, MECARD and
 * vCard contacts, mailto and MATMSG emails, phone numbers, SMS and geo coordinates. Anything else
 * is plain text.
 */
public object QrContentParser {
    private const val WIFI_PREFIX = "WIFI:"
    private const val MECARD_PREFIX = "MECARD:"
    private const val MATMSG_PREFIX = "MATMSG:"
    private const val MAILTO_PREFIX = "mailto:"
    private const val TEL_PREFIX = "tel:"
    private const val SMSTO_PREFIX = "SMSTO:"
    private const val SMS_PREFIX = "sms:"
    private const val GEO_PREFIX = "geo:"
    private const val VCARD_PREFIX = "BEGIN:VCARD"
    private const val MIN_PHONE_DIGITS = 3
    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0

    private val urlPattern = Regex("""^(https?)://([^/?#:\s]+)(:\d{1,5})?([/?#]\S*)?$""", RegexOption.IGNORE_CASE)
    private val emailPattern = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

    public fun parse(raw: String): QrContent {
        val value = raw.trim()

        return parseWifi(value)
            ?: parseEmail(value)
            ?: parsePhone(value)
            ?: parseSms(value)
            ?: parseGeo(value)
            ?: parseMeCard(value)
            ?: parseVCard(value)
            ?: parseUrl(value)
            ?: QrContent.Text(value)
    }

    internal fun isEmail(value: String): Boolean = emailPattern.matches(value)

    internal fun isPhone(value: String): Boolean = value.count(Char::isDigit) >= MIN_PHONE_DIGITS

    private fun parseWifi(value: String): QrContent.Wifi? {
        if (!value.startsWith(WIFI_PREFIX, ignoreCase = true)) return null
        val fields = MeCardFields.parse(value.substring(WIFI_PREFIX.length)).toMap()
        val ssid = fields["S"]?.takeIf { it.isNotEmpty() } ?: return null
        val password = fields["P"]?.takeIf { it.isNotEmpty() }
        val security =
            when (fields["T"]?.uppercase()) {
                "WEP" -> WifiSecurity.WEP
                "NOPASS" -> WifiSecurity.OPEN
                null, "" -> if (password == null) WifiSecurity.OPEN else WifiSecurity.WPA
                else -> WifiSecurity.WPA
            }

        return QrContent.Wifi(
            ssid = ssid,
            password = password.takeIf { security != WifiSecurity.OPEN },
            security = security,
            hidden = fields["H"].equals("true", ignoreCase = true),
        )
    }

    private fun parseEmail(value: String): QrContent.Email? {
        val email =
            when {
                value.startsWith(MAILTO_PREFIX, ignoreCase = true) -> {
                    val rest = value.substring(MAILTO_PREFIX.length)
                    val query = UriText.queryParameters(rest.substringAfter('?', missingDelimiterValue = ""))
                    QrContent.Email(
                        address = UriText.decodePercent(rest.substringBefore('?')).trim(),
                        subject = query["subject"]?.takeIf { it.isNotBlank() },
                        body = query["body"]?.takeIf { it.isNotBlank() },
                    )
                }

                value.startsWith(MATMSG_PREFIX, ignoreCase = true) -> {
                    val fields = MeCardFields.parse(value.substring(MATMSG_PREFIX.length)).toMap()
                    QrContent.Email(
                        address = fields["TO"].orEmpty().trim(),
                        subject = fields["SUB"]?.takeIf { it.isNotBlank() },
                        body = fields["BODY"]?.takeIf { it.isNotBlank() },
                    )
                }

                else -> {
                    return null
                }
            }

        return email.takeIf { isEmail(it.address) }
    }

    private fun parsePhone(value: String): QrContent.Phone? {
        if (!value.startsWith(TEL_PREFIX, ignoreCase = true)) return null
        val number = UriText.decodePercent(value.substring(TEL_PREFIX.length)).trim()

        return QrContent.Phone(number).takeIf { isPhone(number) }
    }

    private fun parseSms(value: String): QrContent.Sms? {
        val sms =
            when {
                value.startsWith(SMSTO_PREFIX, ignoreCase = true) -> {
                    val rest = value.substring(SMSTO_PREFIX.length)
                    QrContent.Sms(
                        number = rest.substringBefore(':').trim(),
                        message = rest.substringAfter(':', missingDelimiterValue = "").takeIf { it.isNotBlank() },
                    )
                }

                value.startsWith(SMS_PREFIX, ignoreCase = true) -> {
                    val rest = value.substring(SMS_PREFIX.length)
                    val query = UriText.queryParameters(rest.substringAfter('?', missingDelimiterValue = ""))
                    QrContent.Sms(
                        number = UriText.decodePercent(rest.substringBefore('?')).trim(),
                        message = query["body"]?.takeIf { it.isNotBlank() },
                    )
                }

                else -> {
                    return null
                }
            }

        return sms.takeIf { isPhone(it.number) }
    }

    private fun parseGeo(value: String): QrContent.Geo? {
        if (!value.startsWith(GEO_PREFIX, ignoreCase = true)) return null
        val rest = value.substring(GEO_PREFIX.length)
        val coordinates = rest.substringBefore('?').substringBefore(';').split(',')
        val latitude = coordinates.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
        val longitude = coordinates.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
        if (latitude !in -MAX_LATITUDE..MAX_LATITUDE || longitude !in -MAX_LONGITUDE..MAX_LONGITUDE) return null
        val query = UriText.queryParameters(rest.substringAfter('?', missingDelimiterValue = ""), plusAsSpace = true)

        return QrContent.Geo(latitude, longitude, query["q"]?.takeIf { it.isNotBlank() })
    }

    private fun parseMeCard(value: String): QrContent.Contact? {
        if (!value.startsWith(MECARD_PREFIX, ignoreCase = true)) return null
        val fields = MeCardFields.parse(value.substring(MECARD_PREFIX.length))
        val values = { key: String -> fields.filter { it.first == key }.map { it.second.trim() }.filter { it.isNotEmpty() } }

        return contactOrNull(
            QrContent.Contact(
                name = values("N").firstOrNull()?.let(::meCardName),
                organization = values("ORG").firstOrNull(),
                phones = values("TEL"),
                emails = values("EMAIL"),
                urls = values("URL"),
                address = values("ADR").firstOrNull(),
            ),
        )
    }

    private fun meCardName(name: String): String {
        val parts = name.split(',').map(String::trim)

        return if (parts.size == 2) "${parts[1]} ${parts[0]}".trim() else name
    }

    private fun parseVCard(value: String): QrContent.Contact? {
        if (!value.startsWith(VCARD_PREFIX, ignoreCase = true)) return null
        val properties = VCard.properties(value)
        val first = { key: String -> properties.firstOrNull { it.name == key } }
        val all = { key: String -> properties.filter { it.name == key }.map { it.text() }.filter { it.isNotEmpty() } }
        val name =
            first("FN")?.text()?.takeIf { it.isNotEmpty() }
                ?: first("N")
                    ?.components()
                    ?.let { parts ->
                        listOf(parts.getOrNull(1), parts.getOrNull(2), parts.getOrNull(0))
                            .filterNot { it.isNullOrEmpty() }
                            .joinToString(" ")
                    }?.takeIf { it.isNotEmpty() }

        return contactOrNull(
            QrContent.Contact(
                name = name,
                organization =
                    first("ORG")
                        ?.components()
                        ?.filter { it.isNotEmpty() }
                        ?.joinToString(" ")
                        ?.takeIf { it.isNotEmpty() },
                phones = all("TEL"),
                emails = all("EMAIL"),
                urls = all("URL"),
                address =
                    first("ADR")
                        ?.components()
                        ?.filter { it.isNotEmpty() }
                        ?.joinToString(", ")
                        ?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    private fun contactOrNull(contact: QrContent.Contact): QrContent.Contact? = contact.takeIf { it.headline.isNotEmpty() }

    private fun parseUrl(value: String): QrContent.Url? {
        val candidate = if (value.startsWith("www.", ignoreCase = true)) "https://$value" else value
        val match = urlPattern.matchEntire(candidate) ?: return null
        val host = match.groupValues[2]
        if ('.' !in host && !host.equals("localhost", ignoreCase = true)) return null
        val scheme = match.groupValues[1]

        return QrContent.Url(scheme.lowercase() + candidate.substring(scheme.length))
    }
}
