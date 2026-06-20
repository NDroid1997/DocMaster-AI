package com.ndroid.docmasterai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.local.preferences.AppPreferences
import com.ndroid.docmasterai.data.repository.FileManagerRepository
import com.ndroid.docmasterai.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: String = "system",
    val defaultSaveFormat: String = "pdf",
    val scanQuality: Int = 90,
    val ocrLanguage: String = "latin",
    val autoEnhance: Boolean = true,
    val isPremium: Boolean = false,
    val storageUsed: Long = 0L,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val fileManagerRepository: FileManagerRepository,
    private val premiumManager: PremiumManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.darkMode,
                preferences.defaultSaveFormat,
                preferences.defaultScanQuality,
                preferences.defaultOcrLanguage,
                preferences.autoEnhance,
            ) { darkMode, format, quality, lang, enhance ->
                _uiState.value.copy(
                    darkMode = darkMode,
                    defaultSaveFormat = format,
                    scanQuality = quality,
                    ocrLanguage = lang,
                    autoEnhance = enhance,
                )
            }.collect { state -> _uiState.value = state }
        }
        viewModelScope.launch {
            preferences.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        viewModelScope.launch {
            val (used, _) = fileManagerRepository.getStorageInfo()
            _uiState.update { it.copy(storageUsed = used) }
        }
    }

    fun cycleDarkMode() {
        viewModelScope.launch {
            val next = when (_uiState.value.darkMode) {
                "system" -> "dark"
                "dark" -> "light"
                else -> "system"
            }
            preferences.setDarkMode(next)
        }
    }

    fun cycleDefaultFormat() {
        viewModelScope.launch {
            val next = when (_uiState.value.defaultSaveFormat) {
                "pdf" -> "jpg"
                "jpg" -> "png"
                "png" -> "pdf"
                else -> "pdf"
            }
            preferences.setDefaultSaveFormat(next)
        }
    }

    fun toggleAutoEnhance() {
        viewModelScope.launch {
            preferences.setAutoEnhance(!_uiState.value.autoEnhance)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            fileManagerRepository.clearCache()
            val (used, _) = fileManagerRepository.getStorageInfo()
            _uiState.update { it.copy(storageUsed = used) }
        }
    }

    fun upgradePremium() {
        premiumManager.launchPurchaseFlow()
    }
}
