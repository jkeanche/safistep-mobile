package com.safistep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.safistep.ui.navigation.SafiStepNavGraph
import com.safistep.ui.theme.SafiStepTheme
import com.safistep.ui.screens.auth.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val splashVm: SplashViewModel = hiltViewModel()
            val isReady by splashVm.isReady.collectAsState()

            splash.setKeepOnScreenCondition { !isReady }

            SafiStepTheme {
                if (isReady) {
                    SafiStepNavGraph(startDestination = splashVm.startDestination)
                }
            }
        }
    }
}
