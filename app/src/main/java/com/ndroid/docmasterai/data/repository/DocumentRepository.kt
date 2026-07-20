package com.ndroid.docmasterai.data.repository

import com.ndroid.docmasterai.data.local.dao.DocumentDao
import com.ndroid.docmasterai.data.local.dao.PageDao
import com.ndroid.docmasterai.data.local.dao.RecentFileDao
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.local.entity.PageEntity
import com.ndroid.docmasterai.data.local.entity.RecentFileEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    private val recentFileDao: RecentFileDao,
) {
    fun getAllDocuments(): Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    fun getRecentDocuments(limit: Int = 10): Flow<List<DocumentEntity>> =
        documentDao.getRecentDocuments(limit)

    fun getFavoriteDocuments(): Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()

    fun getDeletedDocuments(): Flow<List<DocumentEntity>> = documentDao.getDeletedDocuments()

    suspend fun getDocumentById(id: String): DocumentEntity? = documentDao.getDocumentById(id)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
        documentDao.searchDocuments(query)

    fun getDocumentsByFolder(folderPath: String): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByFolder(folderPath)

    suspend fun saveDocument(document: DocumentEntity) {
        documentDao.insertDocument(document)
        // Add to recent
        recentFileDao.insertRecentFile(
            RecentFileEntity(
                id = UUID.randomUUID().toString(),
                documentId = document.id,
                action = "create"
            )
        )
    }

    suspend fun updateDocument(document: DocumentEntity) {
        documentDao.updateDocument(document)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        documentDao.toggleFavorite(id, isFavorite)
    }

    suspend fun softDeleteDocument(id: String) {
        documentDao.softDeleteDocument(id)
    }

    suspend fun restoreDocument(id: String) {
        documentDao.restoreDocument(id)
    }

    suspend fun permanentlyDeleteDocument(id: String) {
        documentDao.permanentlyDeleteDocument(id)
    }

    fun getDocumentCount(): Flow<Int> = documentDao.getDocumentCount()

    fun getTotalStorageUsed(): Flow<Long> = documentDao.getTotalStorageUsed()

    // Page operations
    fun getPagesByDocumentId(documentId: String): Flow<List<PageEntity>> =
        pageDao.getPagesByDocumentId(documentId)

    suspend fun savePage(page: PageEntity) = pageDao.insertPage(page)

    suspend fun savePages(pages: List<PageEntity>) = pageDao.insertPages(pages)

    suspend fun updatePage(page: PageEntity) = pageDao.updatePage(page)

    suspend fun deletePage(id: String) = pageDao.deletePage(id)

    suspend fun deletePagesByDocumentId(documentId: String) =
        pageDao.deletePagesByDocumentId(documentId)

    /** Atomically swap out all pages of a document for a freshly edited set. */
    suspend fun replacePages(documentId: String, pages: List<PageEntity>) {
        pageDao.deletePagesByDocumentId(documentId)
        pageDao.insertPages(pages)
    }

    // Recent files
    fun getRecentFiles(limit: Int = 20): Flow<List<RecentFileEntity>> =
        recentFileDao.getRecentFiles(limit)

    suspend fun addRecentAccess(documentId: String, action: String = "view") {
        recentFileDao.insertRecentFile(
            RecentFileEntity(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                action = action
            )
        )
    }
}
