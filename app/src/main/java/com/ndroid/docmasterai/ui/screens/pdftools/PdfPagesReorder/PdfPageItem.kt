package com.ndroid.docmasterai.ui.screens.pdftools.pdfpagesreorder

import android.graphics.Bitmap

/**
 * [id] is the page's ORIGINAL index in the source PDF and never changes — it's used as the
 * stable Compose key so drag animations and thumbnail updates target the right item even as
 * the item's position in the on-screen list changes.
 *
 * The on-screen List<PdfPageItem> order (in the ViewModel) IS the new page order — no separate
 * "position" field needed.
 */
data class PdfPageItem(
    val id: Int,           // original 0-based page index in the source PDF
    val bitmap: Bitmap? = null
)
