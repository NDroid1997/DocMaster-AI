package com.ndroid.docmasterai.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.docedit.sdk.ocr.OcrProcessor
import com.ndroid.docmasterai.data.remote.OpenAiService
import com.ndroid.docmasterai.data.local.dao.ConversionHistoryDao
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.data.repository.DocumentRepository
import com.ndroid.docmasterai.data.local.entity.ConversionHistoryEntity
import com.ndroid.docmasterai.data.model.ConversionType
import com.ndroid.docmasterai.data.model.ResizeMode
import com.ndroid.docmasterai.data.model.ResizeOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import android.os.Build
import java.io.FileWriter
import com.ndroid.docmasterai.util.StoragePaths
import android.content.ContentValues
import android.graphics.Color
import android.provider.MediaStore
import android.provider.OpenableColumns

@Singleton
class ConversionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversionHistoryDao: ConversionHistoryDao,
    private val documentRepository: DocumentRepository,
    private val openAiService: OpenAiService
) {
    private val outputDir: File
        get() = File(context.filesDir, "conversions").also { it.mkdirs() }

    fun getConversionHistory(): Flow<List<ConversionHistoryEntity>> =
        conversionHistoryDao.getConversionHistory()

    suspend fun convertPdfToImages(pdfPath: String): List<String> = withContext(Dispatchers.IO) {
        val images = mutableListOf<String>()
        val pdfUri = Uri.parse(pdfPath)
        val pfd = try {
            if (pdfUri.scheme == "content") {
                context.contentResolver.openFileDescriptor(pdfUri, "r")
            } else {
                ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (e: Exception) {
            null
        } ?: return@withContext emptyList()

        val pdfName = getFileName(pdfPath).substringBeforeLast(".")
        val renderer = PdfRenderer(pfd)
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            // Using higher scale for better quality
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            // Fill with white to avoid black background in JPEGs
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val fileName = "${pdfName}_page_${i + 1}.jpg"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            bitmap.recycle()

            val savedPath = saveToDownloads(tempFile, fileName, "image/jpeg", pdfName)
            images.add(savedPath)
            
            // Also save a copy internally for the document list
            val internalFile = File(outputDir, fileName)
            tempFile.copyTo(internalFile, overwrite = true)
            registerConvertedDocument(internalFile, "jpg", "PDF to Image")
        }
        renderer.close()
        pfd.close()

        saveConversionHistory(pdfPath, images.firstOrNull() ?: "", ConversionType.PDF_TO_IMAGE)
        images
    }

    suspend fun convertImageToPdf(imagePaths: List<String>, outputName: String): String =
        withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()

            imagePaths.forEachIndexed { index, path ->
                val bitmap = decodeBitmapFromPath(path)
                    ?: throw IllegalArgumentException("Cannot decode image at $path")
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            val fileName = "${outputName}_${UUID.randomUUID()}.pdf"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val savedPath = saveToDownloads(tempFile, fileName, "application/pdf")
            registerConvertedDocument(File(tempFile.absolutePath), "pdf", ConversionType.IMAGE_TO_PDF.displayName)
            saveConversionHistory(imagePaths.first(), savedPath, ConversionType.IMAGE_TO_PDF)
            savedPath
        }

    suspend fun convertImageFormat(
        imagePath: String,
        targetFormat: Bitmap.CompressFormat,
        quality: Int = 95
    ): String = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmapFromPath(imagePath) ?: throw IllegalArgumentException("Cannot decode image at $imagePath")
        val ext = when (targetFormat) {
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.PNG -> "png"
            else -> "webp"
        }
        val fileName = "converted_${UUID.randomUUID()}.$ext"
        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(targetFormat, quality, out)
        }
        bitmap.recycle()

        val type = when (targetFormat) {
            Bitmap.CompressFormat.JPEG -> if (imagePath.endsWith(".png", true)) ConversionType.PNG_TO_JPG else ConversionType.WEBP_TO_JPG
            Bitmap.CompressFormat.PNG -> ConversionType.JPG_TO_PNG
            else -> ConversionType.PNG_TO_JPG
        }
        val mimeType = when (targetFormat) {
            Bitmap.CompressFormat.JPEG -> "image/jpeg"
            Bitmap.CompressFormat.PNG -> "image/png"
            else -> "image/webp"
        }
        val savedPath = saveToDownloads(tempFile, fileName, mimeType)
        registerConvertedDocument(File(tempFile.absolutePath), ext, type.displayName)
        saveConversionHistory(imagePath, savedPath, type)
        savedPath
    }

    suspend fun convertImageToText(imagePath: String): String = withContext(Dispatchers.IO) {
        val uri = try { Uri.parse(imagePath) } catch (e: Exception) { Uri.fromFile(File(imagePath)) }
        val rawText = OcrProcessor.extractText(context, uri)
        if (rawText.isBlank()){
            throw IllegalArgumentException("No text could be found in the selected image. Please try an image with clearer text.")
        }
        val enhancedText = openAiService.processPdfContent(rawText)
        
        val fileName = "ocr_${UUID.randomUUID()}.txt"
        val tempFile = File(context.cacheDir, fileName)
        FileWriter(tempFile).use { writer ->
            writer.write(enhancedText)
        }
        
        val savedPath = saveToDownloads(tempFile, fileName, "text/plain")
        
        // Use a persistent file in internal storage for the database to ensure it's always available
        val internalFile = File(outputDir, fileName)
        FileWriter(internalFile).use { it.write(enhancedText) }
        registerConvertedDocument(internalFile, "txt", ConversionType.IMAGE_TO_TEXT.displayName)
        
        saveConversionHistory(imagePath, savedPath, ConversionType.IMAGE_TO_TEXT)
        savedPath
    }

    suspend fun convertPdfToText(pdfPath: String): String = withContext(Dispatchers.IO) {
        val rawText = try {
            val uri = try { Uri.parse(pdfPath) } catch (e: Exception) { Uri.fromFile(File(pdfPath)) }
            val inputStream = if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)
            } else {
                File(pdfPath).inputStream()
            }
            
            inputStream?.use { input ->
                PDDocument.load(input).use { document ->
                    PDFTextStripper().getText(document)
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }

        if (rawText.isBlank()) {
            throw IllegalArgumentException("No text could be extracted from this PDF. It might be an image-only PDF or scanned document without OCR.")
        }

        val processedText = openAiService.processPdfContent(rawText)

        val fileName = "pdf_text_${UUID.randomUUID()}.txt"
        val tempFile = File(context.cacheDir, fileName)
        FileWriter(tempFile).use { writer ->
            writer.write(processedText)
        }

        val savedPath = saveToDownloads(tempFile, fileName, "text/plain")
        
        val internalFile = File(outputDir, fileName)
        FileWriter(internalFile).use { it.write(processedText) }
        registerConvertedDocument(internalFile, "txt", ConversionType.PDF_TO_TEXT.displayName)
        
        saveConversionHistory(pdfPath, savedPath, ConversionType.PDF_TO_TEXT)
        savedPath
    }

    private suspend fun registerConvertedDocument(file: File, type: String, sourceType: String) {
        val documentEntity = DocumentEntity(
            id = UUID.randomUUID().toString(),
            title = file.name,
            filePath = file.absolutePath,
            thumbnailPath = null,
            fileType = type,
            fileSize = file.length(),
            pageCount = 1,
            sourceType = sourceType,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        documentRepository.saveDocument(documentEntity)
    }

    private fun saveToDownloads(
        tempFile: File, 
        displayName: String, 
        mimeType: String,
        subFolder: String? = null
    ): String {
        val resolver = context.contentResolver
        val relativePath = if (subFolder != null) {
            "${StoragePaths.downloadsRelativePath()}/$subFolder"
        } else {
            StoragePaths.downloadsRelativePath()
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        val finalPath = uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            if (subFolder != null) {
                "Download/${StoragePaths.DOWNLOAD_SUBDIR}/$subFolder/$displayName"
            } else {
                StoragePaths.downloadsUserVisiblePath(displayName)
            }
        } ?: tempFile.absolutePath

        return finalPath
    }

    private fun getFileName(path: String): String {
        val uri = Uri.parse(path)
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return File(path).name
    }

    suspend fun resizeImage(imagePath: String, options: ResizeOptions): String =
        withContext(Dispatchers.IO) {
            // Decode original bitmap (supports file paths and content URIs)
            val original = decodeBitmapFromPath(imagePath)
                ?: throw IllegalArgumentException("Cannot decode image at $imagePath")

            // Calculate desired dimensions
            val (newWidth, newHeight) = calculateDimensions(original, options)

            // Convert Bitmap -> Mat
            val srcMat = Mat()
            Utils.bitmapToMat(original, srcMat)

            // Resize using OpenCV
            val dstMat = Mat()
            Imgproc.resize(srcMat, dstMat, Size(newWidth.toDouble(), newHeight.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)

            // Convert Mat -> Bitmap
            val resized = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(dstMat, resized)

            // Determine output format and write file
            val webpFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }

            val format = when (options.outputFormat.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> webpFormat
                else -> Bitmap.CompressFormat.JPEG
            }

            val outputFile = File(outputDir, "resized_${UUID.randomUUID()}.${options.outputFormat}")
            FileOutputStream(outputFile).use { out ->
                resized.compress(format, options.quality, out)
            }

            // Release resources
            srcMat.release()
            dstMat.release()
            original.recycle()
            resized.recycle()

            registerConvertedDocument(outputFile, options.outputFormat, "Resize")
            outputFile.absolutePath
        }

    private fun decodeBitmapFromPath(path: String): Bitmap? {
        // If path points to an actual file, decode directly
        try {
            val file = File(path)
            if (file.exists()) return BitmapFactory.decodeFile(path)

            // Otherwise try treating the string as a Uri (content://... or file://...)
            val uri = try {
                Uri.parse(path)
            } catch (e: Exception) {
                null
            }

            if (uri != null && uri.scheme != null) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    return BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            // fallthrough to return null
        }
        return null
    }

    private fun calculateDimensions(bitmap: Bitmap, options: ResizeOptions): Pair<Int, Int> {
        return when (options.mode) {
            ResizeMode.WIDTH -> {
                val ratio = options.width.toFloat() / bitmap.width
                val height = if (options.maintainAspectRatio) (bitmap.height * ratio).toInt() else bitmap.height
                options.width to height
            }
            ResizeMode.HEIGHT -> {
                val ratio = options.height.toFloat() / bitmap.height
                val width = if (options.maintainAspectRatio) (bitmap.width * ratio).toInt() else bitmap.width
                width to options.height
            }
            ResizeMode.PERCENTAGE -> {
                val w = (bitmap.width * options.percentage / 100f).toInt()
                val h = (bitmap.height * options.percentage / 100f).toInt()
                w to h
            }
            ResizeMode.CUSTOM -> options.width to options.height
        }
    }

    private suspend fun saveConversionHistory(source: String, output: String, type: ConversionType) {
        conversionHistoryDao.insertConversion(
            ConversionHistoryEntity(
                id = UUID.randomUUID().toString(),
                sourceFile = source,
                outputFile = output,
                conversionType = type.name,
                fileSize = File(output).takeIf { it.exists() }?.length() ?: 0L,
            )
        )
    }
}
