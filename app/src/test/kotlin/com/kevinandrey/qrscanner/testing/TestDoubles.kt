package com.kevinandrey.qrscanner.testing

import com.kevinandrey.qrscanner.core.Scan
import com.kevinandrey.qrscanner.data.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Instant

/** In-memory repository with the same ordering and deduplication rules as the Room one. */
class FakeScanRepository(
    initial: List<Scan> = emptyList(),
) : ScanRepository {
    val scans = MutableStateFlow(initial.sortedByDescending(Scan::scannedAt))
    private var nextId = (initial.maxOfOrNull(Scan::id) ?: 0) + 1
    private var clock = initial.maxOfOrNull(Scan::scannedAt) ?: Instant.parse("2026-11-20T15:00:00Z")

    override fun observeScans(): Flow<List<Scan>> = scans

    override fun observeScan(id: Long): Flow<Scan?> = scans.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun record(raw: String): Long {
        clock = clock.plusSeconds(60)
        val existing = scans.value.firstOrNull { it.raw == raw }
        val scan = existing?.copy(scannedAt = clock) ?: Scan(id = nextId++, raw = raw, scannedAt = clock)
        scans.value = listOf(scan) + scans.value.filterNot { it.id == scan.id }

        return scan.id
    }

    override suspend fun setFavorite(
        id: Long,
        favorite: Boolean,
    ) {
        scans.value = scans.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
    }

    override suspend fun delete(id: Long) {
        scans.value = scans.value.filterNot { it.id == id }
    }

    override suspend fun clearHistory() {
        scans.value = scans.value.filter(Scan::favorite)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

object SampleScans {
    private val base = Instant.parse("2026-11-20T15:00:00Z")

    val link = Scan(1, "https://github.com/KevinAndrey96", base.plusSeconds(500), favorite = true)
    val wifi = Scan(2, "WIFI:T:WPA;S:Oficina Bogotá;P:secreto123;;", base.plusSeconds(400))
    val contact = Scan(3, "MECARD:N:Ríos,Valentina;TEL:+573001234567;EMAIL:valentina@example.com;;", base.plusSeconds(300))
    val text = Scan(4, "Pedido 2045 listo para recoger", base.plusSeconds(200))
    val geo = Scan(5, "geo:4.5981,-74.0758?q=Plaza+de+Bol%C3%ADvar", base.plusSeconds(100))

    val all = listOf(link, wifi, contact, text, geo)
}
