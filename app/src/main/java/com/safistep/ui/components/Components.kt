package com.safistep.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.safistep.ui.theme.SafiColors

// ── Primary Button ────────────────────────────────────────────
@Composable
fun SafiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f, targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmer_offset"
    )

    Button(
        onClick    = { if (!loading) onClick() },
        enabled    = enabled && !loading,
        modifier   = modifier.height(54.dp),
        shape      = RoundedCornerShape(14.dp),
        colors     = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = SafiColors.SurfaceVariant
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled && !loading)
                        Brush.horizontalGradient(
                            colors = listOf(SafiColors.Primary, SafiColors.PrimaryGradEnd)
                        )
                    else Brush.horizontalGradient(
                        colors = listOf(SafiColors.SurfaceVariant, SafiColors.SurfaceVariant)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icon?.let { Icon(it, null, modifier = Modifier.size(18.dp), tint = Color.Black) }
                    Text(
                        text  = text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color      = if (enabled) Color.Black else SafiColors.OnSurfaceVar,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp
                        )
                    )
                }
            }
        }
    }
}

// ── Ghost / Outlined Button ────────────────────────────────────
@Composable
fun SafiOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = SafiColors.Primary
) {
    OutlinedButton(
        onClick   = onClick,
        enabled   = enabled,
        modifier  = modifier.height(54.dp),
        shape     = RoundedCornerShape(14.dp),
        border    = BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
        colors    = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let { Icon(it, null, modifier = Modifier.size(18.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge.copy(color = color))
        }
    }
}

// ── Text Field ────────────────────────────────────────────────
@Composable
fun SafiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val finalTransformation = if (isPassword && passwordVisible)
        VisualTransformation.None else visualTransformation

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color      = SafiColors.OnSurfaceVar,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            enabled       = enabled,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            placeholder   = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.Hint)
                )
            },
            leadingIcon  = leadingIcon,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = SafiColors.OnSurfaceVar
                        )
                    }
                }
            } else trailingIcon,
            visualTransformation = finalTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction    = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone   = { onImeAction() },
                onNext   = { onImeAction() },
                onSearch = { onImeAction() }
            ),
            isError  = error != null,
            maxLines = maxLines,
            colors   = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = SafiColors.Primary,
                unfocusedBorderColor    = SafiColors.CardBorder,
                errorBorderColor        = SafiColors.Danger,
                focusedContainerColor   = SafiColors.SurfaceVariant,
                unfocusedContainerColor = SafiColors.SurfaceVariant,
                cursorColor             = SafiColors.Primary,
                focusedTextColor        = SafiColors.OnBackground,
                unfocusedTextColor      = SafiColors.OnSurface,
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        AnimatedVisibility(visible = error != null) {
            Text(
                text     = error ?: "",
                style    = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Danger),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

// ── OTP Input Row ─────────────────────────────────────────────
@Composable
fun OtpInputRow(
    otp: String,
    onOtpChange: (String) -> Unit,
    length: Int = 6,
    hasError: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    // Open keyboard immediately when screen appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(contentAlignment = Alignment.Center) {

        // Hidden BasicTextField — the only thing that actually receives keyboard input
        BasicTextField(
            value         = otp,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() }.take(length)
                onOtpChange(filtered)
            },
            modifier      = Modifier
                .focusRequester(focusRequester)
                .size(1.dp)
                .alpha(0.01f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {}),
            singleLine      = true
        )

        // Visual digit boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { focusRequester.requestFocus() }
        ) {
            repeat(length) { index ->
                val char      = otp.getOrNull(index)
                val isFocused = otp.length == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(
                            color = SafiColors.SurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = when {
                                hasError     -> SafiColors.Danger
                                isFocused    -> SafiColors.Primary
                                char != null -> SafiColors.Primary.copy(alpha = 0.4f)
                                else         -> SafiColors.CardBorder
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (char != null) {
                        Text(
                            text  = char.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color      = SafiColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    // Blinking cursor at current position
                    if (isFocused) {
                        val cursorAlpha by rememberInfiniteTransition(label = "cursor")
                            .animateFloat(
                                initialValue  = 1f,
                                targetValue   = 0f,
                                animationSpec = infiniteRepeatable(
                                    tween(600), RepeatMode.Reverse
                                ),
                                label = "cursor_alpha"
                            )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .alpha(cursorAlpha)
                                .background(SafiColors.Primary)
                        )
                    }
                }
            }
        }
    }
}

// ── Shield Status Card ────────────────────────────────────────
@Composable
fun ShieldStatusCard(
    isActive: Boolean,
    isSubscribed: Boolean,
    blockedCount: Int,
    totalSaved: Double?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SafiColors.SurfaceVariant, SafiColors.Card)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isActive && isSubscribed) SafiColors.Primary.copy(alpha = 0.5f) else SafiColors.CardBorder,
                        SafiColors.CardBorder
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Shield icon with pulse
            Box(contentAlignment = Alignment.Center) {
                if (isActive && isSubscribed) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .background(
                                SafiColors.Primary.copy(alpha = 0.08f),
                                CircleShape
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = if (isActive && isSubscribed)
                                SafiColors.Primary.copy(alpha = 0.15f)
                            else SafiColors.SurfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive && isSubscribed)
                            Icons.Filled.Shield else Icons.Outlined.ShieldMoon,
                        contentDescription = null,
                        tint = if (isActive && isSubscribed) SafiColors.Primary else SafiColors.Hint,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text  = when {
                    !isSubscribed -> "Protection Inactive"
                    isActive      -> "Protection Active"
                    else          -> "SafiStep Guard Paused"
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color      = if (isActive && isSubscribed) SafiColors.Primary else SafiColors.OnSurfaceVar,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text  = when {
                    !isSubscribed -> "Subscribe to activate payment protection"
                    isActive      -> "Monitoring M-Pesa STK payments"
                    else          -> "Enable accessibility service to protect"
                },
                style     = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp)
            )

            if (isSubscribed) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = blockedCount.toString(),
                        label = "Blocked",
                        color = SafiColors.Danger
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(SafiColors.Divider)
                    )
                    StatItem(
                        value = if (totalSaved != null && totalSaved > 0)
                            "KSh ${totalSaved.toInt()}" else "—",
                        label = "Saved",
                        color = SafiColors.Success
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.typography.titleLarge.copy(
                color      = color,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.OnSurfaceVar)
        )
    }
}

// ── Top App Bar ───────────────────────────────────────────────
@Composable
fun SafiTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = SafiColors.OnBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 4.dp else 8.dp)
        )
        actions()
    }
}

// ── Section Header ────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        if (actionText != null) {
            TextButton(onClick = onAction) {
                Text(
                    actionText,
                    style = MaterialTheme.typography.labelMedium.copy(color = SafiColors.Primary)
                )
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SafiColors.SurfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = SafiColors.Hint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            style     = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar),
            textAlign = TextAlign.Center
        )
    }
}

// ── Loading Overlay ───────────────────────────────────────────
@Composable
fun FullScreenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SafiColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SafiColors.Primary, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Loading…",
                style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar)
            )
        }
    }
}

// ── Gradient Background Scaffold ──────────────────────────────
@Composable
fun SafiScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SafiColors.GradientStart,
                        SafiColors.GradientMid,
                        SafiColors.GradientEnd
                    )
                )
            ),
        content = content
    )
}

// ── Info chip/badge ───────────────────────────────────────────
@Composable
fun SafiChip(text: String, color: Color = SafiColors.Primary) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = color,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

// ── Divider ───────────────────────────────────────────────────
@Composable
fun SafiDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier  = modifier,
        thickness = 1.dp,
        color     = SafiColors.Divider
    )
}