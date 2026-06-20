package com.ndroid.docmasterai.ui.screens.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.model.PdfOperationType
import com.ndroid.docmasterai.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PdfToolsUiState(
    val selectedTool: PdfOperationType? = null,
    val sourcePdfUri: Uri? = null,
    val sourcePdfUris: List<Uri> = emptyList(),
    val isProcessing: Boolean = false,
    val resultPath: String? = null,
    val error: String? = null,
    val watermarkText: String = "",
    val password: String = "",
)

@HiltViewModel
class PdfToolsViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToolsUiState())
    val uiState: StateFlow<PdfToolsUiState> = _uiState.asStateFlow()

    fun selectTool(type: PdfOperationType) {
        _uiState.update { it.copy(selectedTool = type, resultPath = null, error = null) }
    }

    fun setSourcePdf(uri: Uri) {
        _uiState.update { it.copy(sourcePdfUri = uri) }
        processOperation()
    }

    fun setSourcePdfs(uris: List<Uri>) {
        _uiState.update { it.copy(sourcePdfUris = uris) }
        processOperation()
    }

    private fun processOperation() {
        val state = _uiState.value
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            try {
                val result = when (state.selectedTool) {
                    PdfOperationType.MERGE -> {
                        val paths = state.sourcePdfUris.mapNotNull { it.path }
                        if (paths.size >= 2) pdfRepository.mergePdfs(paths, "merged") else ""
                    }
                    PdfOperationType.COMPRESS -> {
                        state.sourcePdfUri?.path?.let { pdfRepository.compressPdf(it) } ?: ""
                    }
                    PdfOperationType.WATERMARK -> {
                        state.sourcePdfUri?.path?.let {
                            pdfRepository.addWatermark(it, state.watermarkText.ifEmpty { "CONFIDENTIAL" })
                        } ?: ""
                    }
                    PdfOperationType.ROTATE -> {
                        state.sourcePdfUri?.path?.let {
                            pdfRepository.rotatePdfPages(it, mapOf(0 to 90))
                        } ?: ""
                    }
                    PdfOperationType.DELETE_PAGES -> {
                        state.sourcePdfUri?.path?.let {
                            pdfRepository.deletePages(it, setOf(0))
                        } ?: ""
                    }
                    PdfOperationType.SPLIT -> {
                        state.sourcePdfUri?.path?.let {
                            val count = pdfRepository.getPdfPageCount(it)
                            val mid = count / 2
                            val results = pdfRepository.splitPdf(it, listOf(0 until mid, mid until count))
                            results.firstOrNull() ?: ""
                        } ?: ""
                    }
                    else -> ""
                }
                _uiState.update { it.copy(isProcessing = false, resultPath = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }
}
