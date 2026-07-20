package com.ndroid.docmasterai.ui.screens.pdftools.pdfpagesreorder

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.repository.PdfRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfReorderViewModel @Inject constructor(
    application: Application,
    private val pdfRepository: PdfRepository,
) : AndroidViewModel(application) {

    init {
        PDFBoxResourceLoader.init(application.applicationContext)
    }

    /** The list order IS the current page order. Compose observes mutations to this directly. */
    val pages = mutableStateListOf<PdfPageItem>()

    private val _sourceFileName = mutableStateOf("")
    val sourceFileName: State<String> = _sourceFileName

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isSaving = mutableStateOf(false)
    val isSaving: State<Boolean> = _isSaving

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _isSuccess = mutableStateOf(false)
    val isSuccess: State<Boolean> = _isSuccess

    private var sourceUri: Uri? = null

    fun loadPdf(uri: Uri, displayName: String) {
        if (sourceUri == uri) return
        sourceUri = uri
        _sourceFileName.value = displayName
        pages.clear()
        _errorMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val pageCount = pdfRepository.getPdfPageCount(uri.toString())
                if (pageCount == 0) {
                    _isLoading.value = false
                    _errorMessage.value = "Could not load PDF"
                    return@launch
                }
                pages.addAll((0 until pageCount).map { PdfPageItem(id = it) })
                _isLoading.value = false

                val bitmaps = withContext(Dispatchers.IO) {
                    (0 until pageCount).map { i ->
                        pdfRepository.renderPdfPage(uri.toString(), i)
                    }
                }
                
                bitmaps.forEachIndexed { i, bitmap ->
                    val idx = pages.indexOfFirst { it.id == i }
                    if (idx != -1) {
                        pages[idx] = pages[idx].copy(bitmap = bitmap)
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.message ?: "Failed to load PDF"
            }
        }
    }

    /** Moves the page at [from] to [to], shifting everything in between — called by the drag state. */
    fun movePage(from: Int, to: Int) {
        if (from == to || from !in pages.indices || to !in pages.indices) return
        pages.add(to, pages.removeAt(from))
    }

    fun resetOrder() {
        pages.sortBy { it.id }
    }

    fun saveReorderedPdf() {
        val uri = sourceUri ?: return
        if (pages.isEmpty()) return
        _isSaving.value = true
        _errorMessage.value = null

        val order = pages.map { it.id }
        val outputName = _sourceFileName.value.substringBeforeLast(".").ifBlank { "document" }

        viewModelScope.launch {
            try {
                pdfRepository.saveReorderedPdf(uri.toString(), order, outputName)
                _isSaving.value = false
                _isSuccess.value = true
            } catch (e: Exception) {
                _isSaving.value = false
                _errorMessage.value = e.message ?: "Failed to save PDF"
            }
        }
    }
}
