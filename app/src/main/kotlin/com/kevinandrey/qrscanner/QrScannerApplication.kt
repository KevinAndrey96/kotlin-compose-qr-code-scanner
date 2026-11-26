package com.kevinandrey.qrscanner

import android.app.Application
import android.content.Context
import com.kevinandrey.qrscanner.data.RoomScanRepository
import com.kevinandrey.qrscanner.data.ScanDatabase
import com.kevinandrey.qrscanner.data.ScanRepository

class QrScannerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Manual dependency graph shared by every screen. */
class AppContainer(
    context: Context,
) {
    private val database by lazy { ScanDatabase.create(context) }

    val scanRepository: ScanRepository by lazy { RoomScanRepository(database.scanDao()) }
}
