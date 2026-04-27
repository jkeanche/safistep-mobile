package com.safistep.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.local.SafiStepPreferences
import com.safistep.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: SafiStepPreferences
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    var startDestination: String = Routes.ONBOARDING
        private set

    init {
        viewModelScope.launch {
            val onboardingDone = prefs.onboardingDone.first()
            val token          = prefs.token.first()
            startDestination = when {
                !onboardingDone -> Routes.ONBOARDING
                token.isNullOrBlank() -> Routes.PHONE_ENTRY
                else -> Routes.HOME
            }
            _isReady.value = true
        }
    }
}
