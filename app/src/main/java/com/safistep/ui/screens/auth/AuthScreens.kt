package com.safistep.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.repository.AuthRepository
import com.safistep.ui.components.*
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.ApiResult
import com.safistep.utils.normalizePhone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════
// PhoneEntry — ViewModel
// ══════════════════════════════════════════════════════════════

@HiltViewModel
class PhoneEntryViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    var phone by mutableStateOf("")
    var phoneError by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    private val _event = MutableSharedFlow<PhoneEvent>()
    val event = _event.asSharedFlow()

    fun onPhoneChange(value: String) {
        phone = value.filter { it.isDigit() }.take(10)
        phoneError = null
    }

    fun requestOtp(purpose: String = "registration") {
        android.util.Log.d("AuthVM", "requestOtp called with phone: $phone, purpose: $purpose")
        if (!validatePhone()) {
            android.util.Log.d("AuthVM", "Phone validation failed")
            return
        }
        viewModelScope.launch {
            isLoading = true
            val normalized = normalizePhone(phone)
            android.util.Log.d("AuthVM", "Normalized phone: $normalized")
            when (val result = authRepo.requestOtp(normalized, purpose)) {
                is ApiResult.Success -> {
                    android.util.Log.d("AuthVM", "OTP request successful")
                    _event.emit(PhoneEvent.OtpSent(normalized))
                }
                is ApiResult.Error   -> {
                    android.util.Log.e("AuthVM", "OTP request error: ${result.message}")
                    phoneError = result.message
                }
                is ApiResult.NetworkError -> {
                    android.util.Log.e("AuthVM", "OTP request network error")
                    phoneError = "No internet connection"
                }
            }
            isLoading = false
        }
    }

    private fun validatePhone(): Boolean {
        if (phone.length < 9) {
            phoneError = "Enter a valid Safaricom number"
            return false
        }
        if (!phone.startsWith("07") && !phone.startsWith("01") && !phone.startsWith("7") && !phone.startsWith("1")) {
            phoneError = "Must be a Safaricom number (07XX or 01XX)"
            return false
        }
        return true
    }
}

sealed class PhoneEvent {
    data class OtpSent(val phone: String) : PhoneEvent()
}

// ══════════════════════════════════════════════════════════════
// PhoneEntry — Screen
// ══════════════════════════════════════════════════════════════

@Composable
fun PhoneEntryScreen(
    onOtpSent: (phone: String, purpose: String) -> Unit,
    onLoginClick: () -> Unit,
    viewModel: PhoneEntryViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is PhoneEvent.OtpSent -> onOtpSent(event.phone, "registration")
            }
        }
    }

    SafiScaffold {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(SafiColors.Primary.copy(alpha = 0.04f), size.width * 0.7f, Offset(size.width, 0f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SafiColors.Primary.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shield, null, tint = SafiColors.Primary, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(32.dp))

            Text("Create Account", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your Safaricom number to get started",
                style = MaterialTheme.typography.bodyLarge.copy(color = SafiColors.OnSurfaceVar)
            )

            Spacer(Modifier.height(40.dp))

            SafiTextField(
                value         = viewModel.phone,
                onValueChange = viewModel::onPhoneChange,
                label         = "Safaricom Number",
                placeholder   = "0712 345 678",
                keyboardType  = KeyboardType.Phone,
                error         = viewModel.phoneError,
                leadingIcon   = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                        Text("+254", style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar))
                        Box(Modifier.width(1.dp).height(20.dp).background(SafiColors.CardBorder).padding(start = 8.dp))
                    }
                },
                onImeAction = { viewModel.requestOtp() }
            )

            Spacer(Modifier.height(24.dp))

            SafiButton(
                text     = "Send OTP",
                onClick  = { viewModel.requestOtp() },
                loading  = viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar))
                TextButton(onClick = onLoginClick, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Sign in", style = MaterialTheme.typography.labelLarge.copy(color = SafiColors.Primary))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Trust badges
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TrustBadge(Icons.Outlined.Lock, "Secure")
                Spacer(Modifier.width(24.dp))
                TrustBadge(Icons.Outlined.VisibilityOff, "Private")
                Spacer(Modifier.width(24.dp))
                TrustBadge(Icons.Outlined.Shield, "Safe")
            }
        }
    }
}

@Composable
private fun TrustBadge(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = SafiColors.Hint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = SafiColors.Hint))
    }
}

// ══════════════════════════════════════════════════════════════
// OTP — ViewModel
// ══════════════════════════════════════════════════════════════

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    var otp by mutableStateOf("")
    var hasError by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var isResending by mutableStateOf(false)
    var resendCountdown by mutableStateOf(60)
    var canResend by mutableStateOf(false)

    var phone by mutableStateOf("")

    private val _event = MutableSharedFlow<OtpEvent>()
    val event = _event.asSharedFlow()


    fun initWithPhone(phone: String) {
    this.phone = phone
    startCountdown()
}

    fun startCountdown() {
        viewModelScope.launch {
            resendCountdown = 60
            canResend = false
            while (resendCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                resendCountdown--
            }
            canResend = true
        }
    }

    fun onOtpChange(value: String) {
        otp = value.filter { it.isDigit() }.take(6)
        hasError = false
        errorMessage = null
        if (otp.length == 6) verify(this.phone)
    }

    fun verify(phone: String) {
        if (otp.length < 6) { hasError = true; return }
        viewModelScope.launch {
            isLoading = true
            val p = if (phone.isNotBlank()) phone else ""
            when (val r = authRepo.verifyOtp(p, otp)) {
                is ApiResult.Success -> {
                    authRepo.saveTempSession(r.data.tempToken, r.data.phone)
                    _event.emit(OtpEvent.Verified)
                }
                is ApiResult.Error -> {
                    hasError = true
                    errorMessage = r.message
                    otp = ""
                }
                is ApiResult.NetworkError -> {
                    hasError = true
                    errorMessage = "No internet connection"
                }
            }
            isLoading = false
        }
    }

    fun resend(phone: String, purpose: String) {
        viewModelScope.launch {
            isResending = true
            authRepo.requestOtp(phone, purpose)
            isResending = false
            startCountdown()
        }
    }
}

sealed class OtpEvent { object Verified : OtpEvent() }

// ══════════════════════════════════════════════════════════════
// OTP — Screen
// ══════════════════════════════════════════════════════════════

@Composable
fun OtpScreen(
    phone: String,
    purpose: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: OtpViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initWithPhone(phone)
        viewModel.event.collect { if (it is OtpEvent.Verified) onVerified() }
    }

    SafiScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, null, tint = SafiColors.OnBackground, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(32.dp))

            Text("Verify Number", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(10.dp))
            Text(
                "We sent a 6-digit code to\n+254${phone.removePrefix("254").removePrefix("0")}",
                style = MaterialTheme.typography.bodyLarge.copy(color = SafiColors.OnSurfaceVar),
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(48.dp))

            OtpInputRow(
                otp         = viewModel.otp,
                onOtpChange = { viewModel.onOtpChange(it) },
                hasError    = viewModel.hasError
            )

            AnimatedVisibility(visible = viewModel.errorMessage != null) {
                Text(
                    text     = viewModel.errorMessage ?: "",
                    style    = MaterialTheme.typography.bodySmall.copy(color = SafiColors.Danger),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            SafiButton(
                text     = "Verify",
                onClick  = { viewModel.verify(phone) },
                loading  = viewModel.isLoading,
                enabled  = viewModel.otp.length == 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Resend
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (!viewModel.canResend) {
                    Text(
                        "Resend in ${viewModel.resendCountdown}s",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.Hint)
                    )
                } else {
                    TextButton(
                        onClick  = { viewModel.resend(phone, purpose) },
                        enabled  = !viewModel.isResending
                    ) {
                        if (viewModel.isResending) {
                            CircularProgressIndicator(Modifier.size(16.dp), SafiColors.Primary, strokeWidth = 2.dp)
                        } else {
                            Text("Resend OTP", style = MaterialTheme.typography.labelLarge.copy(color = SafiColors.Primary))
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// SetPassword — ViewModel
// ══════════════════════════════════════════════════════════════

@HiltViewModel
class SetPasswordViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    var name by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var nameError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var confirmError by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    private val _event = MutableSharedFlow<SetPasswordEvent>()
    val event = _event.asSharedFlow()

    fun onNameChange(v: String) { name = v; nameError = null }
    fun onPasswordChange(v: String) { password = v; passwordError = null }
    fun onConfirmChange(v: String) { confirmPassword = v; confirmError = null }

    fun submit() {
        if (!validate()) return
        viewModelScope.launch {
            isLoading = true
            val tempToken = authRepo.getTempToken() ?: run {
                passwordError = "Session expired. Please start again."
                isLoading = false
                return@launch
            }
            when (val r = authRepo.setPassword(tempToken, password, name.trim().ifBlank { null })) {
                is ApiResult.Success  -> _event.emit(SetPasswordEvent.Success)
                is ApiResult.Error    -> passwordError = r.message
                is ApiResult.NetworkError -> passwordError = "No internet connection"
            }
            isLoading = false
        }
    }

    private fun validate(): Boolean {
        var ok = true
        if (password.length < 6) { passwordError = "Password must be at least 6 characters"; ok = false }
        if (password != confirmPassword) { confirmError = "Passwords do not match"; ok = false }
        return ok
    }
}

sealed class SetPasswordEvent { object Success : SetPasswordEvent() }

// ══════════════════════════════════════════════════════════════
// SetPassword — Screen
// ══════════════════════════════════════════════════════════════

@Composable
fun SetPasswordScreen(
    onSuccess: () -> Unit,
    viewModel: SetPasswordViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.event.collect { if (it is SetPasswordEvent.Success) onSuccess() }
    }

    SafiScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(60.dp))
            Text("Create Password", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Set a secure password for your SafiStep account", style = MaterialTheme.typography.bodyLarge.copy(color = SafiColors.OnSurfaceVar))
            Spacer(Modifier.height(40.dp))

            SafiTextField(
                value         = viewModel.name,
                onValueChange = viewModel::onNameChange,
                label         = "Full Name (optional)",
                placeholder   = "John Doe",
                error         = viewModel.nameError
            )
            Spacer(Modifier.height(16.dp))
            SafiTextField(
                value         = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label         = "Password",
                placeholder   = "Min. 6 characters",
                isPassword    = true,
                error         = viewModel.passwordError
            )
            Spacer(Modifier.height(16.dp))
            SafiTextField(
                value         = viewModel.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                label         = "Confirm Password",
                placeholder   = "Re-enter password",
                isPassword    = true,
                error         = viewModel.confirmError,
                imeAction     = androidx.compose.ui.text.input.ImeAction.Done,
                onImeAction   = { viewModel.submit() }
            )

            Spacer(Modifier.height(32.dp))
            SafiButton(
                text     = "Create Account",
                onClick  = viewModel::submit,
                loading  = viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Login — ViewModel
// ══════════════════════════════════════════════════════════════

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    var phone by mutableStateOf("")
    var password by mutableStateOf("")
    var phoneError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    private val _event = MutableSharedFlow<LoginEvent>()
    val event = _event.asSharedFlow()

    fun onPhoneChange(v: String) { phone = v.filter { it.isDigit() }.take(10); phoneError = null }
    fun onPasswordChange(v: String) { password = v; passwordError = null }

    fun login() {
        if (phone.length < 9) { phoneError = "Enter a valid phone number"; return }
        if (password.length < 6) { passwordError = "Enter your password"; return }
        viewModelScope.launch {
            isLoading = true
            when (val r = authRepo.login(normalizePhone(phone), password)) {
                is ApiResult.Success  -> _event.emit(LoginEvent.Success)
                is ApiResult.Error    -> passwordError = r.message
                is ApiResult.NetworkError -> passwordError = "No internet connection"
            }
            isLoading = false
        }
    }
}

sealed class LoginEvent { object Success : LoginEvent() }

// ══════════════════════════════════════════════════════════════
// Login — Screen
// ══════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.event.collect { if (it is LoginEvent.Success) onSuccess() }
    }

    SafiScaffold {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(SafiColors.Primary.copy(0.04f), size.width * 0.6f, Offset(0f, size.height))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(60.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SafiColors.Primary.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shield, null, tint = SafiColors.Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(32.dp))
            Text("Welcome Back", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Sign in to continue protecting your payments", style = MaterialTheme.typography.bodyLarge.copy(color = SafiColors.OnSurfaceVar))
            Spacer(Modifier.height(40.dp))

            SafiTextField(
                value         = viewModel.phone,
                onValueChange = viewModel::onPhoneChange,
                label         = "Phone Number",
                placeholder   = "0712 345 678",
                keyboardType  = KeyboardType.Phone,
                error         = viewModel.phoneError
            )
            Spacer(Modifier.height(16.dp))
            SafiTextField(
                value         = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label         = "Password",
                placeholder   = "Your password",
                isPassword    = true,
                error         = viewModel.passwordError,
                imeAction     = androidx.compose.ui.text.input.ImeAction.Done,
                onImeAction   = { viewModel.login() }
            )

            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                TextButton(onClick = onForgot) {
                    Text("Forgot password?", style = MaterialTheme.typography.labelMedium.copy(color = SafiColors.Primary))
                }
            }

            Spacer(Modifier.height(16.dp))
            SafiButton(
                text     = "Sign In",
                onClick  = viewModel::login,
                loading  = viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                Text("Don't have an account? ", style = MaterialTheme.typography.bodyMedium.copy(color = SafiColors.OnSurfaceVar))
                TextButton(onClick = onRegister, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Register", style = MaterialTheme.typography.labelLarge.copy(color = SafiColors.Primary))
                }
            }
        }
    }
}
