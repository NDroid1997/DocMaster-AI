package com.ndroid.docmasterai.ui.screens.pdftools

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class SplitMode(val title: String, val description: String) {
    PAGE_RANGE("Page Range", "Extract a continuous range of pages"),
    SPECIFIC_PAGES("Specific Pages", "Extract selected pages into one PDF"),
    CUSTOM_SELECTION("Custom Selection", "Tap pages to pick exactly what you need"),
    INDIVIDUAL_PAGES("Every Page", "Each page becomes its own PDF")
}

data class PdfPageThumbnail(
    val pageIndex: Int,
    val bitmap: Bitmap? = null
)

data class PdfSplitUiState(
    val sourceUri: Uri? = null,
    val sourceFileName: String = "",
    val totalPages: Int = 0,
    val isLoadingPdf: Boolean = false,
    val thumbnails: List<PdfPageThumbnail> = emptyList(),
    val selectedMode: SplitMode? = null,
    val rangeStart: String = "",
    val rangeEnd: String = "",
    val specificPagesText: String = "",
    val previewSelectedPages: Set<Int> = emptySet(),
    val isProcessing: Boolean = false,
    val resultFiles: List<File> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class PdfPageSelectorViewModel @Inject constructor(
    application: Application,
    private val pdfRepository: PdfRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PdfSplitUiState())
    val uiState: StateFlow<PdfSplitUiState> = _uiState.asStateFlow()

    fun loadPdf(uri: Uri, fileName: String) {
        _uiState.update { it.copy(
            sourceUri = uri,
            sourceFileName = fileName,
            isLoadingPdf = true,
            totalPages = 0,
            thumbnails = emptyList(),
            selectedMode = null,
            previewSelectedPages = emptySet(),
            errorMessage = null
        ) }

        viewModelScope.launch {
            try {
                val path = uri.toString()
                val pageCount = pdfRepository.getPdfPageCount(path)
                if (pageCount == 0) {
                     _uiState.update { it.copy(isLoadingPdf = false, errorMessage = "Could not read PDF or PDF is empty") }
                     return@launch
                }
                
                val thumbnails = (0 until pageCount).map { PdfPageThumbnail(it) }

                _uiState.update { it.copy(
                    totalPages = pageCount,
                    thumbnails = thumbnails,
                    isLoadingPdf = false
                ) }

                // Load thumbnails in background
                loadThumbnails(path, pageCount)

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingPdf = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun loadThumbnails(path: String, count: Int) {
        withContext(Dispatchers.IO) {
            for (i in 0 until count) {
                try {
                    val bitmap = pdfRepository.renderPdfPage(path, i)
                    _uiState.update { state ->
                        val newThumbnails = state.thumbnails.map {
                            if (it.pageIndex == i) it.copy(bitmap = bitmap) else it
                        }
                        state.copy(thumbnails = newThumbnails)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun selectMode(mode: SplitMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        updatePreviewSelection()
    }

    fun updateRange(start: String, end: String) {
        _uiState.update { it.copy(rangeStart = start, rangeEnd = end) }
        updatePreviewSelection()
    }

    fun updateSpecificPages(text: String) {
        _uiState.update { it.copy(specificPagesText = text) }
        updatePreviewSelection()
    }

    fun togglePageInCustomSelection(index: Int) {
        if (_uiState.value.selectedMode != SplitMode.CUSTOM_SELECTION) return
        
        val current = _uiState.value.previewSelectedPages.toMutableSet()
        if (current.contains(index)) {
            current.remove(index)
        } else {
            current.add(index)
        }
        _uiState.update { it.copy(previewSelectedPages = current) }
    }

    private fun updatePreviewSelection() {
        val state = _uiState.value
        val selection = when (state.selectedMode) {
            SplitMode.PAGE_RANGE -> {
                val start = state.rangeStart.toIntOrNull()?.minus(1) ?: return
                val end = state.rangeEnd.toIntOrNull()?.minus(1) ?: return
                if (start in 0 until state.totalPages && end in 0 until state.totalPages && start <= end) {
                    (start..end).toSet()
                } else emptySet()
            }
            SplitMode.SPECIFIC_PAGES -> {
                state.specificPagesText.split(",")
                    .mapNotNull { it.trim().toIntOrNull()?.minus(1) }
                    .filter { it in 0 until state.totalPages }
                    .toSet()
            }
            SplitMode.CUSTOM_SELECTION -> state.previewSelectedPages
            SplitMode.INDIVIDUAL_PAGES -> (0 until state.totalPages).toSet()
            null -> emptySet()
        }
        _uiState.update { it.copy(previewSelectedPages = selection) }
    }

    fun performOperation() {
        val state = _uiState.value
        val uri = state.sourceUri ?: return
        
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val path = uri.toString()
                val results = when (state.selectedMode) {
                    SplitMode.INDIVIDUAL_PAGES -> {
                        val ranges = (0 until state.totalPages).map { it..it }
                        pdfRepository.splitPdf(path, ranges)
                    }
                    else -> {
                        if (state.previewSelectedPages.isEmpty()) {
                            _uiState.update { it.copy(isProcessing = false, errorMessage = "No pages selected") }
                            return@launch
                        }
                        val sortedPages = state.previewSelectedPages.toList().sorted()
                        val resultPath = pdfRepository.extractPages(path, sortedPages, "extracted_${state.sourceFileName.substringBeforeLast(".")}")
                        listOf(resultPath)
                    }
                }

                val files = results.map { File(it) }
                _uiState.update { it.copy(isProcessing = false, resultFiles = files) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = e.message) }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultFiles = emptyList()) }
    }
}
