package com.ndroid.docmasterai.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManagerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val appFilesDir: File
        get() = File(context.filesDir, "documents").also { it.mkdirs() }

    fun getAppFilesDirectory(): File = appFilesDir

    suspend fun getFilesInFolder(folderPath: String): List<File> = withContext(Dispatchers.IO) {
        val dir = if (folderPath.isEmpty()) appFilesDir else File(folderPath)
        dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun createFolder(name: String, parentPath: String? = null): File =
        withContext(Dispatchers.IO) {
            val parent = if (parentPath.isNullOrEmpty()) appFilesDir else File(parentPath)
            File(parent, name).also { it.mkdirs() }
        }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).delete()
    }

    suspend fun renameFile(filePath: String, newName: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val newFile = File(file.parent, newName)
        file.renameTo(newFile)
        newFile.absolutePath
    }

    suspend fun moveFile(filePath: String, destFolder: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val destFile = File(destFolder, file.name)
        file.copyTo(destFile, overwrite = true)
        file.delete()
        destFile.absolutePath
    }

    suspend fun copyFile(filePath: String, destFolder: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val destFile = File(destFolder, file.name)
        file.copyTo(destFile, overwrite = true)
        destFile.absolutePath
    }

    suspend fun getStorageInfo(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val usedBytes = calculateDirSize(appFilesDir) +
                calculateDirSize(File(context.filesDir, "scans")) +
                calculateDirSize(File(context.filesDir, "conversions")) +
                calculateDirSize(File(context.filesDir, "pdf_output"))
        val freeBytes = context.filesDir.freeSpace
        usedBytes to freeBytes
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun getShareIntent(filePath: String, mimeType: String = "*/*"): Intent {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getShareMultipleIntent(filePaths: List<String>, mimeType: String = "*/*"): Intent {
        val uris = ArrayList<Uri>(filePaths.map { path ->
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
        })
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val cacheSize = calculateDirSize(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        cacheSize
    }

    fun getMimeType(filePath: String): String {
        return when (File(filePath).extension.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            else -> "*/*"
        }
    }
}
