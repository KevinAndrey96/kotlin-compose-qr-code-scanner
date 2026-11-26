package com.kevinandrey.qrscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kevinandrey.qrscanner.ui.navigation.QrScannerNavHost
import com.kevinandrey.qrscanner.ui.theme.QrScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as QrScannerApplication).container
        setContent {
            QrScannerTheme {
                QrScannerNavHost(repository = container.scanRepository)
            }
        }
    }
}
