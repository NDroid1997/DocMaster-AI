package com.ndroid.docmasterai.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.ndroid.docmasterai.util.StoragePaths
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepository @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val context: Context
) {
    private val outputDir: File
        get() = File(context.filesDir, "pdf_output").also { it.mkdirs() }
    
    private val thumbDir: File
        get() = File(context.filesDir, "thumbnails").also { it.mkdirs() }

    private fun getPfd(path: String): ParcelFileDescriptor? {
        return try {
            val uri = Uri.parse(path)
            if (uri.scheme == "content" || uri.scheme == "file") {
                context.contentResolver.openFileDescriptor(uri, "r")
            } else {
                ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getInputStream(path: String): InputStream? {
        return try {
            val uri = Uri.parse(path)
            if (uri.scheme == "content" || uri.scheme == "file") {
                context.contentResolver.openInputStream(uri)
            } else {
                File(path).inputStream()
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveToDatabase(file: File, sourceType: String) {
        val thumbnailFile = File(thumbDir, "${file.nameWithoutExtension}_thumb.jpg")
        try {
            val bitmap = renderPdfPage(file.absolutePath, 0)
            FileOutputStream(thumbnailFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()
            Log.d("PdfRepository", "Thumbnail generated: ${thumbnailFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("PdfRepository", "Thumbnail generation failed: ${e.message}")
        }

        val documentEntity = DocumentEntity(
            id = UUID.randomUUID().toString(),
            title = file.name,
            filePath = file.absolutePath,
            thumbnailPath = if (thumbnailFile.exists()) thumbnailFile.absolutePath else null,
            fileType = "pdf",
            fileSize = file.length(),
            pageCount = getPdfPageCount(file.absolutePath),
            sourceType = sourceType,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        Log.d("PdfRepository", "Saving document to database: ${documentEntity.title} (size: ${documentEntity.fileSize})")
        documentRepository.saveDocument(documentEntity)
        Log.d("PdfRepository", "Document saved successfully with ID: ${documentEntity.id}")
    }

    suspend fun mergePdfs(pdfPaths: List<String>, outputName: String): String =
        withContext(Dispatchers.IO) {
            val merger = PDFMergerUtility()
            val outputFile = File(outputDir, "${outputName}_merged_${UUID.randomUUID()}.pdf")
            val streams = mutableListOf<InputStream>()

            try {
                pdfPaths.forEach { path ->
                    getInputStream(path)?.let {
                        streams.add(it)
                        merger.addSource(it)
                    }
                }
                merger.destinationFileName = outputFile.absolutePath
                merger.mergeDocuments(null)
                
                saveToDatabase(outputFile, "merge")
            } finally {
                streams.forEach { try { it.close() } catch (e: Exception) {} }
            }

            outputFile.absolutePath
        }

    suspend fun splitPdf(pdfPath: String, pageRanges: List<IntRange>): List<String> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<String>()
            val pfd = getPfd(pdfPath) ?: return@withContext emptyList()
            val renderer = PdfRenderer(pfd)

            try {
                pageRanges.forEachIndexed { index, range ->
                    val splitDocument = PdfDocument()
                    var pageNum = 1

                    for (i in range) {
                        if (i < renderer.pageCount) {
                            val pdfPage = renderer.openPage(i)
                            val bitmap = Bitmap.createBitmap(
                                pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                            )
                            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pdfPage.close()

                            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNum).create()
                            val page = splitDocument.startPage(pageInfo)
                            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                            splitDocument.finishPage(page)
                            bitmap.recycle()
                            pageNum++
                        }
                    }

                    val outputFile = File(outputDir, "split_${index + 1}_${UUID.randomUUID()}.pdf")
                    FileOutputStream(outputFile).use { out ->
                        splitDocument.writeTo(out)
                    }
                    splitDocument.close()
                    
                    saveToDatabase(outputFile, "split")
                    results.add(outputFile.absolutePath)
                }
            } finally {
                renderer.close()
                pfd.close()
            }
            results
        }

    suspend fun rotatePdfPages(pdfPath: String, rotations: Map<Int, Int>): String =
        withContext(Dispatchers.IO) {
            val pfd = getPfd(pdfPath) ?: return@withContext ""
            val renderer = PdfRenderer(pfd)
            val rotatedDocument = PdfDocument()

            try {
                for (i in 0 until renderer.pageCount) {
                    val pdfPage = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                    )
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val rotation = rotations[i] ?: 0
                    val rotated = if (rotation != 0) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else bitmap

                    val pageInfo = PdfDocument.PageInfo.Builder(rotated.width, rotated.height, i + 1).create()
                    val page = rotatedDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(rotated, 0f, 0f, null)
                    rotatedDocument.finishPage(page)

                    if (rotated != bitmap) rotated.recycle()
                    bitmap.recycle()
                }

                val outputFile = File(outputDir, "rotated_${UUID.randomUUID()}.pdf")
                FileOutputStream(outputFile).use { out ->
                    rotatedDocument.writeTo(out)
                }
                rotatedDocument.close()
                
                saveToDatabase(outputFile, "rotate")
                outputFile.absolutePath
            } finally {
                renderer.close()
                pfd.close()
            }
        }

    suspend fun deletePages(pdfPath: String, pageIndices: Set<Int>): String =
        withContext(Dispatchers.IO) {
            val pfd = getPfd(pdfPath) ?: return@withContext ""
            val renderer = PdfRenderer(pfd)
            val newDocument = PdfDocument()
            var newPageNum = 1

            try {
                for (i in 0 until renderer.pageCount) {
                    if (i !in pageIndices) {
                        val pdfPage = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(
                            pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                        )
                        pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pdfPage.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, newPageNum).create()
                        val page = newDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        newDocument.finishPage(page)
                        bitmap.recycle()
                        newPageNum++
                    }
                }

                val outputFile = File(outputDir, "modified_${UUID.randomUUID()}.pdf")
                FileOutputStream(outputFile).use { out ->
                    newDocument.writeTo(out)
                }
                newDocument.close()
                
                saveToDatabase(outputFile, "delete_pages")
                outputFile.absolutePath
            } finally {
                renderer.close()
                pfd.close()
            }
        }

    suspend fun addWatermark(pdfPath: String, watermarkText: String): String =
        withContext(Dispatchers.IO) {
            val pfd = getPfd(pdfPath) ?: return@withContext ""
            val renderer = PdfRenderer(pfd)
            val newDocument = PdfDocument()

            try {
                for (i in 0 until renderer.pageCount) {
                    val pdfPage = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                    )
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                    val page = newDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)

                    // Draw watermark
                    val paint = Paint().apply {
                        color = Color.argb(60, 128, 128, 128)
                        textSize = bitmap.width / 10f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    page.canvas.save()
                    page.canvas.rotate(-45f, bitmap.width / 2f, bitmap.height / 2f)
                    page.canvas.drawText(
                        watermarkText,
                        bitmap.width / 2f,
                        bitmap.height / 2f,
                        paint
                    )
                    page.canvas.restore()

                    newDocument.finishPage(page)
                    bitmap.recycle()
                }

                val outputFile = File(outputDir, "watermarked_${UUID.randomUUID()}.pdf")
                FileOutputStream(outputFile).use { out ->
                    newDocument.writeTo(out)
                }
                newDocument.close()
                
                saveToDatabase(outputFile, "watermark")
                outputFile.absolutePath
            } finally {
                renderer.close()
                pfd.close()
            }
        }

    suspend fun compressPdf(pdfPath: String, quality: Int = 50): String =
        withContext(Dispatchers.IO) {
            val pfd = getPfd(pdfPath) ?: return@withContext ""
            val renderer = PdfRenderer(pfd)
            val compressedDocument = PdfDocument()

            try {
                for (i in 0 until renderer.pageCount) {
                    val pdfPage = renderer.openPage(i)
                    // Use lower resolution for compression
                    val scale = quality / 100f
                    val w = (pdfPage.width * (1 + scale)).toInt()
                    val h = (pdfPage.height * (1 + scale)).toInt()
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                    val page = compressedDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    compressedDocument.finishPage(page)
                    bitmap.recycle()
                }

                val outputFile = File(outputDir, "compressed_${UUID.randomUUID()}.pdf")
                FileOutputStream(outputFile).use { out ->
                    compressedDocument.writeTo(out)
                }
                compressedDocument.close()
                
                saveToDatabase(outputFile, "compress")
                outputFile.absolutePath
            } finally {
                renderer.close()
                pfd.close()
            }
        }

    suspend fun getPdfPageCount(pdfPath: String): Int = withContext(Dispatchers.IO) {
        val pfd = getPfd(pdfPath) ?: run {
            Log.e("PdfRepository", "Could not open PFD for path: $pdfPath")
            return@withContext 0
        }
        try {
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            count
        } catch (e: Exception) {
            Log.e("PdfRepository", "Error getting page count: ${e.message}")
            0
        } finally {
            pfd.close()
        }
    }

    suspend fun isPdfEncrypted(pdfPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(pdfPath)?.use { input ->
                PDDocument.load(input).use { document ->
                    val encrypted = document.isEncrypted
                    encrypted
                }
            } ?: false
        } catch (e: InvalidPasswordException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renderPdfPage(pdfPath: String, pageIndex: Int): Bitmap = withContext(Dispatchers.IO) {
        val pfd = getPfd(pdfPath) ?: run {
            Log.e("PdfRepository", "Could not open PFD for path: $pdfPath")
            return@withContext Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        try {
            val renderer = PdfRenderer(pfd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                return@withContext Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(
                page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            bitmap
        } catch (e: Exception) {
            Log.e("PdfRepository", "Error rendering PDF page: ${e.message}")
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } finally {
            pfd.close()
        }
    }

    suspend fun extractPages(pdfPath: String, pageIndices: List<Int>, outputName: String): String =
        withContext(Dispatchers.IO) {
            val pfd = getPfd(pdfPath) ?: return@withContext ""
            val renderer = PdfRenderer(pfd)
            val newDocument = PdfDocument()
            var newPageNum = 1

            try {
                for (i in pageIndices) {
                    if (i in 0 until renderer.pageCount) {
                        val pdfPage = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(
                            pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(Color.WHITE)
                        pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pdfPage.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, newPageNum).create()
                        val page = newDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        newDocument.finishPage(page)
                        bitmap.recycle()
                        newPageNum++
                    }
                }

                val outputFile = File(outputDir, "${outputName}_${UUID.randomUUID()}.pdf")
                FileOutputStream(outputFile).use { out ->
                    newDocument.writeTo(out)
                }
                newDocument.close()

                saveToDatabase(outputFile, "extract")
                outputFile.absolutePath
            } finally {
                renderer.close()
                pfd.close()
            }
        }

    suspend fun saveReorderedPdf(pdfPath: String, newPageOrder: List<Int>, outputName: String): String =
        withContext(Dispatchers.IO) {
            val pfd = getPfd(pdfPath) ?: return@withContext ""
            val renderer = PdfRenderer(pfd)
            val newDocument = PdfDocument()

            try {
                newPageOrder.forEachIndexed { index, originalIndex ->
                    if (originalIndex in 0 until renderer.pageCount) {
                        val pdfPage = renderer.openPage(originalIndex)
                        val bitmap = Bitmap.createBitmap(
                            pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(Color.WHITE)
                        pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pdfPage.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                        val page = newDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        newDocument.finishPage(page)
                        bitmap.recycle()
                    }
                }

                val outputFile = File(outputDir, "${outputName}_reordered_${UUID.randomUUID()}.pdf")
                FileOutputStream(outputFile).use { out ->
                    newDocument.writeTo(out)
                }
                newDocument.close()

                saveToDatabase(outputFile, "reorder")
                outputFile.absolutePath
            } finally {
                renderer.close()
                pfd.close()
            }
        }

    private fun savePdfToDownloads(tempFile: File, displayName: String): String {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, StoragePaths.downloadsRelativePath())
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            return uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()
            StoragePaths.downloadsUserVisiblePath(displayName)
        } ?: tempFile.absolutePath
    }

    suspend fun protectPdf(pdfPath: String, userPassword: String, ownerPassword: String, originalFileName: String): String =
        withContext(Dispatchers.IO) {
            val fileName = if (originalFileName.lowercase().endsWith(".pdf")) {
                originalFileName.replace(".pdf", "_protected.pdf", ignoreCase = true)
            } else {
                "${originalFileName}_protected.pdf"
            }
            
            val tempFile = File(context.cacheDir, "temp_protected_${UUID.randomUUID()}.pdf")
            
            getInputStream(pdfPath)?.use { input ->
                val document = PDDocument.load(input)
                val accessPermission = AccessPermission()
                val policy = StandardProtectionPolicy(ownerPassword, userPassword, accessPermission)
                policy.encryptionKeyLength = 256
                document.protect(policy)
                document.save(tempFile)
                document.close()
            } ?: throw Exception("Could not open source PDF")
            
            savePdfToDownloads(tempFile, fileName)
        }

    suspend fun removePassword(pdfPath: String, password: String, originalFileName: String): String =
        withContext(Dispatchers.IO) {
            val fileName = if (originalFileName.lowercase().endsWith(".pdf")) {
                originalFileName.replace(".pdf", "_unlocked.pdf", ignoreCase = true)
            } else {
                "${originalFileName}_unlocked.pdf"
            }

            val tempFile = File(context.cacheDir, "temp_unlocked_${UUID.randomUUID()}.pdf")
            
            getInputStream(pdfPath)?.use { input ->
                val document = PDDocument.load(input, password)
                document.setAllSecurityToBeRemoved(true)
                document.save(tempFile)
                document.close()
            } ?: throw Exception("Could not open source PDF")
            
            savePdfToDownloads(tempFile, fileName)
        }
}
