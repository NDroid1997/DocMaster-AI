package com.ndroid.docmasterai.utils

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GlobalViewModel @Inject constructor(val repository: DocumentRepository): ViewModel() {
    fun prepareForEditing(
        doc: DocumentEntity,
        onReady: (documentId: String, images: List<Uri>, pdf: Uri?) -> Unit,
    ) {
        viewModelScope.launch {
            val pages = repository.getPagesByDocumentId(doc.id).first()
            if (pages.isNotEmpty()) {
                val uris = pages.sortedBy { it.pageNumber }
                    .map { Uri.fromFile(File(it.enhancedImagePath ?: it.imagePath)) }
                onReady(doc.id, uris, null)
            } else if (doc.fileType == "pdf") {
                onReady(doc.id, emptyList(), Uri.fromFile(File(doc.filePath)))
            } else {
                onReady(doc.id, listOf(Uri.fromFile(File(doc.filePath))), null)
            }
        }
    }
}
