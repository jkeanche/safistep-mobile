package com.safistep.ui.screens.subscription

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.repository.SubscriptionRepository
import com.safistep.ui.components.*
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════
// ViewModel
// ══════════════════════════════════════════════════════════════

sealed class SubscriptionUiState {
    object Idle : SubscriptionUiState()
    object Initiating : SubscriptionUiState()
    data class AwaitingPayment(val checkoutRequestId: String) : SubscriptionUiState()
    object Polling : SubscriptionUiState()
    object Success : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepo: SubscriptionRepository
) : ViewModel() {

    val subscriptionStatus  = subscriptionRepo.subscriptionStatus
    val subscriptionExpires = subscriptionRepo.subscriptionExpires

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Idle)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { subscriptionRepo.getStatus() }
    }

    fun initiate() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Initiating
            when (val result = subscriptionRepo.initiate()) {
                is ApiResult.Success -> {
                    _uiState.value = SubscriptionUiState.AwaitingPayment(result.data.checkoutRequestId)
                    startPolling()
                }
                is ApiResult.Error        -> _uiState.value = SubscriptionUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = SubscriptionUiState.Error("No internet connection")
            }
        }
    }

    /** Poll subscription status every 5s for up to 2 minutes */
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Polling
            repeat(24) { // 24 × 5s = 2 min
                delay(5_000)
                val statusResult = subscriptionRepo.getStatus()
                if (statusResult is ApiResult.Success &&
                    statusResult.data.subscriptionStatus == "active") {
                    _uiState.value = SubscriptionUiState.Success
                    return@launch
                }
            }
            // Timed out — let user retry
            _uiState.value = SubscriptionUiState.Error(
                "Payment not confirmed yet. If you paid, it will activate shortly."
            )
        }
    }

    fun reset() {
        pollingJob?.cancel()
        _uiState.value = SubscriptionUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

// ══════════════════════════════════════════════════════════════
// Screen
// ══════════════════════════════════════════════════════════════

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val subStatus     by viewModel.subscriptionStatus.collectAsState("inactive")
    val subExpires    by viewModel.subscriptionExpires.collectAsState(null)
    val isSubscribed  = subStatus == "active"

    // Navigate out on success
    LaunchedEffect(uiState) {
        if (uiState is SubscriptionUiState.Success) {
            delay(2000)
            onSuccess()
        }
    }

    SafiScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            SafiTopBar(title = "Subscription", onBack = onBack)
            Spacer(Modifier.height(8.dp))

            when (uiState) {
                is SubscriptionUiState.Polling,
                is SubscriptionUiState.AwaitingPayment -> AwaitingPaymentView()

                is SubscriptionUiState.Success -> SuccessView()

                is SubscriptionUiState.Error -> {
                    ErrorView(
                        message = (uiState as SubscriptionUiState.Error).message,
                        onRetry = { viewModel.reset() }
                    )
                }

                else -> {
                    if (isSubscribed) {
                        ActiveSubscriptionView(
                            expiresAt = subExpires,
                            onRenew   = { viewModel.initiate() },
                            isLoading = uiState is SubscriptionUiState.Initiating
                        )
                    } else {
                        InactiveSubscriptionView(
                            onSubscribe = { viewModel.initiate() },
                            isLoading   = uiState is SubscriptionUiState.Initiating
                        )
                    }
                }
            }
        }
    }
}

// ── Inactive — main purchase view ────────────────────────────
@Composable
private fun InactiveSubscriptionView(onSubscribe: () -> Unit, isLoading: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {

        // Hero card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(SafiColors.SurfaceVariant, SafiColors.Card)),
                    RoundedCornerShape(24.dp)
                )
                .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(SafiColors.Primary.copy(0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = SafiColors.Primary, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("SafiStep Pro", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text("KSh ", style = MaterialTheme.typography.bodyLarge.copy(color = SafiColors.Primary))
                    Text(
                        "200",
                        style = MaterialTheme.typography.displayLarge.copy(
                            color      = SafiColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(" /month", style = MaterialTheme.typography.bodyLarge.copy(color = SafiColors.OnSurfaceVar), modifier = Modifier.align(Alignment.Bottom).padding(bottom = 8.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("Billed monthly via M-Pesa", style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Features
        val features = listOf(
            Triple(Icons.Filled.Shield, "Real-time STK Payment Monitoring", "Every M-Pesa payment prompt is scanned instantly"),
            Triple(Icons.Filled.AutoAwesome, "Live Blacklist Updates", "New betting platforms added automatically"),
            Triple(Icons.Filled.Block, "Instant Block Overlay", "Warning shown before money leaves your account"),
            Triple(Icons.Filled.Analytics, "Block History & Reports", "Full log of every detected payment attempt"),
            Triple(Icons.Filled.SupportAgent, "Priority Support", "Help when you need it most"),
        )

        features.forEach { (icon, title, desc) ->
            FeatureRow(icon = icon, title = title, description = desc)
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(24.dp))

        SafiButton(
            text     = "Subscribe via M-Pesa",
            onClick  = onSubscribe,
            loading  = isLoading,
            modifier = Modifier.fillMaxWidth(),
            icon     = Icons.Filled.PhoneAndroid
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "A payment of KSh 200 will be initiated to your Safaricom number. You will receive an M-Pesa STK push prompt on your phone.",
            style     = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Hint),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ── Active subscription view ──────────────────────────────────
@Composable
private fun ActiveSubscriptionView(expiresAt: String?, onRenew: () -> Unit, isLoading: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SafiColors.SuccessContainer, RoundedCornerShape(20.dp))
                .border(1.dp, SafiColors.Success.copy(0.3f), RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CheckCircle, null, tint = SafiColors.Success, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Subscription Active", style = MaterialTheme.typography.headlineSmall.copy(color = SafiColors.Success, fontWeight = FontWeight.Bold))
                if (expiresAt != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Valid until ${expiresAt.take(10)}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.Success.copy(0.7f))
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SafiOutlinedButton(
            text    = "Renew Subscription",
            onClick = onRenew,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ── Awaiting payment ──────────────────────────────────────────
@Composable
private fun AwaitingPaymentView() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(SafiColors.Primary.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PhoneAndroid, null, tint = SafiColors.Primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Check Your Phone", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text(
                "An M-Pesa prompt has been sent to your phone.\nEnter your PIN to complete the KSh 200 payment.",
                style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = SafiColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text("Waiting for confirmation…", style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Hint))
        }
    }
}

// ── Success view ──────────────────────────────────────────────
@Composable
private fun SuccessView() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CheckCircle, null, tint = SafiColors.Primary, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(20.dp))
            Text("Payment Successful!", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = SafiColors.Primary))
            Spacer(Modifier.height(8.dp))
            Text("SafiStep is now protecting your payments.", style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar), textAlign = TextAlign.Center)
        }
    }
}

// ── Error view ────────────────────────────────────────────────
@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Outlined.ErrorOutline, null, tint = SafiColors.Danger, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Something went wrong", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar), textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        SafiButton("Try Again", onRetry, Modifier.fillMaxWidth())
    }
}

// ── Feature row ───────────────────────────────────────────────
@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(36.dp).background(SafiColors.Primary.copy(0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = SafiColors.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(description, style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar))
        }
    }
}
