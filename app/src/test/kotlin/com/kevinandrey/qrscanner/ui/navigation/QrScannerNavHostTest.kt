package com.kevinandrey.qrscanner.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kevinandrey.qrscanner.testing.FakeScanRepository
import com.kevinandrey.qrscanner.testing.SampleScans
import com.kevinandrey.qrscanner.ui.theme.QrScannerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrScannerNavHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bottomNavigationReachesEveryScreenAndHistoryOpensTheDetail() {
        compose.setContent { QrScannerTheme { QrScannerNavHost(repository = FakeScanRepository(SampleScans.all)) } }

        compose.onNodeWithText("La cámara está desactivada").assertIsDisplayed()

        compose.onNodeWithText("Crear").performClick()
        compose.onNodeWithText("Crear código QR").assertIsDisplayed()

        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Valentina Ríos").performClick()
        compose.onNodeWithText("Guardar contacto").assertIsDisplayed()
        compose.onNodeWithText("Escanear").assertDoesNotExist()

        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Valentina Ríos").assertIsDisplayed()

        compose.onNodeWithText("Escanear").performClick()
        compose.onNodeWithText("Permitir cámara").assertIsDisplayed()
    }

    @Test
    fun deletingFromTheDetailReturnsToTheHistory() {
        val repository = FakeScanRepository(SampleScans.all)
        compose.setContent { QrScannerTheme { QrScannerNavHost(repository = repository) } }

        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Pedido 2045 listo para recoger").performClick()
        compose.onNodeWithContentDescription("Eliminar").performClick()
        compose.onNodeWithText("Eliminar").performClick()

        compose.onNodeWithText("Pedido 2045 listo para recoger").assertDoesNotExist()
        compose.onNodeWithText("Oficina Bogotá").assertIsDisplayed()
    }
}
