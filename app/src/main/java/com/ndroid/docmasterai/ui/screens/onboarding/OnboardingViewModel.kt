package com.ndroid.docmasterai.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndroid.docmasterai.data.local.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setFirstLaunch(false)
        }
    }
}
