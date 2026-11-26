package com.kevinandrey.qrscanner.ui.detail

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import com.kevinandrey.qrscanner.R
import com.kevinandrey.qrscanner.core.QrContent
import com.kevinandrey.qrscanner.core.WifiSecurity
import com.kevinandrey.qrscanner.ui.components.label

enum class ContentAction(
    @param:StringRes val label: Int,
    val icon: ImageVector,
) {
    OPEN_LINK(R.string.action_open_link, Icons.AutoMirrored.Outlined.OpenInNew),
    CONNECT_WIFI(R.string.action_connect_wifi, Icons.Outlined.Wifi),
    COPY_PASSWORD(R.string.action_copy_password, Icons.Outlined.Key),
    SAVE_CONTACT(R.string.action_save_contact, Icons.Outlined.PersonAdd),
    SEND_EMAIL(R.string.action_send_email, Icons.Outlined.Email),
    CALL(R.string.action_call, Icons.Outlined.Call),
    SEND_SMS(R.string.action_send_sms, Icons.AutoMirrored.Outlined.Send),
    OPEN_MAP(R.string.action_open_map, Icons.Outlined.Map),
    SEARCH_WEB(R.string.action_search_web, Icons.Outlined.Search),
}

/** Actions offered for a content, the most useful first. */
fun QrContent.actions(): List<ContentAction> =
    when (this) {
        is QrContent.Url -> listOf(ContentAction.OPEN_LINK)
        is QrContent.Wifi -> listOfNotNull(ContentAction.CONNECT_WIFI, ContentAction.COPY_PASSWORD.takeIf { password != null })
        is QrContent.Contact -> listOf(ContentAction.SAVE_CONTACT)
        is QrContent.Email -> listOf(ContentAction.SEND_EMAIL)
        is QrContent.Phone -> listOf(ContentAction.CALL)
        is QrContent.Sms -> listOf(ContentAction.SEND_SMS)
        is QrContent.Geo -> listOf(ContentAction.OPEN_MAP)
        is QrContent.Text -> listOf(ContentAction.SEARCH_WEB)
    }

data class DetailField(
    @param:StringRes val label: Int,
    val value: String? = null,
    @param:StringRes val valueRes: Int? = null,
)

/** Labeled values shown in the detail card. */
fun QrContent.fields(): List<DetailField> =
    when (this) {
        is QrContent.Url -> {
            listOf(DetailField(R.string.field_url, url))
        }

        is QrContent.Wifi -> {
            listOfNotNull(
                DetailField(R.string.field_network, ssid),
                DetailField(R.string.field_security, valueRes = security.label),
                password?.let { DetailField(R.string.field_password, it) },
                DetailField(R.string.field_hidden, valueRes = R.string.yes).takeIf { hidden },
            )
        }

        is QrContent.Contact -> {
            buildList {
                name?.let { add(DetailField(R.string.field_name, it)) }
                organization?.let { add(DetailField(R.string.field_organization, it)) }
                phones.forEach { add(DetailField(R.string.field_phone, it)) }
                emails.forEach { add(DetailField(R.string.field_email, it)) }
                urls.forEach { add(DetailField(R.string.field_website, it)) }
                address?.let { add(DetailField(R.string.field_address, it)) }
            }
        }

        is QrContent.Email -> {
            listOfNotNull(
                DetailField(R.string.field_to, address),
                subject?.let { DetailField(R.string.field_subject, it) },
                body?.let { DetailField(R.string.field_message, it) },
            )
        }

        is QrContent.Phone -> {
            listOf(DetailField(R.string.field_number, number))
        }

        is QrContent.Sms -> {
            listOfNotNull(DetailField(R.string.field_number, number), message?.let { DetailField(R.string.field_message, it) })
        }

        is QrContent.Geo -> {
            listOfNotNull(
                query?.let { DetailField(R.string.field_place, it) },
                DetailField(R.string.field_latitude, latitude.toString()),
                DetailField(R.string.field_longitude, longitude.toString()),
            )
        }

        is QrContent.Text -> {
            emptyList()
        }
    }

object ContentIntents {
    private const val WEB_SEARCH_URL = "https://www.google.com/search?q="

    /** Intent that performs [action] in another app, or `null` for actions handled inside this app. */
    fun forAction(
        action: ContentAction,
        content: QrContent,
    ): Intent? =
        when {
            action == ContentAction.OPEN_LINK && content is QrContent.Url -> {
                Intent(Intent.ACTION_VIEW, content.url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
            }

            action == ContentAction.CONNECT_WIFI && content is QrContent.Wifi -> {
                wifi(content)
            }

            action == ContentAction.SAVE_CONTACT && content is QrContent.Contact -> {
                contact(content)
            }

            action == ContentAction.SEND_EMAIL && content is QrContent.Email -> {
                Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf(content.address))
                    .putExtra(Intent.EXTRA_SUBJECT, content.subject)
                    .putExtra(Intent.EXTRA_TEXT, content.body)
            }

            action == ContentAction.CALL && content is QrContent.Phone -> {
                Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(content.number)}".toUri())
            }

            action == ContentAction.SEND_SMS && content is QrContent.Sms -> {
                Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(content.number)}".toUri()).putExtra("sms_body", content.message)
            }

            action == ContentAction.OPEN_MAP && content is QrContent.Geo -> {
                val point = "${content.latitude},${content.longitude}"
                val label = content.query?.let { "($it)" }.orEmpty()
                Intent(Intent.ACTION_VIEW, "geo:$point?q=${Uri.encode(point + label)}".toUri())
            }

            action == ContentAction.SEARCH_WEB && content is QrContent.Text -> {
                Intent(Intent.ACTION_VIEW, (WEB_SEARCH_URL + Uri.encode(content.text)).toUri())
            }

            else -> {
                null
            }
        }

    fun share(text: String): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
            null,
        )

    private fun wifi(content: QrContent.Wifi): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && content.security != WifiSecurity.WEP) {
            Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
                .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion(content)))
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun suggestion(content: QrContent.Wifi): WifiNetworkSuggestion =
        WifiNetworkSuggestion
            .Builder()
            .setSsid(content.ssid)
            .setIsHiddenSsid(content.hidden)
            .apply { content.password?.let(::setWpa2Passphrase) }
            .build()

    private fun contact(content: QrContent.Contact): Intent =
        Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            content.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            content.organization?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
            content.address?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
            listOf(
                ContactsContract.Intents.Insert.PHONE,
                ContactsContract.Intents.Insert.SECONDARY_PHONE,
                ContactsContract.Intents.Insert.TERTIARY_PHONE,
            ).zip(content.phones).forEach { (key, value) -> putExtra(key, value) }
            listOf(
                ContactsContract.Intents.Insert.EMAIL,
                ContactsContract.Intents.Insert.SECONDARY_EMAIL,
                ContactsContract.Intents.Insert.TERTIARY_EMAIL,
            ).zip(content.emails).forEach { (key, value) -> putExtra(key, value) }
            if (content.urls.isNotEmpty()) {
                val websites =
                    content.urls.map { url ->
                        ContentValues().apply {
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Website.URL, url)
                        }
                    }
                putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, ArrayList(websites))
            }
        }
}
