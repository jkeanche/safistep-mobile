package com.safistep.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.safistep.ui.components.*
import com.safistep.ui.theme.SafiColors
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val iconBg: Color,
    val title: String,
    val description: String,
    val accentColor: Color
)

val onboardingPages = listOf(
    OnboardingPage(
        icon        = Icons.Filled.Shield,
        iconBg      = SafiColors.Primary.copy(alpha = 0.15f),
        title       = "Protect Your Money",
        description = "SafiStep watches every M-Pesa STK payment prompt and alerts you instantly before money reaches a flagged betting platform.",
        accentColor = SafiColors.Primary
    ),
    OnboardingPage(
        icon        = Icons.Filled.TrackChanges,
        iconBg      = Color(0xFF1A0070).copy(alpha = 0.5f),
        title       = "Smart Detection",
        description = "Our live blacklist of Kenyan betting platforms updates automatically. SafiStep catches new platforms the moment they are added.",
        accentColor = Color(0xFF7B61FF)
    ),
    OnboardingPage(
        icon        = Icons.Filled.Tune,
        iconBg      = Color(0xFF1A3500).copy(alpha = 0.5f),
        title       = "You Stay in Control",
        description = "When a suspicious payment is detected, you choose — block it or proceed. No payment is ever stopped without your full awareness.",
        accentColor = Color(0xFF66BB6A)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()

    SafiScaffold {
        // Background decorative circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val page = onboardingPages[pagerState.currentPage]
            drawCircle(
                color  = page.accentColor.copy(alpha = 0.04f),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.8f, size.height * 0.15f)
            )
            drawCircle(
                color  = page.accentColor.copy(alpha = 0.03f),
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
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    scope.launch { viewModel.completeOnboarding(); onFinish() }
                }) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelLarge.copy(color = SafiColors.OnSurfaceVar)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Page content
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                OnboardingPage(page = onboardingPages[page])
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
                        targetValue   = if (isSelected) SafiColors.Primary else SafiColors.Hint,
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

            // CTA Button
            val isLastPage = pagerState.currentPage == onboardingPages.size - 1
            SafiButton(
                text      = if (isLastPage) "Get Started" else "Next",
                onClick   = {
                    scope.launch {
                        if (isLastPage) {
                            viewModel.completeOnboarding()
                            onFinish()
                        } else {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier  = Modifier.fillMaxWidth(),
                icon      = if (isLastPage) Icons.Filled.ArrowForward else null
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(page.iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint        = page.accentColor,
                modifier    = Modifier.size(56.dp)
            )
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
