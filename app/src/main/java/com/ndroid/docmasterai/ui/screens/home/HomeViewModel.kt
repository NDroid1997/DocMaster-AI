package com.ndroid.docmasterai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.local.preferences.AppPreferences
import com.ndroid.docmasterai.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

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
}
