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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.local.SafiStepPreferences
import com.safistep.data.repository.SubscriptionRepository
import com.safistep.ui.components.*
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.ApiResult
import com.safistep.utils.normalizePhone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════
// Plan model
// ══════════════════════════════════════════════════════════════

enum class SubscriptionPlan(
    val id: String,
    val label: String,
    val price: String,
    val subLabel: String,
    val badge: String?,
    val icon: ImageVector
) {
    TRIAL(
        id       = "trial",
        label    = "3-Day Free Trial",
        price    = "Free",
        subLabel = "No payment needed · Auto-expires",
        badge    = null,
        icon     = Icons.Outlined.HourglassBottom
    ),
    MONTHLY(
        id       = "monthly",
        label    = "Monthly",
        price    = "KES 200",
        subLabel = "per month · Cancel anytime",
        badge    = null,
        icon     = Icons.Outlined.CalendarMonth
    ),
    YEARLY(
        id       = "yearly",
        label    = "Yearly",
        price    = "KES 1,920",
        subLabel = "per year · Save 20%",
        badge    = "Save 20%",
        icon     = Icons.Outlined.CalendarToday
    )
}

// ══════════════════════════════════════════════════════════════
// UI state
// ══════════════════════════════════════════════════════════════

sealed class PlanUiState {
    object Idle : PlanUiState()
    object Loading : PlanUiState()
    data class AwaitingPayment(val checkoutRequestId: String) : PlanUiState()
    object Success : PlanUiState()
    data class Error(val message: String) : PlanUiState()
}

// ══════════════════════════════════════════════════════════════
// ViewModel
// ══════════════════════════════════════════════════════════════

@HiltViewModel
class PlanSelectionViewModel @Inject constructor(
    private val subscriptionRepo: SubscriptionRepository,
    private val prefs: SafiStepPreferences
) : ViewModel() {

    var selectedPlan by mutableStateOf(SubscriptionPlan.TRIAL)
    var paymentPhone by mutableStateOf("")
    var phoneError   by mutableStateOf<String?>(null)

    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Idle)
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun onPlanSelected(plan: SubscriptionPlan) {
        selectedPlan = plan
        phoneError   = null
    }

    fun onPhoneChange(v: String) {
        paymentPhone = v.filter { it.isDigit() }.take(10)
        phoneError   = null
    }

    fun confirm() {
        when (selectedPlan) {
            SubscriptionPlan.TRIAL   -> activateTrial()
            SubscriptionPlan.MONTHLY,
            SubscriptionPlan.YEARLY  -> initiatePaid()
        }
    }

    private fun activateTrial() {
        viewModelScope.launch {
            _uiState.value = PlanUiState.Loading
            when (val result = subscriptionRepo.startTrial()) {
                is ApiResult.Success      -> _uiState.value = PlanUiState.Success
                is ApiResult.Error        -> _uiState.value = PlanUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = PlanUiState.Error("No internet connection")
            }
        }
    }

    private fun initiatePaid() {
        if (!validatePhone()) return
        viewModelScope.launch {
            _uiState.value = PlanUiState.Loading
            val normalized = normalizePhone(paymentPhone)
            when (val result = subscriptionRepo.initiate(selectedPlan.id, normalized)) {
                is ApiResult.Success -> {
                    _uiState.value = PlanUiState.AwaitingPayment(
                        result.data.checkoutRequestId ?: ""
                    )
                    startPolling()
                }
                is ApiResult.Error        -> _uiState.value = PlanUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = PlanUiState.Error("No internet connection")
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repeat(24) { // 24 × 5s = 2 min
                delay(5_000)
                if (subscriptionRepo.pollStatus()) {
                    _uiState.value = PlanUiState.Success
                    return@launch
                }
            }
            _uiState.value = PlanUiState.Error(
                "Payment not confirmed yet. If you paid, your plan will activate shortly."
            )
        }
    }

    private fun validatePhone(): Boolean {
        if (paymentPhone.length < 9) {
            phoneError = "Enter a valid Safaricom number"
            return false
        }
        if (!paymentPhone.startsWith("07") && !paymentPhone.startsWith("01") &&
            !paymentPhone.startsWith("7") && !paymentPhone.startsWith("1")) {
            phoneError = "Must be a Safaricom number (07XX or 01XX)"
            return false
        }
        return true
    }

    fun reset() {
        pollingJob?.cancel()
        _uiState.value = PlanUiState.Idle
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
fun PlanSelectionScreen(
    onSuccess: () -> Unit,
    viewModel: PlanSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate on success after brief delay
    LaunchedEffect(uiState) {
        if (uiState is PlanUiState.Success) {
            delay(1800)
            onSuccess()
        }
    }

    SafiScaffold {
        when (uiState) {
            is PlanUiState.AwaitingPayment -> AwaitingPaymentView(
                plan    = viewModel.selectedPlan,
                onReset = { viewModel.reset() }
            )
            is PlanUiState.Success -> PlanSuccessView(plan = viewModel.selectedPlan)
            is PlanUiState.Error   -> PlanErrorView(
                message = (uiState as PlanUiState.Error).message,
                onRetry = { viewModel.reset() }
            )
            else -> PlanPickerContent(viewModel = viewModel)
        }
    }
}

// ── Plan picker ───────────────────────────────────────────────

@Composable
private fun PlanPickerContent(viewModel: PlanSelectionViewModel) {
    val isLoading = viewModel.uiState.collectAsState().value is PlanUiState.Loading
    val needsPhone = viewModel.selectedPlan != SubscriptionPlan.TRIAL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        // Header
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(SafiColors.Primary.copy(0.15f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Shield, null, tint = SafiColors.Primary, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Choose Your Plan",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Start free or go all-in — your protection, your choice.",
            style = MaterialTheme.typography.bodyLarge.copy(color = SafiColors.OnSurfaceVar)
        )

        Spacer(Modifier.height(28.dp))

        // Plan cards
        SubscriptionPlan.values().forEach { plan ->
            PlanCard(
                plan       = plan,
                isSelected = viewModel.selectedPlan == plan,
                onSelect   = { viewModel.onPlanSelected(plan) }
            )
            Spacer(Modifier.height(12.dp))
        }

        // Phone field — only for paid plans
        AnimatedVisibility(
            visible = needsPhone,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(4.dp))
                SafiTextField(
                    value         = viewModel.paymentPhone,
                    onValueChange = viewModel::onPhoneChange,
                    label         = "M-Pesa Payment Number",
                    placeholder   = "0712 345 678",
                    keyboardType  = KeyboardType.Phone,
                    error         = viewModel.phoneError,
                    leadingIcon   = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                "+254",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = SafiColors.OnSurfaceVar
                                )
                            )
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(SafiColors.CardBorder)
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint     = SafiColors.Hint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "An M-Pesa STK push will be sent to this number",
                        style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.Hint)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        SafiButton(
            text     = when (viewModel.selectedPlan) {
                SubscriptionPlan.TRIAL   -> "Start Free Trial"
                SubscriptionPlan.MONTHLY -> "Pay KES 200 via M-Pesa"
                SubscriptionPlan.YEARLY  -> "Pay KES 1,920 via M-Pesa"
            },
            onClick  = { viewModel.confirm() },
            loading  = isLoading,
            modifier = Modifier.fillMaxWidth(),
            icon     = when (viewModel.selectedPlan) {
                SubscriptionPlan.TRIAL -> Icons.Outlined.HourglassBottom
                else                   -> Icons.Filled.PhoneAndroid
            }
        )

        Spacer(Modifier.height(16.dp))

        // Feature bullets
        FeatureBullets()

        Spacer(Modifier.height(32.dp))
    }
}

// ── Plan card ─────────────────────────────────────────────────

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) SafiColors.Primary else SafiColors.CardBorder,
        label = "border"
    )
    val bgColor by animateColorAsState(
        if (isSelected) SafiColors.Primary.copy(0.07f) else SafiColors.Card,
        label = "bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isSelected) SafiColors.Primary.copy(0.15f) else SafiColors.SurfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                plan.icon,
                null,
                tint     = if (isSelected) SafiColors.Primary else SafiColors.OnSurfaceVar,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Labels
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plan.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isSelected) SafiColors.OnBackground else SafiColors.OnSurface
                    )
                )
                if (plan.badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(SafiColors.Primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            plan.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 9.sp
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                plan.subLabel,
                style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Price
        Column(horizontalAlignment = Alignment.End) {
            Text(
                plan.price,
                style = MaterialTheme.typography.titleLarge.copy(
                    color      = if (isSelected) SafiColors.Primary else SafiColors.OnSurface,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.width(8.dp))

        // Radio
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = SafiColors.CardBorder,
                    shape = CircleShape
                )
                .background(
                    if (isSelected) SafiColors.Primary else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    null,
                    tint     = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ── Feature bullets ───────────────────────────────────────────

@Composable
private fun FeatureBullets() {
    val items = listOf(
        "Real-time STK prompt detection",
        "100+ Kenyan betting platforms blocked",
        "15-minute rethink window",
        "Encrypted — we never see your PIN"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafiColors.SurfaceVariant, RoundedCornerShape(14.dp))
            .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint     = SafiColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    item,
                    style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurface)
                )
            }
        }
    }
}

// ── Awaiting payment ──────────────────────────────────────────

@Composable
private fun AwaitingPaymentView(plan: SubscriptionPlan, onReset: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "scale"
    )
    Box(
        modifier        = Modifier.fillMaxSize().systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(SafiColors.Primary.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PhoneAndroid,
                    null,
                    tint     = SafiColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Check Your Phone",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "An M-Pesa STK push has been sent.\nEnter your PIN to pay ${plan.price}.",
                style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                color        = SafiColors.Primary,
                strokeWidth  = 3.dp,
                modifier     = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Waiting for confirmation…",
                style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Hint)
            )
            Spacer(Modifier.height(32.dp))
            TextButton(onClick = onReset) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelMedium.copy(color = SafiColors.Hint)
                )
            }
        }
    }
}

// ── Success ───────────────────────────────────────────────────

@Composable
private fun PlanSuccessView(plan: SubscriptionPlan) {
    Box(
        modifier         = Modifier.fillMaxSize().systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Filled.CheckCircle,
                null,
                tint     = SafiColors.Primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                if (plan == SubscriptionPlan.TRIAL) "Trial Activated!" else "Payment Successful!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = SafiColors.Primary
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (plan == SubscriptionPlan.TRIAL)
                    "Your 3-day free trial has started.\nSafiStep is now protecting your payments."
                else
                    "SafiStep is now protecting your payments.",
                style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Error ─────────────────────────────────────────────────────

@Composable
private fun PlanErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                null,
                tint     = SafiColors.Danger,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Something went wrong",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            SafiButton("Try Again", onRetry, Modifier.fillMaxWidth())
        }
    }
}