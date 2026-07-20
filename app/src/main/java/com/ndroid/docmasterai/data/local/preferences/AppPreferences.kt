package com.ndroid.docmasterai.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "docmaster_prefs")

@Singleton
class AppPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        val DARK_MODE = stringPreferencesKey("dark_mode") // system, dark, light
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val DEFAULT_SAVE_FORMAT = stringPreferencesKey("default_save_format")
        val DEFAULT_SCAN_QUALITY = intPreferencesKey("default_scan_quality")
        val DEFAULT_OCR_LANGUAGE = stringPreferencesKey("default_ocr_language")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val AUTO_ENHANCE = booleanPreferencesKey("auto_enhance")
        val GRID_VIEW = booleanPreferencesKey("grid_view")
        val TRIAL_START_DATE = longPreferencesKey("trial_start_date")
        val HAS_USED_TRIAL = booleanPreferencesKey("has_used_trial")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val darkMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: "system"
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FIRST_LAUNCH] ?: true
    }

    val defaultSaveFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_SAVE_FORMAT] ?: "pdf"
    }

    val defaultScanQuality: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_SCAN_QUALITY] ?: 90
    }

    val defaultOcrLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_OCR_LANGUAGE] ?: "latin"
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PREMIUM] ?: false
    }

    val autoEnhance: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_ENHANCE] ?: true
    }

    val isGridView: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[GRID_VIEW] ?: true
    }

    val trialStartDate: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[TRIAL_START_DATE] ?: 0L
    }

    val hasUsedTrial: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_USED_TRIAL] ?: false
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN] ?: false
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[DARK_MODE] = mode }
    }

    suspend fun setFirstLaunch(value: Boolean) {
        context.dataStore.edit { it[FIRST_LAUNCH] = value }
    }

    suspend fun setDefaultSaveFormat(format: String) {
        context.dataStore.edit { it[DEFAULT_SAVE_FORMAT] = format }
    }

    suspend fun setDefaultScanQuality(quality: Int) {
        context.dataStore.edit { it[DEFAULT_SCAN_QUALITY] = quality }
    }

    suspend fun setDefaultOcrLanguage(language: String) {
        context.dataStore.edit { it[DEFAULT_OCR_LANGUAGE] = language }
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { it[IS_PREMIUM] = isPremium }
    }

    suspend fun setAutoEnhance(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_ENHANCE] = enabled }
    }

    suspend fun setGridView(isGrid: Boolean) {
        context.dataStore.edit { it[GRID_VIEW] = isGrid }
    }

    suspend fun setTrialStartDate(timestamp: Long) {
        context.dataStore.edit { it[TRIAL_START_DATE] = timestamp }
    }

    suspend fun setHasUsedTrial(value: Boolean) {
        context.dataStore.edit { it[HAS_USED_TRIAL] = value }
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { it[IS_LOGGED_IN] = value }
    }
}
