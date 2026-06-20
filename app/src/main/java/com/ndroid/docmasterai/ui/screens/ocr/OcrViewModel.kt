package com.ndroid.docmasterai.ui.screens.ocr

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrUiState(
    val extractedText: String = "",
    val selectedLanguage: String = "latin",
    val isProcessing: Boolean = false,
    val processingTimeMs: Long = 0,
    val error: String? = null,
    val selectedImageUri: Uri? = null,
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    fun processImage(uri: Uri) {
        _uiState.update { it.copy(isProcessing = true, error = null, selectedImageUri = uri) }
        viewModelScope.launch {
            try {
                val result = ocrRepository.extractTextFromUri(uri)
                _uiState.update {
                    it.copy(
                        extractedText = result.text,
                        processingTimeMs = result.processingTimeMs,
                        isProcessing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, error = "Failed to extract text: ${e.message}")
                }
            }
        }
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(extractedText = text) }
    }

    fun shareText(context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, _uiState.value.extractedText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Text"))
    }

    fun exportAsTxt() {
        viewModelScope.launch {
            try {
                val path = "${System.getProperty("java.io.tmpdir")}/ocr_result.txt"
                ocrRepository.exportToTxt(_uiState.value.extractedText, path)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    fun exportAsPdf() {
        viewModelScope.launch {
            try {
                val path = "${System.getProperty("java.io.tmpdir")}/ocr_result.pdf"
                ocrRepository.exportToPdf(_uiState.value.extractedText, path)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }
}
