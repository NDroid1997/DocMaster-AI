package com.ndroid.docmasterai.ui.screens.pdftools.pdfpagesreorder

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File
import java.io.FileOutputStream

object PdfReorderHelper {

    fun getPageCount(context: Context, uri: Uri): Int {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open PDF" }
            PDDocument.load(input).use { doc -> return doc.numberOfPages }
        }
    }

    /** Renders every page once, in original order. Reordering afterwards is purely a list operation. */
    fun renderThumbnails(
        context: Context,
        uri: Uri,
        dpi: Float = 45f,
        onPageRendered: (originalIndex: Int, bitmap: Bitmap) -> Unit
    ) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open PDF" }
            PDDocument.load(input).use { doc ->
                val renderer = PDFRenderer(doc)
                for (i in 0 until doc.numberOfPages) {
                    onPageRendered(i, renderer.renderImageWithDPI(i, dpi))
                }
            }
        }
    }

    /**
     * Writes a new PDF to [outputFile] whose pages are the source document's pages copied in
     * the order given by [newPageOrder] (a list of 0-based ORIGINAL page indices).
     */
    fun saveReordered(context: Context, uri: Uri, newPageOrder: List<Int>, outputFile: File) {
        require(newPageOrder.isNotEmpty()) { "No pages to save" }
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open PDF" }
            PDDocument.load(input).use { source ->
                PDDocument().use { output ->
                    newPageOrder.forEach { originalIndex ->
                        output.importPage(source.getPage(originalIndex))
                    }
                    FileOutputStream(outputFile).use { out -> output.save(out) }
                }
            }
        }
    }
}
