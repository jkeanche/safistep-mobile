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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
// UI State
// ══════════════════════════════════════════════════════════════

sealed class SubscriptionUiState {
    object Idle : SubscriptionUiState()
    object Initiating : SubscriptionUiState()
    data class AwaitingPayment(val checkoutRequestId: String) : SubscriptionUiState()
    object Polling : SubscriptionUiState()
    object Success : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

// ── Reuse the plan enum from PlanSelectionScreen ──────────────
// RenewPlan mirrors SubscriptionPlan but scoped to renewal
// (trial not offered on renewal — only monthly/yearly)
enum class RenewPlan(
    val id: String,
    val label: String,
    val price: String,
    val subLabel: String,
    val badge: String?,
    val icon: ImageVector
) {
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
// ViewModel
// ══════════════════════════════════════════════════════════════

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepo: SubscriptionRepository
) : ViewModel() {

    val subscriptionStatus  = subscriptionRepo.subscriptionStatus
    val subscriptionExpires = subscriptionRepo.subscriptionExpires

    // Plan & phone selection state
    var selectedPlan by mutableStateOf(RenewPlan.MONTHLY)
    var paymentPhone by mutableStateOf("")
    var phoneError   by mutableStateOf<String?>(null)

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Idle)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { subscriptionRepo.getStatus() }
    }

    fun onPlanSelected(plan: RenewPlan) {
        selectedPlan = plan
        phoneError   = null
    }

    fun onPhoneChange(v: String) {
        paymentPhone = v.filter { it.isDigit() }.take(10)
        phoneError   = null
    }

    fun initiate() {
        if (!validatePhone()) return
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Initiating
            val normalized = normalizePhone(paymentPhone)
            when (val result = subscriptionRepo.initiate(selectedPlan.id, normalized)) {
                is ApiResult.Success -> {
                    _uiState.value = SubscriptionUiState.AwaitingPayment(
                        result.data.checkoutRequestId ?: ""
                    )
                    startPolling()
                }
                is ApiResult.Error        -> _uiState.value = SubscriptionUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = SubscriptionUiState.Error("No internet connection")
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Polling
            repeat(24) { // 24 × 5s = 2 min
                delay(5_000)
                if (subscriptionRepo.pollStatus()) {
                    _uiState.value = SubscriptionUiState.Success
                    return@launch
                }
            }
            _uiState.value = SubscriptionUiState.Error(
                "Payment not confirmed yet. If you paid, it will activate shortly."
            )
        }
    }

    fun reset() {
        pollingJob?.cancel()
        _uiState.value = SubscriptionUiState.Idle
    }

    private fun validatePhone(): Boolean {
        if (paymentPhone.length < 9) {
            phoneError = "Enter a valid Safaricom number"
            return false
        }
        if (!paymentPhone.startsWith("07") && !paymentPhone.startsWith("01") &&
            !paymentPhone.startsWith("7")  && !paymentPhone.startsWith("1")) {
            phoneError = "Must be a Safaricom number (07XX or 01XX)"
            return false
        }
        return true
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
    val uiState      by viewModel.uiState.collectAsState()
    val subStatus    by viewModel.subscriptionStatus.collectAsState("inactive")
    val subExpires   by viewModel.subscriptionExpires.collectAsState(null)
    val isSubscribed = subStatus == "active"

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

                is SubscriptionUiState.Error -> ErrorView(
                    message = (uiState as SubscriptionUiState.Error).message,
                    onRetry = { viewModel.reset() }
                )

                else -> {
                    if (isSubscribed) {
                        ActiveSubscriptionView(
                            expiresAt    = subExpires,
                            selectedPlan = viewModel.selectedPlan,
                            onPlanSelect = viewModel::onPlanSelected,
                            paymentPhone = viewModel.paymentPhone,
                            onPhoneChange = viewModel::onPhoneChange,
                            phoneError   = viewModel.phoneError,
                            onRenew      = { viewModel.initiate() },
                            isLoading    = uiState is SubscriptionUiState.Initiating
                        )
                    } else {
                        InactiveSubscriptionView(
                            selectedPlan  = viewModel.selectedPlan,
                            onPlanSelect  = viewModel::onPlanSelected,
                            paymentPhone  = viewModel.paymentPhone,
                            onPhoneChange = viewModel::onPhoneChange,
                            phoneError    = viewModel.phoneError,
                            onSubscribe   = { viewModel.initiate() },
                            isLoading     = uiState is SubscriptionUiState.Initiating
                        )
                    }
                }
            }
        }
    }
}

// ── Inactive — main purchase view ─────────────────────────────

@Composable
private fun InactiveSubscriptionView(
    selectedPlan: RenewPlan,
    onPlanSelect: (RenewPlan) -> Unit,
    paymentPhone: String,
    onPhoneChange: (String) -> Unit,
    phoneError: String?,
    onSubscribe: () -> Unit,
    isLoading: Boolean
) {
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
                Spacer(Modifier.height(4.dp))
                Text(
                    "Full payment protection · Cancel anytime",
                    style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Plan selector
        Text(
            "Select Plan",
            style    = MaterialTheme.typography.labelMedium.copy(color = SafiColors.OnSurfaceVar),
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        RenewPlan.values().forEach { plan ->
            RenewPlanCard(
                plan       = plan,
                isSelected = selectedPlan == plan,
                onSelect   = { onPlanSelect(plan) }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Payment phone
        SafiTextField(
            value         = paymentPhone,
            onValueChange = onPhoneChange,
            label         = "M-Pesa Payment Number",
            placeholder   = "0712 345 678",
            keyboardType  = KeyboardType.Phone,
            error         = phoneError,
            leadingIcon   = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(start = 12.dp)
                ) {
                    Text("+254", style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar))
                    Box(Modifier.width(1.dp).height(20.dp).background(SafiColors.CardBorder).padding(start = 8.dp))
                }
            }
        )

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = SafiColors.Hint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "An M-Pesa STK push will be sent to this number",
                style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.Hint)
            )
        }

        Spacer(Modifier.height(20.dp))

        SafiButton(
            text     = when (selectedPlan) {
                RenewPlan.MONTHLY -> "Pay KES 200 via M-Pesa"
                RenewPlan.YEARLY  -> "Pay KES 1,920 via M-Pesa"
            },
            onClick  = onSubscribe,
            loading  = isLoading,
            modifier = Modifier.fillMaxWidth(),
            icon     = Icons.Filled.PhoneAndroid
        )

        Spacer(Modifier.height(16.dp))

        // Features
        val features = listOf(
            Triple(Icons.Filled.Shield,       "Real-time STK Payment Monitoring", "Every M-Pesa payment prompt is scanned instantly"),
            Triple(Icons.Filled.AutoAwesome,  "Live Blacklist Updates",            "New betting platforms added automatically"),
            Triple(Icons.Filled.Block,        "Instant Block Overlay",             "Warning shown before money leaves your account"),
            Triple(Icons.Filled.Analytics,    "Block History & Reports",           "Full log of every detected payment attempt"),
            Triple(Icons.Filled.SupportAgent, "Priority Support",                  "Help when you need it most"),
        )
        features.forEach { (icon, title, desc) ->
            FeatureRow(icon = icon, title = title, description = desc)
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Active subscription view ──────────────────────────────────

@Composable
private fun ActiveSubscriptionView(
    expiresAt: String?,
    selectedPlan: RenewPlan,
    onPlanSelect: (RenewPlan) -> Unit,
    paymentPhone: String,
    onPhoneChange: (String) -> Unit,
    phoneError: String?,
    onRenew: () -> Unit,
    isLoading: Boolean
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {

        // Active status card
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
                Text(
                    "Subscription Active",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color      = SafiColors.Success,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (expiresAt != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Valid until ${expiresAt.take(10)}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.Success.copy(0.7f))
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Renew Subscription",
            style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        // Plan selector
        RenewPlan.values().forEach { plan ->
            RenewPlanCard(
                plan       = plan,
                isSelected = selectedPlan == plan,
                onSelect   = { onPlanSelect(plan) }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Payment phone
        SafiTextField(
            value         = paymentPhone,
            onValueChange = onPhoneChange,
            label         = "M-Pesa Payment Number",
            placeholder   = "0712 345 678",
            keyboardType  = KeyboardType.Phone,
            error         = phoneError,
            leadingIcon   = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(start = 12.dp)
                ) {
                    Text("+254", style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar))
                    Box(Modifier.width(1.dp).height(20.dp).background(SafiColors.CardBorder).padding(start = 8.dp))
                }
            }
        )

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = SafiColors.Hint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "An M-Pesa STK push will be sent to this number",
                style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.Hint)
            )
        }

        Spacer(Modifier.height(20.dp))

        SafiButton(
            text     = when (selectedPlan) {
                RenewPlan.MONTHLY -> "Renew — Pay KES 200"
                RenewPlan.YEARLY  -> "Renew — Pay KES 1,920"
            },
            onClick  = onRenew,
            loading  = isLoading,
            modifier = Modifier.fillMaxWidth(),
            icon     = Icons.Filled.PhoneAndroid
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ── Renew plan card (monthly / yearly only) ───────────────────

@Composable
private fun RenewPlanCard(
    plan: RenewPlan,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) SafiColors.Primary else SafiColors.CardBorder,
        label = "renewBorder"
    )
    val bgColor by animateColorAsState(
        if (isSelected) SafiColors.Primary.copy(0.07f) else SafiColors.Card,
        label = "renewBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) SafiColors.Primary.copy(0.15f) else SafiColors.SurfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                plan.icon, null,
                tint     = if (isSelected) SafiColors.Primary else SafiColors.OnSurfaceVar,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

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
            Text(
                plan.subLabel,
                style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar)
            )
        }

        Text(
            plan.price,
            style = MaterialTheme.typography.titleLarge.copy(
                color      = if (isSelected) SafiColors.Primary else SafiColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.width(10.dp))

        // Radio dot
        Box(
            modifier = Modifier
                .size(20.dp)
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
                Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ── Awaiting payment ──────────────────────────────────────────

@Composable
private fun AwaitingPaymentView() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.9f,
        targetValue   = 1.1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "pulseScale"
    )

    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .background(SafiColors.Primary.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PhoneAndroid, null, tint = SafiColors.Primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Check Your Phone",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "An M-Pesa prompt has been sent to your phone.\nEnter your PIN to complete the payment.",
                style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                color       = SafiColors.Primary,
                strokeWidth = 3.dp,
                modifier    = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Waiting for confirmation…",
                style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Hint)
            )
        }
    }
}

// ── Success view ──────────────────────────────────────────────

@Composable
private fun SuccessView() {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CheckCircle, null,
                tint     = SafiColors.Primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Payment Successful!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = SafiColors.Primary
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "SafiStep is now protecting your payments.",
                style     = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Error view ────────────────────────────────────────────────

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Outlined.ErrorOutline, null, tint = SafiColors.Danger, modifier = Modifier.size(64.dp))
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

// ── Feature row ───────────────────────────────────────────────

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SafiColors.Primary.copy(0.1f), RoundedCornerShape(10.dp)),
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