package com.kevinandrey.qrscanner.core

/** Content typed by the user in the generator, validated before it becomes a QR payload. */
public sealed interface QrDraft {
    /** Every problem at once, so the form can show them together. Empty when the draft is valid. */
    public fun problems(): List<DraftProblem>

    /** Text to encode. Only meaningful when [problems] is empty. */
    public fun payload(): String

    public fun isValid(): Boolean = problems().isEmpty()

    public data class Text(
        val text: String = "",
    ) : QrDraft {
        override fun problems(): List<DraftProblem> =
            buildList {
                if (text.isBlank()) add(DraftProblem.EMPTY_TEXT)
                addAll(sizeProblems(payload()))
            }

        override fun payload(): String = text.trim()
    }

    public data class Link(
        val url: String = "",
    ) : QrDraft {
        override fun problems(): List<DraftProblem> =
            buildList {
                if (QrContentParser.parse(payload()) !is QrContent.Url) add(DraftProblem.INVALID_URL)
                addAll(sizeProblems(payload()))
            }

        override fun payload(): String {
            val value = url.trim()

            return if (value.isEmpty() || "://" in value) value else "https://$value"
        }
    }

    public data class Wifi(
        val ssid: String = "",
        val password: String = "",
        val security: WifiSecurity = WifiSecurity.WPA,
        val hidden: Boolean = false,
    ) : QrDraft {
        override fun problems(): List<DraftProblem> =
            buildList {
                if (ssid.isBlank()) add(DraftProblem.EMPTY_SSID)
                when (security) {
                    WifiSecurity.WPA -> if (password.length !in WPA_PASSWORD_LENGTH) add(DraftProblem.INVALID_WPA_PASSWORD)
                    WifiSecurity.WEP -> if (password.isEmpty()) add(DraftProblem.EMPTY_PASSWORD)
                    WifiSecurity.OPEN -> Unit
                }
                addAll(sizeProblems(payload()))
            }

        override fun payload(): String =
            buildString {
                append("WIFI:T:")
                append(
                    when (security) {
                        WifiSecurity.WPA -> "WPA"
                        WifiSecurity.WEP -> "WEP"
                        WifiSecurity.OPEN -> "nopass"
                    },
                )
                append(";S:").append(MeCardFields.escape(ssid.trim())).append(';')
                if (security != WifiSecurity.OPEN && password.isNotEmpty()) {
                    append("P:").append(MeCardFields.escape(password)).append(';')
                }
                if (hidden) append("H:true;")
                append(';')
            }
    }

    public data class Contact(
        val name: String = "",
        val phone: String = "",
        val email: String = "",
        val organization: String = "",
    ) : QrDraft {
        override fun problems(): List<DraftProblem> =
            buildList {
                if (name.isBlank()) add(DraftProblem.EMPTY_NAME)
                if (phone.isNotBlank() && !QrContentParser.isPhone(phone)) add(DraftProblem.INVALID_PHONE)
                if (email.isNotBlank() && !QrContentParser.isEmail(email.trim())) add(DraftProblem.INVALID_EMAIL)
                addAll(sizeProblems(payload()))
            }

        override fun payload(): String =
            buildString {
                append("MECARD:N:").append(MeCardFields.escape(name.trim())).append(';')
                if (organization.isNotBlank()) append("ORG:").append(MeCardFields.escape(organization.trim())).append(';')
                if (phone.isNotBlank()) append("TEL:").append(MeCardFields.escape(phone.trim())).append(';')
                if (email.isNotBlank()) append("EMAIL:").append(MeCardFields.escape(email.trim())).append(';')
                append(';')
            }
    }

    public companion object {
        /** Keeps codes dense enough to scan reliably from a phone screen. */
        public const val MAX_PAYLOAD_BYTES: Int = 1200
        private val WPA_PASSWORD_LENGTH = 8..63

        private fun sizeProblems(payload: String): List<DraftProblem> =
            if (payload.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES) listOf(DraftProblem.TOO_LONG) else emptyList()
    }
}

public enum class DraftProblem {
    EMPTY_TEXT,
    INVALID_URL,
    EMPTY_SSID,
    INVALID_WPA_PASSWORD,
    EMPTY_PASSWORD,
    EMPTY_NAME,
    INVALID_PHONE,
    INVALID_EMAIL,
    TOO_LONG,
}
