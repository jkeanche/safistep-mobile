package com.safistep.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.local.SafiStepPreferences
import com.safistep.data.repository.AuthRepository
import com.safistep.ui.components.*
import com.safistep.ui.screens.home.isAccessibilityServiceEnabled
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SafiStepPreferences,
    private val authRepo: AuthRepository
) : ViewModel() {

    val phone             = prefs.userPhone
    val subscriptionStatus = prefs.subscriptionStatus
    val protectionEnabled  = prefs.protectionEnabled

    var isLoggingOut by mutableStateOf(false)

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            isLoggingOut = true
            authRepo.logout()
            _logoutEvent.emit(Unit)
            isLoggingOut = false
        }
    }

    fun toggleProtection(enabled: Boolean) {
        viewModelScope.launch { prefs.setProtectionEnabled(enabled) }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onBlacklist: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context           = LocalContext.current
    val phone             by viewModel.phone.collectAsState(null)
    val subStatus         by viewModel.subscriptionStatus.collectAsState("inactive")
    val protectionEnabled by viewModel.protectionEnabled.collectAsState(true)
    val accessibilityOn   = isAccessibilityServiceEnabled(context)
    var showLogoutDialog  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLogout() }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = SafiColors.Card,
            title   = { Text("Sign Out") },
            text    = { Text("Are you sure you want to sign out?", style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar)) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout() }) {
                    Text("Sign Out", color = SafiColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = SafiColors.OnSurfaceVar)
                }
            }
        )
    }

    SafiScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            SafiTopBar(title = "Settings", onBack = onBack)
            Spacer(Modifier.height(8.dp))

            // Profile card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(SafiColors.Card, RoundedCornerShape(16.dp))
                    .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(SafiColors.Primary.copy(0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = SafiColors.Primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        phone?.let { "0" + it.removePrefix("254") } ?: "—",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    SafiChip(
                        text  = if (subStatus == "active") "Active" else "Inactive",
                        color = if (subStatus == "active") SafiColors.Success else SafiColors.Hint
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Protection section
            SettingsSection(title = "Protection") {
                SettingsToggleItem(
                    icon     = Icons.Outlined.Shield,
                    title    = "Payment Protection",
                    subtitle = if (protectionEnabled) "Monitoring all STK prompts" else "Paused",
                    checked  = protectionEnabled,
                    onToggle = viewModel::toggleProtection
                )
                SafiDivider(Modifier.padding(horizontal = 16.dp))
                SettingsActionItem(
                    icon     = Icons.Outlined.Block,
                    title    = "Blacklisted Platforms",
                    subtitle = "View and sync blocked platforms",
                    onClick  = onBlacklist
                )
                SafiDivider(Modifier.padding(horizontal = 16.dp))
                SettingsActionItem(
                    icon     = Icons.Outlined.AccessibilityNew,
                    title    = "Accessibility Service",
                    subtitle = if (accessibilityOn) "Enabled ✓" else "Not enabled — tap to fix",
                    subtitleColor = if (accessibilityOn) SafiColors.Success else SafiColors.Warning,
                    onClick  = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
                SafiDivider(Modifier.padding(horizontal = 16.dp))
                SettingsActionItem(
                    icon     = Icons.Outlined.Security,
                    title    = "Draw Over Other Apps",
                    subtitle = if (Settings.canDrawOverlays(context)) "Enabled ✓" else "Required for overlay — tap to enable",
                    subtitleColor = if (Settings.canDrawOverlays(context)) SafiColors.Success else SafiColors.Warning,
                    onClick  = {
                        context.startActivity(Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // About section
            SettingsSection(title = "About") {
                SettingsActionItem(
                    icon     = Icons.Outlined.Info,
                    title    = "App Version",
                    subtitle = "1.0.0"
                )
                SafiDivider(Modifier.padding(horizontal = 16.dp))
                SettingsActionItem(
                    icon    = Icons.Outlined.Gavel,
                    title   = "Privacy Policy",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://safistep.co.ke/privacy")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
                SafiDivider(Modifier.padding(horizontal = 16.dp))
                SettingsActionItem(
                    icon    = Icons.Outlined.HelpOutline,
                    title   = "Help & Support",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://safistep.co.ke/support")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Logout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(SafiColors.DangerContainer, RoundedCornerShape(14.dp))
                    .border(1.dp, SafiColors.Danger.copy(0.2f), RoundedCornerShape(14.dp))
                    .clickable { showLogoutDialog = true }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (viewModel.isLoggingOut) {
                        CircularProgressIndicator(Modifier.size(20.dp), SafiColors.Danger, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Logout, null, tint = SafiColors.Danger, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Sign Out", style = MaterialTheme.typography.titleMedium.copy(color = SafiColors.Danger, fontWeight = FontWeight.SemiBold))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            title,
            style    = MaterialTheme.typography.labelMedium.copy(color = SafiColors.OnSurfaceVar),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SafiColors.Card, RoundedCornerShape(16.dp))
                .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(16.dp))
        ) { content() }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleColor: androidx.compose.ui.graphics.Color = SafiColors.OnSurfaceVar,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SafiColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = subtitleColor))
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = SafiColors.Hint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SafiColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar))
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.Black,
                checkedTrackColor   = SafiColors.Primary,
                uncheckedTrackColor = SafiColors.SurfaceVariant
            )
        )
    }
}
