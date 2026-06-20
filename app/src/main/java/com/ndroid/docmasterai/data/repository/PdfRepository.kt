package com.ndroid.docmasterai.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val outputDir: File
        get() = File(context.filesDir, "pdf_output").also { it.mkdirs() }

    suspend fun mergePdfs(pdfPaths: List<String>, outputName: String): String =
        withContext(Dispatchers.IO) {
            val mergedDocument = PdfDocument()
            var pageNum = 1

            pdfPaths.forEach { path ->
                val file = File(path)
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)

                for (i in 0 until renderer.pageCount) {
                    val pdfPage = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                    )
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNum).create()
                    val page = mergedDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    mergedDocument.finishPage(page)
                    bitmap.recycle()
                    pageNum++
                }
                renderer.close()
                pfd.close()
            }

            val outputFile = File(outputDir, "${outputName}_merged_${UUID.randomUUID()}.pdf")
            FileOutputStream(outputFile).use { out ->
                mergedDocument.writeTo(out)
            }
            mergedDocument.close()
            outputFile.absolutePath
        }

    suspend fun splitPdf(pdfPath: String, pageRanges: List<IntRange>): List<String> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<String>()
            val file = File(pdfPath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

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
                results.add(outputFile.absolutePath)
            }

            renderer.close()
            pfd.close()
            results
        }

    suspend fun rotatePdfPages(pdfPath: String, rotations: Map<Int, Int>): String =
        withContext(Dispatchers.IO) {
            val file = File(pdfPath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val rotatedDocument = PdfDocument()

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

            renderer.close()
            pfd.close()

            val outputFile = File(outputDir, "rotated_${UUID.randomUUID()}.pdf")
            FileOutputStream(outputFile).use { out ->
                rotatedDocument.writeTo(out)
            }
            rotatedDocument.close()
            outputFile.absolutePath
        }

    suspend fun deletePages(pdfPath: String, pageIndices: Set<Int>): String =
        withContext(Dispatchers.IO) {
            val file = File(pdfPath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val newDocument = PdfDocument()
            var newPageNum = 1

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

            renderer.close()
            pfd.close()

            val outputFile = File(outputDir, "modified_${UUID.randomUUID()}.pdf")
            FileOutputStream(outputFile).use { out ->
                newDocument.writeTo(out)
            }
            newDocument.close()
            outputFile.absolutePath
        }

    suspend fun addWatermark(pdfPath: String, watermarkText: String): String =
        withContext(Dispatchers.IO) {
            val file = File(pdfPath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val newDocument = PdfDocument()

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

            renderer.close()
            pfd.close()

            val outputFile = File(outputDir, "watermarked_${UUID.randomUUID()}.pdf")
            FileOutputStream(outputFile).use { out ->
                newDocument.writeTo(out)
            }
            newDocument.close()
            outputFile.absolutePath
        }

    suspend fun compressPdf(pdfPath: String, quality: Int = 50): String =
        withContext(Dispatchers.IO) {
            val file = File(pdfPath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val compressedDocument = PdfDocument()

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

            renderer.close()
            pfd.close()

            val outputFile = File(outputDir, "compressed_${UUID.randomUUID()}.pdf")
            FileOutputStream(outputFile).use { out ->
                compressedDocument.writeTo(out)
            }
            compressedDocument.close()
            outputFile.absolutePath
        }

    suspend fun getPdfPageCount(pdfPath: String): Int = withContext(Dispatchers.IO) {
        val file = File(pdfPath)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val count = renderer.pageCount
        renderer.close()
        pfd.close()
        count
    }

    suspend fun renderPdfPage(pdfPath: String, pageIndex: Int): Bitmap = withContext(Dispatchers.IO) {
        val file = File(pdfPath)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(
            page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
        )
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        pfd.close()
        bitmap
    }
}
