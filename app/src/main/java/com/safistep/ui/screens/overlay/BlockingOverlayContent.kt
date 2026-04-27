package com.safistep.ui.screens.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.safistep.ui.components.SafiButton
import com.safistep.ui.components.SafiOutlinedButton
import com.safistep.ui.theme.SafiColors

@Composable
fun BlockingOverlayContent(
    platformName: String,
    platformCategory: String,
    amount: Double?,
    paybill: String?,
    onBlock: () -> Unit,
    onProceed: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning")
    val warningScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "warning_scale"
    )
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label         = "warning_alpha"
    )

    var showConfirmProceed by remember { mutableStateOf(false) }

    // Full screen scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        // Decorative background circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color  = SafiColors.Danger.copy(alpha = 0.06f),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.5f, size.height * 0.3f)
            )
            drawCircle(
                color  = SafiColors.Danger.copy(alpha = 0.04f),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.5f, size.height * 0.6f)
            )
        }

        AnimatedVisibility(
            visible = !showConfirmProceed,
            enter   = scaleIn(tween(300)) + fadeIn(tween(300)),
            exit    = scaleOut(tween(200)) + fadeOut(tween(200))
        ) {
            MainBlockCard(
                platformName     = platformName,
                platformCategory = platformCategory,
                amount           = amount,
                paybill          = paybill,
                warningScale     = warningScale,
                warningAlpha     = warningAlpha,
                onBlock          = onBlock,
                onProceed        = { showConfirmProceed = true }
            )
        }

        AnimatedVisibility(
            visible = showConfirmProceed,
            enter   = scaleIn(tween(300)) + fadeIn(tween(300)),
            exit    = scaleOut(tween(200)) + fadeOut(tween(200))
        ) {
            ConfirmProceedCard(
                platformName = platformName,
                amount       = amount,
                onConfirm    = onProceed,
                onCancel     = { showConfirmProceed = false }
            )
        }
    }
}

@Composable
private fun MainBlockCard(
    platformName: String,
    platformCategory: String,
    amount: Double?,
    paybill: String?,
    warningScale: Float,
    warningAlpha: Float,
    onBlock: () -> Unit,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(SafiColors.Surface, RoundedCornerShape(28.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(listOf(SafiColors.Danger.copy(0.6f), SafiColors.Danger.copy(0.2f))),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Warning icon with pulse
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(warningScale)
                    .alpha(warningAlpha * 0.3f)
                    .background(SafiColors.Danger.copy(alpha = 0.15f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(SafiColors.DangerContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint     = SafiColors.Danger,
                    modifier = Modifier.size(40.dp).scale(warningScale)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Header
        Text(
            "⚠ Betting Platform Detected",
            style = MaterialTheme.typography.titleLarge.copy(
                color      = SafiColors.Danger,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "This payment is going to a flagged ${platformCategory} platform",
            style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // Platform info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SafiColors.SurfaceVariant, RoundedCornerShape(16.dp))
                .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            InfoRow(label = "Platform", value = platformName, valueColor = SafiColors.Danger)
            if (paybill != null) {
                Spacer(Modifier.height(8.dp))
                InfoRow(label = "Paybill", value = paybill)
            }
            if (amount != null) {
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label      = "Amount",
                    value      = "KSh ${amount.toInt()}",
                    valueColor = SafiColors.Warning
                )
            }
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Category", value = platformCategory.replaceFirstChar { it.uppercase() })
        }

        Spacer(Modifier.height(12.dp))

        // SafiStep branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(SafiColors.Primary.copy(0.08f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Filled.Shield, null, tint = SafiColors.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Protected by SafiStep",
                style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.Primary, fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Primary action
        SafiButton(
            text     = "Block This Payment",
            onClick  = onBlock,
            modifier = Modifier.fillMaxWidth(),
            icon     = Icons.Filled.Block
        )

        Spacer(Modifier.height(12.dp))

        // Secondary action (proceed anyway)
        TextButton(
            onClick  = onProceed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Proceed Anyway",
                style = MaterialTheme.typography.labelLarge.copy(color = SafiColors.OnSurfaceVar)
            )
        }
    }
}

@Composable
private fun ConfirmProceedCard(
    platformName: String,
    amount: Double?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .background(SafiColors.Surface, RoundedCornerShape(28.dp))
            .border(1.dp, SafiColors.Warning.copy(0.4f), RoundedCornerShape(28.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.HelpOutline, null, tint = SafiColors.Warning, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Are you sure?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You are about to send${if (amount != null) " KSh ${amount.toInt()}" else " money"} to $platformName, a flagged betting platform. This payment will NOT be reversed.",
            style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        // Big red confirm
        Button(
            onClick  = onConfirm,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = SafiColors.Danger)
        ) {
            Text(
                "Yes, I understand — Proceed",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White)
            )
        }
        Spacer(Modifier.height(12.dp))
        SafiOutlinedButton("← Go Back", onCancel, Modifier.fillMaxWidth())
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = SafiColors.OnBackground) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = valueColor, fontWeight = FontWeight.SemiBold))
    }
}
