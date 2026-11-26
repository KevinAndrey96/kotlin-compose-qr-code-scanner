package com.kevinandrey.qrscanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevinandrey.qrscanner.data.ScanRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val torchOn: Boolean = false,
    val message: ScannerMessage? = null,
)

enum class ScannerMessage {
    NO_CODE_IN_IMAGE,
    IMAGE_UNREADABLE,
}

sealed interface ScannerEvent {
    data class OpenScan(
        val id: Long,
    ) : ScannerEvent
}

class ScannerViewModel(
    private val repository: ScanRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private val _events = Channel<ScannerEvent>(Channel.BUFFERED)
    val events: Flow<ScannerEvent> = _events.receiveAsFlow()

    /** The analyzer reports the same code many times per second; only the first one is handled. */
    private var handlingCode = false

    fun onCodeDetected(raw: String) {
        if (handlingCode || raw.isBlank()) return
        handlingCode = true
        viewModelScope.launch {
            val id = repository.record(raw)
            _events.send(ScannerEvent.OpenScan(id))
        }
    }

    /** Called every time the scanner becomes visible again, for example after closing a result. */
    fun onScannerVisible() {
        handlingCode = false
    }

    fun onImageRead(result: ImageReadResult) {
        when (result) {
            is ImageReadResult.Found -> onCodeDetected(result.raw)
            ImageReadResult.NotFound -> _state.update { it.copy(message = ScannerMessage.NO_CODE_IN_IMAGE) }
            ImageReadResult.Unreadable -> _state.update { it.copy(message = ScannerMessage.IMAGE_UNREADABLE) }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    fun onTorchToggled() {
        _state.update { it.copy(torchOn = !it.torchOn) }
    }
}
