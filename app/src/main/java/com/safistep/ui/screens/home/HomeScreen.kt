package com.safistep.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.local.SafiStepPreferences
import com.safistep.data.local.entity.BlockHistoryEntity
import com.safistep.data.repository.*
import com.safistep.service.scheduleSyncWork
import com.safistep.ui.components.*
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.ApiResult
import com.safistep.utils.formatCurrency
import com.safistep.utils.formatRelativeTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════
// HomeViewModel
// ══════════════════════════════════════════════════════════════

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: SafiStepPreferences,
    private val blacklistRepo: BlacklistRepository,
    private val subscriptionRepo: SubscriptionRepository,
    private val reportRepo: ReportRepository
) : ViewModel() {

    val phone             = prefs.userPhone
    val subscriptionStatus = subscriptionRepo.subscriptionStatus
    val subscriptionExpires = subscriptionRepo.subscriptionExpires
    val protectionEnabled = prefs.protectionEnabled
    val recentHistory     = reportRepo.recentHistory
    val blockedCount      = reportRepo.blockedCount
    val totalSaved        = reportRepo.totalSaved

    var isSyncing by mutableStateOf(false)
    var syncMessage by mutableStateOf<String?>(null)

    init {
        refreshSubscriptionStatus()
        sync()
    }

    fun refreshSubscriptionStatus() {
        viewModelScope.launch { subscriptionRepo.getStatus() }
    }

    fun sync() {
        viewModelScope.launch {
            isSyncing = true
            when (val result = blacklistRepo.syncIfNeeded()) {
                is SyncResult.Updated   -> syncMessage = "Blacklist updated (${result.count} platforms)"
                is SyncResult.UpToDate  -> syncMessage = null
                is SyncResult.Error     -> syncMessage = null
            }
            reportRepo.syncPendingReports()
            isSyncing = false
        }
    }

    fun toggleProtection(enabled: Boolean) {
        viewModelScope.launch { prefs.setProtectionEnabled(enabled) }
    }
}

// ══════════════════════════════════════════════════════════════
// HomeScreen
// ══════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(
    onNavigateHistory: () -> Unit,
    onNavigateSubscription: () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context            = LocalContext.current
    val phone              by viewModel.phone.collectAsState(null)
    val subStatus          by viewModel.subscriptionStatus.collectAsState("inactive")
    val subExpires         by viewModel.subscriptionExpires.collectAsState(null)
    val protectionEnabled  by viewModel.protectionEnabled.collectAsState(true)
    val recentHistory      by viewModel.recentHistory.collectAsState(emptyList())
    val blockedCount       by viewModel.blockedCount.collectAsState(0)
    val totalSaved         by viewModel.totalSaved.collectAsState(null)
    val isSubscribed       = subStatus == "active"
    val accessibilityEnabled = isAccessibilityServiceEnabled(context)

    SafiScaffold {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(SafiColors.Primary.copy(0.03f), size.width * 0.9f, Offset(size.width * 0.9f, 0f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            // ── Top Bar ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text  = "SafiStep",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color      = SafiColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    phone?.let {
                        Text(
                            text  = it.let { p -> "0" + p.removePrefix("254") },
                            style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (viewModel.isSyncing) {
                        CircularProgressIndicator(Modifier.size(20.dp), SafiColors.Primary, strokeWidth = 2.dp)
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Outlined.Settings, null, tint = SafiColors.OnSurfaceVar, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // ── Subscription Banner ────────────────────────────
            AnimatedVisibility(
                visible = !isSubscribed,
                enter   = slideInVertically() + fadeIn()
            ) {
                SubscriptionBanner(onClick = onNavigateSubscription)
            }

            // ── Accessibility Warning ──────────────────────────
            AnimatedVisibility(
                visible = isSubscribed && !accessibilityEnabled,
                enter   = slideInVertically() + fadeIn()
            ) {
                AccessibilityBanner(context = context)
            }

            Spacer(Modifier.height(8.dp))

            // ── Shield Card ────────────────────────────────────
            ShieldStatusCard(
                isActive      = protectionEnabled && accessibilityEnabled,
                isSubscribed  = isSubscribed,
                blockedCount  = blockedCount,
                totalSaved    = totalSaved,
                modifier      = Modifier.padding(horizontal = 20.dp)
            )

            // ── Protection Toggle ──────────────────────────────
            if (isSubscribed && accessibilityEnabled) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(SafiColors.Card, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Payment Protection", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (protectionEnabled) "Actively monitoring" else "Paused",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (protectionEnabled) SafiColors.Success else SafiColors.OnSurfaceVar
                            )
                        )
                    }
                    Switch(
                        checked         = protectionEnabled,
                        onCheckedChange = viewModel::toggleProtection,
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor     = Color.Black,
                            checkedTrackColor     = SafiColors.Primary,
                            uncheckedThumbColor   = SafiColors.OnSurfaceVar,
                            uncheckedTrackColor   = SafiColors.SurfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Subscription expiry ────────────────────────────
            if (isSubscribed && subExpires != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = SafiColors.Hint, modifier = Modifier.size(14.dp))
                    Text(
                        "Subscription active until ${subExpires?.take(10)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Hint)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Quick Actions ──────────────────────────────────
            SectionHeader("Quick Actions")
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon     = Icons.Outlined.History,
                    label    = "Block History",
                    badge    = if (blockedCount > 0) blockedCount.toString() else null,
                    modifier = Modifier.weight(1f),
                    onClick  = onNavigateHistory
                )
                QuickActionCard(
                    icon    = Icons.Outlined.CreditCard,
                    label   = "Subscription",
                    badge   = if (!isSubscribed) "!" else null,
                    badgeColor = SafiColors.Warning,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateSubscription
                )
                QuickActionCard(
                    icon    = Icons.Outlined.Refresh,
                    label   = "Sync List",
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::sync
                )
            }

            // ── Recent Activity ────────────────────────────────
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title      = "Recent Activity",
                actionText = if (recentHistory.isNotEmpty()) "See all" else null,
                onAction   = onNavigateHistory
            )
            Spacer(Modifier.height(12.dp))

            if (recentHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(SafiColors.Card, RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon        = Icons.Outlined.CheckCircle,
                        title       = "All Clear",
                        description = "No betting payment attempts detected yet. SafiStep is watching."
                    )
                }
            } else {
                Column(
                    modifier              = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    recentHistory.take(5).forEach { event ->
                        BlockEventCard(event = event)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Quick Action Card ─────────────────────────────────────────
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = SafiColors.Danger,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(SafiColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Icon(icon, null, tint = SafiColors.Primary, modifier = Modifier.size(24.dp))
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(16.dp)
                            .background(badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(badge, style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.OnSurface), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ── Block Event Card ──────────────────────────────────────────
@Composable
fun BlockEventCard(event: BlockHistoryEntity) {
    val isBlocked = event.actionTaken == "blocked"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafiColors.Card, RoundedCornerShape(14.dp))
            .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isBlocked) SafiColors.DangerContainer else SafiColors.WarningContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isBlocked) Icons.Filled.Block else Icons.Filled.Warning,
                null,
                tint     = if (isBlocked) SafiColors.Danger else SafiColors.Warning,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                event.platformName ?: event.paybillDetected ?: "Unknown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                buildString {
                    if (event.amountAttempted != null) append("KSh ${event.amountAttempted.toInt()} · ")
                    append(if (isBlocked) "Blocked" else "Overrode")
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isBlocked) SafiColors.Danger else SafiColors.Warning
                )
            )
        }
        Text(
            formatRelativeTime(event.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.Hint)
        )
    }
}

// ── Subscription Banner ───────────────────────────────────────
@Composable
private fun SubscriptionBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(
                Brush.horizontalGradient(listOf(SafiColors.WarningContainer, Color(0xFF2D1800))),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, SafiColors.Warning.copy(0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Stars, null, tint = SafiColors.Warning, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Activate Protection", style = MaterialTheme.typography.titleMedium.copy(color = SafiColors.Warning, fontWeight = FontWeight.SemiBold))
            Text("KSh 200/month · Cancel anytime", style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Warning.copy(0.7f)))
        }
        Icon(Icons.Filled.ArrowForwardIos, null, tint = SafiColors.Warning, modifier = Modifier.size(16.dp))
    }
}

// ── Accessibility Banner ──────────────────────────────────────
@Composable
private fun AccessibilityBanner(context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(SafiColors.DangerContainer, RoundedCornerShape(14.dp))
            .border(1.dp, SafiColors.Danger.copy(0.3f), RoundedCornerShape(14.dp))
            .clickable {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Warning, null, tint = SafiColors.Danger, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Enable Accessibility", style = MaterialTheme.typography.titleMedium.copy(color = SafiColors.Danger, fontWeight = FontWeight.SemiBold))
            Text("Tap to enable SafiStep in accessibility settings", style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Danger.copy(0.7f)))
        }
        Icon(Icons.Filled.ArrowForwardIos, null, tint = SafiColors.Danger, modifier = Modifier.size(16.dp))
    }
}

// ── Helpers ───────────────────────────────────────────────────
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/com.safistep.service.SafiStepAccessibilityService"
    return try {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        enabled.split(":").any { it.equals(service, ignoreCase = true) }
    } catch (_: Exception) { false }
}
