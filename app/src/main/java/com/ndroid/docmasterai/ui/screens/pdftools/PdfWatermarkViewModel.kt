package com.ndroid.docmasterai.ui.screens.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PdfWatermarkUiState(
    val sourceUri: Uri? = null,
    val sourceFileName: String = "",
    val watermarkText: String = "",
    val isProcessing: Boolean = false,
    val resultPath: String? = null,
    val error: String? = null
)

@HiltViewModel
class PdfWatermarkViewModel @Inject constructor(
    private val pdfRepository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfWatermarkUiState())
    val uiState: StateFlow<PdfWatermarkUiState> = _uiState.asStateFlow()

    fun initialize(uri: Uri, fileName: String) {
        _uiState.update { it.copy(sourceUri = uri, sourceFileName = fileName) }
    }

    fun updateWatermarkText(text: String) {
        _uiState.update { it.copy(watermarkText = text) }
    }

    fun applyWatermark() {
        val state = _uiState.value
        val uri = state.sourceUri ?: return
        if (state.watermarkText.isBlank()) {
            _uiState.update { it.copy(error = "Please enter watermark text") }
            return
        }

        _uiState.update { it.copy(isProcessing = true, error = null, resultPath = null) }

        viewModelScope.launch {
            try {
                val result = pdfRepository.addWatermark(uri.toString(), state.watermarkText)
                if (result.isNotEmpty()) {
                    _uiState.update { it.copy(isProcessing = false, resultPath = result) }
                } else {
                    _uiState.update { it.copy(isProcessing = false, error = "Failed to add watermark") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message ?: "An error occurred") }
            }
        }
    }
}
