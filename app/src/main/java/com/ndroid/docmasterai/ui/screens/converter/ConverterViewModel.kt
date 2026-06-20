package com.ndroid.docmasterai.ui.screens.converter

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.model.ConversionType
import com.ndroid.docmasterai.data.repository.ConversionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConverterUiState(
    val selectedType: ConversionType? = null,
    val isConverting: Boolean = false,
    val resultPath: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val conversionRepository: ConversionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    fun selectConversionType(type: ConversionType) {
        _uiState.update { it.copy(selectedType = type, resultPath = null, error = null) }
    }

    fun startConversion(uri: Uri) {
        val type = _uiState.value.selectedType ?: return
        _uiState.update { it.copy(isConverting = true, error = null) }

        viewModelScope.launch {
            try {
                val result = when (type) {
                    ConversionType.IMAGE_TO_PDF -> {
                        conversionRepository.convertImageToPdf(
                            listOf(uri.path ?: ""), "converted"
                        )
                    }
                    ConversionType.PNG_TO_JPG -> {
                        conversionRepository.convertImageFormat(
                            uri.path ?: "",
                            android.graphics.Bitmap.CompressFormat.JPEG,
                        )
                    }
                    ConversionType.JPG_TO_PNG -> {
                        conversionRepository.convertImageFormat(
                            uri.path ?: "",
                            android.graphics.Bitmap.CompressFormat.PNG,
                        )
                    }
                    else -> uri.path ?: ""
                }
                _uiState.update { it.copy(isConverting = false, resultPath = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isConverting = false, error = e.message) }
            }
        }
    }
}
