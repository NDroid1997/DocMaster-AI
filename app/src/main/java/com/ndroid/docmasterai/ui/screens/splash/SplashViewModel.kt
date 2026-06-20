package com.ndroid.docmasterai.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.ndroid.docmasterai.data.local.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    preferences: AppPreferences
) : ViewModel() {
    val isFirstLaunch = preferences.isFirstLaunch
}
