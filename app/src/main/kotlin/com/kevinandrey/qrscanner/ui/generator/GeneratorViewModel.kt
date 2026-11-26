package com.kevinandrey.qrscanner.ui.generator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kevinandrey.qrscanner.core.DraftProblem
import com.kevinandrey.qrscanner.core.QrDraft
import com.kevinandrey.qrscanner.core.QrEncoder
import com.kevinandrey.qrscanner.core.QrMatrix

enum class GeneratorType {
    TEXT,
    LINK,
    WIFI,
    CONTACT,
}

data class GeneratorUiState(
    val type: GeneratorType = GeneratorType.TEXT,
    val text: QrDraft.Text = QrDraft.Text(),
    val link: QrDraft.Link = QrDraft.Link(),
    val wifi: QrDraft.Wifi = QrDraft.Wifi(),
    val contact: QrDraft.Contact = QrDraft.Contact(),
    val touched: Set<GeneratorType> = emptySet(),
    val matrix: QrMatrix? = null,
) {
    val draft: QrDraft
        get() =
            when (type) {
                GeneratorType.TEXT -> text
                GeneratorType.LINK -> link
                GeneratorType.WIFI -> wifi
                GeneratorType.CONTACT -> contact
            }

    /** Problems are hidden until the user edits the current form, so a fresh form is not shown as wrong. */
    val problems: List<DraftProblem>
        get() = if (type in touched) draft.problems() else emptyList()
}

class GeneratorViewModel : ViewModel() {
    /** Compose state so text fields stay in sync with the keyboard. */
    var state by mutableStateOf(GeneratorUiState())
        private set

    fun onTypeSelected(type: GeneratorType) = update { it.copy(type = type) }

    fun onTextChanged(text: String) = edit { it.copy(text = QrDraft.Text(text)) }

    fun onLinkChanged(url: String) = edit { it.copy(link = QrDraft.Link(url)) }

    fun onWifiChanged(wifi: QrDraft.Wifi) = edit { it.copy(wifi = wifi) }

    fun onContactChanged(contact: QrDraft.Contact) = edit { it.copy(contact = contact) }

    private fun edit(transform: (GeneratorUiState) -> GeneratorUiState) =
        update {
            transform(it).let { next ->
                next.copy(
                    touched =
                        next.touched + next.type,
                )
            }
        }

    private fun update(transform: (GeneratorUiState) -> GeneratorUiState) {
        val next = transform(state)
        state = next.copy(matrix = next.draft.takeIf(QrDraft::isValid)?.let { QrEncoder.encode(it.payload()) })
    }
}
