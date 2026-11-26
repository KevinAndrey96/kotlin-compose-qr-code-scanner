package com.kevinandrey.qrscanner.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanSearchTest {
    private val link = scan(1, "https://kevin.dev/proyectos", favorite = true)
    private val wifi = scan(2, "WIFI:T:WPA;S:Oficina Bogotá;P:secreto123;;")
    private val contact = scan(3, "MECARD:N:Ríos,Valentina;TEL:3001234567;;")
    private val text = scan(4, "Pedido 2045 listo")
    private val phone = scan(5, "tel:3001234567")
    private val all = listOf(link, wifi, contact, text, phone)

    @Test
    fun `filters split the history by content kind and favorites`() {
        assertEquals(all, ScanSearch.apply(all, "", ScanFilter.ALL))
        assertEquals(listOf(link), ScanSearch.apply(all, "", ScanFilter.FAVORITES))
        assertEquals(listOf(link), ScanSearch.apply(all, "", ScanFilter.LINKS))
        assertEquals(listOf(wifi), ScanSearch.apply(all, "", ScanFilter.WIFI))
        assertEquals(listOf(contact), ScanSearch.apply(all, "", ScanFilter.CONTACTS))
        assertEquals(listOf(text, phone), ScanSearch.apply(all, "", ScanFilter.OTHER))
    }

    @Test
    fun `search ignores accents and case and requires every word`() {
        assertEquals(listOf(wifi), ScanSearch.apply(all, "OFICINA bogota", ScanFilter.ALL))
        assertEquals(listOf(contact), ScanSearch.apply(all, "valentina rios", ScanFilter.ALL))
        assertEquals(listOf(contact, phone), ScanSearch.apply(all, "3001234567", ScanFilter.ALL))
        assertEquals(listOf(phone), ScanSearch.apply(all, "3001234567", ScanFilter.OTHER))
        assertEquals(emptyList(), ScanSearch.apply(all, "pedido 9999", ScanFilter.ALL))
    }

    @Test
    fun `normalization collapses whitespace`() {
        assertEquals("cafe con leche", ScanSearch.normalize("  Café   CON\tleche "))
    }

    private fun scan(
        id: Long,
        raw: String,
        favorite: Boolean = false,
    ) = Scan(id = id, raw = raw, scannedAt = Instant.EPOCH.plusSeconds(id), favorite = favorite)
}
