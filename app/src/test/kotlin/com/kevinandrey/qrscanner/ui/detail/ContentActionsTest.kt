package com.kevinandrey.qrscanner.ui.detail

import android.content.ContentValues
import android.content.Intent
import android.net.wifi.WifiNetworkSuggestion
import android.provider.ContactsContract
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kevinandrey.qrscanner.R
import com.kevinandrey.qrscanner.core.QrContent
import com.kevinandrey.qrscanner.core.WifiSecurity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ContentActionsTest {
    private val wifi = QrContent.Wifi(ssid = "Casa", password = "secreto123", hidden = true)
    private val contact =
        QrContent.Contact(
            name = "Valentina Ríos",
            organization = "ACME",
            phones = listOf("+573001234567", "6011234567"),
            emails = listOf("valentina@example.com"),
            urls = listOf("https://acme.example"),
            address = "Bogotá",
        )

    @Test
    fun everyContentOffersItsMainActionFirst() {
        assertEquals(listOf(ContentAction.OPEN_LINK), QrContent.Url("https://kevin.dev").actions())
        assertEquals(listOf(ContentAction.CONNECT_WIFI, ContentAction.COPY_PASSWORD), wifi.actions())
        assertEquals(listOf(ContentAction.CONNECT_WIFI), QrContent.Wifi("Cafe", security = WifiSecurity.OPEN).actions())
        assertEquals(listOf(ContentAction.SAVE_CONTACT), contact.actions())
        assertEquals(listOf(ContentAction.SEND_EMAIL), QrContent.Email("a@b.co").actions())
        assertEquals(listOf(ContentAction.CALL), QrContent.Phone("300").actions())
        assertEquals(listOf(ContentAction.SEND_SMS), QrContent.Sms("300").actions())
        assertEquals(listOf(ContentAction.OPEN_MAP), QrContent.Geo(1.0, 2.0).actions())
        assertEquals(listOf(ContentAction.SEARCH_WEB), QrContent.Text("hola").actions())
    }

    @Test
    fun fieldsDescribeEachContent() {
        assertEquals(
            listOf(
                DetailField(R.string.field_network, "Casa"),
                DetailField(R.string.field_security, valueRes = R.string.security_wpa),
                DetailField(R.string.field_password, "secreto123"),
                DetailField(R.string.field_hidden, valueRes = R.string.yes),
            ),
            wifi.fields(),
        )
        assertEquals(7, contact.fields().size)
        assertEquals(3, QrContent.Email("a@b.co", "Asunto", "Cuerpo").fields().size)
        assertEquals(2, QrContent.Sms("300", "Hola").fields().size)
        assertEquals(3, QrContent.Geo(1.0, 2.0, "Plaza").fields().size)
        assertEquals(listOf(DetailField(R.string.field_number, "300")), QrContent.Phone("300").fields())
        assertEquals(listOf(DetailField(R.string.field_url, "https://kevin.dev")), QrContent.Url("https://kevin.dev").fields())
        assertTrue(QrContent.Text("hola").fields().isEmpty())
    }

    @Test
    fun simpleActionsBuildTheStandardIntents() {
        val link = intent(ContentAction.OPEN_LINK, QrContent.Url("https://kevin.dev"))
        assertEquals(Intent.ACTION_VIEW, link.action)
        assertEquals("https://kevin.dev", link.dataString)

        val email = intent(ContentAction.SEND_EMAIL, QrContent.Email("a@b.co", "Hola", "Texto"))
        assertEquals(Intent.ACTION_SENDTO, email.action)
        assertEquals(listOf("a@b.co"), email.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList())
        assertEquals("Hola", email.getStringExtra(Intent.EXTRA_SUBJECT))

        assertEquals("tel:%2B57%20300", intent(ContentAction.CALL, QrContent.Phone("+57 300")).dataString)

        val sms = intent(ContentAction.SEND_SMS, QrContent.Sms("300", "Hola"))
        assertEquals("smsto:300", sms.dataString)
        assertEquals("Hola", sms.getStringExtra("sms_body"))

        assertEquals(
            "geo:4.6,-74.0?q=4.6%2C-74.0(Plaza)",
            intent(ContentAction.OPEN_MAP, QrContent.Geo(4.6, -74.0, "Plaza")).dataString,
        )
        assertEquals(
            "https://www.google.com/search?q=hola%20mundo",
            intent(ContentAction.SEARCH_WEB, QrContent.Text("hola mundo")).dataString,
        )
    }

    @Test
    fun copyPasswordAndMismatchedActionsHaveNoIntent() {
        assertNull(ContentIntents.forAction(ContentAction.COPY_PASSWORD, wifi))
        assertNull(ContentIntents.forAction(ContentAction.CALL, QrContent.Text("hola")))
    }

    @Test
    fun contactsAreInsertedWithEveryKnownField() {
        val insert = intent(ContentAction.SAVE_CONTACT, contact)

        assertEquals(ContactsContract.Intents.Insert.ACTION, insert.action)
        assertEquals("Valentina Ríos", insert.getStringExtra(ContactsContract.Intents.Insert.NAME))
        assertEquals("ACME", insert.getStringExtra(ContactsContract.Intents.Insert.COMPANY))
        assertEquals("6011234567", insert.getStringExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE))
        assertEquals("valentina@example.com", insert.getStringExtra(ContactsContract.Intents.Insert.EMAIL))
        assertEquals("Bogotá", insert.getStringExtra(ContactsContract.Intents.Insert.POSTAL))
        @Suppress("DEPRECATION")
        val websites = insert.getParcelableArrayListExtra<ContentValues>(ContactsContract.Intents.Insert.DATA)
        assertEquals("https://acme.example", websites?.single()?.getAsString(ContactsContract.CommonDataKinds.Website.URL))
    }

    @Test
    fun wifiUsesTheAddNetworksPanelOnAndroid11AndLater() {
        val add = intent(ContentAction.CONNECT_WIFI, wifi)

        assertEquals(Settings.ACTION_WIFI_ADD_NETWORKS, add.action)
        @Suppress("DEPRECATION")
        val suggestions = add.getParcelableArrayListExtra<WifiNetworkSuggestion>(Settings.EXTRA_WIFI_NETWORK_LIST)
        assertEquals("Casa", suggestions?.single()?.ssid)
        assertEquals(
            Settings.ACTION_WIFI_SETTINGS,
            intent(ContentAction.CONNECT_WIFI, wifi.copy(security = WifiSecurity.WEP)).action,
        )
    }

    @Test
    @Config(sdk = [29])
    fun wifiFallsBackToSettingsOnOlderAndroid() {
        assertEquals(Settings.ACTION_WIFI_SETTINGS, intent(ContentAction.CONNECT_WIFI, wifi).action)
    }

    @Test
    fun shareWrapsTheTextInAChooser() {
        val chooser = ContentIntents.share("hola")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val target = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertEquals("hola", target?.getStringExtra(Intent.EXTRA_TEXT))
    }

    private fun intent(
        action: ContentAction,
        content: QrContent,
    ): Intent = assertNotNull(ContentIntents.forAction(action, content))
}
