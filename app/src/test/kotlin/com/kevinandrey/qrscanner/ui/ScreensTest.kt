package com.kevinandrey.qrscanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kevinandrey.qrscanner.core.QrContent
import com.kevinandrey.qrscanner.core.Scan
import com.kevinandrey.qrscanner.core.ScanFilter
import com.kevinandrey.qrscanner.testing.SampleScans
import com.kevinandrey.qrscanner.ui.detail.ContentAction
import com.kevinandrey.qrscanner.ui.detail.ScanDetailContent
import com.kevinandrey.qrscanner.ui.detail.ScanDetailUiState
import com.kevinandrey.qrscanner.ui.generator.GeneratorContent
import com.kevinandrey.qrscanner.ui.generator.GeneratorViewModel
import com.kevinandrey.qrscanner.ui.history.HistoryContent
import com.kevinandrey.qrscanner.ui.history.HistoryUiState
import com.kevinandrey.qrscanner.ui.scanner.ScannerContent
import com.kevinandrey.qrscanner.ui.theme.QrScannerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HistoryContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyHistoryExplainsWhatWillAppear() {
        compose.setContent { QrScannerTheme { HistoryUnderTest(HistoryUiState(loading = false)) } }

        compose.onNodeWithText("Aún no has escaneado códigos").assertIsDisplayed()
        compose.onNodeWithContentDescription("Borrar historial").assertDoesNotExist()
    }

    @Test
    fun scansShowTheirHeadlineKindAndOpenOnClick() {
        var opened: Long? = null
        var favorite: Scan? = null
        compose.setContent {
            QrScannerTheme {
                HistoryUnderTest(
                    HistoryUiState(loading = false, scans = SampleScans.all, hasHistory = true),
                    onOpenScan = { opened = it },
                    onFavoriteToggled = { favorite = it },
                )
            }
        }

        compose.onNodeWithText("Oficina Bogotá").assertIsDisplayed()
        compose.onNodeWithText("Valentina Ríos").performClick()
        compose.onNodeWithContentDescription("Quitar de favoritos").performClick()

        assertEquals(SampleScans.contact.id, opened)
        assertEquals(SampleScans.link, favorite)
    }

    @Test
    fun searchFiltersAndClearingAreWiredToCallbacks() {
        var query by mutableStateOf("")
        var filter: ScanFilter? = null
        var cleared = false
        compose.setContent {
            QrScannerTheme {
                HistoryContent(
                    state = HistoryUiState(loading = false, scans = emptyList(), hasHistory = true),
                    query = query,
                    onQueryChanged = { query = it },
                    onFilterSelected = { filter = it },
                    onFavoriteToggled = {},
                    onClearConfirmed = { cleared = true },
                    onOpenScan = {},
                )
            }
        }

        compose.onNodeWithText("Buscar en el historial").performTextInput("xyz")
        compose.onNodeWithText("Ningún código coincide con la búsqueda").assertIsDisplayed()
        compose.onNodeWithContentDescription("Borrar búsqueda").performClick()
        compose.onNodeWithText("Wi-Fi").performClick()
        compose.onNodeWithContentDescription("Borrar historial").performClick()
        compose.onNodeWithText("Se eliminarán todos los códigos excepto tus favoritos.").assertIsDisplayed()
        compose.onNodeWithText("Borrar").performClick()

        assertEquals("", query)
        assertEquals(ScanFilter.WIFI, filter)
        assertTrue(cleared)
    }

    @Composable
    private fun HistoryUnderTest(
        state: HistoryUiState,
        onOpenScan: (Long) -> Unit = {},
        onFavoriteToggled: (Scan) -> Unit = {},
    ) = HistoryContent(
        state = state,
        query = "",
        onQueryChanged = {},
        onFilterSelected = {},
        onFavoriteToggled = onFavoriteToggled,
        onClearConfirmed = {},
        onOpenScan = onOpenScan,
    )
}

@RunWith(AndroidJUnit4::class)
class ScanDetailContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun wifiDetailShowsCredentialsAndActions() {
        val actions = mutableListOf<ContentAction>()
        var copied: String? = null
        compose.setContent {
            QrScannerTheme {
                DetailUnderTest(ScanDetailUiState.Loaded(SampleScans.wifi), onAction = { action, _ -> actions += action }, onCopy = {
                    copied =
                        it
                })
            }
        }

        compose.onNodeWithText("Red Wi-Fi").assertIsDisplayed()
        compose.onNodeWithText("secreto123").assertExists()
        compose.onNodeWithText("Conectarse").performClick()
        compose.onNodeWithText("Copiar contraseña").performClick()
        compose.onNodeWithText("Copiar").performClick()

        assertEquals(listOf(ContentAction.CONNECT_WIFI, ContentAction.COPY_PASSWORD), actions)
        assertEquals(SampleScans.wifi.raw, copied)
    }

    @Test
    fun linksShareTheUrlAndFavoritesAndDeletionAreConfirmed() {
        var shared: String? = null
        var favoriteToggled = false
        var deleted = false
        compose.setContent {
            QrScannerTheme {
                ScanDetailContent(
                    state = ScanDetailUiState.Loaded(SampleScans.link),
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onFavoriteToggled = { favoriteToggled = true },
                    onDeleteConfirmed = { deleted = true },
                    onAction = { _, content -> assertTrue(content is QrContent.Url) },
                    onCopy = {},
                    onShare = { shared = it },
                )
            }
        }

        compose.onNodeWithText("Compartir").performClick()
        compose.onNodeWithContentDescription("Quitar de favoritos").performClick()
        compose.onNodeWithContentDescription("Eliminar").performClick()
        compose.onNodeWithText("¿Eliminar este código?").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()

        assertEquals("https://github.com/KevinAndrey96", shared)
        assertTrue(favoriteToggled)
        assertTrue(deleted)
    }

    @Test
    fun missingAndLoadingStatesRender() {
        var state: ScanDetailUiState by mutableStateOf(ScanDetailUiState.Loading)
        var back = false
        compose.setContent { QrScannerTheme { DetailUnderTest(state, onBack = { back = true }) } }

        state = ScanDetailUiState.NotFound
        compose.onNodeWithText("Este código ya no está en el historial").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()

        assertTrue(back)
    }

    @Test
    fun everyContentKindRendersItsFields() {
        var scan by mutableStateOf(SampleScans.contact)
        compose.setContent { QrScannerTheme { DetailUnderTest(ScanDetailUiState.Loaded(scan)) } }

        compose.onNodeWithText("Guardar contacto").assertIsDisplayed()
        scan = SampleScans.geo
        compose.onNodeWithText("Abrir en mapas").assertIsDisplayed()
        compose.onAllNodesWithText("Plaza de Bolívar").assertCountEquals(2)
        scan = SampleScans.text
        compose.onNodeWithText("Buscar en la web").assertIsDisplayed()
    }

    @Composable
    private fun DetailUnderTest(
        state: ScanDetailUiState,
        onBack: () -> Unit = {},
        onAction: (ContentAction, QrContent) -> Unit = { _, _ -> },
        onCopy: (String) -> Unit = {},
    ) = ScanDetailContent(
        state = state,
        snackbarHostState = SnackbarHostState(),
        onBack = onBack,
        onFavoriteToggled = {},
        onDeleteConfirmed = {},
        onAction = onAction,
        onCopy = onCopy,
        onShare = {},
    )
}

@RunWith(AndroidJUnit4::class)
class GeneratorContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val viewModel = GeneratorViewModel()

    @Test
    fun typingTextRendersTheCodeAndCopiesThePayload() {
        var copied: String? = null
        setContent(onCopy = { copied = it })

        compose.onNodeWithText("Completa los datos para ver tu código QR").assertIsDisplayed()
        compose.onNodeWithTag("generatorText").performTextInput("Hola Kevin")
        compose.onNodeWithContentDescription("Código QR generado").assertIsDisplayed()
        compose.onNodeWithText("Copiar").performScrollTo().performClick()

        assertEquals("Hola Kevin", copied)
    }

    @Test
    fun wifiFormValidatesThePasswordAndSharesTheImage() {
        var shared = false
        setContent(onShare = { shared = true })

        compose.onNodeWithText("Wi-Fi").performClick()
        compose.onNodeWithText("Nombre de la red").performTextInput("Casa")
        compose.onNodeWithText("La contraseña WPA debe tener entre 8 y 63 caracteres").assertIsDisplayed()
        compose.onNodeWithTag("wifiPassword").performTextInput("secreto123")
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Mostrar contraseña").performClick()
        compose.onNodeWithText("Compartir").performScrollTo().performClick()
        compose.onNodeWithText("Abierta").performClick()
        compose.onNodeWithText("Abierta").assertIsSelected()

        assertTrue(shared)
        assertEquals("secreto123", viewModel.state.wifi.password)
        assertNotNull(viewModel.state.matrix)
    }

    @Test
    fun contactFormRequiresAName() {
        setContent()

        compose.onNodeWithText("Contacto").performClick()
        compose.onNodeWithText("Teléfono (opcional)").performTextInput("12")
        compose.onNodeWithText("Escribe el nombre del contacto").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("El teléfono debe tener al menos 3 dígitos").assertIsDisplayed()
        compose.onNodeWithText("Enlace").performClick()
        compose.onNodeWithText("Dirección web").performTextInput("kevin.dev")
        compose.onNodeWithContentDescription("Código QR generado").assertIsDisplayed()
    }

    private fun setContent(
        onShare: () -> Unit = {},
        onCopy: (String) -> Unit = {},
    ) {
        compose.setContent {
            QrScannerTheme {
                GeneratorContent(
                    state = viewModel.state,
                    snackbarHostState = SnackbarHostState(),
                    onTypeSelected = viewModel::onTypeSelected,
                    onTextChanged = viewModel::onTextChanged,
                    onLinkChanged = viewModel::onLinkChanged,
                    onWifiChanged = viewModel::onWifiChanged,
                    onContactChanged = viewModel::onContactChanged,
                    onShareImage = { onShare() },
                    onCopyContent = onCopy,
                )
            }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class ScannerContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun withoutPermissionTheScannerExplainsAndStillAllowsImages() {
        var requested = false
        var settings = false
        var picked = false
        compose.setContent {
            QrScannerTheme {
                ScannerUnderTest(
                    cameraAllowed = false,
                    onRequestPermission = { requested = true },
                    onOpenSettings = { settings = true },
                    onPickImage = { picked = true },
                )
            }
        }

        compose.onNodeWithText("La cámara está desactivada").assertIsDisplayed()
        compose.onNodeWithText("Permitir cámara").performClick()
        compose.onNodeWithText("Abrir ajustes").performClick()
        compose.onNodeWithText("Leer desde una imagen").performClick()
        compose.onNodeWithTag("torch").assertDoesNotExist()

        assertTrue(requested && settings && picked)
    }

    @Test
    fun withPermissionTheCameraAndTorchAreShown() {
        var torch = false
        compose.setContent {
            QrScannerTheme {
                ScannerUnderTest(cameraAllowed = true, torchOn = torch, onTorchToggled = { torch = !torch })
            }
        }

        compose.onNodeWithTag("camera").assertExists()
        compose.onNodeWithText("Apunta la cámara a un código QR").assertIsDisplayed()
        compose.onNodeWithContentDescription("Encender linterna").performClick()

        assertTrue(torch)
    }

    @Composable
    private fun ScannerUnderTest(
        cameraAllowed: Boolean,
        torchOn: Boolean = false,
        onTorchToggled: () -> Unit = {},
        onPickImage: () -> Unit = {},
        onRequestPermission: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) = ScannerContent(
        cameraAllowed = cameraAllowed,
        torchOn = torchOn,
        snackbarHostState = SnackbarHostState(),
        onCodeDetected = {},
        onTorchToggled = onTorchToggled,
        onPickImage = onPickImage,
        onRequestPermission = onRequestPermission,
        onOpenSettings = onOpenSettings,
        camera = { modifier -> Box(modifier.testTag("camera")) },
    )
}
