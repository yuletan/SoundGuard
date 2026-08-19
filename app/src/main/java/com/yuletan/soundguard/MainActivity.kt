package com.yuletan.soundguard

import android.Manifest
import kotlinx.coroutines.Dispatchers
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Mic
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

private enum class AppScreen {
    Loading,
    Login,
    RoleSelection,
    Setup,
    CameraTest,
    Terms,
    BeneficiaryDashboard,
    CaregiverDashboard,
    Chat,
    Settings,
}

class MainActivity : ComponentActivity() {
    private var screen by mutableStateOf(AppScreen.Loading)
    private var selectedRole by mutableStateOf<String?>(null)
    private var fullName by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var monitoringConsent by mutableStateOf(false)
    private var autoApproveCameraRequests by mutableStateOf(true)
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
    private var monitoredBeneficiaries by mutableStateOf<List<MonitoredBeneficiary>>(emptyList())
    private var caregiverNotifications by mutableStateOf<List<CaregiverNotification>>(emptyList())
    private var generatedPairingCode by mutableStateOf<String?>(null)
    private var dashboardLoading by mutableStateOf(false)
    private var dashboardMessage by mutableStateOf<String?>(null)
    private var demoCaregiverLinked by mutableStateOf(false)
    private var demoLabUnlocked by mutableStateOf(false)
    private var returnToPreviewAfterCamera by mutableStateOf(false)
    private var demoPhotoRequested by mutableStateOf(false)
    private var demoPhotoDecision by mutableStateOf<String?>(null)
    private var demoPhotoRequestedAt by mutableStateOf<Long?>(null)
    private var demoPhotoDecisionAt by mutableStateOf<Long?>(null)
    private var demoPhotoPath by mutableStateOf<String?>(null)
    private var demoSnapshotRequest by mutableStateOf<SnapshotRequest?>(null)
    private var demoSnapshotMessage by mutableStateOf<String?>(null)
    private var pendingSnapshotRequest by mutableStateOf<PendingSnapshotRequest?>(null)
    private var loadedSessionUserId: String? = null
    private var showResetDataDialog by mutableStateOf(false)

    // Chat State
    private var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
    private var selectedChatPartner by mutableStateOf<ChatPreview?>(null)
    private var chatLoading by mutableStateOf(false)

    // Role Switch Dialog States
    private var showBlockedSwitchDialog by mutableStateOf(false)
    private var blockedActiveConnectionsCount by mutableStateOf(0)
    private var showConfirmSwitchDialog by mutableStateOf(false)

    private val authClient by lazy { AuthClient(this) }
    private val profileClient by lazy { ProfileClient(this) }
    private val careClient by lazy { CareClient(this) }
    private val notificationClient by lazy { NotificationClient(this) }
    private val chatRepository by lazy { ChatRepository(this) }

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
        enableEdgeToEdge()

        setContent {
            val liveAudioState by AudioMonitoringService.audioState.collectAsState()

            SoundGuardTheme {
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
                             autoApproveCameraRequests = autoApproveCameraRequests,
                            termsAccepted = termsAccepted,
                            microphoneGranted = microphoneGranted,
                            notificationsGranted = notificationsGranted,
                            cameraReady = cameraReady,
                            setupMessage = setupMessage,
                            onFullNameChange = { fullName = it },
                            onPhoneChange = { phone = it },
                             onConsentChange = { monitoringConsent = it },
                             onAutoApproveCameraRequestsChange = { autoApproveCameraRequests = it },
                            onTermsChange = { termsAccepted = it },
                            onOpenTerms = { screen = AppScreen.Terms },
                            onRequestPermissions = { requestAppPermissions() },
                            onCameraTest = { cameraPermission.launch(Manifest.permission.CAMERA) },
                            onBack = { screen = AppScreen.RoleSelection },
                            onConfirm = { confirmSetup() },
                        )
                        AppScreen.CameraTest -> CameraTestScreen(
                            onBack = {
                                screen = if (returnToPreviewAfterCamera) AppScreen.BeneficiaryDashboard else AppScreen.Setup
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
                                screen = if (returnToPreviewAfterCamera) AppScreen.BeneficiaryDashboard else AppScreen.Setup
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
                             pairingCode = generatedPairingCode,
                            loading = dashboardLoading,
                            message = dashboardMessage,
                            onGenerateCode = { generatePairingCode() },
                             onCopyCode = { code -> copyToClipboard(code) },
                             onShareCode = { code -> sharePairingCode(code) },
                             onSetPrimary = { connectionId, caregiverId -> setPrimaryCaregiver(connectionId, caregiverId) },
                             onRemoveCaregiver = { connectionId -> removeCaregiver(connectionId) },
                             onCall = { targetPhone -> callPhoneNumber(targetPhone) },
                             onOpenCaregiverChat = { partner ->
                                 selectedChatPartner = partner
                                 screen = AppScreen.Chat
                                 loadChatMessages(partner.partnerId)
                             },
                             onIncidentResponse = { response -> AudioMonitoringService.respondToIncident(response) },
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
                             onOpenBeneficiaryChat = { partner ->
                                 selectedChatPartner = partner
                                 screen = AppScreen.Chat
                                 loadChatMessages(partner.partnerId)
                             },
                              onOpenSettings = { screen = AppScreen.Settings },
                              onRefresh = { refreshCaregiverData() },
                          )
                        AppScreen.Chat -> ChatScreen(
                            partnerName = selectedChatPartner?.partnerName ?: "Chat",
                            partnerPhone = selectedChatPartner?.partnerPhone.orEmpty(),
                            messages = chatMessages,
                            loading = chatLoading,
                            isCaregiverView = selectedRole.equals("caregiver", ignoreCase = true),
                            snapshotMessage = demoSnapshotMessage,
                            onRequestPhoto = {
                                if (selectedRole.equals("caregiver", ignoreCase = true)) requestDemoSnapshotAndOpenCamera(false)
                                else demoSnapshotMessage = "Photo requests are made by caregivers."
                            },
                            onCall = { callPhoneNumber(selectedChatPartner?.partnerPhone.orEmpty()) },
                            onBack = {
                                screen = if (selectedRole.equals("caregiver", ignoreCase = true))
                                    AppScreen.CaregiverDashboard else AppScreen.BeneficiaryDashboard
                            },
                            onRefresh = {
                                selectedChatPartner?.let { partner ->
                                    loadChatMessages(partner.partnerId)
                                }
                            },
                        )
                         AppScreen.Settings -> SettingsScreen(
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            currentRole = selectedRole ?: "User",
                             microphoneGranted = microphoneGranted,
                             notificationsGranted = notificationsGranted,
                             autoApproveCameraRequests = autoApproveCameraRequests,
                            onFullNameChange = { fullName = it },
                             onPhoneChange = { phone = it },
                             onAutoApproveCameraRequestsChange = { value ->
                                 autoApproveCameraRequests = value
                                 lifecycleScope.launch {
                                     profileClient.saveBeneficiarySettings(
                                         monitoringConsent = monitoringConsent,
                                         autoApproveCameraRequests = autoApproveCameraRequests,
                                     )
                                         .onFailure { dashboardMessage = it.message }
                                 }
                             },
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
                             demoLabUnlocked = demoLabUnlocked,
                             onUnlockDemoLab = { demoLabUnlocked = true },
                             onOpenDemoChat = {
                                 selectedChatPartner = ChatPreview("demo", "Demo Caregiver", "", "", System.currentTimeMillis(), 0)
                                 screen = AppScreen.Chat
                                 loadChatMessages("demo")
                             },
                         )
                    }

                    pendingSnapshotRequest?.let { request ->
                        AlertDialog(
                            onDismissRequest = { pendingSnapshotRequest = null },
                            title = { Text("Verification photo request") },
                            text = { Text("Your caregiver requested a verification photo during an active incident. Approve this request to allow the photo check.") },
                            confirmButton = {
                                Button(onClick = { decideSnapshotRequest(request.id, true) }) { Text("Approve") }
                            },
                            dismissButton = {
                                TextButton(onClick = { decideSnapshotRequest(request.id, false) }) { Text("Decline") }
                            },
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
                            title = { Text("Cannot Switch Role") },
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
        handleChatDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
        handleChatDeepLink(intent)
    }

    private fun checkExistingSession() {
        lifecycleScope.launch {
            val token = try { authClient.getToken() } catch (_: Exception) { null }
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
                        if (profile.role.equals("beneficiary", ignoreCase = true)) {
                            profileClient.fetchBeneficiarySettings()
                                .onSuccess { autoApproveCameraRequests = it.autoApproveCameraRequests }
                        }
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

    private fun handleChatDeepLink(intent: Intent?) {
        val beneficiaryId = intent?.getStringExtra("beneficiary_id") ?: return
        if (screen != AppScreen.CaregiverDashboard && screen != AppScreen.BeneficiaryDashboard) return
        lifecycleScope.launch {
            val partnerName = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == beneficiaryId }?.name
                ?: connectedCaregivers.firstOrNull { it.caregiverId == beneficiaryId }?.name
                ?: "Chat"
            val partnerPhone = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == beneficiaryId }?.phone
                ?: connectedCaregivers.firstOrNull { it.caregiverId == beneficiaryId }?.phone
                ?: ""
            selectedChatPartner = ChatPreview(
                partnerId = beneficiaryId,
                partnerName = partnerName,
                partnerPhone = partnerPhone,
                lastMessage = "",
                lastTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
            )
            screen = AppScreen.Chat
            loadChatMessages(beneficiaryId)
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
                             profileClient.saveBeneficiarySettings(
                                 monitoringConsent = monitoringConsent,
                                 autoApproveCameraRequests = autoApproveCameraRequests,
                             )
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
            SnapshotClient(this@MainActivity).fetchPendingForBeneficiary()
                .onSuccess { request -> pendingSnapshotRequest = request }
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

    private fun loadChatMessages(partnerId: String) {
        chatLoading = true
        chatMessages = emptyList()
        lifecycleScope.launch {
            chatRepository.buildChatMessages(partnerId, selectedRole.equals("caregiver", ignoreCase = true))
                .onSuccess { chatMessages = it }
                .onFailure { dashboardMessage = it.message }
            chatLoading = false
        }
    }

    private fun decideSnapshotRequest(id: String, approved: Boolean) {
        lifecycleScope.launch {
            val client = SnapshotClient(this@MainActivity)
            if (approved) {
                client.decidePendingRequest(id, true)
                    .onSuccess {
                        client.fetchRequest(id)
                            .onSuccess { request ->
                                demoSnapshotRequest = request
                                demoPhotoRequested = true
                                demoPhotoDecision = "approved"
                                demoPhotoRequestedAt = System.currentTimeMillis()
                                demoPhotoDecisionAt = System.currentTimeMillis()
                                pendingSnapshotRequest = null
                                openCameraForBeneficiarySelfie()
                            }
                            .onFailure {
                                Toast.makeText(this@MainActivity, "Could not load request: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .onFailure {
                        Toast.makeText(this@MainActivity, "Could not save decision: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                client.decidePendingRequest(id, false)
                    .onSuccess {
                        pendingSnapshotRequest = null
                        refreshBeneficiaryData()
                    }
                    .onFailure {
                        Toast.makeText(this@MainActivity, "Could not save decision: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    private fun openCameraForBeneficiarySelfie() {
        returnToPreviewAfterCamera = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            screen = AppScreen.CameraTest
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestDemoSnapshotAndOpenCamera(forceApproval: Boolean) {
        val liveIncidentId = AudioMonitoringService.audioState.value.backendIncidentId
        val beneficiaryId = selectedChatPartner?.partnerId
        if (beneficiaryId == null) {
            demoSnapshotMessage = "No chat partner selected."
            return
        }
        if (liveIncidentId != null) {
            requestSnapshotForIncident(liveIncidentId, beneficiaryId, forceApproval)
        } else {
            demoSnapshotMessage = "Looking up latest incident..."
            lifecycleScope.launch {
                IncidentClient(this@MainActivity).fetchIncidentsForBeneficiary(beneficiaryId)
                    .onSuccess { incidents ->
                        val latest = incidents.lastOrNull()
                        if (latest != null) {
                            requestSnapshotForIncident(latest.id, beneficiaryId, forceApproval)
                        } else {
                            demoSnapshotMessage = "No incidents found. Trigger an alert first."
                        }
                    }
                    .onFailure { demoSnapshotMessage = "Failed to look up incidents: ${it.message}" }
            }
        }
    }

    private fun requestSnapshotForIncident(incidentId: String, beneficiaryId: String, forceApproval: Boolean) {
        lifecycleScope.launch {
            SnapshotClient(this@MainActivity)
                .requestSnapshot(incidentId, beneficiaryId)
                .onSuccess { request ->
                    demoSnapshotRequest = request
                    demoPhotoRequested = true
                    demoPhotoDecision = if (forceApproval || request.approvalStatus == "approved") "approved" else null
                    demoPhotoRequestedAt = demoPhotoRequestedAt ?: System.currentTimeMillis()
                    demoPhotoDecisionAt = System.currentTimeMillis()
                    if (forceApproval || request.approvalStatus == "approved") {
                        demoSnapshotMessage = "Photo request approved. Waiting for beneficiary to capture the photo."
                    } else {
                        demoSnapshotMessage = "Photo request is waiting for beneficiary approval."
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
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${targetPhone.trim()}"))
            startActivity(dialIntent)
        } catch (_: Exception) {
            Toast.makeText(this, "No phone app available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SoundGuard Pairing Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Pairing code copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun sharePairingCode(text: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Use this SoundGuard pairing code: $text")
        }, "Share pairing code"))
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
        monitoredBeneficiaries = emptyList()
        caregiverNotifications = emptyList()
        generatedPairingCode = null
        demoCaregiverLinked = false
        demoPhotoRequested = false
        demoPhotoDecision = null
        demoPhotoRequestedAt = null
        demoPhotoDecisionAt = null
        demoPhotoPath = null
        demoSnapshotRequest = null
        demoSnapshotMessage = null
        autoApproveCameraRequests = true
        chatMessages = emptyList()
        selectedChatPartner = null
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
                Icon(Icons.Outlined.Shield, contentDescription = "SoundGuard", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Open settings")
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Text("SoundGuard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
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
    var selectedRole by remember { mutableStateOf("Beneficiary") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(44.dp))
        Text("Who's this for?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("You can invite the other side to connect once you're set up.", modifier = Modifier.padding(top = 12.dp, bottom = 48.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        RoleOptionCard(
            title = "I want to be monitored",
            description = "Emergency sounds get detected and sent to family who can check on you.",
            symbol = "♡", selected = selectedRole == "Beneficiary", onClick = { selectedRole = "Beneficiary" },
        )
        Spacer(Modifier.height(16.dp))
        RoleOptionCard(
            title = "I'm a caregiver",
            description = "Get notified if someone you care for may be in danger.",
            symbol = "♧", selected = selectedRole == "Caregiver", onClick = { selectedRole = "Caregiver" },
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onRoleSelected(selectedRole) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(50)) { Text("Continue", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun RoleOptionCard(title: String, description: String, symbol: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(210.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text(symbol, fontSize = 38.sp, color = if (selected) Color.White else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SetupScreen(
    role: String?,
    fullName: String,
    phone: String,
    consent: Boolean,
    autoApproveCameraRequests: Boolean,
    termsAccepted: Boolean,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    cameraReady: Boolean,
    setupMessage: String?,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onAutoApproveCameraRequestsChange: (Boolean) -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoApproveCameraRequests, onCheckedChange = onAutoApproveCameraRequestsChange)
                Column {
                    Text("Auto-approve caregiver photo requests", style = MaterialTheme.typography.bodyMedium)
                    Text("Recommended during setup for faster safety checks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun BeneficiaryDashboard(
    fullName: String,
    email: String,
    audioState: LiveAudioState,
    onToggleMonitoring: () -> Unit,
    onIncidentResponse: (BeneficiaryResponse) -> Unit,
    onSimulateSound: (category: String, label: String, confidence: Float, isEmergency: Boolean) -> Unit,
    caregivers: List<CaregiverMember>,
    pairingCode: String?,
    loading: Boolean,
    message: String?,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onShareCode: (String) -> Unit,
    onSetPrimary: (String, String) -> Unit,
    onRemoveCaregiver: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenCaregiverChat: (ChatPreview) -> Unit,
    onLinkDemoCaregiver: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    var showPairingSheet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        onRefresh()
        while (true) {
            delay(15_000L)
            onRefresh()
        }
    }

    val statusCardColor by animateColorAsState(
        targetValue = when {
            audioState.isEmergency -> MaterialTheme.colorScheme.errorContainer
            audioState.isMonitoring -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "status_color",
    )

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
        HomeTopBar(
            title = fullName.ifBlank { "Beneficiary" },
            roleSubtitle = "Beneficiary",
            onOpenSettings = onOpenSettings,
        )

        val incident = audioState.activeIncident
        if (incident?.status == IncidentStatus.WaitingUser && incident.nextDeadlineAt != null) {
            var remaining by remember { mutableStateOf(IncidentStateMachine.TWO_MINUTES_MS) }
            LaunchedEffect(incident.nextDeadlineAt) {
                while (true) {
                    remaining = (incident.nextDeadlineAt - System.currentTimeMillis()).coerceAtLeast(0L)
                    if (remaining == 0L) break
                    delay(1_000L)
                }
            }
            val seconds = (remaining / 1_000L).toInt()
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CountdownRing(remaining, IncidentStateMachine.TWO_MINUTES_MS)
                        Column {
                            Text("Sound detected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Are you okay?", style = MaterialTheme.typography.bodyLarge)
                            Text(incident.soundLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onDangerTint)
                        }
                    }
                    Text("Confirm your safety before we notify caregivers.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onIncidentResponse(BeneficiaryResponse.Safe) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50)) { Text("I'm safe") }
                    OutlinedButton(onClick = { onIncidentResponse(BeneficiaryResponse.NeedHelp) }, modifier = Modifier.fillMaxWidth()) { Text("Send help", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        // Live Monitoring Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(86.dp).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(36.dp)) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(if (audioState.isEmergency) "Sound detected" else if (audioState.isMonitoring) "Listening" else "Monitoring paused", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (audioState.isEmergency) "We’re alerting your caregivers" else if (audioState.isMonitoring) "All quiet right now · Details" else "Start when you’re ready", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onToggleMonitoring, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(50)) { Text(if (audioState.isMonitoring) "Stop monitoring" else "Start monitoring", fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Caregiver Team Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Your caregivers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("+ Add", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
                            onOpenChat = {
                                onOpenCaregiverChat(
                                    ChatPreview(
                                        partnerId = caregiver.caregiverId,
                                        partnerName = caregiver.name,
                                        partnerPhone = caregiver.phone,
                                        lastMessage = "",
                                        lastTimestamp = System.currentTimeMillis(),
                                        unreadCount = 0,
                                    )
                                )
                            },
                        )
                    }
                }
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
                            .clickable { showPairingSheet = true }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            "••••••",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Tap to view code and share", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
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

        Spacer(Modifier.height(16.dp))
        NavigationBar(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Outlined.Home, "Home") }, label = { Text("Home") })
            NavigationBarItem(selected = false, onClick = { caregivers.firstOrNull()?.let { caregiver -> onOpenCaregiverChat(ChatPreview(caregiver.caregiverId, caregiver.name, caregiver.phone, "", System.currentTimeMillis(), 0)) } }, icon = { Icon(Icons.Outlined.Notifications, "Activity") }, label = { Text("Activity") })
            NavigationBarItem(selected = false, onClick = onOpenSettings, icon = { Icon(Icons.Outlined.Settings, "Settings") }, label = { Text("Settings") })
        }
        if (showPairingSheet && pairingCode != null) {
            ModalBottomSheet(onDismissRequest = { showPairingSheet = false }, sheetState = rememberModalBottomSheetState()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pairing code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(pairingCode, fontFamily = FontFamily.Monospace, fontSize = 24.sp, letterSpacing = 4.sp, modifier = Modifier.padding(vertical = 20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onCopyCode(pairingCode) }) { Text("Copy") }
                        OutlinedButton(onClick = { onShareCode(pairingCode) }) { Text("Share") }
                    }
                    TextButton(onClick = onGenerateCode) { Text("Generate new") }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
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
        onClick = onOpenChat,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Text(caregiver.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(caregiver.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    if (caregiver.isPrimary) { Spacer(Modifier.width(8.dp)); StatusChip("Primary") }
                }
                Text("Tap to open chat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (caregiver.phone.isNotBlank()) IconButton(onClick = onCall) { Text("☎", fontSize = 22.sp) }
            IconButton(onClick = { if (caregiver.isPrimary) onRemove() else onSetPrimary() }) { Text("⋮", fontSize = 24.sp) }
        }
    }
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
    onOpenBeneficiaryChat: (ChatPreview) -> Unit,
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
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
            HomeTopBar(
            title = fullName.ifBlank { "Caregiver" },
            roleSubtitle = "Caregiver",
             onOpenSettings = onOpenSettings,
             )

        // Connect Beneficiary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Connect a beneficiary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OtpCodeInput(value = codeInput, onValueChange = { codeInput = it }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Button(onClick = { if (codeInput.length == 6) { onConnectBeneficiary(codeInput); codeInput = "" } }, enabled = !loading && codeInput.length == 6, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(50)) { Text("Connect", fontWeight = FontWeight.Bold) }
                TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Scan QR instead") }
            }
        }

        Spacer(Modifier.height(20.dp))

        /* Chat is opened directly from the people list, matching the redesign. */
        /*
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Beneficiary Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "View all alerts, photo requests, and status updates for each connected beneficiary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                )
                if (beneficiaries.isEmpty()) {
                    Text("Connect to a beneficiary first to start chatting.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                } else {
                    beneficiaries.forEach { beneficiary ->
                        OutlinedButton(
                            onClick = {
                                onOpenBeneficiaryChat(
                                    ChatPreview(
                                        partnerId = beneficiary.beneficiaryId,
                                        partnerName = beneficiary.name,
                                        partnerPhone = beneficiary.phone,
                                        lastMessage = "",
                                        lastTimestamp = System.currentTimeMillis(),
                                        unreadCount = 0,
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        ) {
                            Text("Chat with ${beneficiary.name}")
                        }
                    }
                }
            }
        }
        */

        // Beneficiaries List
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Text("People you monitor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            onOpen = {
                                onOpenBeneficiaryChat(
                                    ChatPreview(
                                        partnerId = beneficiary.beneficiaryId,
                                        partnerName = beneficiary.name,
                                        partnerPhone = beneficiary.phone,
                                        lastMessage = "",
                                        lastTimestamp = System.currentTimeMillis(),
                                        unreadCount = 0,
                                    )
                                )
                            },
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
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Text(beneficiary.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(beneficiary.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                StatusChip(if (beneficiary.isPrimary) "All quiet" else "Awaiting response", if (beneficiary.isPrimary) IncidentStatusTone.Success else IncidentStatusTone.Warning)
            }
            if (beneficiary.phone.isNotBlank()) IconButton(onClick = onCall) { Text("☎", fontSize = 22.sp) }
            IconButton(onClick = onRemove) { Text("⋮", fontSize = 24.sp) }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETTINGS SCREEN
// -------------------------------------------------------------------------------------------------

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SettingsScreen(
    fullName: String,
    email: String,
    phone: String,
    currentRole: String,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    autoApproveCameraRequests: Boolean,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAutoApproveCameraRequestsChange: (Boolean) -> Unit,
    onSaveProfile: () -> Unit,
    onRequestSwitchRole: () -> Unit,
    onOpenTerms: () -> Unit,
    onCameraTest: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onResetAllData: () -> Unit,
    demoLabUnlocked: Boolean,
    onUnlockDemoLab: () -> Unit,
    onOpenDemoChat: () -> Unit,
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

        if (currentRole.equals("beneficiary", ignoreCase = true)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-approve photo requests", fontWeight = FontWeight.Bold)
                        Text("Caregiver verification photos open immediately without waiting for approval.", style = MaterialTheme.typography.bodySmall)
                    }
                    Checkbox(checked = autoApproveCameraRequests, onCheckedChange = onAutoApproveCameraRequestsChange)
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
            "SoundGuard v0.4",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).combinedClickable(
                onClick = {},
                onLongClick = onUnlockDemoLab,
            ),
        )
        if (demoLabUnlocked) {
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningTint)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Demo Lab", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Testing tools are hidden from regular users.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onOpenDemoChat, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Preview caregiver chat")
                    }
                }
            }
        }
    }
}
