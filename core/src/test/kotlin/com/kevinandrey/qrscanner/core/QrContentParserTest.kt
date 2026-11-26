package com.kevinandrey.qrscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QrContentParserTest {
    @Test
    fun `plain text keeps the trimmed value and uses the first line as headline`() {
        val content = assertIs<QrContent.Text>(QrContentParser.parse("  \nHola mundo\nsegunda línea  "))

        assertEquals("Hola mundo\nsegunda línea", content.text)
        assertEquals("Hola mundo", content.headline)
        assertEquals(ContentKind.TEXT, content.kind)
    }

    @Test
    fun `links are detected with any scheme casing and www prefixes become https`() {
        assertEquals(QrContent.Url("https://example.com/Path?q=1"), QrContentParser.parse("HTTPS://example.com/Path?q=1"))
        assertEquals(QrContent.Url("http://localhost:8080/"), QrContentParser.parse("http://localhost:8080/"))
        assertEquals(QrContent.Url("https://www.kevin.dev"), QrContentParser.parse("www.kevin.dev"))
        assertEquals("kevin.dev/blog", QrContent.Url("https://www.kevin.dev/blog/").headline)
    }

    @Test
    fun `text that only looks like a link stays text`() {
        listOf("https://intranet", "ftp://files.example.com", "visita example.com", "https://exa mple.com").forEach {
            assertIs<QrContent.Text>(QrContentParser.parse(it), it)
        }
    }

    @Test
    fun `wifi credentials support escapes, hidden networks and every security type`() {
        assertEquals(
            QrContent.Wifi(ssid = "Casa;Kevin", password = "p:ss\\word", security = WifiSecurity.WPA, hidden = true),
            QrContentParser.parse("WIFI:T:WPA;S:Casa\\;Kevin;P:p\\:ss\\\\word;H:true;;"),
        )
        assertEquals(
            QrContent.Wifi(ssid = "Oficina", password = "abcde", security = WifiSecurity.WEP),
            QrContentParser.parse("wifi:S:Oficina;T:WEP;P:abcde;;"),
        )
        assertEquals(
            QrContent.Wifi(ssid = "Cafe", password = null, security = WifiSecurity.OPEN),
            QrContentParser.parse("WIFI:T:nopass;S:Cafe;P:ignored;;"),
        )
        assertEquals(WifiSecurity.WPA, (QrContentParser.parse("WIFI:S:Sin tipo;P:secreto123;;") as QrContent.Wifi).security)
        assertEquals(WifiSecurity.OPEN, (QrContentParser.parse("WIFI:S:Abierta;;") as QrContent.Wifi).security)
    }

    @Test
    fun `wifi without ssid is plain text`() {
        assertIs<QrContent.Text>(QrContentParser.parse("WIFI:T:WPA;P:secreto123;;"))
    }

    @Test
    fun `mailto and MATMSG emails decode subject and body`() {
        assertEquals(
            QrContent.Email("kevin+qr@example.com", "Hola Kevin", "Línea 1\nLínea 2"),
            QrContentParser.parse("mailto:kevin+qr@example.com?subject=Hola%20Kevin&body=L%C3%ADnea%201%0AL%C3%ADnea%202"),
        )
        assertEquals(
            QrContent.Email("ventas@example.com", "Cotización", null),
            QrContentParser.parse("MATMSG:TO:ventas@example.com;SUB:Cotización;BODY:;;"),
        )
        assertIs<QrContent.Text>(QrContentParser.parse("mailto:no-es-un-correo"))
    }

    @Test
    fun `phone and sms numbers need at least three digits`() {
        assertEquals(QrContent.Phone("+57 300 123 4567"), QrContentParser.parse("tel:+57%20300%20123%204567"))
        assertEquals(QrContent.Sms("+573001234567", "Llego en 5: espera"), QrContentParser.parse("SMSTO:+573001234567:Llego en 5: espera"))
        assertEquals(QrContent.Sms("3001234567", "Hola"), QrContentParser.parse("sms:3001234567?body=Hola"))
        assertEquals(QrContent.Sms("3001234567", null), QrContentParser.parse("smsto:3001234567"))
        assertIs<QrContent.Text>(QrContentParser.parse("tel:12"))
    }

    @Test
    fun `geo coordinates are validated and keep the search query`() {
        assertEquals(QrContent.Geo(4.6097, -74.0817, null), QrContentParser.parse("geo:4.6097,-74.0817"))
        assertEquals(
            QrContent.Geo(4.6097, -74.0817, "Plaza de Bolívar"),
            QrContentParser.parse("GEO:4.6097,-74.0817,2600;u=10?q=Plaza+de+Bol%C3%ADvar"),
        )
        assertEquals("4.6097, -74.0817", QrContent.Geo(4.6097, -74.0817).headline)
        assertIs<QrContent.Text>(QrContentParser.parse("geo:120,10"))
        assertIs<QrContent.Text>(QrContentParser.parse("geo:abc"))
    }

    @Test
    fun `MECARD contacts reorder last and first names`() {
        assertEquals(
            QrContent.Contact(
                name = "Kevin Herrera",
                organization = "Instance Shape",
                phones = listOf("+573001234567", "6011234567"),
                emails = listOf("kevin@example.com"),
                urls = listOf("https://kevin.dev"),
                address = "Bogotá",
            ),
            QrContentParser.parse(
                "MECARD:N:Herrera,Kevin;ORG:Instance Shape;TEL:+573001234567;TEL:6011234567;" +
                    "EMAIL:kevin@example.com;URL:https\\://kevin.dev;ADR:Bogotá;;",
            ),
        )
    }

    @Test
    fun `vCard contacts unfold lines, read parameters and escapes`() {
        val card =
            """
            BEGIN:VCARD
            VERSION:3.0
            N:Herrera Silva;Kevin;Andrey;;
            ORG:Instance Shape;Ingeniería
            TEL;TYPE=CELL:+57 300 123 4567
            item1.EMAIL;TYPE=INTERNET:kevin@example.com
            ADR;TYPE=WORK:;;Calle 100 \, oficina 2;Bogotá;;110111;Colombia
            NOTE:Una nota muy
             larga
            END:VCARD
            """.trimIndent()

        assertEquals(
            QrContent.Contact(
                name = "Kevin Andrey Herrera Silva",
                organization = "Instance Shape Ingeniería",
                phones = listOf("+57 300 123 4567"),
                emails = listOf("kevin@example.com"),
                address = "Calle 100 , oficina 2, Bogotá, 110111, Colombia",
            ),
            QrContentParser.parse(card),
        )
    }

    @Test
    fun `vCard formatted name wins and empty cards fall back to text`() {
        val content = QrContentParser.parse("BEGIN:VCARD\r\nFN:Ana Ríos\r\nN:Ríos;Ana\r\nEND:VCARD") as QrContent.Contact

        assertEquals("Ana Ríos", content.headline)
        assertIs<QrContent.Text>(QrContentParser.parse("BEGIN:VCARD\nVERSION:4.0\nEND:VCARD"))
        assertIs<QrContent.Text>(QrContentParser.parse("MECARD:NOTE:solo una nota;;"))
    }

    @Test
    fun `contact headline falls back to organization, phone and email`() {
        assertEquals("ACME", QrContent.Contact(organization = "ACME", phones = listOf("123")).headline)
        assertEquals("123", QrContent.Contact(phones = listOf("123")).headline)
        assertEquals("a@b.co", QrContent.Contact(emails = listOf("a@b.co")).headline)
    }

    @Test
    fun `percent decoding keeps invalid escapes and multibyte text`() {
        assertEquals("100% real 😀", UriText.decodePercent("100% real 😀"))
        assertEquals("ñ%zz", UriText.decodePercent("%C3%B1%zz"))
    }
}
