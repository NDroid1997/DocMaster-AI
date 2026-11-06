package com.docedit.sdk.presentation

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.docedit.sdk.api.DocumentEditor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class EditorState(
    val pages: List<EditorPage> = emptyList(),
    val selectedIndex: Int = 0,
    val preview: Bitmap? = null,
    val busy: Boolean = true,
    val error: String? = null,
    val result: DocumentEditor.Result? = null,
)

internal class EditorViewModel(application: Application, private val savedState: SavedStateHandle) :
    AndroidViewModel(application) {
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()
    private var files: EditorFiles? = null
    private var previewJob: Job? = null

    fun load(input: DocumentEditor.Input) {
        if (files != null) return
        val editorFiles = EditorFiles(getApplication(), input)
        files = editorFiles
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) { editorFiles.load() }
                val order = savedState.get<IntArray>("pageOrder")
                val rotations = savedState.get<IntArray>("pageRotations")
                val pages = if (order != null && rotations != null && order.isNotEmpty() &&
                    order.size == rotations.size && order.all { it in 0 until count }
                ) {
                    order.mapIndexed { index, source -> EditorPage(source, rotations[index]) }
                } else {
                    List(count) { EditorPage(it) }
                }
                val selected = (savedState.get<Int>("selectedPage") ?: 0).coerceIn(pages.indices)
                _state.value = EditorState(pages = pages, selectedIndex = selected, busy = false)
                refreshPreview()
            } catch (e: CancellationException) {
                editorFiles.close()
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: "Cannot open document.") }
            }
        }
    }

    fun select(index: Int) {
        val current = _state.value
        if (current.busy || index !in current.pages.indices) return
        _state.update { it.copy(selectedIndex = index) }
        rememberPages()
        refreshPreview()
    }

    fun rotate() = changePages { pages, index ->
        pages[index] = pages[index].copy(rotation = (pages[index].rotation + 90) % 360)
        index
    }

    fun remove() = changePages { pages, index ->
        if (pages.size > 1) pages.removeAt(index)
        index.coerceAtMost(pages.lastIndex)
    }

    fun move(offset: Int) = changePages { pages, index ->
        val target = (index + offset).coerceIn(pages.indices)
        pages.add(target, pages.removeAt(index))
        target
    }

    fun save() {
        val current = _state.value
        val editorFiles = files ?: return
        if (current.busy || current.pages.isEmpty() || current.result != null) return
        previewJob?.cancel()
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { editorFiles.export(current.pages) }
                _state.update { it.copy(result = result, busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: "Cannot save document.") }
            }
        }
    }

    private fun changePages(change: (MutableList<EditorPage>, Int) -> Int) {
        val current = _state.value
        if (current.busy || current.pages.isEmpty()) return
        val pages = current.pages.toMutableList()
        val selected = change(pages, current.selectedIndex)
        _state.update { it.copy(pages = pages, selectedIndex = selected) }
        rememberPages()
        refreshPreview()
    }

    private fun rememberPages() {
        val current = _state.value
        savedState["pageOrder"] = current.pages.map { it.sourceIndex }.toIntArray()
        savedState["pageRotations"] = current.pages.map { it.rotation }.toIntArray()
        savedState["selectedPage"] = current.selectedIndex
    }

    private fun refreshPreview() {
        previewJob?.cancel()
        val current = _state.value
        val page = current.pages.getOrNull(current.selectedIndex) ?: return
        val editorFiles = files ?: return
        _state.update { it.copy(preview = null, error = null) }
        previewJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { editorFiles.preview(page) }
                _state.update { it.copy(preview = bitmap) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Cannot preview page.") }
            }
        }
    }

    override fun onCleared() {
        files?.close()
        super.onCleared()
    }
}
