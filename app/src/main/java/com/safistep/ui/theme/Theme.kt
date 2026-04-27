package com.safistep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ──────────────────────────────────────────────────
object SafiColors {
    val Primary        = Color(0xFF00C896)
    val PrimaryDark    = Color(0xFF009970)
    val PrimaryLight   = Color(0xFF33E0AC)
    val PrimaryContainer = Color(0xFF003D2E)

    val Background     = Color(0xFF0A0F1E)
    val Surface        = Color(0xFF111827)
    val SurfaceVariant = Color(0xFF1A2236)
    val Card           = Color(0xFF1E2A3A)
    val CardBorder     = Color(0xFF263348)

    val OnBackground   = Color(0xFFF0F4FF)
    val OnSurface      = Color(0xFFCDD5E0)
    val OnSurfaceVar   = Color(0xFF8899AA)
    val Hint           = Color(0xFF4A5568)

    val Danger         = Color(0xFFFF4D4D)
    val DangerContainer = Color(0xFF3D0A0A)
    val Warning        = Color(0xFFFFA726)
    val WarningContainer = Color(0xFF3D2500)
    val Success        = Color(0xFF00C896)
    val SuccessContainer = Color(0xFF003D2E)

    val Divider        = Color(0xFF1E2A3A)

    // Gradient stops
    val GradientStart  = Color(0xFF0A0F1E)
    val GradientMid    = Color(0xFF0D1929)
    val GradientEnd    = Color(0xFF111827)

    val PrimaryGradStart = Color(0xFF00C896)
    val PrimaryGradEnd   = Color(0xFF0070FF)
}

// ── Typography ───────────────────────────────────────────────
// Using system fonts for compatibility; replace with custom fonts if added
val SafiTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = SafiColors.OnBackground
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp,
        color = SafiColors.OnBackground
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = SafiColors.OnBackground
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = SafiColors.OnBackground
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = SafiColors.OnBackground
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = SafiColors.OnBackground
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = SafiColors.OnSurface
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = SafiColors.OnSurface
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = SafiColors.OnSurface
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = SafiColors.OnSurfaceVar
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = SafiColors.OnBackground
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = SafiColors.OnSurfaceVar
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = SafiColors.OnSurfaceVar
    )
)

private val DarkColorScheme = darkColorScheme(
    primary            = SafiColors.Primary,
    onPrimary          = Color.Black,
    primaryContainer   = SafiColors.PrimaryContainer,
    onPrimaryContainer = SafiColors.PrimaryLight,
    secondary          = SafiColors.PrimaryLight,
    onSecondary        = Color.Black,
    background         = SafiColors.Background,
    onBackground       = SafiColors.OnBackground,
    surface            = SafiColors.Surface,
    onSurface          = SafiColors.OnSurface,
    surfaceVariant     = SafiColors.SurfaceVariant,
    onSurfaceVariant   = SafiColors.OnSurfaceVar,
    error              = SafiColors.Danger,
    onError            = Color.White,
    errorContainer     = SafiColors.DangerContainer,
    outline            = SafiColors.CardBorder,
    outlineVariant     = SafiColors.Divider,
)

@Composable
fun SafiStepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = SafiTypography,
        content     = content
    )
}
