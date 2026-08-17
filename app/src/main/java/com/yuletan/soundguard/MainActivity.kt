package com.yuletan.soundguard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

private const val JAMES_TEST_ID = "7f3c2a91-6b84-4d1e-9f52-8a7c6b3d1042"

private enum class AppScreen {
    Loading,
    Login,
    RoleSelection,
    Setup,
    CameraTest,
    Terms,
    BeneficiaryDashboard,
    CaregiverDashboard,
    CaregiverPreview,
    Settings,
}

class MainActivity : ComponentActivity() {
    private var screen by mutableStateOf(AppScreen.Loading)
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

    // Dashboard State
    private var connectedCaregivers by mutableStateOf<List<CaregiverMember>>(emptyList())
    private var sharedIncidents by mutableStateOf<List<SharedIncident>>(emptyList())
    private var monitoredBeneficiaries by mutableStateOf<List<MonitoredBeneficiary>>(emptyList())
    private var caregiverNotifications by mutableStateOf<List<CaregiverNotification>>(emptyList())
    private var selectedNotification by mutableStateOf<CaregiverNotification?>(null)
    private var previewBeneficiary by mutableStateOf<MonitoredBeneficiary?>(null)
    private var generatedPairingCode by mutableStateOf<String?>(null)
    private var dashboardLoading by mutableStateOf(false)
    private var dashboardMessage by mutableStateOf<String?>(null)
    private var demoCaregiverLinked by mutableStateOf(false)
    private var returnToPreviewAfterCamera by mutableStateOf(false)
    private var demoPhotoRequested by mutableStateOf(false)
    private var demoPhotoDecision by mutableStateOf<String?>(null)
    private var demoPhotoRequestedAt by mutableStateOf<Long?>(null)
    private var demoPhotoDecisionAt by mutableStateOf<Long?>(null)
    private var demoPhotoPath by mutableStateOf<String?>(null)
    private var demoSnapshotRequest by mutableStateOf<SnapshotRequest?>(null)
    private var demoSnapshotMessage by mutableStateOf<String?>(null)
    private var loadedSessionUserId: String? = null
    private var showResetDataDialog by mutableStateOf(false)

    // Role Switch Dialog States
    private var showBlockedSwitchDialog by mutableStateOf(false)
    private var blockedActiveConnectionsCount by mutableStateOf(0)
    private var showConfirmSwitchDialog by mutableStateOf(false)

    private val authClient by lazy { AuthClient(this) }
    private val profileClient by lazy { ProfileClient(this) }
    private val careClient by lazy { CareClient(this) }
    private val notificationClient by lazy { NotificationClient(this) }

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
            val liveAudioState by AudioMonitoringService.audioState.collectAsState()

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        AppScreen.Loading -> LoadingScreen()
                        AppScreen.Login -> LoginScreen(
                            email = email,
                            otp = otp,
                            otpSent = otpSent,
                            otpCooldownSeconds = otpCooldownSeconds,
                            busy = authBusy,
                            message = authMessage,
                            onEmailChange = { email = it },
                            onOtpChange = { otp = it },
                            onSendOtp = { sendOtp() },
                            onVerifyOtp = { verifyOtp() },
                            onGoogleLogin = {
                                startActivity(Intent(Intent.ACTION_VIEW, authClient.googleAuthorizationUri()))
                            },
                        )
                        AppScreen.RoleSelection -> RoleSelection { role ->
                            selectedRole = role
                            screen = AppScreen.Setup
                        }
                        AppScreen.Setup -> SetupScreen(
                            role = selectedRole,
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
                            onRequestPermissions = { requestAppPermissions() },
                            onCameraTest = { cameraPermission.launch(Manifest.permission.CAMERA) },
                            onBack = { screen = AppScreen.RoleSelection },
                            onConfirm = { confirmSetup() },
                        )
                        AppScreen.CameraTest -> CameraTestScreen(
                            onBack = {
                                screen = if (returnToPreviewAfterCamera) AppScreen.CaregiverPreview else AppScreen.Setup
                                returnToPreviewAfterCamera = false
                            },
                            onFinished = { capturedPath ->
                                cameraReady = true
                                if (returnToPreviewAfterCamera) {
                                    demoPhotoPath = capturedPath
                                    demoSnapshotRequest?.let { request ->
                                        val savedPath = capturedPath ?: return@let
                                        lifecycleScope.launch {
                                            SnapshotClient(this@MainActivity)
                                                .uploadSnapshot(request, java.io.File(savedPath))
                                                .onSuccess { demoSnapshotMessage = "Photo uploaded to secure storage." }
                                                .onFailure { demoSnapshotMessage = "Photo upload failed: ${it.message}" }
                                        }
                                    }
                                }
                                screen = if (returnToPreviewAfterCamera) AppScreen.CaregiverPreview else AppScreen.Setup
                                returnToPreviewAfterCamera = false
                            },
                        )
                        AppScreen.Terms -> TermsScreen(
                            onBack = {
                                screen = if (selectedRole != null && connectedCaregivers.isNotEmpty()) {
                                    AppScreen.Settings
                                } else {
                                    AppScreen.Setup
                                }
                            },
                            onAccept = {
                                termsAccepted = true
                                screen = AppScreen.Setup
                            },
                        )
                        AppScreen.BeneficiaryDashboard -> BeneficiaryDashboard(
                            fullName = fullName,
                            email = email,
                            audioState = liveAudioState,
                            onToggleMonitoring = {
                                if (liveAudioState.isMonitoring) {
                                    AudioMonitoringService.stop(this@MainActivity)
                                } else {
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                        requestAppPermissions()
                                    } else {
                                        AudioMonitoringService.start(this@MainActivity)
                                    }
                                }
                            },
                            onSimulateSound = { category, label, confidence, isEmergency ->
                                if (!liveAudioState.isMonitoring) {
                                    AudioMonitoringService.start(this@MainActivity)
                                }
                                AudioMonitoringService.simulateSound(category, label, confidence, isEmergency)
                            },
                             caregivers = connectedCaregivers,
                             sharedIncidents = sharedIncidents,
                            pairingCode = generatedPairingCode,
                            loading = dashboardLoading,
                            message = dashboardMessage,
                            onGenerateCode = { generatePairingCode() },
                            onCopyCode = { code -> copyToClipboard(code) },
                             onSetPrimary = { connectionId, caregiverId -> setPrimaryCaregiver(connectionId, caregiverId) },
                             onRemoveCaregiver = { connectionId -> removeCaregiver(connectionId) },
                             onCall = { targetPhone -> callPhoneNumber(targetPhone) },
                             onOpenCaregiverPreview = { previewBeneficiary = null; screen = AppScreen.CaregiverPreview },
                             onLinkDemoCaregiver = { demoCaregiverLinked = true },
                             onOpenSettings = { screen = AppScreen.Settings },
                            onRefresh = { refreshBeneficiaryData() },
                        )
                        AppScreen.CaregiverDashboard -> CaregiverDashboard(
                            fullName = fullName,
                            email = email,
                            beneficiaries = monitoredBeneficiaries,
                            loading = dashboardLoading,
                            message = dashboardMessage,
                            onConnectBeneficiary = { code -> pairBeneficiary(code) },
                             onRemoveBeneficiary = { connectionId -> removeBeneficiary(connectionId) },
                             onCall = { targetPhone -> callPhoneNumber(targetPhone) },
                             onOpenBeneficiary = { beneficiary -> previewBeneficiary = beneficiary; screen = AppScreen.CaregiverPreview },
                             notifications = caregiverNotifications,
                             onAcknowledgeNotification = { id -> acknowledgeNotification(id) },
                             onOpenNotification = { notification ->
                                 selectedNotification = notification
                                 previewBeneficiary = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == notification.beneficiaryId }
                                 screen = AppScreen.CaregiverPreview
                             },
                             onOpenSettings = { screen = AppScreen.Settings },
                             onRefresh = { refreshCaregiverData() },
                         )
                         AppScreen.CaregiverPreview -> CaregiverPreviewScreen(
                             beneficiaryName = previewBeneficiary?.name ?: fullName.ifBlank { "Beneficiary" },
                             beneficiaryPhone = previewBeneficiary?.phone ?: phone,
                             audioState = liveAudioState,
                             demoCaregiverLinked = demoCaregiverLinked,
                              onBack = { screen = if (previewBeneficiary != null) AppScreen.CaregiverDashboard else AppScreen.BeneficiaryDashboard },
                              onLinkDemoCaregiver = {
                                  lifecycleScope.launch {
                                      careClient.linkDemoJames()
                                          .onSuccess {
                                              demoCaregiverLinked = true
                                              demoSnapshotMessage = "Demo caregiver linked to James."
                                          }
                                          .onFailure { demoSnapshotMessage = "Demo link failed: ${it.message}" }
                                  }
                              },
                              onCall = { callPhoneNumber(previewBeneficiary?.phone ?: phone) },
                              photoRequested = demoPhotoRequested,
                              photoDecision = demoPhotoDecision,
                              photoRequestedAt = demoPhotoRequestedAt,
                               photoDecisionAt = demoPhotoDecisionAt,
                               photoPath = demoPhotoPath,
                               snapshotMessage = demoSnapshotMessage,
                               backendAlert = selectedNotification,
                               onOpenCamera = {
                                   if (demoCaregiverLinked) requestDemoSnapshotAndOpenCamera()
                                   else demoSnapshotMessage = "Link the demo caregiver first."
                               },
                         )
                         AppScreen.Settings -> SettingsScreen(
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            currentRole = selectedRole ?: "User",
                            microphoneGranted = microphoneGranted,
                            notificationsGranted = notificationsGranted,
                            onFullNameChange = { fullName = it },
                            onPhoneChange = { phone = it },
                            onSaveProfile = { updateProfile() },
                            onRequestSwitchRole = { handleRoleSwitchRequest() },
                            onOpenTerms = { screen = AppScreen.Terms },
                            onCameraTest = { cameraPermission.launch(Manifest.permission.CAMERA) },
                            onBack = {
                                screen = if (selectedRole.equals("caregiver", ignoreCase = true)) {
                                    AppScreen.CaregiverDashboard
                                } else {
                                    AppScreen.BeneficiaryDashboard
                                }
                            },
                             onSignOut = { signOut() },
                             onResetAllData = { showResetDataDialog = true },
                         )
                    }

                    // Blocked Role Switch Dialog
                    if (showResetDataDialog) {
                        AlertDialog(
                            onDismissRequest = { showResetDataDialog = false },
                            title = { Text("Delete all account data?") },
                            text = { Text("This permanently deletes your connections, incidents, notifications, snapshots, device tokens, settings, and profile setup. You will return to role selection." ) },
                            confirmButton = {
                                Button(onClick = { showResetDataDialog = false; resetAllAccountData() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("Delete all data")
                                }
                            },
                            dismissButton = { TextButton(onClick = { showResetDataDialog = false }) { Text("Cancel") } },
                        )
                    }

                    // Blocked Role Switch Dialog
                    if (showBlockedSwitchDialog) {
                        AlertDialog(
                            onDismissRequest = { showBlockedSwitchDialog = false },
                            title = { Text("⚠️ Cannot Switch Role") },
                            text = {
                                Text(
                                    "You currently have $blockedActiveConnectionsCount active care connection(s). " +
                                        "To protect ongoing safety monitoring, you must remove all connections before switching your account role.",
                                )
                            },
                            confirmButton = {
                                Button(onClick = { showBlockedSwitchDialog = false }) {
                                    Text("Got It")
                                }
                            },
                        )
                    }

                    // Confirm Role Switch Dialog
                    if (showConfirmSwitchDialog) {
                        val targetRole = if (selectedRole.equals("caregiver", ignoreCase = true)) "Beneficiary" else "Caregiver"
                        AlertDialog(
                            onDismissRequest = { showConfirmSwitchDialog = false },
                            title = { Text("Switch Role to $targetRole?") },
                            text = {
                                Text(
                                    "Your current role will be changed to $targetRole. You will be redirected to complete the readiness setup for this role.",
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showConfirmSwitchDialog = false
                                        executeRoleSwitch()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                ) {
                                    Text("Yes, Switch Role")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showConfirmSwitchDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }
                }
            }
        }

        checkExistingSession()
        PushTokenRegistrar.register(this)
        handleOAuthIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun checkExistingSession() {
        lifecycleScope.launch {
            val token = authClient.accessToken()
            if (token.isNullOrBlank()) {
                resetAccountState()
                screen = AppScreen.Login
                return@launch
            }

            profileClient.fetchMyProfile()
                .onSuccess { profile ->
                    if (profile != null) {
                        val currentUserId = authClient.userId()
                        if (loadedSessionUserId != null && loadedSessionUserId != currentUserId) {
                            resetAccountState()
                        }
                        loadedSessionUserId = currentUserId
                        PushTokenRegistrar.register(this@MainActivity)
                        fullName = profile.fullName
                        email = profile.email
                        phone = profile.phone
                        selectedRole = profile.role
                            .takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
                            ?.replaceFirstChar { it.uppercase() }

                        if (profile.setupCompletedAt != null) {
                            if (profile.role.equals("caregiver", ignoreCase = true)) {
                                screen = AppScreen.CaregiverDashboard
                                refreshCaregiverData()
                            } else {
                                screen = AppScreen.BeneficiaryDashboard
                                refreshBeneficiaryData()
                            }
                        } else if (profile.role.isNotBlank()) {
                            screen = AppScreen.Setup
                        } else {
                            screen = AppScreen.RoleSelection
                        }
                    } else {
                        screen = AppScreen.RoleSelection
                    }
                }
                .onFailure {
                    screen = AppScreen.Login
                }
        }
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val callback = intent?.data ?: return
        if (callback.scheme != "soundguard" || callback.host != "auth") return
        lifecycleScope.launch {
            authClient.handleGoogleCallback(callback)
                .onSuccess {
                    checkExistingSession()
                }
                .onFailure { authMessage = it.message ?: "Google sign-in failed." }
        }
    }

    private fun sendOtp() {
        if (!Validation.isEmailValid(email)) {
            authMessage = "Enter a valid email address."
            return
        }
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
                        "Email rate limit reached. Wait before requesting another code."
                    } else {
                        detail.ifBlank { "Could not send OTP." }
                    }
                }
            authBusy = false
        }
    }

    private fun verifyOtp() {
        if (otp.trim().length < 6) {
            authMessage = "Enter the verification code from your email."
            return
        }
        authBusy = true
        authMessage = null
        lifecycleScope.launch {
            authClient.verifyOtp(email, otp)
                .onSuccess {
                    checkExistingSession()
                }
                .onFailure { authMessage = it.message ?: "Could not verify OTP." }
            authBusy = false
        }
    }

    private fun confirmSetup() {
        selectedRole?.let { role ->
            setupMessage = null
            lifecycleScope.launch {
                profileClient.saveProfile(email, fullName, phone, role)
                    .onSuccess {
                        if (role.equals("beneficiary", ignoreCase = true)) {
                            profileClient.saveBeneficiarySettings(monitoringConsent)
                                .onFailure { setupMessage = it.message ?: "Could not save monitoring consent." }
                        }
                        if (setupMessage == null) {
                        if (role.equals("caregiver", ignoreCase = true)) {
                            screen = AppScreen.CaregiverDashboard
                            refreshCaregiverData()
                        } else {
                            screen = AppScreen.BeneficiaryDashboard
                            refreshBeneficiaryData()
                        }
                        }
                    }
                    .onFailure { setupMessage = it.message ?: "Could not save profile." }
            }
        }
    }

    private fun updateProfile() {
        if (!Validation.isPhoneValid(phone)) {
            Toast.makeText(this, "Enter a valid international phone number.", Toast.LENGTH_SHORT).show()
            return
        }
        selectedRole?.takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }?.let { role ->
            lifecycleScope.launch {
                profileClient.saveProfile(email, fullName, phone, role)
                    .onSuccess {
                        Toast.makeText(this@MainActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(this@MainActivity, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
        } ?: Toast.makeText(this, "Choose a role before saving your profile.", Toast.LENGTH_LONG).show()
    }

    private fun handleRoleSwitchRequest() {
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.countActiveConnections()
                .onSuccess { activeCount ->
                    dashboardLoading = false
                    if (activeCount > 0) {
                        blockedActiveConnectionsCount = activeCount
                        showBlockedSwitchDialog = true
                    } else {
                        showConfirmSwitchDialog = true
                    }
                }
                .onFailure {
                    dashboardLoading = false
                    showConfirmSwitchDialog = true
                }
        }
    }

    private fun executeRoleSwitch() {
        lifecycleScope.launch {
            profileClient.resetRole()
                .onSuccess {
                    selectedRole = null
                    screen = AppScreen.RoleSelection
                    Toast.makeText(this@MainActivity, "Role reset. Please select your new role.", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    selectedRole = null
                    screen = AppScreen.RoleSelection
                }
        }
    }

    private fun requestAppPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setupPermissions.launch(permissions.toTypedArray())
    }

    private fun refreshBeneficiaryData() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.fetchCaregiversForBeneficiary()
                .onSuccess { list ->
                    connectedCaregivers = list
                }
                .onFailure { err ->
                    dashboardMessage = err.message
                }
            IncidentClient(this@MainActivity).fetchOwnIncidents()
                .onSuccess { list -> sharedIncidents = list }
            dashboardLoading = false
        }
    }

    private fun refreshCaregiverData() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.fetchBeneficiariesForCaregiver()
                .onSuccess { list ->
                    monitoredBeneficiaries = list
                }
                .onFailure { err ->
                    dashboardMessage = err.message
                }
            notificationClient.fetchMine()
                .onSuccess { list -> caregiverNotifications = list }
            dashboardLoading = false
        }
    }

    private fun acknowledgeNotification(id: String) {
        lifecycleScope.launch {
            notificationClient.acknowledge(id)
                .onSuccess { refreshCaregiverData() }
                .onFailure { dashboardMessage = it.message }
        }
    }

    private fun requestDemoSnapshotAndOpenCamera() {
        val incidentId = selectedNotification?.incidentId ?: AudioMonitoringService.audioState.value.backendIncidentId
        if (incidentId == null) {
            demoSnapshotMessage = "Waiting for the incident to finish syncing to Supabase."
            return
        }
        lifecycleScope.launch {
            val beneficiaryId = previewBeneficiary?.beneficiaryId ?: JAMES_TEST_ID
            SnapshotClient(this@MainActivity)
                .requestSnapshot(incidentId, beneficiaryId)
                .onSuccess { request ->
                    demoSnapshotRequest = request
                    demoPhotoRequested = true
                    demoPhotoDecision = "approved"
                    demoPhotoRequestedAt = demoPhotoRequestedAt ?: System.currentTimeMillis()
                    demoPhotoDecisionAt = System.currentTimeMillis()
                    demoSnapshotMessage = "Photo request logged. Capture a photo to upload it."
                    returnToPreviewAfterCamera = true
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        screen = AppScreen.CameraTest
                    } else {
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                }
                .onFailure { demoSnapshotMessage = "Photo request failed: ${it.message}" }
        }
    }

    private fun generatePairingCode() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.createPairingCode()
                .onSuccess { code ->
                    generatedPairingCode = code
                }
                .onFailure { err ->
                    dashboardMessage = err.message ?: "Failed to create pairing code."
                }
            dashboardLoading = false
        }
    }

    private fun pairBeneficiary(code: String) {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.acceptPairingCode(code)
                .onSuccess { beneficiaryName ->
                    Toast.makeText(this@MainActivity, "Connected to $beneficiaryName!", Toast.LENGTH_SHORT).show()
                    refreshCaregiverData()
                }
                .onFailure { err ->
                    dashboardMessage = err.message ?: "Failed to pair with code."
                }
            dashboardLoading = false
        }
    }

    private fun setPrimaryCaregiver(connectionId: String, caregiverId: String) {
        val userId = authClient.userId() ?: return
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.setPrimaryCaregiver(userId, connectionId)
                .onSuccess { refreshBeneficiaryData() }
                .onFailure { dashboardMessage = it.message }
            dashboardLoading = false
        }
    }

    private fun removeCaregiver(connectionId: String) {
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.removeCareConnection(connectionId)
                .onSuccess { refreshBeneficiaryData() }
                .onFailure { dashboardMessage = it.message }
            dashboardLoading = false
        }
    }

    private fun removeBeneficiary(connectionId: String) {
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.removeCareConnection(connectionId)
                .onSuccess { refreshCaregiverData() }
                .onFailure { dashboardMessage = it.message }
            dashboardLoading = false
        }
    }

    private fun callPhoneNumber(targetPhone: String) {
        if (targetPhone.isBlank()) {
            Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show()
            return
        }
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${targetPhone.trim()}"))
        startActivity(dialIntent)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SoundGuard Pairing Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Pairing code copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun signOut() {
        AudioMonitoringService.stop(this)
        resetAccountState()
        authClient.signOut()
        screen = AppScreen.Login
    }

    private fun resetAllAccountData() {
        lifecycleScope.launch {
            profileClient.resetAllAccountData()
                .onSuccess {
                    resetAccountState()
                    screen = AppScreen.RoleSelection
                    Toast.makeText(this@MainActivity, "All account data deleted. Choose your role again.", Toast.LENGTH_LONG).show()
                }
                .onFailure { Toast.makeText(this@MainActivity, "Could not delete account data: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun resetAccountState() {
        demoPhotoPath?.let { path -> runCatching { java.io.File(path).delete() } }
        AudioMonitoringService.resetSessionState()
        loadedSessionUserId = null
        email = ""
        otp = ""
        otpSent = false
        fullName = ""
        phone = ""
        selectedRole = null
        monitoringConsent = false
        termsAccepted = false
        connectedCaregivers = emptyList()
        sharedIncidents = emptyList()
        monitoredBeneficiaries = emptyList()
        caregiverNotifications = emptyList()
        previewBeneficiary = null
        generatedPairingCode = null
        demoCaregiverLinked = false
        demoPhotoRequested = false
        demoPhotoDecision = null
        demoPhotoRequestedAt = null
        demoPhotoDecisionAt = null
        demoPhotoPath = null
        demoSnapshotRequest = null
        demoSnapshotMessage = null
    }
}

// -------------------------------------------------------------------------------------------------
// UI SCREENS
// -------------------------------------------------------------------------------------------------

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeTopBar(
    title: String,
    roleSubtitle: String,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🛡️", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    roleSubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.padding(end = 6.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Text("🔄 Refresh", fontSize = 13.sp)
            }
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Text("⚙️ Settings", fontSize = 13.sp)
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🛡️ SoundGuard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Privacy-first sound monitoring & caregiver alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onGoogleLogin,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Continue with Google", fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f))
            Text(" OR EMAIL OTP ", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelSmall)
            HorizontalDivider(Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        if (otpSent) {
            OutlinedTextField(
                value = otp,
                onValueChange = onOtpChange,
                label = { Text("6-digit verification code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onVerifyOtp,
                enabled = !busy && otp.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Verify Code & Sign In")
            }
        } else {
            Button(
                onClick = onSendOtp,
                enabled = !busy && email.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (otpCooldownSeconds > 0) "Resend in ${otpCooldownSeconds}s" else "Send OTP Code")
            }
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RoleSelection(onRoleSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🛡️ Select Your Role", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "How do you plan to use SoundGuard?",
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            onClick = { onRoleSelected("Beneficiary") },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🛡️ I Need Monitoring (Beneficiary)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "This device will monitor environmental sounds locally and alert my caregivers when assistance is needed.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            onClick = { onRoleSelected("Caregiver") },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("❤️ I Am a Caregiver", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "I will link with family members or beneficiaries to receive instant alerts, view status timelines, and verify safety.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("← Change Role / Back")
        }
        Spacer(Modifier.height(16.dp))

        Text("${role ?: "User"} Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Complete profile information and verify safety permissions.",
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Emergency phone number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (Validation.isPhoneValid(phone)) "✓ Valid international phone number"
            else "Use international format (e.g. +65 81234567, +1 5551234567)",
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (Validation.isPhoneValid(phone)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (role == "Beneficiary") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = onConsentChange)
                Text("I consent to local audio monitoring & emergency alerts", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = onTermsChange)
            Text("I agree to the Terms of Use", style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onOpenTerms) {
            Text("Read Terms of Use and camera privacy rules")
        }

        Spacer(Modifier.height(16.dp))
        Text("Device Readiness Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        ChecklistItem(isDone = fullName.isNotBlank() && phone.isNotBlank(), title = "Profile and Phone Number")
        ChecklistItem(isDone = role != "Beneficiary" || consent, title = "Monitoring Consent")
        ChecklistItem(isDone = microphoneGranted, title = "Microphone Permission")
        ChecklistItem(isDone = notificationsGranted, title = "Notification Permission")
        ChecklistItem(isDone = cameraReady, title = "Camera Readiness")

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRequestPermissions, modifier = Modifier.weight(1f)) {
                Text("Permissions")
            }
            Button(onClick = onCameraTest, modifier = Modifier.weight(1f)) {
                Text("Test Camera")
            }
        }

        Spacer(Modifier.height(20.dp))
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Complete Setup & Open Dashboard")
        }

        setupMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChecklistItem(isDone: Boolean, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(if (isDone) "✓ " else "○ ", color = if (isDone) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TermsScreen(onBack: () -> Unit, onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(16.dp))
        Text("Terms of Use & Privacy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "SoundGuard is a safety support prototype, not a medical device or certified emergency response system. " +
                "It may miss sounds or produce false alerts. Keep the device powered, online, and positioned safely.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera privacy: Connected caregivers may request a verification snapshot during an active incident. " +
                "Snapshots are access-controlled and automatically deleted after 10 minutes. You can revoke this in Settings.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Emergency Notice: SoundGuard does not automatically contact police or ambulance services. " +
                "If an emergency occurs, caregivers and beneficiaries must contact local emergency services.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
            Text("I Understand and Agree")
        }
    }
}

@Composable
private fun CameraTestScreen(onBack: () -> Unit, onFinished: (String?) -> Unit) {
    var capturedPath by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Text(
                "Camera Readiness Test",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        CameraPreview(
            modifier = Modifier.weight(1f),
            onPhotoCaptured = { capturedPath = it },
        )
        if (capturedPath != null) {
            Text(
                "✓ Photo captured locally for verification",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Button(
            onClick = { capturedPath?.let(onFinished) },
            enabled = capturedPath != null,
            modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally).fillMaxWidth(),
        ) {
            Text(if (capturedPath == null) "Capture a photo first" else "Finish Camera Test")
        }
    }
}

// -------------------------------------------------------------------------------------------------
// BENEFICIARY DASHBOARD
// -------------------------------------------------------------------------------------------------

@Composable
private fun BeneficiaryDashboard(
    fullName: String,
    email: String,
    audioState: LiveAudioState,
    onToggleMonitoring: () -> Unit,
    onSimulateSound: (category: String, label: String, confidence: Float, isEmergency: Boolean) -> Unit,
    caregivers: List<CaregiverMember>,
    sharedIncidents: List<SharedIncident>,
    pairingCode: String?,
    loading: Boolean,
    message: String?,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onSetPrimary: (String, String) -> Unit,
    onRemoveCaregiver: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenCaregiverPreview: () -> Unit,
    onLinkDemoCaregiver: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val statusCardColor by animateColorAsState(
        targetValue = when {
            audioState.isEmergency -> MaterialTheme.colorScheme.errorContainer
            audioState.isMonitoring -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "status_color",
    )

        Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        HomeTopBar(
            title = fullName.ifBlank { "Beneficiary" },
            roleSubtitle = "Beneficiary Mode",
            onOpenSettings = onOpenSettings,
            onRefresh = onRefresh,
        )

        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Shared caregiver chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (sharedIncidents.isEmpty()) {
                    Text("No shared incidents yet.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                } else {
                    sharedIncidents.take(10).forEach { incident ->
                        Text(
                            "${if (incident.severity == "high") "🚨" else "ℹ️"} ${incident.label} • ${(incident.confidence * 100).toInt()}% • ${incident.status}",
                            color = if (incident.severity == "high") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        // Live Monitoring Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = statusCardColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when {
                                audioState.isEmergency -> "🚨 EMERGENCY SOUND DETECTED"
                                audioState.isMonitoring -> "🟢 Active Monitoring (Local YAMNet)"
                                else -> "⏸️ Monitoring Paused"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            when {
                                audioState.isEmergency -> "Alert event detected: ${audioState.displayLabel}"
                                audioState.isMonitoring -> "Detected: ${audioState.displayLabel} (${(audioState.confidence * 100).toInt()}%)"
                                else -> "Tap Start to begin continuous local sound monitoring"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Button(
                        onClick = onToggleMonitoring,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (audioState.isMonitoring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(if (audioState.isMonitoring) "Stop" else "Start")
                    }
                }

                if (audioState.isMonitoring) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Live Mic Level:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${(audioState.amplitude * 100).toInt()}%" + if (audioState.isSimulated) " (Simulated)" else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (audioState.isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { audioState.amplitude },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = if (audioState.isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sound Testing & Model Simulator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sound Model Testing & Simulator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Test sound classification events directly on this device or emulator:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSimulateSound("alarm", "Smoke Alarm (Simulated)", 0.94f, true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("🔥 Smoke Alarm", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onSimulateSound("glass_break", "Glass Break (Simulated)", 0.91f, true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("💥 Glass Break", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSimulateSound("doorbell", "Doorbell / Knock", 0.85f, false) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("🔔 Doorbell", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onSimulateSound("background", "Normal Background", 0.96f, false) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("🍃 Normal Ambient", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Caregiver Team Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Caregiver Team (${caregivers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                }

                if (caregivers.isEmpty()) {
                    Text(
                        "No caregivers linked yet. Generate a pairing code below to invite a family member or caregiver.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    caregivers.forEach { caregiver ->
                        Spacer(Modifier.height(8.dp))
                        CaregiverRow(
                            caregiver = caregiver,
                            onSetPrimary = { onSetPrimary(caregiver.connectionId, caregiver.caregiverId) },
                            onRemove = { onRemoveCaregiver(caregiver.connectionId) },
                            onCall = { onCall(caregiver.phone) },
                            onOpenChat = onOpenCaregiverPreview,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Caregiver Admin Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Link a demo caregiver on this phone and preview exactly what the caregiver dashboard will show after an alert.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenCaregiverPreview) {
                        Text("Preview Dashboard")
                    }
                }
                Text(
                    "Demo-only: this does not create a real Supabase caregiver connection.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Pairing Code Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Invite Caregiver", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Generate a 6-character code for your caregiver to connect from their app.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )

                if (pairingCode != null) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            pairingCode,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onCopyCode(pairingCode) }) {
                            Text("Copy Code")
                        }
                        OutlinedButton(onClick = onGenerateCode) {
                            Text("Generate New")
                        }
                    }
                    Text("Code expires in 24 hours.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                } else {
                    Button(onClick = onGenerateCode, enabled = !loading) {
                        Text("Generate Pairing Code")
                    }
                }
            }
        }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CaregiverRow(
    caregiver: CaregiverMember,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
    onCall: () -> Unit,
    onOpenChat: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(caregiver.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(caregiver.phone.ifBlank { caregiver.email }, style = MaterialTheme.typography.bodySmall)
                }
                if (caregiver.isPrimary) {
                    Text("⭐ Primary", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                } else {
                    Text("#${caregiver.escalationOrder} Backup", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (caregiver.phone.isNotBlank()) {
                    OutlinedButton(onClick = onCall, modifier = Modifier.padding(end = 6.dp)) {
                        Text("📞 Call")
                    }
                }
                OutlinedButton(onClick = onOpenChat, modifier = Modifier.padding(end = 6.dp)) {
                    Text("Open Chat")
                }
                if (!caregiver.isPrimary) {
                    TextButton(onClick = onSetPrimary) {
                        Text("Make Primary")
                    }
                }
                TextButton(onClick = onRemove) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CaregiverPreviewScreen(
    beneficiaryName: String,
    beneficiaryPhone: String,
    audioState: LiveAudioState,
    backendAlert: CaregiverNotification?,
    demoCaregiverLinked: Boolean,
    onBack: () -> Unit,
    onLinkDemoCaregiver: () -> Unit,
    onCall: () -> Unit,
    photoRequested: Boolean,
    photoDecision: String?,
    photoRequestedAt: Long?,
    photoDecisionAt: Long?,
    photoPath: String?,
    snapshotMessage: String?,
    onOpenCamera: () -> Unit,
) {
    var showAlertDialog by remember { mutableStateOf(false) }
    var shownAlertTimestamp by remember { mutableStateOf(audioState.alertHistory.lastOrNull()?.timestamp) }
    var acknowledged by remember { mutableStateOf(false) }
    var photoRequested by remember { mutableStateOf(photoRequested) }
    var photoDecision by remember { mutableStateOf(photoDecision) }
    var photoRequestedAt by remember { mutableStateOf(photoRequestedAt) }
    var photoDecisionAt by remember { mutableStateOf(photoDecisionAt) }
    var showFollowUpDialog by remember { mutableStateOf(photoPath != null) }
    var showPhotoDetail by remember { mutableStateOf(false) }
    var acknowledgedAt by remember { mutableStateOf<Long?>(null) }
    val previewBitmap = remember(photoPath) { photoPath?.let { BitmapFactory.decodeFile(it) } }

    LaunchedEffect(audioState.alertHistory) {
        val event = audioState.alertHistory.lastOrNull()
        val timestamp = event?.timestamp
        if (event?.severity == SoundSeverity.High && timestamp != shownAlertTimestamp) {
            shownAlertTimestamp = timestamp
            showAlertDialog = true
            acknowledged = false
            photoRequested = false
            photoDecision = null
            photoRequestedAt = null
            photoDecisionAt = null
            acknowledgedAt = null
        }
    }

    val latestEvent = audioState.alertHistory.lastOrNull()
    val hasAlert = latestEvent != null || backendAlert != null
    val alertLabel = latestEvent?.label ?: backendAlert?.soundLabel?.ifBlank { null }
        ?: audioState.lastEmergencyLabel ?: audioState.displayLabel
    val alertConfidence = latestEvent?.confidence ?: backendAlert?.confidence
        ?: audioState.lastEmergencyConfidence ?: audioState.confidence
    val alertTimestamp = latestEvent?.timestamp ?: audioState.lastEmergencyTimestamp
    val alertTimeText = if (alertTimestamp != null) formatAlertTime(alertTimestamp) else backendAlert?.createdAt ?: "Unknown"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onBack) { Text("← Back") }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(beneficiaryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Caregiver chat", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onCall) { Text("📞") }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Linked caregiver", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (demoCaregiverLinked) "Demo Caregiver" else "No demo caregiver linked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasAlert) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${if (hasAlert) "🚨" else "🟢"} ${beneficiaryName}", fontWeight = FontWeight.Bold)
                Text(beneficiaryPhone.ifBlank { "No phone number saved" }, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                if (hasAlert) {
                    Text("ALERT: $alertLabel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Confidence: ${(alertConfidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Text("Detected at $alertTimeText", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        if (acknowledged) "Status: Acknowledged by caregiver" else "Status: Caregiver notification required",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCall) { Text("Call") }
                    }
                    OutlinedButton(
                        onClick = {
                            photoRequested = true
                            photoDecision = null
                            photoRequestedAt = System.currentTimeMillis()
                        },
                        enabled = !photoRequested,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(if (photoRequested) "Photo Request Sent" else "Request Verification Photo")
                    }
                    if (photoRequested) {
                        Text(
                            when (photoDecision) {
                                "approved" -> "Beneficiary approved the verification photo request."
                                "declined" -> "Beneficiary declined the verification photo request."
                                else -> "Photo request pending beneficiary approval."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (photoDecision == null) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(onClick = { photoDecision = "approved"; photoDecisionAt = System.currentTimeMillis(); onOpenCamera() }) { Text("Approve") }
                                OutlinedButton(onClick = { photoDecision = "declined"; photoDecisionAt = System.currentTimeMillis() }) { Text("Decline") }
                            }
                        }
                        snapshotMessage?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                } else {
                    Text("No active alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("The caregiver would see this beneficiary as safe while monitoring is active.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (audioState.alertHistory.isEmpty() && backendAlert == null) {
                    ChatBubble(
                        text = "SoundGuard • Monitoring active\nNo incidents to review",
                        fromSystem = true,
                        timestamp = null,
                    )
                } else {
                    audioState.alertHistory.forEach { event ->
                        ChatBubble(
                            text = "${if (event.severity == SoundSeverity.High) "🚨" else "ℹ️"} ${event.label}\nConfidence: ${(event.confidence * 100).toInt()}%",
                            fromSystem = true,
                            timestamp = event.timestamp,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (audioState.alertHistory.isEmpty() && backendAlert != null) {
                        ChatBubble(
                            text = "🚨 ${backendAlert.soundLabel.ifBlank { "Alert" }}\nConfidence: ${(backendAlert.confidence * 100).toInt()}%\nStatus: ${backendAlert.status}",
                            fromSystem = true,
                            timestamp = null,
                        )
                    }
                }
                if (photoRequested) {
                    Spacer(Modifier.height(8.dp))
                    ChatBubble(text = "Verification photo requested by caregiver.", fromSystem = false, timestamp = photoRequestedAt)
                if (photoDecision != null) {
                        Spacer(Modifier.height(8.dp))
                        ChatBubble(
                            text = if (photoDecision == "approved") "Beneficiary approved the request. Camera opened." else "Beneficiary declined the request.",
                            fromSystem = true,
                            timestamp = photoDecisionAt,
                        )
                    }
                    if (photoDecision == "approved" && photoPath != null) {
                        Spacer(Modifier.height(8.dp))
                        ChatBubble(
                            text = "Verification photo captured and ready for review.",
                            fromSystem = true,
                            timestamp = photoDecisionAt,
                        )
                        if (previewBitmap != null) {
                            Spacer(Modifier.height(8.dp))
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Verification photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clickable { showPhotoDetail = true },
                            )
                            TextButton(
                                onClick = { showPhotoDetail = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text("View photo in detail")
                            }
                        }
                    }
                }
                if (acknowledged) {
                    Spacer(Modifier.height(8.dp))
                    ChatBubble(text = "Caregiver acknowledged the alert.", fromSystem = false, timestamp = acknowledgedAt)
                }
            }
        }

        if (showAlertDialog && hasAlert) {
            AlertDialog(
                onDismissRequest = { showAlertDialog = false },
                title = { Text("🚨 SoundGuard Alert") },
                text = {
                    Text(
                        "$alertLabel detected for $beneficiaryName.\n\n" +
                            "Confidence: ${(alertConfidence * 100).toInt()}%\n" +
                            "Time: $alertTimeText\n\n" +
                            "Check on the beneficiary and request a verification photo if authorized.",
                    )
                },
                confirmButton = {
                     TextButton(onClick = { showAlertDialog = false }) { Text("Close") }
                },
                dismissButton = {
                    TextButton(onClick = { showAlertDialog = false; onCall() }) { Text("Call") }
                },
            )
        }

        if (showFollowUpDialog && photoPath != null) {
            AlertDialog(
                onDismissRequest = { showFollowUpDialog = false },
                title = { Text("Verification complete") },
                text = {
                    Text(
                        "The verification photo is ready. For further confirmation, SoundGuard recommends calling or video calling $beneficiaryName.",
                    )
                },
                confirmButton = {
                    Button(onClick = { showFollowUpDialog = false; onCall() }) { Text("Call $beneficiaryName") }
                },
                dismissButton = {
                    TextButton(onClick = { showFollowUpDialog = false }) { Text("Not now") }
                },
            )
        }

        if (showPhotoDetail && previewBitmap != null) {
            AlertDialog(
                onDismissRequest = { showPhotoDetail = false },
                title = { Text("Verification photo") },
                text = {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Detailed verification photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPhotoDetail = false }) { Text("Close") }
                },
            )
        }
    }
}

@Composable
private fun ChatBubble(text: String, fromSystem: Boolean, timestamp: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromSystem) Arrangement.Start else Arrangement.End,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (fromSystem) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatAlertTime(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun formatAlertTime(timestamp: Long?): String {
    if (timestamp == null) return "--"
    return SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(timestamp))
}

// -------------------------------------------------------------------------------------------------
// CAREGIVER DASHBOARD
// -------------------------------------------------------------------------------------------------

@Composable
private fun CaregiverDashboard(
    fullName: String,
    email: String,
    beneficiaries: List<MonitoredBeneficiary>,
    loading: Boolean,
    message: String?,
    onConnectBeneficiary: (String) -> Unit,
    onRemoveBeneficiary: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenBeneficiary: (MonitoredBeneficiary) -> Unit,
    notifications: List<CaregiverNotification>,
    onAcknowledgeNotification: (String) -> Unit,
    onOpenNotification: (CaregiverNotification) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onRefresh()
        while (true) {
            delay(15_000L)
            onRefresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
            HomeTopBar(
            title = fullName.ifBlank { "Caregiver" },
            roleSubtitle = "Caregiver Mode",
            onOpenSettings = onOpenSettings,
            onRefresh = onRefresh,
            )

        // Connect Beneficiary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Connect to Beneficiary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Enter the 6-character code displayed on the beneficiary's device.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 6) codeInput = it.uppercase() },
                        label = { Text("6-Character Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (codeInput.length == 6) {
                                onConnectBeneficiary(codeInput)
                                codeInput = ""
                            }
                        },
                        enabled = !loading && codeInput.length == 6,
                        modifier = Modifier.height(56.dp),
                    ) {
                        Text("Connect")
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Notification timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (notifications.isEmpty()) {
                    Text("No backend notifications yet.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                } else {
                    notifications.take(10).forEach { notification ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clickable { onOpenNotification(notification) }
                                .background(
                                    if (notification.status != "acknowledged") MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    notification.soundLabel.ifBlank { "Incident ${notification.incidentId.take(8)}" },
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${notification.severity.ifBlank { "alert" }} • ${(notification.confidence * 100).toInt()}% • ${notification.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(onClick = { onOpenNotification(notification) }) { Text("Open Chat") }
                        }
                    }
                }
            }
        }

        // Beneficiaries List
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Monitored Beneficiaries (${beneficiaries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                }

                if (beneficiaries.isEmpty()) {
                    Text(
                        "You have not connected to any beneficiaries yet. Enter a pairing code above to begin monitoring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    beneficiaries.forEach { beneficiary ->
                        Spacer(Modifier.height(8.dp))
                        BeneficiaryRow(
                            beneficiary = beneficiary,
                            onRemove = { onRemoveBeneficiary(beneficiary.connectionId) },
                            onCall = { onCall(beneficiary.phone) },
                            onOpen = { onOpenBeneficiary(beneficiary) },
                        )
                    }
                }
            }
        }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BeneficiaryRow(
    beneficiary: MonitoredBeneficiary,
    onRemove: () -> Unit,
    onCall: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(beneficiary.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(beneficiary.phone.ifBlank { beneficiary.email }, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onOpen) { Text("Open chat") }
                if (beneficiary.isPrimary) {
                    Text("⭐ Primary Caregiver", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (beneficiary.phone.isNotBlank()) {
                    OutlinedButton(onClick = onCall, modifier = Modifier.padding(end = 6.dp)) {
                        Text("📞 Call")
                    }
                }
                TextButton(onClick = onRemove) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETTINGS SCREEN
// -------------------------------------------------------------------------------------------------

@Composable
private fun SettingsScreen(
    fullName: String,
    email: String,
    phone: String,
    currentRole: String,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onRequestSwitchRole: () -> Unit,
    onOpenTerms: () -> Unit,
    onCameraTest: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onResetAllData: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(16.dp))
            Text("Settings & Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        // Profile Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profile Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Emergency Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text("Signed in as: $email", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(12.dp))
                Button(onClick = onSaveProfile, modifier = Modifier.align(Alignment.End)) {
                    Text("Save Changes")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Account Role & Safeguards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Account Role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Current active role: $currentRole",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Switching roles is only allowed when you have 0 active connections to prevent leaving beneficiaries unprotected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                val targetRole = if (currentRole.equals("caregiver", ignoreCase = true)) "Beneficiary" else "Caregiver"
                Button(
                    onClick = onRequestSwitchRole,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text("Switch Role to $targetRole", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Diagnostics & Readiness
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Device & Safety Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ChecklistItem(isDone = microphoneGranted, title = "Microphone Permission (Audio Monitoring)")
                ChecklistItem(isDone = notificationsGranted, title = "Push Notification Permission")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCameraTest, modifier = Modifier.weight(1f)) {
                        Text("Test Camera")
                    }
                    OutlinedButton(onClick = onOpenTerms, modifier = Modifier.weight(1f)) {
                        Text("Terms & Privacy")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Sign Out
        OutlinedButton(
            onClick = onResetAllData,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Delete All Data & Start Over")
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Sign Out of SoundGuard")
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "SoundGuard v0.4 • Hackathon Prototype",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
