package com.kevinandrey.qrscanner.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kevinandrey.qrscanner.QrScannerApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RoomScanRepositoryTest {
    private lateinit var database: ScanDatabase
    private lateinit var repository: RoomScanRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScanDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomScanRepository(database.scanDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun recordStoresScansNewestFirstAndParsesTheirContent() =
        runTest {
            val first = repository.record("https://kevin.dev")
            Thread.sleep(2)
            val second = repository.record("tel:3001234567")

            val scans = repository.observeScans().first()

            assertEquals(listOf(second, first), scans.map { it.id })
            assertEquals("3001234567", scans.first().content.headline)
        }

    @Test
    fun scanningTheSameCodeAgainMovesItToTheTopWithoutDuplicating() =
        runTest {
            val original = repository.record("Pedido 2045")
            Thread.sleep(2)
            repository.record("Pedido 2046")
            Thread.sleep(2)
            val repeated = repository.record("Pedido 2045")

            val scans = repository.observeScans().first()

            assertEquals(original, repeated)
            assertEquals(listOf("Pedido 2045", "Pedido 2046"), scans.map { it.raw })
        }

    @Test
    fun favoritesSurviveClearingTheHistory() =
        runTest {
            val kept = repository.record("https://kevin.dev")
            repository.record("Pedido 2045")
            repository.setFavorite(kept, favorite = true)

            repository.clearHistory()

            val scans = repository.observeScans().first()
            assertEquals(listOf(kept), scans.map { it.id })
            assertTrue(scans.single().favorite)
        }

    @Test
    fun deleteRemovesASingleScan() =
        runTest {
            val id = repository.record("Pedido 2045")

            repository.delete(id)

            assertNull(repository.observeScan(id).first())
        }

    @Test
    fun applicationContainerBuildsTheRoomRepository() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<QrScannerApplication>()
            val repository = application.container.scanRepository

            val id = repository.record("container smoke test")

            assertEquals("container smoke test", repository.observeScan(id).first()?.raw)
        }
}
