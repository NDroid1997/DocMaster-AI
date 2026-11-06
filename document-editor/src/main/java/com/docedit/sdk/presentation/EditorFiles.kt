package com.docedit.sdk.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.docedit.sdk.api.DocumentEditor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.ensureActive
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

internal data class EditorPage(val sourceIndex: Int, val rotation: Int = 0)

/** Blocking file operations, called only from Dispatchers.IO. */
internal class EditorFiles(private val context: Context, private val input: DocumentEditor.Input) {
    private var sourcePdf: File? = null

    fun load(): Int {
        if (input.pdf == null) return input.images.size
        val file = File.createTempFile("editor_source_", ".pdf", context.cacheDir)
        try {
            requireNotNull(context.contentResolver.openInputStream(input.pdf)) {
                "Cannot read the selected PDF."
            }.use { source -> file.outputStream().use(source::copyTo) }
            val count = openPdf(file).use { it.pageCount }
            require(count > 0) { "This PDF has no pages." }
            sourcePdf = file
            return count
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    fun preview(page: EditorPage): Bitmap {
        val bitmap = sourcePdf?.let { file ->
            openPdf(file).use { renderer ->
                renderer.openPage(page.sourceIndex).use { pdfPage ->
                    val scale = 1600f / max(pdfPage.width, pdfPage.height)
                    Bitmap.createBitmap(
                        (pdfPage.width * scale).roundToInt().coerceAtLeast(1),
                        (pdfPage.height * scale).roundToInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888,
                    ).also {
                        it.eraseColor(Color.WHITE)
                        pdfPage.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        } ?: decodeImage(input.images[page.sourceIndex], preview = true)
        return rotate(bitmap, page.rotation)
    }

    suspend fun export(pages: List<EditorPage>): DocumentEditor.Result {
        require(pages.isNotEmpty()) { "Keep at least one page." }
        val directory = File(context.cacheDir, "editor_output/${UUID.randomUUID()}")
        check(directory.mkdirs()) { "Cannot create editor output directory." }
        try {
            val pdfFile = File(directory, "document.pdf")
            val images = mutableListOf<Uri>()
            val source = sourcePdf
            if (source != null) {
                PDFBoxResourceLoader.init(context)
                PDDocument.load(source).use { original ->
                    PDDocument().use { output ->
                        for (page in pages) {
                            coroutineContext.ensureActive()
                            val originalPage = original.getPage(page.sourceIndex)
                            val imported = output.importPage(originalPage)
                            imported.resources = originalPage.resources
                            imported.rotation = (imported.rotation + page.rotation) % 360
                        }
                        output.save(pdfFile)
                    }
                }
            } else {
                val output = PdfDocument()
                try {
                    pages.forEachIndexed { index, page ->
                        coroutineContext.ensureActive()
                        val bitmap = rotate(decodeImage(input.images[page.sourceIndex]), page.rotation)
                        try {
                            val imageFile = File(directory, "page_${index + 1}.png")
                            imageFile.outputStream().use {
                                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                                    "Cannot write page ${index + 1}."
                                }
                            }
                            images += Uri.fromFile(imageFile)
                            val info = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                            val pdfPage = output.startPage(info)
                            pdfPage.canvas.drawColor(Color.WHITE)
                            pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                            output.finishPage(pdfPage)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    pdfFile.outputStream().use(output::writeTo)
                } finally {
                    output.close()
                }
            }
            coroutineContext.ensureActive()
            val thumbnail = File(directory, "thumbnail.png")
            val bitmap = preview(pages.first())
            try {
                thumbnail.outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Cannot write thumbnail." }
                }
            } finally {
                bitmap.recycle()
            }
            return DocumentEditor.Result(
                wasCancelled = false,
                images = images,
                pdf = Uri.fromFile(pdfFile),
                thumbnail = Uri.fromFile(thumbnail),
                pageCount = pages.size,
            )
        } catch (e: Exception) {
            directory.deleteRecursively()
            throw e
        }
    }

    fun close() {
        sourcePdf?.delete()
    }

    private fun openPdf(file: File): PdfRenderer {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return try {
            PdfRenderer(descriptor)
        } catch (e: Exception) {
            descriptor.close()
            throw e
        }
    }

    private fun decodeImage(uri: Uri, preview: Boolean = false): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            if (preview) {
                val scale = (1600f / max(info.size.width, info.size.height)).coerceAtMost(1f)
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }

    private fun rotate(bitmap: Bitmap, rotation: Int): Bitmap {
        if (rotation == 0) return bitmap
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, true,
        )
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}

