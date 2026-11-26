package com.kevinandrey.qrscanner.core

import java.io.ByteArrayOutputStream

/** Fields of the MECARD family (`WIFI:`, `MECARD:`, `MATMSG:`): `KEY:value;` with backslash escapes. */
internal object MeCardFields {
    private const val SPECIAL_CHARACTERS = "\\;,:\""

    fun parse(body: String): List<Pair<String, String>> =
        splitUnescaped(body, ';').mapNotNull { field ->
            val separator = indexOfUnescaped(field, ':')
            if (separator <= 0) null else field.substring(0, separator).trim().uppercase() to unescape(field.substring(separator + 1))
        }

    fun escape(value: String): String =
        buildString {
            value.forEach { character ->
                if (character in SPECIAL_CHARACTERS) append('\\')
                append(character)
            }
        }

    fun splitUnescaped(
        value: String,
        delimiter: Char,
    ): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < value.length) {
            val character = value[index]
            when {
                character == '\\' && index + 1 < value.length -> {
                    current.append(character).append(value[index + 1])
                    index++
                }

                character == delimiter -> {
                    parts += current.toString()
                    current.clear()
                }

                else -> {
                    current.append(character)
                }
            }
            index++
        }
        if (current.isNotEmpty()) parts += current.toString()

        return parts
    }

    private fun indexOfUnescaped(
        value: String,
        target: Char,
    ): Int {
        var index = 0
        while (index < value.length) {
            when (value[index]) {
                '\\' -> index++
                target -> return index
            }
            index++
        }

        return -1
    }

    private fun unescape(value: String): String =
        buildString {
            var escaped = false
            value.forEach { character ->
                if (!escaped && character == '\\') {
                    escaped = true
                } else {
                    append(character)
                    escaped = false
                }
            }
        }
}

/** Minimal vCard 2.1, 3.0 and 4.0 reader for the properties a contact card needs. */
internal object VCard {
    data class Property(
        val name: String,
        val value: String,
    ) {
        fun text(): String = unescape(value).trim()

        fun components(): List<String> = MeCardFields.splitUnescaped(value, ';').map { unescape(it).trim() }
    }

    fun properties(card: String): List<Property> =
        unfold(card).mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) {
                null
            } else {
                val name =
                    line
                        .substring(0, separator)
                        .substringBefore(';')
                        .substringAfter('.')
                        .trim()
                        .uppercase()
                Property(name, line.substring(separator + 1))
            }
        }

    private fun unfold(card: String): List<String> {
        val lines = mutableListOf<String>()
        card.split("\r\n", "\n", "\r").forEach { line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && lines.isNotEmpty()) {
                lines[lines.lastIndex] = lines.last() + line.substring(1)
            } else {
                lines += line
            }
        }

        return lines
    }

    private fun unescape(value: String): String =
        buildString {
            var index = 0
            while (index < value.length) {
                val character = value[index]
                if (character == '\\' && index + 1 < value.length) {
                    val next = value[index + 1]
                    append(if (next == 'n' || next == 'N') '\n' else next)
                    index++
                } else {
                    append(character)
                }
                index++
            }
        }
}

/** Percent decoding and query parsing for `mailto:`, `sms:` and `geo:` URIs. */
internal object UriText {
    private const val HEX_RADIX = 16

    fun queryParameters(
        query: String,
        plusAsSpace: Boolean = false,
    ): Map<String, String> =
        query
            .split('&')
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val value = pair.substringAfter('=', missingDelimiterValue = "")
                pair.substringBefore('=').lowercase() to decodePercent(if (plusAsSpace) value.replace('+', ' ') else value)
            }

    fun decodePercent(value: String): String {
        if ('%' !in value) return value
        val bytes = ByteArrayOutputStream()
        var index = 0
        while (index < value.length) {
            val byte =
                if (value[index] == '%' && index + 2 <= value.lastIndex) {
                    value.substring(index + 1, index + 3).toIntOrNull(HEX_RADIX)
                } else {
                    null
                }
            if (byte != null) {
                bytes.write(byte)
                index += 3
            } else {
                val end = value.indexOf('%', index + 1).takeIf { it >= 0 } ?: value.length
                bytes.writeBytes(value.substring(index, end).toByteArray(Charsets.UTF_8))
                index = end
            }
        }

        return bytes.toString(Charsets.UTF_8)
    }
}
