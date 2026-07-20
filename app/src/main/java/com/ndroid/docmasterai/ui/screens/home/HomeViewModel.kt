package com.ndroid.docmasterai.ui.screens.home

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.local.entity.PageEntity
import com.ndroid.docmasterai.data.local.preferences.AppPreferences
import com.ndroid.docmasterai.data.repository.DocumentRepository
import com.ndroid.docmasterai.data.repository.ScannerRepository
import com.ndroid.docmasterai.premium.PremiumManager
import com.ndroid.docmasterai.scanner.ScannerManager
import com.ndroid.docmasterai.ui.screens.scanner.ScannerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val preferences: AppPreferences,
    private val scannerRepository: ScannerRepository,
    private val scannerManager: ScannerManager,
    val premiumManager: PremiumManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    init {
        viewModelScope.launch {
            scannerManager.scanResult.collect { result ->
                handleScanResult(result)
            }
        }
    }

    val recentDocuments: StateFlow<List<DocumentEntity>> =
        documentRepository.getRecentDocuments(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDocuments: StateFlow<List<DocumentEntity>> =
        documentRepository.getFavoriteDocuments()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentCount: StateFlow<Int> =
        documentRepository.getDocumentCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStorage: StateFlow<Long> =
        documentRepository.getTotalStorageUsed()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val isPremium: StateFlow<Boolean> =
        preferences.isPremium
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleFavorite(document: DocumentEntity) {
        viewModelScope.launch {
            documentRepository.toggleFavorite(document.id, !document.isFavorite)
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            documentRepository.softDeleteDocument(id)
        }
    }

    private fun handleScanResult(result: GmsDocumentScanningResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pages = result.pages ?: return@launch
                if (pages.isEmpty()) return@launch

                val timestamp = System.currentTimeMillis()
                val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
                val title = "Scan $dateStr"
                val docId = UUID.randomUUID().toString()

                val savedPagesData = pages.mapIndexed { index, page ->
                    val fileName = "scan_${docId}_${index + 1}.jpg"
                    val destFile = File(context.filesDir, "documents/$fileName").apply {
                        parentFile?.mkdirs()
                    }

                    // Optimized: Use AssetFileDescriptor to get file size directly
                    val size = context.contentResolver.openAssetFileDescriptor(page.imageUri, "r")?.use { 
                        it.length 
                    } ?: 0L

                    // Optimized: Copy directly from URI stream to internal storage
                    context.contentResolver.openInputStream(page.imageUri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    Triple(destFile.absolutePath, index + 1, size)
                }

                val totalSize = savedPagesData.sumOf { it.third }

                val doc = DocumentEntity(
                    id = docId,
                    title = title,
                    filePath = savedPagesData.first().first,
                    thumbnailPath = savedPagesData.first().first,
                    fileType = "jpg",
                    fileSize = totalSize,
                    pageCount = savedPagesData.size,
                    sourceType = "scan",
                    createdAt = timestamp,
                    updatedAt = timestamp,
                )
                
                val pageEntities = savedPagesData.map { (path, num, _) ->
                    PageEntity(
                        id = UUID.randomUUID().toString(),
                        documentId = docId,
                        pageNumber = num,
                        imagePath = path,
                        createdAt = timestamp,
                    )
                }

                documentRepository.saveDocument(doc)
                documentRepository.savePages(pageEntities)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var imageCapture: ImageCapture? = null

    fun setImageCapture(capture: ImageCapture) {
        imageCapture = capture
        applyFlashMode()
    }

    fun onDocumentDetected(rect: Rect?) {
        _uiState.update { it.copy(detectedRect = rect) }
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
        applyFlashMode()
    }

    private fun applyFlashMode() {
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
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state.scannedPages.isNotEmpty()) {
                    val title = "Scan ${
                        SimpleDateFormat(
                            "MMM dd, HH:mm",
                            Locale.getDefault()
                        ).format(Date())
                    }"

                    // Move images from temp cache to permanent storage
                    val permanentPaths =
                        scannerRepository.saveScanToStorage(state.scannedPages, title)

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

                    // Create and save PageEntities
                    val pageEntities = state.scannedPages.mapIndexed { index, scannedPage ->
                        PageEntity(
                            id = scannedPage.id,
                            documentId = state.documentId,
                            pageNumber = index + 1,
                            imagePath = permanentPaths[index],
                            filterApplied = "original",
                        )
                    }
                    documentRepository.savePages(pageEntities)

                    // Clean up temp files
                    scannerRepository.cleanTempFiles()

                    _uiState.update { it.copy(isSaving = false, scanCompleted = true) }
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
