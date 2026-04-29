package com.safistep.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safistep.ui.screens.auth.*
import com.safistep.ui.screens.home.HomeScreen
import com.safistep.ui.screens.history.HistoryScreen
import com.safistep.ui.screens.onboarding.OnboardingScreen
import com.safistep.ui.screens.settings.SettingsScreen
import com.safistep.ui.screens.subscription.PlanSelectionScreen
import com.safistep.ui.screens.subscription.SubscriptionScreen

object Routes {
    const val ONBOARDING     = "onboarding"
    const val PHONE_ENTRY    = "phone_entry"
    const val OTP_VERIFY     = "otp_verify/{phone}/{purpose}"
    const val SET_PASSWORD   = "set_password"
    const val PLAN_SELECTION = "plan_selection"   // ← NEW: post-registration plan picker
    const val LOGIN          = "login"
    const val HOME           = "home"
    const val HISTORY        = "history"
    const val SUBSCRIPTION   = "subscription"
    const val SETTINGS       = "settings"

    fun otpVerify(phone: String, purpose: String = "registration") =
        "otp_verify/$phone/$purpose"
}

@Composable
fun SafiStepNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                    fadeIn(tween(300))
        },
        exitTransition   = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeOut(tween(150))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                    fadeOut(tween(150))
        }
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.PHONE_ENTRY) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PHONE_ENTRY) {
            PhoneEntryScreen(
                onOtpSent    = { phone, purpose ->
                    navController.navigate(Routes.otpVerify(phone, purpose))
                },
                onLoginClick = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.OTP_VERIFY) { backStack ->
            val phone   = backStack.arguments?.getString("phone") ?: ""
            val purpose = backStack.arguments?.getString("purpose") ?: "registration"
            OtpScreen(
                phone   = phone,
                purpose = purpose,
                onVerified = {
                    navController.navigate(Routes.SET_PASSWORD) {
                        popUpTo(Routes.PHONE_ENTRY) { inclusive = false }
                    }
                },
                onBack  = { navController.popBackStack() }
            )
        }

        composable(Routes.SET_PASSWORD) {
            SetPasswordScreen(
                onSuccess = {
                    // After account creation → show plan picker
                    navController.navigate(Routes.PLAN_SELECTION) {
                        popUpTo(Routes.PHONE_ENTRY) { inclusive = true }
                    }
                }
            )
        }

        // ── Plan selection (post-registration only) ───────────
        composable(Routes.PLAN_SELECTION) {
            PlanSelectionScreen(
                onSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onSuccess  = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRegister = { navController.popBackStack() },
                onForgot   = { navController.navigate(Routes.PHONE_ENTRY) }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateHistory      = { navController.navigate(Routes.HISTORY) },
                onNavigateSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                onNavigateSettings     = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SUBSCRIPTION) {
            SubscriptionScreen(
                onBack    = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack   = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.PHONE_ENTRY) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}