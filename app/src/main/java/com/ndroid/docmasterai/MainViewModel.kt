package com.ndroid.docmasterai

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.docedit.sdk.api.DocumentEditor
import com.ndroid.docmasterai.data.local.db.DocMasterDatabase
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.local.entity.PageEntity
import com.ndroid.docmasterai.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: DocumentRepository,
    private val database: DocMasterDatabase,
) : ViewModel() {
    private val messagesChannel = Channel<String>(Channel.BUFFERED)
    val messages = messagesChannel.receiveAsFlow()

    fun saveEditedDocument(documentId: String, result: DocumentEditor.Result) = persist(documentId, result)

    fun createNewDocument(result: DocumentEditor.Result) = persist(null, result)

    private fun persist(documentId: String?, result: DocumentEditor.Result) {
        if (result.wasCancelled) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { saveResult(documentId, result) }
                messagesChannel.send("Document saved")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                messagesChannel.send("Could not save document: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private suspend fun saveResult(documentId: String?, result: DocumentEditor.Result) {
        val pdf = requireNotNull(result.pdf) { "The editor returned no PDF." }
        require(result.pageCount > 0) { "The document has no pages." }
        require(result.images.isEmpty() || result.images.size == result.pageCount) { "Incomplete editor pages." }
        val id = documentId ?: UUID.randomUUID().toString()
        // A fresh directory ensures failed saves never overwrite the previous revision.
        val directory = File(context.filesDir, "documents/$id/${UUID.randomUUID()}")
        check(directory.mkdirs()) { "Cannot create document directory." }
        var committed = false
        try {
            val pdfFile = copyTo(pdf, File(directory, "document.pdf"))
            val pages = result.images.mapIndexed { index, uri ->
                PageEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = id,
                    pageNumber = index,
                    imagePath = copyTo(uri, File(directory, "page_${index + 1}.png")).absolutePath,
                )
            }
            val thumbnail = result.thumbnail?.let { copyTo(it, File(directory, "thumbnail.png")).absolutePath }
                ?: pages.firstOrNull()?.imagePath

            // Commit and its cleanup marker must complete together, even if the host closes.
            withContext(NonCancellable) {
                database.withTransaction {
                    val existing = if (documentId != null) {
                        requireNotNull(repository.getDocumentById(documentId)) { "The document no longer exists." }
                    } else null
                    val document = existing?.copy(
                        filePath = pdfFile.absolutePath,
                        thumbnailPath = thumbnail,
                        fileType = "pdf",
                        fileSize = pdfFile.length(),
                        pageCount = result.pageCount,
                        updatedAt = System.currentTimeMillis(),
                    ) ?: DocumentEntity(
                        id = id,
                        title = "Document_${System.currentTimeMillis()}",
                        filePath = pdfFile.absolutePath,
                        thumbnailPath = thumbnail,
                        fileType = "pdf",
                        fileSize = pdfFile.length(),
                        pageCount = result.pageCount,
                        sourceType = "import",
                    )
                    if (existing == null) {
                        repository.saveDocument(document)
                    } else {
                        repository.updateDocument(document)
                        repository.addRecentAccess(id, "edit")
                    }
                    repository.replacePages(id, pages)
                }
                committed = true
            }
        } finally {
            if (!committed) directory.deleteRecursively()
        }
    }

    private fun copyTo(uri: Uri, destination: File): File {
        requireNotNull(context.contentResolver.openInputStream(uri)) { "Cannot read editor output." }.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        check(destination.length() > 0L) { "The editor output is empty." }
        return destination
    }
}

