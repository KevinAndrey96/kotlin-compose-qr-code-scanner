package com.kevinandrey.qrscanner.ui

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import com.kevinandrey.qrscanner.core.DraftProblem
import com.kevinandrey.qrscanner.core.QrDraft
import com.kevinandrey.qrscanner.core.QrEncoder
import com.kevinandrey.qrscanner.core.ScanFilter
import com.kevinandrey.qrscanner.core.WifiSecurity
import com.kevinandrey.qrscanner.testing.FakeScanRepository
import com.kevinandrey.qrscanner.testing.MainDispatcherRule
import com.kevinandrey.qrscanner.testing.SampleScans
import com.kevinandrey.qrscanner.ui.detail.ScanDetailUiState
import com.kevinandrey.qrscanner.ui.detail.ScanDetailViewModel
import com.kevinandrey.qrscanner.ui.generator.GeneratorType
import com.kevinandrey.qrscanner.ui.generator.GeneratorViewModel
import com.kevinandrey.qrscanner.ui.history.HistoryViewModel
import com.kevinandrey.qrscanner.ui.scanner.ImageReadResult
import com.kevinandrey.qrscanner.ui.scanner.ScannerEvent
import com.kevinandrey.qrscanner.ui.scanner.ScannerMessage
import com.kevinandrey.qrscanner.ui.scanner.ScannerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScannerViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeScanRepository()
    private val viewModel by lazy { ScannerViewModel(repository) }

    @Test
    fun theFirstDetectedCodeIsStoredAndOpenedAndRepeatsAreIgnored() =
        runTest {
            viewModel.events.test {
                viewModel.onCodeDetected("https://kevin.dev")
                viewModel.onCodeDetected("https://otro.dev")

                assertEquals(ScannerEvent.OpenScan(1), awaitItem())
                expectNoEvents()
            }
            assertEquals(listOf("https://kevin.dev"), repository.scans.value.map { it.raw })
        }

    @Test
    fun scanningResumesWhenTheScannerIsVisibleAgain() =
        runTest {
            viewModel.events.test {
                viewModel.onCodeDetected("primero")
                assertIs<ScannerEvent.OpenScan>(awaitItem())

                viewModel.onScannerVisible()
                viewModel.onCodeDetected("segundo")

                assertEquals(ScannerEvent.OpenScan(2), awaitItem())
            }
        }

    @Test
    fun blankCodesAreIgnored() {
        viewModel.onCodeDetected("   ")

        assertTrue(repository.scans.value.isEmpty())
    }

    @Test
    fun imageResultsOpenTheCodeOrExplainWhyNot() =
        runTest {
            viewModel.onImageRead(ImageReadResult.NotFound)
            assertEquals(ScannerMessage.NO_CODE_IN_IMAGE, viewModel.state.value.message)

            viewModel.onImageRead(ImageReadResult.Unreadable)
            assertEquals(ScannerMessage.IMAGE_UNREADABLE, viewModel.state.value.message)

            viewModel.onMessageShown()
            assertNull(viewModel.state.value.message)

            viewModel.events.test {
                viewModel.onImageRead(ImageReadResult.Found("Pedido 2045"))
                assertEquals(ScannerEvent.OpenScan(1), awaitItem())
            }
        }

    @Test
    fun torchToggles() {
        viewModel.onTorchToggled()
        assertTrue(viewModel.state.value.torchOn)

        viewModel.onTorchToggled()
        assertFalse(viewModel.state.value.torchOn)
    }
}

class HistoryViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeScanRepository(SampleScans.all)
    private val viewModel by lazy { HistoryViewModel(repository) }

    @Test
    fun filtersAndSearchNarrowTheHistory() =
        runTest {
            viewModel.state.test {
                val loaded = awaitItem().takeUnless { it.loading } ?: awaitItem()
                assertEquals(SampleScans.all, loaded.scans)

                viewModel.onFilterSelected(ScanFilter.WIFI)
                Snapshot.sendApplyNotifications()
                assertEquals(listOf(SampleScans.wifi), awaitItem().scans)

                viewModel.onFilterSelected(ScanFilter.ALL)
                viewModel.onQueryChanged("valentina")
                Snapshot.sendApplyNotifications()
                val searched = awaitItem()
                assertEquals(listOf(SampleScans.contact), searched.scans)
                assertTrue(searched.hasHistory)
                assertEquals("valentina", viewModel.query)
            }
        }

    @Test
    fun favoritesToggleAndClearingKeepsThem() =
        runTest {
            viewModel.onFavoriteToggled(SampleScans.wifi)
            viewModel.onClearConfirmed()

            assertEquals(listOf(SampleScans.link.id, SampleScans.wifi.id), repository.scans.value.map { it.id })
            assertTrue(repository.scans.value.all { it.favorite })
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScanDetailViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeScanRepository(SampleScans.all)

    @Test
    fun loadsTheScanAndTogglesFavorite() =
        runTest {
            val viewModel = ScanDetailViewModel(SampleScans.wifi.id, repository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            assertEquals(ScanDetailUiState.Loaded(SampleScans.wifi), viewModel.state.value)

            viewModel.onFavoriteToggled()

            assertTrue(assertIs<ScanDetailUiState.Loaded>(viewModel.state.value).scan.favorite)
        }

    @Test
    fun deletingReportsCompletion() =
        runTest {
            val viewModel = ScanDetailViewModel(SampleScans.text.id, repository)

            viewModel.onDeleteConfirmed()

            assertTrue(viewModel.deleted.value)
            assertTrue(repository.scans.value.none { it.id == SampleScans.text.id })
        }

    @Test
    fun unknownScansAreReportedAsMissingAndFavoriteIsIgnored() =
        runTest {
            val viewModel = ScanDetailViewModel(99, repository)

            viewModel.onFavoriteToggled()

            assertEquals(ScanDetailUiState.NotFound, viewModel.state.first { it != ScanDetailUiState.Loading })
        }
}

class GeneratorViewModelTest {
    private val viewModel = GeneratorViewModel()

    @Test
    fun freshFormsShowNoProblemsAndNoCode() {
        assertEquals(GeneratorType.TEXT, viewModel.state.type)
        assertTrue(viewModel.state.problems.isEmpty())
        assertNull(viewModel.state.matrix)
    }

    @Test
    fun validTextProducesTheMatchingMatrix() {
        viewModel.onTextChanged("Hola Kevin")

        assertEquals(QrEncoder.encode("Hola Kevin"), viewModel.state.matrix)
    }

    @Test
    fun editedInvalidFormsShowTheirProblems() {
        viewModel.onTypeSelected(GeneratorType.LINK)
        viewModel.onLinkChanged("no es un enlace")

        assertEquals(listOf(DraftProblem.INVALID_URL), viewModel.state.problems)
        assertNull(viewModel.state.matrix)
    }

    @Test
    fun eachTypeKeepsItsOwnDraft() {
        viewModel.onTypeSelected(GeneratorType.WIFI)
        viewModel.onWifiChanged(QrDraft.Wifi(ssid = "Casa", security = WifiSecurity.OPEN))
        val wifiMatrix = assertNotNull(viewModel.state.matrix)

        viewModel.onTypeSelected(GeneratorType.CONTACT)
        assertNull(viewModel.state.matrix)
        assertTrue(viewModel.state.problems.isEmpty())

        viewModel.onContactChanged(QrDraft.Contact(name = "Kevin"))
        assertNotNull(viewModel.state.matrix)

        viewModel.onTypeSelected(GeneratorType.WIFI)
        assertEquals(wifiMatrix, viewModel.state.matrix)
    }
}
