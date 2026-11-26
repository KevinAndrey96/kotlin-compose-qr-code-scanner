package com.kevinandrey.qrscanner.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevinandrey.qrscanner.core.Scan
import com.kevinandrey.qrscanner.data.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanDetailUiState {
    data object Loading : ScanDetailUiState

    data object NotFound : ScanDetailUiState

    data class Loaded(
        val scan: Scan,
    ) : ScanDetailUiState
}

class ScanDetailViewModel(
    private val id: Long,
    private val repository: ScanRepository,
) : ViewModel() {
    val state: StateFlow<ScanDetailUiState> =
        repository
            .observeScan(id)
            .map { scan -> scan?.let(ScanDetailUiState::Loaded) ?: ScanDetailUiState.NotFound }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScanDetailUiState.Loading)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun onFavoriteToggled() {
        val scan = (state.value as? ScanDetailUiState.Loaded)?.scan ?: return
        viewModelScope.launch { repository.setFavorite(id, !scan.favorite) }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            repository.delete(id)
            _deleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
