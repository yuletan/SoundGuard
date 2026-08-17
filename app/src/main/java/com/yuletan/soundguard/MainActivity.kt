package com.yuletan.soundguard

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class AppScreen {
    Login,
    RoleSelection,
    Setup,
    CameraTest,
    Terms,
    SetupComplete,
}

class MainActivity : ComponentActivity() {
    private var screen by mutableStateOf(AppScreen.Login)
    private var selectedRole by mutableStateOf<String?>(null)
    private var fullName by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var monitoringConsent by mutableStateOf(false)
    private var termsAccepted by mutableStateOf(false)
    private var microphoneGranted by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(false)
    private var cameraReady by mutableStateOf(false)
    private var email by mutableStateOf("")
    private var otp by mutableStateOf("")
    private var otpSent by mutableStateOf(false)
    private var otpCooldownSeconds by mutableStateOf(0)
    private var authBusy by mutableStateOf(false)
    private var authMessage by mutableStateOf<String?>(null)
    private var setupMessage by mutableStateOf<String?>(null)
    private val authClient by lazy { AuthClient(this) }
    private val profileClient by lazy { ProfileClient(this) }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) screen = AppScreen.CameraTest
    }

    private val setupPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        microphoneGranted = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            results[Manifest.permission.POST_NOTIFICATIONS] == true ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundGuardApp(
                screen = screen,
                role = selectedRole,
                onRoleSelected = { role ->
                    selectedRole = role
                    screen = AppScreen.Setup
                },
                onGoogleLogin = {
                    startActivity(Intent(Intent.ACTION_VIEW, authClient.googleAuthorizationUri()))
                },
                email = email,
                otp = otp,
                otpSent = otpSent,
                otpCooldownSeconds = otpCooldownSeconds,
                authBusy = authBusy,
                authMessage = authMessage,
                onEmailChange = { email = it },
                onOtpChange = { otp = it },
                onSendOtp = {
                    if (!Validation.isEmailValid(email)) {
                        authMessage = "Enter a valid email address."
                    } else {
                        authBusy = true
                        authMessage = null
                        lifecycleScope.launch {
                            authClient.sendOtp(email)
                                .onSuccess {
                                    otpSent = true
                                    authMessage = "Check your email for the one-time code."
                                    lifecycleScope.launch {
                                        for (seconds in 60 downTo 1) {
                                            otpCooldownSeconds = seconds
                                            delay(1_000)
                                        }
                                        otpCooldownSeconds = 0
                                    }
                                }
                                .onFailure {
                                    val detail = it.message.orEmpty()
                                    authMessage = if (detail.contains("rate", ignoreCase = true)) {
                                        "Email rate limit reached. Wait before requesting another code, or use another test email."
                                    } else {
                                        detail.ifBlank { "Could not send OTP." }
                                    }
                                }
                            authBusy = false
                        }
                    }
                },
                onVerifyOtp = {
                    if (otp.trim().length < 6) {
                        authMessage = "Enter the verification code from your email."
                    } else {
                        authBusy = true
                        authMessage = null
                        lifecycleScope.launch {
                            authClient.verifyOtp(email, otp)
                                .onSuccess {
                                    screen = AppScreen.RoleSelection
                                    authMessage = null
                                }
                                .onFailure { authMessage = it.message ?: "Could not verify OTP." }
                            authBusy = false
                        }
                    }
                },
                fullName = fullName,
                phone = phone,
                consent = monitoringConsent,
                termsAccepted = termsAccepted,
                microphoneGranted = microphoneGranted,
                notificationsGranted = notificationsGranted,
                cameraReady = cameraReady,
                setupMessage = setupMessage,
                onFullNameChange = { fullName = it },
                onPhoneChange = { phone = it },
                onConsentChange = { monitoringConsent = it },
                onTermsChange = { termsAccepted = it },
                onOpenTerms = { screen = AppScreen.Terms },
                onRequestPermissions = {
                    val permissions = buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    setupPermissions.launch(permissions.toTypedArray())
                },
                onCameraTest = {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                },
                onSetupBack = { screen = AppScreen.RoleSelection },
                onCameraBack = { screen = AppScreen.Setup },
                onCameraFinished = {
                    cameraReady = true
                    screen = AppScreen.Setup
                },
                onTermsBack = { screen = AppScreen.Setup },
                onTermsAccepted = {
                    termsAccepted = true
                    screen = AppScreen.Setup
                },
                onConfirmSetup = {
                    selectedRole?.let { role ->
                        setupMessage = null
                        lifecycleScope.launch {
                            profileClient.saveProfile(email, fullName, phone, role)
                                .onSuccess { screen = AppScreen.SetupComplete }
                                .onFailure { setupMessage = it.message ?: "Could not save profile." }
                        }
                    }
                },
            )
        }
        handleOAuthIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val callback = intent?.data ?: return
        if (callback.scheme != "soundguard" || callback.host != "auth") return
        lifecycleScope.launch {
            authClient.handleGoogleCallback(callback)
                .onSuccess {
                    screen = AppScreen.RoleSelection
                    authMessage = null
                }
                .onFailure { authMessage = it.message ?: "Google sign-in failed." }
        }
    }
}

@Composable
private fun SoundGuardApp(
    screen: AppScreen,
    role: String?,
                onRoleSelected: (String) -> Unit,
    onGoogleLogin: () -> Unit,
    email: String,
    otp: String,
    otpSent: Boolean,
    otpCooldownSeconds: Int,
    authBusy: Boolean,
    authMessage: String?,
    onEmailChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    fullName: String,
    phone: String,
    consent: Boolean,
    termsAccepted: Boolean,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    cameraReady: Boolean,
    setupMessage: String?,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onCameraTest: () -> Unit,
    onSetupBack: () -> Unit,
    onCameraBack: () -> Unit,
    onCameraFinished: () -> Unit,
    onTermsBack: () -> Unit,
    onTermsAccepted: () -> Unit,
    onConfirmSetup: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                AppScreen.Login -> LoginScreen(
                    email = email,
                    otp = otp,
                    otpSent = otpSent,
                    otpCooldownSeconds = otpCooldownSeconds,
                    busy = authBusy,
                    message = authMessage,
                    onEmailChange = onEmailChange,
                    onOtpChange = onOtpChange,
                    onSendOtp = onSendOtp,
                    onVerifyOtp = onVerifyOtp,
                    onGoogleLogin = onGoogleLogin,
                )
                AppScreen.RoleSelection -> RoleSelection(onRoleSelected)
                AppScreen.Setup -> SetupScreen(
                    role = role,
                    fullName = fullName,
                    phone = phone,
                    consent = consent,
                    termsAccepted = termsAccepted,
                    microphoneGranted = microphoneGranted,
                    notificationsGranted = notificationsGranted,
                    cameraReady = cameraReady,
                    setupMessage = setupMessage,
                    onFullNameChange = onFullNameChange,
                    onPhoneChange = onPhoneChange,
                    onConsentChange = onConsentChange,
                    onTermsChange = onTermsChange,
                    onOpenTerms = onOpenTerms,
                    onRequestPermissions = onRequestPermissions,
                    onCameraTest = onCameraTest,
                    onBack = onSetupBack,
                    onConfirm = onConfirmSetup,
                )
                AppScreen.CameraTest -> CameraTestScreen(onCameraBack, onCameraFinished)
                AppScreen.Terms -> TermsScreen(onTermsBack, onTermsAccepted)
                AppScreen.SetupComplete -> SetupCompleteScreen(setupMessage)
            }
        }
    }
}

@Composable
private fun LoginScreen(
    email: String,
    otp: String,
    otpSent: Boolean,
    otpCooldownSeconds: Int,
    busy: Boolean,
    message: String?,
    onEmailChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onGoogleLogin: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SoundGuard", style = MaterialTheme.typography.headlineLarge)
        Text("Sign in securely with Google", modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGoogleLogin, enabled = !busy) {
            Text("Continue with Google")
        }
        Text(
            "Your Google account identifies your SoundGuard account. No password or OTP is stored by SoundGuard.",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        message?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RoleSelection(onRoleSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SoundGuard", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Privacy-first caregiver support",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        Text("Choose your role", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onRoleSelected("Beneficiary") }) {
            Text("I need monitoring")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onRoleSelected("Caregiver") }) {
            Text("I am a caregiver")
        }
    }
}

@Composable
private fun SetupScreen(
    role: String?,
    fullName: String,
    phone: String,
    consent: Boolean,
    termsAccepted: Boolean,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    cameraReady: Boolean,
    setupMessage: String?,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onCameraTest: () -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }
        Spacer(Modifier.height(16.dp))
        Text("${role ?: "User"} setup", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Setup will verify permissions and connections before monitoring is enabled.",
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full name") },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone number") },
            singleLine = true,
        )
        Text(
            if (Validation.isPhoneValid(phone)) "Valid international phone number"
            else "Use international format, for example +65 81234567",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        if (role == "Beneficiary") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = onConsentChange)
                Text("I consent to SoundGuard monitoring and caregiver alerts")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = onTermsChange)
            Text("I agree to the Terms of Use")
        }
        TextButton(onClick = onOpenTerms) {
            Text("Read Terms of Use and camera privacy rules")
        }
        Spacer(Modifier.height(16.dp))
        Text("Readiness checklist", style = MaterialTheme.typography.titleMedium)
        Text(if (fullName.isNotBlank() && phone.isNotBlank()) "✓ Profile and phone number" else "○ Profile and phone number")
        Text(if (role != "Beneficiary" || consent) "✓ Monitoring consent" else "○ Monitoring consent")
        Text(if (microphoneGranted) "✓ Microphone permission" else "○ Microphone permission")
        Text(if (notificationsGranted) "✓ Notification permission" else "○ Notification permission")
        Text(if (cameraReady) "✓ Camera readiness" else "○ Camera readiness")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermissions) {
            Text("Check microphone and notifications")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCameraTest) {
            Text("Test camera")
        }
        Spacer(Modifier.height(16.dp))
        val ready = fullName.isNotBlank() &&
            Validation.isPhoneValid(phone) &&
            termsAccepted &&
            (role != "Beneficiary" || consent) &&
            microphoneGranted &&
            notificationsGranted &&
            cameraReady
        Button(
            onClick = onConfirm,
            enabled = ready,
        ) {
            Text("Confirm setup")
        }
        if (!ready) {
            Text(
                "Complete the checklist and accept the Terms of Use before confirming.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        setupMessage?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SetupCompleteScreen(message: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Setup confirmed", style = MaterialTheme.typography.headlineMedium)
        Text(
            message ?: "Your readiness checks passed and your profile was saved.",
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun TermsScreen(onBack: () -> Unit, onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
    ) {
        Button(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(16.dp))
        Text("Terms of Use", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "SoundGuard is a safety support prototype, not a medical device or emergency service. " +
                "It may miss sounds or produce false alerts. Keep the device powered, online, and positioned safely.",
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera privacy: I understand that my connected caregiver may request a verification photo " +
                "during an active incident. A request is recorded, the camera indicator is shown, and photos " +
                "are stored privately for no longer than 10 minutes. I can revoke this permission in Settings.",
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "SoundGuard does not automatically contact police or emergency services. If there is an emergency, " +
                "the beneficiary or caregiver must contact the appropriate local service.",
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAccept) {
            Text("I understand and agree")
        }
    }
}

@Composable
private fun CameraTestScreen(onBack: () -> Unit, onFinished: () -> Unit) {
    var capturedPath by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onBack) { Text("Back") }
            Text(
                "Camera readiness test",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        CameraPreview(
            modifier = Modifier.weight(1f),
            onPhotoCaptured = { capturedPath = it },
        )
        if (capturedPath != null) {
            Text(
                "Photo captured locally for testing",
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        Button(
            onClick = onFinished,
            modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally),
        ) {
            Text("Finish camera test")
        }
    }
}
