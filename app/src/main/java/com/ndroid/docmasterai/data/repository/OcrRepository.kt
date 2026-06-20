package com.ndroid.docmasterai.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ndroid.docmasterai.data.model.OcrResult
import com.ndroid.docmasterai.data.model.TextBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(imagePath: String): OcrResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val image = InputImage.fromBitmap(bitmap, 0)
        extractText(image, startTime)
    }

    suspend fun extractTextFromUri(uri: Uri): OcrResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val image = InputImage.fromFilePath(context, uri)
        extractText(image, startTime)
    }

    private suspend fun extractText(image: InputImage, startTime: Long): OcrResult =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        TextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox?.let {
                                listOf(it.left, it.top, it.right, it.bottom)
                            } ?: emptyList(),
                            confidence = block.lines.firstOrNull()?.confidence ?: 0f,
                        )
                    }
                    val result = OcrResult(
                        text = visionText.text,
                        blocks = blocks,
                        processingTimeMs = System.currentTimeMillis() - startTime,
                    )
                    continuation.resume(result)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    suspend fun exportToTxt(text: String, outputPath: String) = withContext(Dispatchers.IO) {
        FileWriter(outputPath).use { it.write(text) }
    }

    suspend fun exportToPdf(text: String, outputPath: String) = withContext(Dispatchers.IO) {
        // Simple PDF with text using Android's PdfDocument
        val document = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)

        val paint = android.graphics.Paint().apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        val lines = text.split("\n")
        var y = 40f
        for (line in lines) {
            if (y > 800f) {
                document.finishPage(page)
                val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 2).create()
                val newPage = document.startPage(newPageInfo)
                y = 40f
                page.canvas.drawText(line, 40f, y, paint)
                y += 18f
            } else {
                page.canvas.drawText(line, 40f, y, paint)
                y += 18f
            }
        }
        document.finishPage(page)

        File(outputPath).outputStream().use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    fun close() {
        recognizer.close()
    }
}
