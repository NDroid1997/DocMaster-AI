package com.ndroid.docmasterai.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.ndroid.docmasterai.data.local.dao.ConversionHistoryDao
import com.ndroid.docmasterai.data.local.entity.ConversionHistoryEntity
import com.ndroid.docmasterai.data.model.ConversionType
import com.ndroid.docmasterai.data.model.ResizeMode
import com.ndroid.docmasterai.data.model.ResizeOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversionHistoryDao: ConversionHistoryDao,
) {
    private val outputDir: File
        get() = File(context.filesDir, "conversions").also { it.mkdirs() }

    fun getConversionHistory(): Flow<List<ConversionHistoryEntity>> =
        conversionHistoryDao.getConversionHistory()

    suspend fun convertPdfToImages(pdfPath: String): List<String> = withContext(Dispatchers.IO) {
        val images = mutableListOf<String>()
        val file = File(pdfPath)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(
                page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val outputFile = File(outputDir, "pdf_page_${i + 1}_${UUID.randomUUID()}.jpg")
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            bitmap.recycle()
            images.add(outputFile.absolutePath)
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
                val bitmap = BitmapFactory.decodeFile(path)
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            val outputFile = File(outputDir, "${outputName}_${UUID.randomUUID()}.pdf")
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            saveConversionHistory(imagePaths.first(), outputFile.absolutePath, ConversionType.IMAGE_TO_PDF)
            outputFile.absolutePath
        }

    suspend fun convertImageFormat(
        imagePath: String,
        targetFormat: Bitmap.CompressFormat,
        quality: Int = 95
    ): String = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val ext = when (targetFormat) {
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP_LOSSY -> "webp"
            else -> "jpg"
        }
        val outputFile = File(outputDir, "converted_${UUID.randomUUID()}.$ext")
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(targetFormat, quality, out)
        }
        bitmap.recycle()

        val type = when (targetFormat) {
            Bitmap.CompressFormat.JPEG -> ConversionType.PNG_TO_JPG
            Bitmap.CompressFormat.PNG -> ConversionType.JPG_TO_PNG
            else -> ConversionType.PNG_TO_JPG
        }
        saveConversionHistory(imagePath, outputFile.absolutePath, type)
        outputFile.absolutePath
    }

    suspend fun resizeImage(imagePath: String, options: ResizeOptions): String =
        withContext(Dispatchers.IO) {
            val original = BitmapFactory.decodeFile(imagePath)
            val (newWidth, newHeight) = calculateDimensions(original, options)

            val resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
            val format = when (options.outputFormat.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> Bitmap.CompressFormat.WEBP_LOSSY
                else -> Bitmap.CompressFormat.JPEG
            }
            val outputFile = File(outputDir, "resized_${UUID.randomUUID()}.${options.outputFormat}")
            FileOutputStream(outputFile).use { out ->
                resized.compress(format, options.quality, out)
            }
            original.recycle()
            resized.recycle()
            outputFile.absolutePath
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
