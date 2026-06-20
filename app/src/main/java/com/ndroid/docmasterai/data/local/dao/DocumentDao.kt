package com.ndroid.docmasterai.data.local.dao

import androidx.room.*
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 10): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query(
        """SELECT * FROM documents WHERE isDeleted = 0 AND 
        (title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC"""
    )
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 0 AND folderPath = :folderPath ORDER BY updatedAt DESC")
    fun getDocumentsByFolder(folderPath: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteDocument(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isDeleted = 0, deletedAt = null WHERE id = :id")
    suspend fun restoreDocument(id: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun permanentlyDeleteDocument(id: String)

    @Query("DELETE FROM documents WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun purgeOldDeletedDocuments(cutoff: Long)

    @Query("SELECT COUNT(*) FROM documents WHERE isDeleted = 0")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM documents WHERE isDeleted = 0")
    fun getTotalStorageUsed(): Flow<Long>
}
