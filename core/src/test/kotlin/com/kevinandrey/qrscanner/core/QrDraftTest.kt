package com.kevinandrey.qrscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrDraftTest {
    @Test
    fun `text drafts need content and stay under the size limit`() {
        assertEquals(listOf(DraftProblem.EMPTY_TEXT), QrDraft.Text("   ").problems())
        assertEquals("Hola", QrDraft.Text("  Hola ").payload())
        assertTrue(QrDraft.Text("Hola").isValid())
        assertEquals(listOf(DraftProblem.TOO_LONG), QrDraft.Text("ñ".repeat(QrDraft.MAX_PAYLOAD_BYTES)).problems())
    }

    @Test
    fun `link drafts add https when the scheme is missing`() {
        assertEquals("https://kevin.dev", QrDraft.Link(" kevin.dev ").payload())
        assertEquals("http://kevin.dev", QrDraft.Link("http://kevin.dev").payload())
        assertTrue(QrDraft.Link("kevin.dev/blog").isValid())
        assertEquals(listOf(DraftProblem.INVALID_URL), QrDraft.Link("").problems())
        assertEquals(listOf(DraftProblem.INVALID_URL), QrDraft.Link("no es un enlace").problems())
    }

    @Test
    fun `wifi drafts validate password rules per security type`() {
        assertEquals(
            listOf(DraftProblem.EMPTY_SSID, DraftProblem.INVALID_WPA_PASSWORD),
            QrDraft.Wifi(ssid = " ", password = "corta").problems(),
        )
        assertEquals(listOf(DraftProblem.EMPTY_PASSWORD), QrDraft.Wifi(ssid = "Casa", security = WifiSecurity.WEP).problems())
        assertTrue(QrDraft.Wifi(ssid = "Casa", password = "ignorada", security = WifiSecurity.OPEN).isValid())
    }

    @Test
    fun `wifi payload escapes special characters and parses back to the same network`() {
        val draft = QrDraft.Wifi(ssid = "Casa;Kevin", password = "p\"ss:word", hidden = true)

        assertEquals("WIFI:T:WPA;S:Casa\\;Kevin;P:p\\\"ss\\:word;H:true;;", draft.payload())
        assertEquals(
            QrContent.Wifi(ssid = "Casa;Kevin", password = "p\"ss:word", security = WifiSecurity.WPA, hidden = true),
            QrContentParser.parse(draft.payload()),
        )
        assertEquals("WIFI:T:nopass;S:Cafe;;", QrDraft.Wifi(ssid = "Cafe", password = "x", security = WifiSecurity.OPEN).payload())
    }

    @Test
    fun `contact drafts require a name and validate optional fields`() {
        assertEquals(
            listOf(DraftProblem.EMPTY_NAME, DraftProblem.INVALID_PHONE, DraftProblem.INVALID_EMAIL),
            QrDraft.Contact(phone = "12", email = "kevin@").problems(),
        )
        assertFalse(QrDraft.Contact().isValid())
    }

    @Test
    fun `contact payload is a MECARD that parses back`() {
        val draft =
            QrDraft.Contact(
                name = "Kevin Herrera",
                phone = "+57 300 123 4567",
                email = "kevin@example.com",
                organization = "ACME, Inc",
            )

        assertEquals("MECARD:N:Kevin Herrera;ORG:ACME\\, Inc;TEL:+57 300 123 4567;EMAIL:kevin@example.com;;", draft.payload())
        assertEquals(
            QrContent.Contact(
                name = "Kevin Herrera",
                organization = "ACME, Inc",
                phones = listOf("+57 300 123 4567"),
                emails = listOf("kevin@example.com"),
            ),
            QrContentParser.parse(draft.payload()),
        )
        assertEquals("MECARD:N:Ana;;", QrDraft.Contact(name = "Ana").payload())
    }
}
