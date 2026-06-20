package com.ndroid.docmasterai.ui.screens.resize

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.model.ResizeMode
import com.ndroid.docmasterai.data.model.ResizeOptions
import com.ndroid.docmasterai.data.repository.ConversionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResizeUiState(
    val selectedImageUri: Uri? = null,
    val selectedMode: ResizeMode = ResizeMode.PERCENTAGE,
    val width: String = "",
    val height: String = "",
    val percentage: Int = 100,
    val quality: Int = 90,
    val maintainAspectRatio: Boolean = true,
    val outputFormat: String = "jpg",
    val isProcessing: Boolean = false,
    val resultPath: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ResizeViewModel @Inject constructor(
    private val conversionRepository: ConversionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResizeUiState())
    val uiState: StateFlow<ResizeUiState> = _uiState.asStateFlow()

    fun setImage(uri: Uri) { _uiState.update { it.copy(selectedImageUri = uri) } }
    fun setMode(mode: ResizeMode) { _uiState.update { it.copy(selectedMode = mode) } }
    fun setWidth(w: String) { _uiState.update { it.copy(width = w) } }
    fun setHeight(h: String) { _uiState.update { it.copy(height = h) } }
    fun setPercentage(p: Int) { _uiState.update { it.copy(percentage = p) } }
    fun setQuality(q: Int) { _uiState.update { it.copy(quality = q) } }
    fun setOutputFormat(f: String) { _uiState.update { it.copy(outputFormat = f) } }
    fun toggleAspectRatio() { _uiState.update { it.copy(maintainAspectRatio = !it.maintainAspectRatio) } }

    fun resize() {
        val state = _uiState.value
        val imagePath = state.selectedImageUri?.path ?: return
        _uiState.update { it.copy(isProcessing = true, error = null) }

        viewModelScope.launch {
            try {
                val options = ResizeOptions(
                    mode = state.selectedMode,
                    width = state.width.toIntOrNull() ?: 0,
                    height = state.height.toIntOrNull() ?: 0,
                    percentage = state.percentage,
                    quality = state.quality,
                    maintainAspectRatio = state.maintainAspectRatio,
                    outputFormat = state.outputFormat,
                )
                val result = conversionRepository.resizeImage(imagePath, options)
                _uiState.update { it.copy(isProcessing = false, resultPath = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }
}
