package com.safistep.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.safistep.ui.components.*
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.isAccessibilityServiceEnabled
import com.safistep.utils.isOverlayPermissionGranted
import com.safistep.utils.openAccessibilitySettings
import com.safistep.utils.openOverlaySettings
import com.safistep.utils.getOverlayInstructions
import com.safistep.utils.needsSpecialOverlayHandling
import kotlinx.coroutines.launch

// ── Data model ────────────────────────────────────────────────

sealed class OnboardingPageData {
    data class InfoPage(
        val icon: ImageVector,
        val iconBg: Color,
        val title: String,
        val description: String,
        val accentColor: Color,
        val badge: String? = null
    ) : OnboardingPageData()

    data class PermissionsPage(
        val accentColor: Color = SafiColors.Primary
    ) : OnboardingPageData()
}

val onboardingPages: List<OnboardingPageData> = listOf(
    OnboardingPageData.InfoPage(
        icon        = Icons.Filled.Shield,
        iconBg      = SafiColors.Primary.copy(alpha = 0.15f),
        title       = "Welcome to SafiStep",
        description = "Your Digital Shield Against Gambling. We provide the willpower when you need it most, helping you stay in control of your finances.",
        accentColor = SafiColors.Primary
    ),
    OnboardingPageData.InfoPage(
        icon        = Icons.Filled.TrackChanges,
        iconBg      = Color(0xFF1A0070).copy(alpha = 0.5f),
        title       = "Smart Detection",
        description = "Real-Time Protection. Our database of 100+ Kenyan betting platforms updates in the background. As soon as a new site launches, SafiStep is already ahead of it.",
        accentColor = Color(0xFF7B61FF),
        badge       = "100+ platforms"
    ),
    OnboardingPageData.InfoPage(
        icon        = Icons.Filled.Tune,
        iconBg      = Color(0xFF1A3500).copy(alpha = 0.5f),
        title       = "STK Protection",
        description = "Smart Intervention. SafiStep identifies betting-related STK prompts and pauses them instantly. We don't see your PIN; we only see the risk, giving you a 15-minute window to rethink.",
        accentColor = Color(0xFF66BB6A),
        badge       = "15-min window"
    ),
    OnboardingPageData.PermissionsPage()
)

// ── Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()

    val accentColor = when (val page = onboardingPages[pagerState.currentPage]) {
        is OnboardingPageData.InfoPage        -> page.accentColor
        is OnboardingPageData.PermissionsPage -> page.accentColor
    }

    SafiScaffold {
        // Background decorative circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color  = accentColor.copy(alpha = 0.04f),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.8f, size.height * 0.15f)
            )
            drawCircle(
                color  = accentColor.copy(alpha = 0.03f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.1f, size.height * 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Skip button (hidden on last page)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(visible = pagerState.currentPage < onboardingPages.size - 1) {
                    TextButton(onClick = {
                        scope.launch { viewModel.completeOnboarding(); onFinish() }
                    }) {
                        Text(
                            "Skip",
                            style = MaterialTheme.typography.labelLarge.copy(color = SafiColors.OnSurfaceVar)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Page content
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { index ->
                when (val page = onboardingPages[index]) {
                    is OnboardingPageData.InfoPage        -> InfoPageContent(page = page)
                    is OnboardingPageData.PermissionsPage -> PermissionsPageContent(
                        onFinish = {
                            scope.launch { viewModel.completeOnboarding(); onFinish() }
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Page indicators
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue   = if (isSelected) 28.dp else 8.dp,
                        animationSpec = tween(300),
                        label         = "indicator_width"
                    )
                    val color by animateColorAsState(
                        targetValue   = if (isSelected) accentColor else SafiColors.Hint,
                        animationSpec = tween(300),
                        label         = "indicator_color"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // CTA Button (hidden on last page — it has its own buttons)
            val isLastPage = pagerState.currentPage == onboardingPages.size - 1
            AnimatedVisibility(visible = !isLastPage) {
                SafiButton(
                    text     = "Next",
                    onClick  = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon     = Icons.Filled.ArrowForward
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Info page content ─────────────────────────────────────────

@Composable
private fun InfoPageContent(page: OnboardingPageData.InfoPage) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with optional badge
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(page.iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = page.icon,
                    contentDescription = null,
                    tint               = page.accentColor,
                    modifier           = Modifier.size(56.dp)
                )
            }
            if (page.badge != null) {
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp, y = (-4).dp)
                        .background(page.accentColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text  = page.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 9.sp
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text      = page.title,
            style     = MaterialTheme.typography.displayMedium.copy(
                color      = SafiColors.OnBackground,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text      = page.description,
            style     = MaterialTheme.typography.bodyLarge.copy(
                color      = SafiColors.OnSurfaceVar,
                lineHeight = 26.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ── Permissions page content ──────────────────────────────────

@Composable
private fun PermissionsPageContent(onFinish: () -> Unit) {
    val context = LocalContext.current
    var accessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var overlayGranted       by remember { mutableStateOf(isOverlayPermissionGranted(context)) }

    // Dialog state
    var showAccessibilityGuide by remember { mutableStateOf(false) }
    var showOverlayGuide       by remember { mutableStateOf(false) }

    // Re-check permissions when user returns from settings
    LaunchedEffect(Unit) {
        // Poll until both granted or user proceeds
        while (true) {
            kotlinx.coroutines.delay(500)
            accessibilityGranted = isAccessibilityServiceEnabled(context)
            overlayGranted       = isOverlayPermissionGranted(context)
        }
    }

    val bothGranted = accessibilityGranted && overlayGranted

    // ── Accessibility guide dialog ─────────────────────────────
    if (showAccessibilityGuide) {
        AlertDialog(
            onDismissRequest = { showAccessibilityGuide = false },
            containerColor   = SafiColors.Card,
            icon = {
                Icon(
                    Icons.Outlined.AccessibilityNew,
                    contentDescription = null,
                    tint     = SafiColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Enable Accessibility",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "On the next screen, follow these steps:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    StepRow(number = "1", text = "Look for \"SafiStep\" or \"SafiStep Payment Guardian\" in the list")
                    StepRow(number = "2", text = "Tap it, then tap \"Use SafiStep\"")
                    StepRow(number = "3", text = "Tap \"Allow\" on the confirmation dialog")
                    Text(
                        "If you see \"Restricted setting\" tap the ⋮ menu (top-right) → Allow restricted settings.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Warning)
                    )
                }
            },
            confirmButton = {
                SafiButton(
                    text     = "Open Settings",
                    onClick  = {
                        showAccessibilityGuide = false
                        openAccessibilitySettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityGuide = false }) {
                    Text("Cancel", color = SafiColors.OnSurfaceVar)
                }
            }
        )
    }

    // ── Overlay guide dialog ───────────────────────────────────
    if (showOverlayGuide) {
        AlertDialog(
            onDismissRequest = { showOverlayGuide = false },
            containerColor   = SafiColors.Card,
            icon = {
                Icon(
                    Icons.Outlined.PictureInPicture,
                    contentDescription = null,
                    tint     = SafiColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Allow Display Over Apps",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "On the next screen, follow these steps:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    StepRow(number = "1", text = "Find \"SafiStep\" in the list")
                    StepRow(number = "2", text = "Toggle \"Allow display over other apps\" ON")
                    
                    if (needsSpecialOverlayHandling()) {
                        Text(
                            "Special instructions for your device:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            getOverlayInstructions(),
                            style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Warning)
                        )
                    } else {
                        Text(
                            "If the toggle is greyed out, tap the ⋮ menu → Allow restricted settings first.",
                            style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Warning)
                        )
                    }
                }
            },
            confirmButton = {
                SafiButton(
                    text     = "Open Settings",
                    onClick  = {
                        showOverlayGuide = false
                        openOverlaySettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { showOverlayGuide = false }) {
                    Text("Cancel", color = SafiColors.OnSurfaceVar)
                }
            }
        )
    }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(SafiColors.Primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.AdminPanelSettings,
                contentDescription = null,
                tint               = SafiColors.Primary,
                modifier           = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text      = "One Last Step",
            style     = MaterialTheme.typography.displayMedium.copy(
                color      = SafiColors.OnBackground,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = "To protect you, SafiStep needs Accessibility and Overlay permissions. This allows us to detect betting prompts and show the timer. Your data is encrypted and never shared.",
            style     = MaterialTheme.typography.bodyLarge.copy(
                color      = SafiColors.OnSurfaceVar,
                lineHeight = 26.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Accessibility permission row
        PermissionRow(
            icon        = Icons.Outlined.AccessibilityNew,
            title       = "Accessibility Service",
            description = "Detects betting STK prompts",
            granted     = accessibilityGranted,
            onGrant     = {
                showAccessibilityGuide = true
            }
        )

        Spacer(Modifier.height(12.dp))

        // Overlay permission row
        PermissionRow(
            icon        = Icons.Outlined.PictureInPicture,
            title       = "Display Over Apps",
            description = "Shows the intervention timer",
            granted     = overlayGranted,
            onGrant     = {
                showOverlayGuide = true
            }
        )

        Spacer(Modifier.height(24.dp))

        // Privacy note
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier              = Modifier
                .background(SafiColors.Primary.copy(0.07f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint     = SafiColors.Primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "We never read your PIN or personal data",
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = SafiColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(Modifier.height(28.dp))

        // CTA
        SafiButton(
            text     = if (bothGranted) "All Set — Get Started" else "Grant Permissions & Continue",
            onClick  = onFinish,
            modifier = Modifier.fillMaxWidth(),
            icon     = if (bothGranted) Icons.Filled.CheckCircle else Icons.Filled.ArrowForward
        )

        if (!bothGranted) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Skip for now",
                    style = MaterialTheme.typography.labelMedium.copy(color = SafiColors.Hint)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (granted) SafiColors.SuccessContainer else SafiColors.SurfaceVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = if (granted) SafiColors.Success.copy(0.4f) else SafiColors.CardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !granted, onClick = onGrant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (granted) SafiColors.Success.copy(0.15f) else SafiColors.Primary.copy(0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint     = if (granted) SafiColors.Success else SafiColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar)
            )
        }
        Spacer(Modifier.width(8.dp))
        if (granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                tint     = SafiColors.Success,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .background(SafiColors.Primary.copy(0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text  = "Enable",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color      = SafiColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

// ── Step row helper ───────────────────────────────────────────

@Composable
private fun StepRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(SafiColors.Primary.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = SafiColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurface)
        )
    }
}