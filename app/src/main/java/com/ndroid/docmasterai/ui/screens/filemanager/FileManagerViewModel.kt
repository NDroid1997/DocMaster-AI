package com.ndroid.docmasterai.ui.screens.filemanager

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.repository.DocumentRepository
import com.ndroid.docmasterai.data.repository.FileManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FileManagerUiState(
    val allDocuments: List<DocumentEntity> = emptyList(),
    val recentDocuments: List<DocumentEntity> = emptyList(),
    val favoriteDocuments: List<DocumentEntity> = emptyList(),
    val deletedDocuments: List<DocumentEntity> = emptyList(),
    val sortBy: String = "date",
    val isGridView: Boolean = true,
)

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val fileManagerRepository: FileManagerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { docs ->
                _uiState.update { it.copy(allDocuments = docs) }
            }
        }
        viewModelScope.launch {
            documentRepository.getRecentDocuments(20).collect { docs ->
                _uiState.update { it.copy(recentDocuments = docs) }
            }
        }
        viewModelScope.launch {
            documentRepository.getFavoriteDocuments().collect { docs ->
                _uiState.update { it.copy(favoriteDocuments = docs) }
            }
        }
        viewModelScope.launch {
            documentRepository.getDeletedDocuments().collect { docs ->
                _uiState.update { it.copy(deletedDocuments = docs) }
            }
        }
    }

    fun toggleFavorite(doc: DocumentEntity) {
        viewModelScope.launch {
            documentRepository.toggleFavorite(doc.id, !doc.isFavorite)
        }
    }

    fun softDelete(id: String) {
        viewModelScope.launch { documentRepository.softDeleteDocument(id) }
    }

    fun permanentlyDelete(id: String) {
        viewModelScope.launch { documentRepository.permanentlyDeleteDocument(id) }
    }

    fun restoreDocument(id: String) {
        viewModelScope.launch { documentRepository.restoreDocument(id) }
    }

    fun setSortBy(sort: String) {
        _uiState.update { it.copy(sortBy = sort) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch { fileManagerRepository.createFolder(name) }
    }

    /**
     * Loads the page images for [doc] and delivers them (with the document id) so the
     * caller can open them in the DocumentEditor. Falls back to the document's own
     * file if it has no separate page records.
     */
//    fun prepareForEditing(
//        doc: DocumentEntity,
//        onReady: (documentId: String, uris: List<Uri>) -> Unit,
//    ) {
//        viewModelScope.launch {
//            val pages = documentRepository.getPagesByDocumentId(doc.id).first()
//            val uris = if (pages.isNotEmpty()) {
//                pages.sortedBy { it.pageNumber }
//                    .map { Uri.fromFile(File(it.enhancedImagePath ?: it.imagePath)) }
//            } else {
//                listOf(Uri.fromFile(File(doc.filePath)))
//            }
//            onReady(doc.id, uris)
//        }
//    }
}
