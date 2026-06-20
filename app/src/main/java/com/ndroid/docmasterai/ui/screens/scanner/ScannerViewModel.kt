package com.ndroid.docmasterai.ui.screens.scanner

import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.local.entity.PageEntity
import com.ndroid.docmasterai.data.model.ScannedPage
import com.ndroid.docmasterai.data.repository.DocumentRepository
import com.ndroid.docmasterai.data.repository.ScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ScannerUiState(
    val scannedPages: List<ScannedPage> = emptyList(),
    val isCapturing: Boolean = false,
    val isFlashOn: Boolean = false,
    val isBatchMode: Boolean = false,
    val documentId: String = UUID.randomUUID().toString(),
    val error: String? = null,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scannerRepository: ScannerRepository,
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var imageCapture: ImageCapture? = null

    fun setImageCapture(capture: ImageCapture) {
        imageCapture = capture
    }

    fun captureImage() {
        val capture = imageCapture ?: return
        _uiState.update { it.copy(isCapturing = true, error = null) }

        viewModelScope.launch {
            try {
                val page = scannerRepository.captureImage(capture)
                val updatedPage = page.copy(pageNumber = _uiState.value.scannedPages.size + 1)
                _uiState.update { state ->
                    state.copy(
                        scannedPages = state.scannedPages + updatedPage,
                        isCapturing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCapturing = false, error = e.message) }
            }
        }
    }

    fun toggleFlash() {
        _uiState.update { it.copy(isFlashOn = !it.isFlashOn) }
        imageCapture?.flashMode = if (_uiState.value.isFlashOn) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    fun toggleBatchMode() {
        _uiState.update { it.copy(isBatchMode = !it.isBatchMode) }
    }

    fun removePage(index: Int) {
        _uiState.update { state ->
            state.copy(scannedPages = state.scannedPages.toMutableList().apply { removeAt(index) })
        }
    }

    fun finishScanning() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.scannedPages.isNotEmpty()) {
                val title = "Scan ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
                
                // Move images from temp cache to permanent storage
                val permanentPaths = scannerRepository.saveScanToStorage(state.scannedPages, title)
                
                // Create and save PageEntities
                val pageEntities = state.scannedPages.mapIndexed { index, scannedPage ->
                    PageEntity(
                        id = scannedPage.id,
                        documentId = state.documentId,
                        pageNumber = index + 1,
                        imagePath = permanentPaths[index],
                        filterApplied = "original"
                    )
                }
                documentRepository.savePages(pageEntities)

                // Save DocumentEntity
                val doc = DocumentEntity(
                    id = state.documentId,
                    title = title,
                    filePath = permanentPaths.first(),
                    thumbnailPath = permanentPaths.first(),
                    fileType = "jpg",
                    pageCount = state.scannedPages.size,
                    sourceType = "scan",
                )
                documentRepository.saveDocument(doc)
                
                // Clean up temp files
                scannerRepository.cleanTempFiles()
            }
        }
    }
}
