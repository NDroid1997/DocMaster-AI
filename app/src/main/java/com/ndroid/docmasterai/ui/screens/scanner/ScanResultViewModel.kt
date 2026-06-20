package com.ndroid.docmasterai.ui.screens.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.local.entity.PageEntity
import com.ndroid.docmasterai.data.model.ImageFilter
import com.ndroid.docmasterai.data.repository.DocumentRepository
import com.ndroid.docmasterai.data.repository.ScannerRepository
import com.ndroid.docmasterai.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanResultUiState(
    val document: DocumentEntity? = null,
    val pages: List<PageEntity> = emptyList(),
    val currentPageIndex: Int = 0,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ScanResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository,
    private val scannerRepository: ScannerRepository
) : ViewModel() {

    private val documentId: String = savedStateHandle["documentId"] ?: ""

    private val _uiState = MutableStateFlow(ScanResultUiState())
    val uiState: StateFlow<ScanResultUiState> = _uiState.asStateFlow()

    init {
        if (documentId.isNotEmpty()) {
            loadDocument()
        }
    }

    private fun loadDocument() {
        viewModelScope.launch {
            val doc = documentRepository.getDocumentById(documentId)
            documentRepository.getPagesByDocumentId(documentId).collect { pages ->
                _uiState.update { 
                    it.copy(
                        document = doc, 
                        pages = pages.sortedBy { p -> p.pageNumber }
                    ) 
                }
            }
        }
    }

    fun applyFilter(filter: ImageFilter) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.pages.isEmpty()) return@launch
            val currentPage = state.pages[state.currentPageIndex]
            
            _uiState.update { it.copy(isSaving = true) }
            try {
                val enhancedPath = scannerRepository.applyFilter(currentPage.imagePath, filter)
                val updatedPage = currentPage.copy(enhancedImagePath = enhancedPath, filterApplied = filter.name)
                documentRepository.updatePage(updatedPage)
                
                // If it's the first page, update document thumbnail
                if (state.currentPageIndex == 0) {
                    state.document?.copy(thumbnailPath = enhancedPath)?.let {
                        documentRepository.updateDocument(it)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun setCurrentPage(index: Int) {
        _uiState.update { it.copy(currentPageIndex = index) }
    }
}
