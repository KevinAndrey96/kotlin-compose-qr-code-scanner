package com.kevinandrey.qrscanner.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevinandrey.qrscanner.core.Scan
import com.kevinandrey.qrscanner.core.ScanFilter
import com.kevinandrey.qrscanner.core.ScanSearch
import com.kevinandrey.qrscanner.data.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    val filter: ScanFilter = ScanFilter.ALL,
    val scans: List<Scan> = emptyList(),
    val hasHistory: Boolean = false,
)

class HistoryViewModel(
    private val repository: ScanRepository,
) : ViewModel() {
    /** Compose state so the search field never lags behind the keyboard. */
    var query by mutableStateOf("")
        private set

    private var filter by mutableStateOf(ScanFilter.ALL)

    val state: StateFlow<HistoryUiState> =
        combine(repository.observeScans(), snapshotFlow { query to filter }) { scans, (query, filter) ->
            HistoryUiState(
                loading = false,
                filter = filter,
                scans = ScanSearch.apply(scans, query, filter),
                hasHistory = scans.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    fun onQueryChanged(value: String) {
        query = value
    }

    fun onFilterSelected(value: ScanFilter) {
        filter = value
    }

    fun onFavoriteToggled(scan: Scan) {
        viewModelScope.launch { repository.setFavorite(scan.id, !scan.favorite) }
    }

    fun onClearConfirmed() {
        viewModelScope.launch { repository.clearHistory() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
