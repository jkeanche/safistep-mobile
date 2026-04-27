package com.safistep.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.local.SafiStepPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SafiStepPreferences
) : ViewModel() {
    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingDone() }
    }
}
