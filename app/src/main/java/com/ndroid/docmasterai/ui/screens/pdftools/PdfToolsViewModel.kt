package com.ndroid.docmasterai.ui.screens.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.model.PdfOperationType
import com.ndroid.docmasterai.data.repository.PdfRepository
import com.ndroid.docmasterai.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PdfToolsUiState(
    val selectedTool: PdfOperationType? = null,
    val sourcePdfUri: Uri? = null,
    val sourcePdfUris: List<Uri> = emptyList(),
    val isProcessing: Boolean = false,
    val resultPath: String? = null,
    val error: String? = null,
    val sourceFileName: String? = null,
    val watermarkText: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val navigateToPasswordScreen: Boolean = false,
)

@HiltViewModel
class PdfToolsViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
    val premiumManager: PremiumManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToolsUiState())
    val uiState: StateFlow<PdfToolsUiState> = _uiState.asStateFlow()

    fun selectTool(type: PdfOperationType) {
        _uiState.update { it.copy(selectedTool = type, resultPath = null, error = null, password = "", confirmPassword = "", navigateToPasswordScreen = false) }
    }

    fun resetNavigation() {
        _uiState.update { it.copy(navigateToPasswordScreen = false) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun setConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password) }
    }

    fun setSourcePdf(uri: Uri, fileName: String? = null) {
        _uiState.update { it.copy(sourcePdfUri = uri, sourceFileName = fileName, error = null) }
        val tool = _uiState.value.selectedTool
        
        if (tool == PdfOperationType.REMOVE_PASSWORD) {
            viewModelScope.launch {
                _uiState.update { it.copy(isProcessing = true) }
                val isEncrypted = pdfRepository.isPdfEncrypted(uri.toString())
                _uiState.update { it.copy(isProcessing = false) }
                if (!isEncrypted) {
                    _uiState.update { it.copy(error = "This file is not password protected") }
                } else {
                    _uiState.update { it.copy(navigateToPasswordScreen = true) }
                }
            }
        } else if (tool == PdfOperationType.PASSWORD_PROTECT) {
            _uiState.update { it.copy(navigateToPasswordScreen = true) }
        } else if (tool != null) {
            processOperation()
        }
    }

    fun setSourcePdfs(uris: List<Uri>) {
        _uiState.update { it.copy(sourcePdfUris = uris) }
        processOperation()
    }

    fun processOperation() {
        val currentState = _uiState.value
        if (currentState.isProcessing) return

        val tool = currentState.selectedTool ?: return
        val sourceUri = currentState.sourcePdfUri
        val sourceUris = currentState.sourcePdfUris

        // Basic validation
        if (tool == PdfOperationType.MERGE && sourceUris.size < 2) return
        if (tool != PdfOperationType.MERGE && sourceUri == null) return

        if (tool == PdfOperationType.PASSWORD_PROTECT) {
            if (currentState.password.isEmpty()) {
                _uiState.update { it.copy(error = "Password cannot be empty") }
                return
            }
            if (currentState.password != currentState.confirmPassword) {
                _uiState.update { it.copy(error = "Passwords do not match") }
                return
            }
        }

        if (tool == PdfOperationType.REMOVE_PASSWORD && currentState.password.isEmpty()) {
            _uiState.update { it.copy(error = "Password is required to unlock") }
            return
        }

        _uiState.update { it.copy(isProcessing = true, resultPath = null, error = null) }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    when (tool) {
                        PdfOperationType.MERGE -> {
                            val paths = sourceUris.map { it.toString() }
                            pdfRepository.mergePdfs(paths, "merged")
                        }
                        PdfOperationType.COMPRESS -> {
                            sourceUri?.toString()?.let { pdfRepository.compressPdf(it) } ?: ""
                        }
                        PdfOperationType.WATERMARK -> {
                            sourceUri?.toString()?.let {
                                pdfRepository.addWatermark(it, currentState.watermarkText.ifEmpty { "CONFIDENTIAL" })
                            } ?: ""
                        }
                        PdfOperationType.ROTATE -> {
                            sourceUri?.toString()?.let {
                                pdfRepository.rotatePdfPages(it, mapOf(0 to 90))
                            } ?: ""
                        }
                        PdfOperationType.DELETE_PAGES -> {
                            sourceUri?.toString()?.let {
                                pdfRepository.deletePages(it, setOf(0))
                            } ?: ""
                        }
                        PdfOperationType.SPLIT -> {
                            sourceUri?.toString()?.let {
                                val count = pdfRepository.getPdfPageCount(it)
                                val mid = count / 2
                                val results = pdfRepository
                                    .splitPdf(it, listOf(0 until mid, mid until count))
                                results.firstOrNull() ?: ""
                            } ?: ""
                        }
                        PdfOperationType.PASSWORD_PROTECT -> {
                            sourceUri?.toString()?.let {
                                pdfRepository.protectPdf(
                                    it,
                                    currentState.password,
                                    currentState.password,
                                    currentState.sourceFileName ?: "protected"
                                )
                            } ?: ""
                        }
                        PdfOperationType.REMOVE_PASSWORD -> {
                            sourceUri?.toString()?.let {
                                pdfRepository.removePassword(
                                    it,
                                    currentState.password,
                                    currentState.sourceFileName ?: "unlocked"
                                )
                            } ?: ""
                        }
                        else -> ""
                    }
                }
                _uiState.update { it.copy(isProcessing = false, resultPath = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }
}
