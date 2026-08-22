package com.yuletan.soundguard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

private enum class AppScreen {
    Loading,
    Login,
    RoleSelection,
    Setup,
    CameraTest,
    Terms,
    Onboarding,
    BeneficiaryDashboard,
    CaregiverDashboard,
    Lab,
    Chat,
    ChatList,
    Settings,
}

class MainActivity : ComponentActivity() {
    private var screen by mutableStateOf(AppScreen.Loading)
    private var selectedRole by mutableStateOf<String?>(null)
    private var fullName by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var monitoringConsent by mutableStateOf(false)
    private var autoApproveCameraRequests by mutableStateOf(false)
    private var termsAccepted by mutableStateOf(false)
    private var microphoneGranted by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(false)
    private var batteryExempted by mutableStateOf(false)
    private var cameraReady by mutableStateOf(false)
    private var email by mutableStateOf("")
    private var otp by mutableStateOf("")
    private var otpSent by mutableStateOf(false)
    private var otpCooldownSeconds by mutableStateOf(0)
    private var authBusy by mutableStateOf(false)
    private var authMessage by mutableStateOf<String?>(null)
    private var setupMessage by mutableStateOf<String?>(null)

    // Dark Mode Testing Toggle State (persisted in preferences)
    private var darkModeEnabled by mutableStateOf<Boolean?>(null)

    // Dashboard State
    private var connectedCaregivers by mutableStateOf<List<CaregiverMember>>(emptyList())
    private var monitoredBeneficiaries by mutableStateOf<List<MonitoredBeneficiary>>(emptyList())
    private var caregiverNotifications by mutableStateOf<List<CaregiverNotification>>(emptyList())
    private var generatedPairingCode by mutableStateOf<String?>(null)
    private var beneficiaryRiskSummary by mutableStateOf<RiskSummary?>(null)
    private var beneficiaryRiskSummaries by mutableStateOf<Map<String, RiskSummary>>(emptyMap())
    private val seenNotificationIds = mutableSetOf<String>()
    private var notificationIdsInitialized = false
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
    private var snapshotUploading by mutableStateOf(false)
    private var loadedSessionUserId: String? = null
    private var showResetDataDialog by mutableStateOf(false)
    private var resetErrorText by mutableStateOf<String?>(null)

    // Chat State
    private var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
    private var selectedChatPartner by mutableStateOf<ChatPreview?>(null)
    private var chatLoading by mutableStateOf(false)
    private var chatListPreviews by mutableStateOf<List<ChatPreview>>(emptyList())
    private var chatListLoading by mutableStateOf(false)

    // Safety confirmation
    private var pendingRemoveConnectionId by mutableStateOf<String?>(null)

    private var beneficiaryHighRiskDialogIncident by mutableStateOf<IncidentRecord?>(null)
    private var caregiverHighRiskPopupNotification by mutableStateOf<CaregiverNotification?>(null)

    private var devFakeCounter by mutableStateOf(0)

    // Role Switch Dialog States
    private var showBlockedSwitchDialog by mutableStateOf(false)
    private var showForceRemoveInstructionsDialog by mutableStateOf(false)
    private var blockedActiveConnectionsCount by mutableStateOf(0)
    private var blockedConnections by mutableStateOf<List<CareClient.RawConnection>>(emptyList())
    private var showConfirmSwitchDialog by mutableStateOf(false)
    private var showClearChatConfirm by mutableStateOf(false)
    private var pendingClearChatBeneficiaryId by mutableStateOf<String?>(null)

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
        checkPermissionsState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load dark mode preference and camera readiness
        val prefs = getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE)
        if (prefs.contains("dark_mode")) {
            darkModeEnabled = prefs.getBoolean("dark_mode", false)
        }
        cameraReady = prefs.getBoolean("camera_ready", false)

        checkPermissionsState()

        // Periodically check for pending and auto-approved snapshot requests (beneficiary only).
        // Gated to post-setup screens so onboarding/setup can never be hijacked by the camera.
        lifecycleScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000L)
                if (selectedRole.equals("beneficiary", ignoreCase = true)) {
                    val onPostSetupScreen = screen == AppScreen.BeneficiaryDashboard ||
                        screen == AppScreen.Chat ||
                        screen == AppScreen.ChatList
                    if (!onPostSetupScreen) continue
                    val client = SnapshotClient(this@MainActivity)
                    if (pendingSnapshotRequest == null && demoSnapshotRequest == null) {
                        val approved = client.fetchApprovedSnapshotForBeneficiary().getOrNull()
                        if (approved != null) {
                            pendingSnapshotRequest = null
                            client.fetchRequest(approved.id)
                                .onSuccess { req ->
                                    demoSnapshotRequest = req
                                    demoPhotoRequested = true
                                    demoPhotoDecision = "approved"
                                    demoPhotoRequestedAt = System.currentTimeMillis()
                                    demoPhotoDecisionAt = System.currentTimeMillis()
                                    openCameraForBeneficiarySelfie()
                                }
                            continue
                        }
                    }
                    if (pendingSnapshotRequest == null) {
                        client.fetchPendingForBeneficiary()
                            .onSuccess { request ->
                                if (request != null && pendingSnapshotRequest == null) {
                                    pendingSnapshotRequest = request
                                }
                            }
                    }
                }
            }
        }

        lifecycleScope.launch {
            AudioMonitoringService.audioState.collect { state ->
                val active = state.activeIncident
                if (active != null && active.status == IncidentStatus.WaitingUser && selectedRole.equals("beneficiary", ignoreCase = true)) {
                    if (beneficiaryHighRiskDialogIncident?.id != active.id) {
                        beneficiaryHighRiskDialogIncident = active
                    }
                }
            }
        }

        setContent {
            val liveAudioState by AudioMonitoringService.audioState.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val isDark = darkModeEnabled ?: systemInDark

            SoundGuardTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
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
                        AppScreen.RoleSelection -> RoleSelection(
                            initialRole = selectedRole ?: "Beneficiary",
                            onRoleSelected = { role ->
                                selectedRole = role
                                screen = AppScreen.Setup
                            }
                        )
                        AppScreen.Setup -> SetupScreen(
                            role = selectedRole,
                            fullName = fullName,
                            phone = phone,
                            consent = monitoringConsent,
                            autoApproveCameraRequests = autoApproveCameraRequests,
                            termsAccepted = termsAccepted,
                            microphoneGranted = microphoneGranted,
                            notificationsGranted = notificationsGranted,
                            batteryExempted = batteryExempted,
                            cameraReady = cameraReady,
                            setupMessage = setupMessage,
                            onFullNameChange = { fullName = it },
                            onPhoneChange = { phone = it },
                            onConsentChange = { monitoringConsent = it },
                            onAutoApproveCameraRequestsChange = { autoApproveCameraRequests = it },
                            onTermsChange = { termsAccepted = it },
                            onOpenTerms = { screen = AppScreen.Terms },
                            onRequestMic = { requestMicPermission() },
                            onRequestNotifications = { requestNotificationsPermission() },
                            onRequestBatteryExemption = { requestBatteryExemption() },
                            onCameraTest = { cameraPermission.launch(Manifest.permission.CAMERA) },
                            onBack = { screen = AppScreen.RoleSelection },
                            onConfirm = { confirmSetup() },
                        )
                        AppScreen.CameraTest -> CameraTestScreen(
                            onBack = {
                                if (returnToPreviewAfterCamera && selectedChatPartner != null) {
                                    screen = AppScreen.Chat
                                } else if (returnToPreviewAfterCamera) {
                                    screen = AppScreen.BeneficiaryDashboard
                                } else {
                                    screen = AppScreen.Setup
                                }
                                returnToPreviewAfterCamera = false
                            },
                            onFinished = { capturedPath ->
                                cameraReady = true
                                getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE)
                                    .edit().putBoolean("camera_ready", true).apply()
                                if (returnToPreviewAfterCamera) {
                                    demoPhotoPath = capturedPath
                                    demoSnapshotRequest?.let { request ->
                                        val savedPath = capturedPath ?: return@let
                                        snapshotUploading = true
                                        demoSnapshotMessage = "Uploading photo..."
                                        lifecycleScope.launch {
                                            SnapshotClient(this@MainActivity)
                                                .uploadSnapshot(request, java.io.File(savedPath))
                                                .onSuccess {
                                                    demoSnapshotMessage = "Photo uploaded — visible to caregiver and beneficiary for 10 minutes."
                                                    selectedChatPartner?.let { partner -> loadChatMessages(partner.partnerId, clearFirst = false) }
                                                }
                                                .onFailure { demoSnapshotMessage = "Photo upload failed: ${it.message}" }
                                            snapshotUploading = false
                                        }
                                    }
                                    if (selectedChatPartner != null) screen = AppScreen.Chat
                                    else screen = AppScreen.BeneficiaryDashboard
                                } else {
                                    screen = AppScreen.Setup
                                }
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
                        AppScreen.Onboarding -> OnboardingScreen(
                            role = selectedRole,
                            onConfirm = {
                                if (selectedRole.equals("caregiver", ignoreCase = true)) {
                                    screen = AppScreen.CaregiverDashboard
                                    refreshCaregiverData()
                                } else {
                                    screen = AppScreen.BeneficiaryDashboard
                                    refreshBeneficiaryData()
                                }
                            },
                        )
                        AppScreen.BeneficiaryDashboard -> BeneficiaryDashboard(
                            fullName = fullName,
                            email = email,
                            audioState = liveAudioState,
                            riskSummary = beneficiaryRiskSummary,
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
                            onConnectCaregiver = { code -> pairBeneficiary(code) },
                            onSetPrimary = { connectionId, caregiverId -> setPrimaryCaregiver(connectionId, caregiverId) },
                            onRemoveCaregiver = { connectionId -> requestRemoveConnection(connectionId) },
                            onCall = { targetPhone -> callPhoneNumber(targetPhone) },
                            onOpenCaregiverChat = { partner ->
                                selectedChatPartner = partner
                                screen = AppScreen.Chat
                                loadChatMessages(partner.partnerId)
                            },
                            onIncidentResponse = { response -> AudioMonitoringService.respondToIncident(response) },
                            onLinkDemoCaregiver = { demoCaregiverLinked = true },
                            onOpenSettings = { screen = AppScreen.Settings },
                            onOpenLab = { screen = AppScreen.Lab },
                            onTestHighRiskNotification = { sendTestHighRiskNotificationForBeneficiary() },
                            onAddFakeCaregiver = { addFakeCaregiverLocal() },
                            onRefresh = { refreshBeneficiaryData() },
                            onOpenChatList = { openChatList() },
                        )
                        AppScreen.Lab -> MicLabScreen(
                            audioState = liveAudioState,
                            onToggleMonitoring = {
                                if (liveAudioState.isMonitoring) AudioMonitoringService.stop(this@MainActivity)
                                else if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) AudioMonitoringService.start(this@MainActivity)
                                else requestAppPermissions()
                            },
                            onClear = { AudioMonitoringService.clearProbeLog() },
                            onBack = { screen = AppScreen.BeneficiaryDashboard },
                        )
                        AppScreen.CaregiverDashboard -> CaregiverDashboard(
                            fullName = fullName,
                            email = email,
                            beneficiaries = monitoredBeneficiaries,
                            riskSummaries = beneficiaryRiskSummaries,
                            pairingCode = generatedPairingCode,
                            loading = dashboardLoading,
                            message = dashboardMessage,
                            onConnectBeneficiary = { code -> pairBeneficiary(code) },
                            onGenerateCode = { generatePairingCode() },
                            onCopyCode = { code -> copyToClipboard(code) },
                            onShareCode = { code -> sharePairingCode(code) },
                            onRemoveBeneficiary = { connectionId -> requestRemoveConnection(connectionId) },
                            onCall = { targetPhone -> callPhoneNumber(targetPhone) },
                            onOpenBeneficiaryChat = { partner ->
                                selectedChatPartner = partner
                                screen = AppScreen.Chat
                                loadChatMessages(partner.partnerId)
                            },
                            onOpenSettings = { screen = AppScreen.Settings },
                            onRefresh = { refreshCaregiverData() },
                            onOpenChatList = { openChatList() },
                            onTestHighRiskNotification = { sendTestHighRiskNotificationForCaregiver() },
                            onAddFakeBeneficiary = { addFakeBeneficiaryLocal() },
                        )
                        AppScreen.Chat -> {
                            val activeIncident = liveAudioState.activeIncident
                            val bannerText = if (liveAudioState.isEmergency && activeIncident != null) {
                                "Active incident · ${activeIncident.soundLabel}"
                            } else null

                            ChatScreen(
                                partnerName = selectedChatPartner?.partnerName ?: "Chat",
                                partnerPhone = selectedChatPartner?.partnerPhone.orEmpty(),
                                messages = chatMessages,
                                loading = chatLoading,
                                isCaregiverView = selectedRole.equals("caregiver", ignoreCase = true),
                                snapshotMessage = demoSnapshotMessage,
                                activeIncidentBannerText = bannerText,
                                isUploadingPhoto = snapshotUploading,
                                onRequestPhoto = {
                                    if (selectedRole.equals("caregiver", ignoreCase = true)) requestDemoSnapshotAndOpenCamera(false)
                                    else demoSnapshotMessage = "Photo requests are made by caregivers."
                                },
                                onCall = { callPhoneNumber(selectedChatPartner?.partnerPhone.orEmpty()) },
                                onBack = {
                                    val hasList = chatListPreviews.size > 1
                                    screen = if (hasList) AppScreen.ChatList
                                    else if (selectedRole.equals("caregiver", ignoreCase = true)) AppScreen.CaregiverDashboard
                                    else AppScreen.BeneficiaryDashboard
                                },
                                onRefresh = {
                                    selectedChatPartner?.let { partner -> loadChatMessages(partner.partnerId, clearFirst = false) }
                                },
                                onClearChat = {
                                    val bid = selectedChatPartner?.partnerId
                                    if (bid != null && selectedRole.equals("caregiver", ignoreCase = true)) {
                                        pendingClearChatBeneficiaryId = bid
                                        showClearChatConfirm = true
                                    }
                                },
                                partnerDeactivated = selectedChatPartner?.deactivated == true,
                                onRemoveConnection = {
                                    val connId = selectedChatPartner?.connectionId
                                    if (!connId.isNullOrEmpty()) {
                                        requestRemoveConnection(connId)
                                        screen = if (selectedRole.equals("caregiver", ignoreCase = true))
                                            AppScreen.CaregiverDashboard else AppScreen.BeneficiaryDashboard
                                    }
                                },
                            )
                            if (snapshotUploading) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = {},
                                    title = { Text("Uploading photo...", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                            Text("Securely uploading to private storage. Please keep the app open.", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
                                    confirmButton = {},
                                )
                            }
                        }
                        AppScreen.ChatList -> ChatListScreen(
                            title = "Chats",
                            subtitle = if (selectedRole.equals("caregiver", ignoreCase = true)) "${monitoredBeneficiaries.size} beneficiaries" else "${connectedCaregivers.size} caregivers",
                            previews = chatListPreviews,
                            loading = chatListLoading,
                            onOpenChat = { partner ->
                                selectedChatPartner = partner
                                screen = AppScreen.Chat
                                loadChatMessages(partner.partnerId)
                            },
                            onBack = {
                                screen = if (selectedRole.equals("caregiver", ignoreCase = true))
                                    AppScreen.CaregiverDashboard else AppScreen.BeneficiaryDashboard
                            },
                        )
                        AppScreen.Settings -> SettingsScreen(
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            currentRole = selectedRole ?: "Beneficiary",
                            microphoneGranted = microphoneGranted,
                            notificationsGranted = notificationsGranted,
                            batteryExempted = batteryExempted,
                            autoApproveCameraRequests = autoApproveCameraRequests,
                            darkModeEnabled = darkModeEnabled ?: isSystemInDarkTheme(),
                            onDarkModeToggle = { enabled ->
                                darkModeEnabled = enabled
                                getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("dark_mode", enabled)
                                    .apply()
                            },
                            onRequestPermissions = { requestAppPermissions() },
                            onRequestBatteryExemption = { requestBatteryExemption() },
                            onFullNameChange = {
                                fullName = it
                                updateProfileSilently()
                            },
                            onPhoneChange = {
                                phone = it
                                updateProfileSilently()
                            },
                            onAutoApproveCameraRequestsChange = { value ->
                                autoApproveCameraRequests = value
                                lifecycleScope.launch {
                                    profileClient.saveBeneficiarySettings(
                                        monitoringConsent = monitoringConsent,
                                        autoApproveCameraRequests = autoApproveCameraRequests,
                                    ).onFailure { dashboardMessage = it.message }
                                }
                            },
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

                    beneficiaryHighRiskDialogIncident?.let { incident ->
                        val primaryCaregiver = connectedCaregivers.firstOrNull { it.isPrimary } ?: connectedCaregivers.firstOrNull()
                        var caregiverExpanded by remember { mutableStateOf(false) }
                        var expandedChoiceId by remember { mutableStateOf(primaryCaregiver?.caregiverId) }
                        val chosen = connectedCaregivers.firstOrNull { it.caregiverId == expandedChoiceId } ?: primaryCaregiver
                        AlertDialog(
                            onDismissRequest = { beneficiaryHighRiskDialogIncident = null },
                            title = { Text("High-risk alert: ${incident.soundLabel}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.danger) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("${incident.soundLabel} was just detected with ${(incident.confidence * 100).toInt()}% confidence. Are you okay?", style = MaterialTheme.typography.bodyMedium)
                                    if (connectedCaregivers.size > 1) {
                                        Text("Call caregiver:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.ink700)
                                        Box {
                                            OutlinedButton(
                                                onClick = { caregiverExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                            ) { Text(chosen?.name ?: "Choose caregiver", fontWeight = FontWeight.SemiBold) }
                                            DropdownMenu(expanded = caregiverExpanded, onDismissRequest = { caregiverExpanded = false }) {
                                                connectedCaregivers.forEach { cg ->
                                                    DropdownMenuItem(
                                                        text = { Text("${cg.name}${if (cg.isPrimary) " (primary)" else ""}") },
                                                        onClick = { expandedChoiceId = cg.caregiverId; caregiverExpanded = false },
                                                    )
                                                }
                                            }
                                        }
                                    } else if (primaryCaregiver != null) {
                                        Text("Primary caregiver: ${primaryCaregiver.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                                    } else {
                                        Text("No caregiver linked yet — ask someone to connect with your pairing code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        beneficiaryHighRiskDialogIncident = null
                                        AudioMonitoringService.respondToIncident(BeneficiaryResponse.Safe)
                                    },
                                ) { Text("I'm OK", fontWeight = FontWeight.Bold) }
                            },
                            dismissButton = {
                                val canCall = chosen != null && chosen.phone.isNotBlank()
                                Button(
                                    onClick = {
                                        beneficiaryHighRiskDialogIncident = null
                                        if (canCall) callPhoneNumber(chosen!!.phone) else Toast.makeText(this@MainActivity, "No phone number available.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) { Text(if (canCall) "Call ${chosen?.name ?: "caregiver"}" else "Call caregiver") }
                            },
                        )
                    }

                    caregiverHighRiskPopupNotification?.let { alert ->
                        val beneficiaryName = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == alert.beneficiaryId }?.name ?: "your beneficiary"
                        AlertDialog(
                            onDismissRequest = { caregiverHighRiskPopupNotification = null },
                            title = { Text("High-risk alert — $beneficiaryName", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.danger) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${alert.soundLabel} detected at ${beneficiaryName}'s place · ${(alert.confidence * 100).toInt()}% confidence. We recommend requesting a verification photo — with the beneficiary's auto-approve on, it will open and capture automatically and appear in chat.", style = MaterialTheme.typography.bodyMedium)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val bId = alert.beneficiaryId
                                        caregiverHighRiskPopupNotification = null
                                        if (bId.isNotBlank()) {
                                            val b = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == bId }
                                            selectedChatPartner = ChatPreview(
                                                partnerId = bId,
                                                partnerName = b?.name ?: beneficiaryName,
                                                partnerPhone = b?.phone.orEmpty(),
                                                lastMessage = "",
                                                lastTimestamp = System.currentTimeMillis(),
                                                unreadCount = 0,
                                                connectionId = b?.connectionId.orEmpty(),
                                                deactivated = b?.deactivated == true,
                                            )
                                            val labelLower = alert.soundLabel.lowercase()
                                            val trustLabel = when {
                                                "explosion" in labelLower || "gunshot" in labelLower -> "Explosion / Gunshot"
                                                else -> alert.soundLabel
                                            }
                                            val existingIncidents = incidentClientForPopup()
                                            lifecycleScope.launch {
                                                val incidentId = existingIncidents?.let {
                                                    IncidentClient(this@MainActivity).fetchIncidentsForBeneficiary(bId).getOrNull()
                                                        ?.maxByOrNull { row -> row.startedAt }?.id
                                                }
                                                val useIncidentId = incidentId ?: alert.incidentId
                                                if (useIncidentId.isNotBlank()) {
                                                    SnapshotClient(this@MainActivity).requestSnapshot(useIncidentId, bId)
                                                        .onSuccess { req ->
                                                            demoSnapshotRequest = req
                                                            demoPhotoRequested = true
                                                            demoPhotoDecision = req.approvalStatus
                                                            demoPhotoRequestedAt = System.currentTimeMillis()
                                                            demoPhotoDecisionAt = System.currentTimeMillis()
                                                            demoSnapshotMessage = if (req.approvalStatus == "approved") {
                                                                "High-risk: $trustLabel — photo request auto-approved. Beneficiary camera will open to capture and send to chat."
                                                            } else {
                                                                "Photo request sent for $trustLabel — waiting for beneficiary approval. It will appear in chat once captured."
                                                            }
                                                            loadChatMessages(bId)
                                                            screen = AppScreen.Chat
                                                            if (req.approvalStatus == "approved") {
                                                            }
                                                        }
                                                        .onFailure { demoSnapshotMessage = "Photo request failed: ${it.message}" }
                                                } else {
                                                    demoSnapshotMessage = "Could not find incident for photo request. Open chat and use Request photo."
                                                    loadChatMessages(bId)
                                                    screen = AppScreen.Chat
                                                }
                                            }
                                        }
                                    },
                                ) { Text("Request photo", fontWeight = FontWeight.Bold) }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { caregiverHighRiskPopupNotification = null }) { Text("Dismiss") }
                                TextButton(
                                    onClick = {
                                        val bId = alert.beneficiaryId
                                        caregiverHighRiskPopupNotification = null
                                        if (bId.isNotBlank()) {
                                            val b = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == bId }
                                            selectedChatPartner = ChatPreview(
                                                partnerId = bId,
                                                partnerName = b?.name ?: beneficiaryName,
                                                partnerPhone = b?.phone.orEmpty(),
                                                lastMessage = "",
                                                lastTimestamp = System.currentTimeMillis(),
                                                unreadCount = 0,
                                                connectionId = b?.connectionId.orEmpty(),
                                                deactivated = b?.deactivated == true,
                                            )
                                            loadChatMessages(bId)
                                            screen = AppScreen.Chat
                                        }
                                    },
                                ) { Text("Open chat") }
                            },
                        )
                    }

                    pendingRemoveConnectionId?.let { connId ->
                        AlertDialog(
                            onDismissRequest = { pendingRemoveConnectionId = null },
                            title = { Text("Remove this connection?", fontWeight = FontWeight.Bold) },
                            text = { Text("This will immediately revoke access. The other person will be notified that the care connection was removed. You can reconnect later with a new pairing code.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val id = pendingRemoveConnectionId
                                        pendingRemoveConnectionId = null
                                        if (id != null) lifecycleScope.launch {
                                            if (selectedRole.equals("caregiver", ignoreCase = true)) removeBeneficiary(id) else removeCaregiver(id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) { Text("Remove") }
                            },
                            dismissButton = { TextButton(onClick = { pendingRemoveConnectionId = null }) { Text("Cancel") } },
                        )
                    }

                    pendingSnapshotRequest?.let { request ->
                        AlertDialog(
                            onDismissRequest = { pendingSnapshotRequest = null },
                            title = { Text("Verification photo request", fontWeight = FontWeight.Bold) },
                            text = { Text("Your caregiver requested a verification photo during an active incident. Approve this request to allow the photo check.") },
                            confirmButton = {
                                Button(onClick = { decideSnapshotRequest(request.id, true) }) { Text("Approve") }
                            },
                            dismissButton = {
                                TextButton(onClick = { decideSnapshotRequest(request.id, false) }) { Text("Decline") }
                            },
                        )
                    }

                    if (showResetDataDialog) {
                        AlertDialog(
                            onDismissRequest = { showResetDataDialog = false },
                            title = { Text("Delete all account data?", fontWeight = FontWeight.Bold) },
                            text = { Text("This deactivates your account: your profile, device tokens, and settings are cleared. Your care connections are removed and shared chats are deleted for both sides. You will return to role selection.") },
                            confirmButton = {
                                Button(
                                    onClick = { showResetDataDialog = false; resetAllAccountData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text("Delete all data")
                                }
                            },
                            dismissButton = { TextButton(onClick = { showResetDataDialog = false }) { Text("Cancel") } },
                        )
                    }

                    resetErrorText?.let { err ->
                        AlertDialog(
                            onDismissRequest = { resetErrorText = null },
                            title = { Text("Delete account data failed", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SelectionContainer { Text(err) }
                                    Text(
                                        "Tap Copy and send this text to support.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.ink500,
                                    )
                                }
                            },
                            confirmButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        copyToClipboard(err)
                                        Toast.makeText(this@MainActivity, "Error copied", Toast.LENGTH_SHORT).show()
                                    }) { Text("Copy") }
                                    Button(onClick = { resetErrorText = null }) { Text("OK") }
                                }
                            },
                        )
                    }

                    if (showBlockedSwitchDialog) {
                        AlertDialog(
                            onDismissRequest = { showBlockedSwitchDialog = false },
                            title = { Text("Cannot Switch Role", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "You currently have $blockedActiveConnectionsCount active care connection(s). " +
                                            "To protect ongoing safety monitoring, you must remove all connections before switching your account role.",
                                    )
                                    if (blockedConnections.isNotEmpty()) {
                                        Text(
                                            "Tap \"View removal steps\" to see how to clear them.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.ink500,
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showBlockedSwitchDialog = false }) {
                                    Text("Got It")
                                }
                            },
                            dismissButton = if (blockedConnections.isNotEmpty()) {
                                {
                                    TextButton(
                                        onClick = {
                                            showBlockedSwitchDialog = false
                                            showForceRemoveInstructionsDialog = true
                                        }
                                    ) { Text("View removal steps") }
                                }
                            } else null,
                        )
                    }

                    if (showForceRemoveInstructionsDialog) {
                        AlertDialog(
                            onDismissRequest = { showForceRemoveInstructionsDialog = false },
                            title = { Text("Remove connections one by one", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    "You still have $blockedActiveConnectionsCount active connection(s). " +
                                        "Open your dashboard and remove each caregiver/beneficiary individually via the \u22ee menu \u2192 \"Remove\". " +
                                        "Role switching unlocks only when none are left — then tap \"Switch mode\" again.",
                                )
                            },
                            confirmButton = {
                                Button(onClick = { showForceRemoveInstructionsDialog = false }) {
                                    Text("OK")
                                }
                            },
                        )
                    }

                    if (showClearChatConfirm) {
                        AlertDialog(
                            onDismissRequest = { showClearChatConfirm = false; pendingClearChatBeneficiaryId = null },
                            title = { Text("Clear chat history?", fontWeight = FontWeight.Bold) },
                            text = { Text("This permanently deletes all incident alerts for this beneficiary's chat. Snapshots linked to those incidents will be removed. This cannot be undone.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val bid = pendingClearChatBeneficiaryId
                                        showClearChatConfirm = false
                                        pendingClearChatBeneficiaryId = null
                                        if (bid != null) lifecycleScope.launch {
                                            IncidentClient(this@MainActivity).clearIncidentsForPartnerAsCaregiver(bid)
                                                .onSuccess {
                                                    chatMessages = emptyList()
                                                    Toast.makeText(this@MainActivity, "Chat cleared.", Toast.LENGTH_SHORT).show()
                                                }
                                                .onFailure {
                                                    Toast.makeText(this@MainActivity, it.message ?: "Could not clear chat.", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) { Text("Clear chat") }
                            },
                            dismissButton = { TextButton(onClick = { showClearChatConfirm = false; pendingClearChatBeneficiaryId = null }) { Text("Cancel") } },
                        )
                    }

                    if (showConfirmSwitchDialog) {
                        val targetRole = if (selectedRole.equals("caregiver", ignoreCase = true)) "Beneficiary" else "Caregiver"
                        AlertDialog(
                            onDismissRequest = { showConfirmSwitchDialog = false },
                            title = { Text("Switch Role to $targetRole?", fontWeight = FontWeight.Bold) },
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

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        microphoneGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        batteryExempted = runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        }.getOrDefault(false)
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
                        if (loadedSessionUserId == currentUserId && selectedRole != null && profile.role.isNotBlank() &&
                            !profile.role.equals(selectedRole, ignoreCase = true)) {
                            // Don't clobber the role we just switched to; another stale fetchMyProfile racing.
                        } else {
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
                        }

                        when {
                            // Brand-new account or deliberate reset (deactivated / role
                            // cleared): choose a role first, then walk full setup.
                            profile.deactivatedAt != null || profile.role.isBlank() -> screen = AppScreen.RoleSelection
                            // Role already known but details/setup incomplete: skip the
                            // redundant role pick and go straight to setup.
                            profile.setupCompletedAt == null || profile.fullName.isBlank() || profile.phone.isBlank() ->
                                screen = AppScreen.Setup
                            profile.role.equals("caregiver", ignoreCase = true) -> {
                                screen = AppScreen.CaregiverDashboard
                                refreshCaregiverData()
                            }
                            else -> {
                                screen = AppScreen.BeneficiaryDashboard
                                refreshBeneficiaryData()
                            }
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
                            ).onFailure { setupMessage = it.message ?: "Could not save monitoring consent." }
                        }
                        if (setupMessage == null) {
                            screen = AppScreen.Onboarding
                        }
                    }
                    .onFailure { setupMessage = it.message ?: "Could not save profile." }
            }
        }
    }

    private fun updateProfileSilently() {
        val role = selectedRole ?: return
        lifecycleScope.launch {
            profileClient.saveProfile(email, fullName, phone, role)
        }
    }

    private fun handleRoleSwitchRequest() {
        dashboardLoading = true
        lifecycleScope.launch {
            val detailed = careClient.fetchAllMyActiveConnectionIds()
            val fallbackCount = if (detailed.isSuccess) null else careClient.countActiveConnections().getOrNull()
            val rawConnections = detailed.getOrNull()
            val activeCount = rawConnections?.size ?: fallbackCount ?: 0
            dashboardLoading = false
            if (activeCount > 0) {
                blockedActiveConnectionsCount = activeCount
                blockedConnections = rawConnections ?: emptyList()
                val wasShowingBeneficiary = selectedRole.equals("beneficiary", ignoreCase = true)
                if (wasShowingBeneficiary && connectedCaregivers.isEmpty()) {
                    refreshBeneficiaryData()
                } else if (!wasShowingBeneficiary && monitoredBeneficiaries.isEmpty()) {
                    refreshCaregiverData()
                }
                showBlockedSwitchDialog = true
            } else {
                blockedConnections = emptyList()
                showConfirmSwitchDialog = true
            }
            if (detailed.isFailure && fallbackCount == null) {
                dashboardMessage = detailed.exceptionOrNull()?.message
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
                    // Stay on the current dashboard and tell the user why the switch failed.
                    Toast.makeText(
                        this@MainActivity,
                        "Could not switch role: ${it.message}",
                        Toast.LENGTH_LONG,
                    ).show()
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

    private fun requestMicPermission() {
        setupPermissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupPermissions.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        } else {
            notificationsGranted = true
        }
    }

    @android.annotation.SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        runCatching {
            val intent = Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName"),
            )
            startActivity(intent)
        }.onFailure {
            Toast.makeText(this, "Battery optimization settings are not available on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshBeneficiaryData() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.fetchCaregiversForBeneficiary()
                .onSuccess { list -> connectedCaregivers = list }
                .onFailure { err -> dashboardMessage = err.message }
            SnapshotClient(this@MainActivity).fetchPendingForBeneficiary()
                .onSuccess { request -> pendingSnapshotRequest = request }
            IncidentClient(this@MainActivity).fetchOwnIncidents()
                .onSuccess { incidents -> beneficiaryRiskSummary = computeRiskSummary(incidents) }
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
                    if (list.isNotEmpty()) {
                        IncidentClient(this@MainActivity)
                            .fetchRecentIncidentsForBeneficiaries(list.map { it.beneficiaryId })
                            .onSuccess { grouped ->
                                beneficiaryRiskSummaries = list.associate { b ->
                                    b.beneficiaryId to computeRiskSummary(grouped[b.beneficiaryId].orEmpty())
                                }
                            }
                    } else {
                        beneficiaryRiskSummaries = emptyMap()
                    }
                }
                .onFailure { err -> dashboardMessage = err.message }
            notificationClient.fetchMine()
                .onSuccess { list ->
                    caregiverNotifications = list
                    checkForNewHighRiskAlerts(list)
                }
            dashboardLoading = false
        }
    }

    private fun incidentClientForPopup(): IncidentClient? = runCatching { IncidentClient(this) }.getOrNull()

    /** Fire a device notification for each newly-queued HIGH severity caregiver alert, plus an in-app popup. */
    private fun checkForNewHighRiskAlerts(notifications: List<CaregiverNotification>) {
        val currentIds = notifications.map { it.id }.toSet()
        if (!notificationIdsInitialized) {
            seenNotificationIds.addAll(currentIds)
            notificationIdsInitialized = true
            return
        }
        val oneHourMs = 60L * 60 * 1000
        val fresh = notifications.filter {
            if (it.id in seenNotificationIds) return@filter false
            if (!it.severity.equals("high", ignoreCase = true)) return@filter false
            val ts = parseIsoMillis(it.incidentStartedAt) ?: parseIsoMillis(it.createdAt) ?: return@filter false
            System.currentTimeMillis() - ts <= oneHourMs
        }
        fresh.forEach { alert ->
            caregiverHighRiskPopupNotification = alert
            val beneficiaryName = monitoredBeneficiaries
                .firstOrNull { it.beneficiaryId == alert.beneficiaryId }?.name
                ?: "your beneficiary"
            showHighRiskAlertNotification(
                beneficiaryId = alert.beneficiaryId,
                beneficiaryName = beneficiaryName,
                soundLabel = alert.soundLabel.ifBlank { "Emergency sound" },
                confidence = alert.confidence,
            )
        }
        seenNotificationIds.addAll(currentIds)
    }

    private fun showHighRiskAlertNotification(
        beneficiaryId: String,
        beneficiaryName: String,
        soundLabel: String,
        confidence: Float,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val channelId = "soundguard_alerts"
        val manager = getSystemService(android.app.NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(
                android.app.NotificationChannel(
                    channelId,
                    "SoundGuard alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Urgent caregiver notifications from SoundGuard"
                    enableVibration(true)
                },
            )
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (beneficiaryId.isNotBlank()) putExtra("beneficiary_id", beneficiaryId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            beneficiaryId.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val body = "$soundLabel detected at $beneficiaryName's place · ${(confidence * 100).toInt()}% confidence. Open to check in."
        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("High-risk alert — $beneficiaryName")
            .setContentText(body)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager?.notify(beneficiaryId.hashCode(), notification)
    }

    private fun loadChatMessages(partnerId: String, clearFirst: Boolean = true) {
        if (clearFirst) {
            chatLoading = true
            chatMessages = emptyList()
        }
        lifecycleScope.launch {
            chatRepository.buildChatMessages(partnerId, selectedRole.equals("caregiver", ignoreCase = true))
                .onSuccess { chatMessages = it }
                .onFailure { dashboardMessage = it.message }
            if (clearFirst) chatLoading = false
        }
    }

    private fun openChatList() {
        chatListLoading = true
        val isCaregiver = selectedRole.equals("caregiver", ignoreCase = true)
        lifecycleScope.launch {
            if (isCaregiver) {
                val local = monitoredBeneficiaries.map { b ->
                    ChatPreview(b.beneficiaryId, b.name, b.phone, "", System.currentTimeMillis(), 0, b.connectionId, b.deactivated)
                }
                if (local.size == 1 && chatListPreviews.isEmpty()) {
                    chatListPreviews = local
                    selectedChatPartner = local.first()
                    screen = AppScreen.Chat
                    loadChatMessages(local.first().partnerId)
                    chatListLoading = false
                    return@launch
                }
                chatRepository.buildChatListForCaregiver()
                    .onSuccess { chatListPreviews = it; screen = AppScreen.ChatList }
                    .onFailure {
                        if (local.isNotEmpty()) { chatListPreviews = local; screen = AppScreen.ChatList }
                        else dashboardMessage = it.message
                    }
            } else {
                val local = connectedCaregivers.map { c ->
                    ChatPreview(c.caregiverId, c.name, c.phone, "", System.currentTimeMillis(), 0, c.connectionId, c.deactivated)
                }
                if (local.size == 1 && chatListPreviews.isEmpty()) {
                    chatListPreviews = local
                    selectedChatPartner = local.first()
                    screen = AppScreen.Chat
                    loadChatMessages(local.first().partnerId)
                    chatListLoading = false
                    return@launch
                }
                chatRepository.buildChatListForBeneficiary()
                    .onSuccess { chatListPreviews = it; screen = AppScreen.ChatList }
                    .onFailure {
                        if (local.isNotEmpty()) { chatListPreviews = local; screen = AppScreen.ChatList }
                        else dashboardMessage = it.message
                    }
            }
            chatListLoading = false
        }
    }

    private fun requestRemoveConnection(connectionId: String) {
        pendingRemoveConnectionId = connectionId
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
        demoSnapshotMessage = "Requesting photo..."
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
                        demoSnapshotMessage = "Photo request sent — waiting for beneficiary approval. The photo will appear in chat once captured."
                    }
                    selectedChatPartner?.let { partner -> loadChatMessages(partner.partnerId, clearFirst = false) }
                }
                .onFailure { demoSnapshotMessage = "Photo request failed: ${it.message}" }
        }
    }

    private fun generatePairingCode() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.createPairingCode()
                .onSuccess { code -> generatedPairingCode = code }
                .onFailure { err -> dashboardMessage = err.message ?: "Failed to create pairing code." }
            dashboardLoading = false
        }
    }

    private fun pairBeneficiary(code: String) {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.acceptPairingCode(code)
                .onSuccess { partnerName ->
                    Toast.makeText(this@MainActivity, "Connected to $partnerName!", Toast.LENGTH_SHORT).show()
                    // Either role can redeem a code — refresh whichever dashboard is active.
                    if (selectedRole.equals("caregiver", ignoreCase = true)) {
                        refreshCaregiverData()
                    } else {
                        refreshBeneficiaryData()
                    }
                }
                .onFailure { err -> dashboardMessage = err.message ?: "Failed to pair with code." }
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
        if (connectionId.startsWith("dev_")) {
            val removed = connectedCaregivers.firstOrNull { it.connectionId == connectionId }
            connectedCaregivers = connectedCaregivers.filterNot { it.connectionId == connectionId }
            if (removed != null) {
                chatListPreviews = chatListPreviews.filterNot { it.partnerId == removed.caregiverId }
                if (selectedChatPartner?.partnerId == removed.caregiverId) {
                    chatMessages = emptyList()
                    selectedChatPartner = null
                }
            }
            Toast.makeText(this, "Test caregiver removed — chat deleted.", Toast.LENGTH_SHORT).show()
            return
        }
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.removeCareConnection(connectionId)
                .onSuccess {
                    chatListPreviews = chatListPreviews.filterNot { preview ->
                        connectedCaregivers.firstOrNull { it.connectionId == connectionId }?.let { it.caregiverId == preview.partnerId } ?: false
                    }
                    refreshBeneficiaryData()
                }
                .onFailure { dashboardMessage = it.message }
            dashboardLoading = false
        }
    }

    private fun removeBeneficiary(connectionId: String) {
        if (connectionId.startsWith("dev_")) {
            val removed = monitoredBeneficiaries.firstOrNull { it.connectionId == connectionId }
            monitoredBeneficiaries = monitoredBeneficiaries.filterNot { it.connectionId == connectionId }
            if (removed != null) {
                beneficiaryRiskSummaries = beneficiaryRiskSummaries - removed.beneficiaryId
                chatListPreviews = chatListPreviews.filterNot { it.partnerId == removed.beneficiaryId }
                if (selectedChatPartner?.partnerId == removed.beneficiaryId) {
                    chatMessages = emptyList()
                    selectedChatPartner = null
                }
            }
            Toast.makeText(this, "Test beneficiary removed — chat deleted.", Toast.LENGTH_SHORT).show()
            return
        }
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.removeCareConnection(connectionId)
                .onSuccess {
                    refreshCaregiverData()
                }
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
                .onFailure {
                    // Full, copyable detail — the toast truncated the fallback error.
                    resetErrorText = it.message ?: "Unknown error."
                }
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
        beneficiaryRiskSummary = null
        beneficiaryRiskSummaries = emptyMap()
        seenNotificationIds.clear()
        notificationIdsInitialized = false
        demoCaregiverLinked = false
        demoPhotoRequested = false
        demoPhotoDecision = null
        demoPhotoRequestedAt = null
        demoPhotoDecisionAt = null
        demoPhotoPath = null
        demoSnapshotRequest = null
        demoSnapshotMessage = null
        autoApproveCameraRequests = false
        cameraReady = false
        getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE)
            .edit().remove("camera_ready").apply()
        chatMessages = emptyList()
        selectedChatPartner = null
        beneficiaryHighRiskDialogIncident = null
        caregiverHighRiskPopupNotification = null
    }

    private fun sendTestHighRiskNotificationForBeneficiary() {
        val label = listOf("Glass Breaking", "Siren / Smoke Alarm", "Fire / Crackling", "Explosion / Gunshot").random()
        val confidence = 0.92f
        val incident = IncidentRecord(
            id = java.util.UUID.randomUUID().toString(),
            soundLabel = label,
            severity = SoundSeverity.High,
            confidence = confidence,
            status = IncidentStatus.WaitingUser,
            startedAt = System.currentTimeMillis(),
            nextDeadlineAt = System.currentTimeMillis() + IncidentStateMachine.TWO_MINUTES_MS,
        )
        beneficiaryHighRiskDialogIncident = incident
        AudioMonitoringService.simulateSound("emergency", label, confidence, true)
        lifecycleScope.launch {
            IncidentClient(this@MainActivity).createIncident(AlertEvent("emergency", label, confidence, SoundSeverity.High, System.currentTimeMillis()))
                .onSuccess { refreshBeneficiaryData() }
        }
        val first = connectedCaregivers.firstOrNull()
        val hint = if (first != null) "Recommended: request a verification photo — auto-approve will capture and send to chat." else "No caregiver linked yet."
        Toast.makeText(this, "$label detected — high risk. $hint", Toast.LENGTH_LONG).show()
        showHighRiskAlertNotification(
            beneficiaryId = authClient.userId().orEmpty(),
            beneficiaryName = fullName.ifBlank { "you" },
            soundLabel = label,
            confidence = confidence,
        )
    }

    private fun sendTestHighRiskNotificationForCaregiver() {
        val target = monitoredBeneficiaries.firstOrNull()
        val label = listOf("Glass Breaking", "Siren / Smoke Alarm", "Explosion / Gunshot", "Fire / Crackling").random()
        val nowIso = java.time.Instant.now().toString()
        val fake = CaregiverNotification(
            id = "dev_${System.currentTimeMillis()}",
            incidentId = java.util.UUID.randomUUID().toString(),
            beneficiaryId = target?.beneficiaryId.orEmpty(),
            soundLabel = label,
            severity = "high",
            confidence = 0.91f,
            status = "queued",
            createdAt = nowIso,
            incidentStartedAt = nowIso,
        )
        caregiverHighRiskPopupNotification = fake
        showHighRiskAlertNotification(
            beneficiaryId = fake.beneficiaryId,
            beneficiaryName = target?.name ?: "beneficiary",
            soundLabel = label,
            confidence = fake.confidence,
        )
        Toast.makeText(this, "$label detected — test alert sent to you as caregiver.", Toast.LENGTH_LONG).show()
        if (target != null) {
            lifecycleScope.launch {
                IncidentClient(this@MainActivity).createIncident(
                    AlertEvent("emergency", label, 0.91f, SoundSeverity.High, System.currentTimeMillis()),
                )
                refreshCaregiverData()
            }
        }
    }

    private fun addFakeCaregiverLocal() {
        devFakeCounter += 1
        val idx = devFakeCounter
        val fake = CaregiverMember(
            connectionId = "dev_caregiver_$idx",
            caregiverId = "dev_caregiver_id_$idx",
            name = listOf("Ava", "Ben", "Clara", "Dan", "Eva", "Finn", "Grace", "Hana").shuffled().first() + " $idx",
            phone = "+65 9000 ${1000 + idx}",
            email = "caregiver$idx@example.com",
            isPrimary = connectedCaregivers.isEmpty(),
            escalationOrder = connectedCaregivers.size + 1,
            status = "active",
        )
        connectedCaregivers = connectedCaregivers + fake
        val incident = SharedIncident(
            id = "dev_inc_cg_$idx",
            label = listOf("Glass Breaking", "Siren / Smoke Alarm").random(),
            severity = "high",
            confidence = 0.88f,
            status = "detected",
            startedAt = java.time.Instant.now().toString(),
        )
        val devKey = "dev_chats_caregiver_ids"
        getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE).edit()
            .putString(devKey, (getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE).getString(devKey, "").orEmpty() + ",${fake.caregiverId}").trim(','))
            .apply()
        chatListPreviews = (chatListPreviews + ChatPreview(fake.caregiverId, fake.name, fake.phone, "${incident.label} • high", System.currentTimeMillis(), 1)).distinctBy { it.partnerId }
        Toast.makeText(this, "Added ${fake.name}. Chat will be removed when you remove this caregiver.", Toast.LENGTH_SHORT).show()
    }

    private fun addFakeBeneficiaryLocal() {
        devFakeCounter += 1
        val idx = devFakeCounter
        val fake = MonitoredBeneficiary(
            connectionId = "dev_beneficiary_$idx",
            beneficiaryId = "dev_beneficiary_id_$idx",
            name = listOf("James", "Maya", "Noah", "Liam", "Aisha", "Omar", "Zoe", "Iris").shuffled().first() + " $idx",
            phone = "+65 9001 ${1000 + idx}",
            email = "beneficiary$idx@example.com",
            isPrimary = false,
            status = "active",
        )
        monitoredBeneficiaries = monitoredBeneficiaries + fake
        val label = listOf("Glass Breaking", "Explosion / Gunshot", "Siren / Smoke Alarm").random()
        val incident = SharedIncident(
            id = "dev_inc_bn_$idx",
            label = label,
            severity = "high",
            confidence = 0.89f,
            status = "waiting_user",
            startedAt = java.time.Instant.now().toString(),
            beneficiaryId = fake.beneficiaryId,
        )
        beneficiaryRiskSummaries = beneficiaryRiskSummaries + (fake.beneficiaryId to RiskSummary(RiskTier.High, label, System.currentTimeMillis()))
        chatListPreviews = (chatListPreviews + ChatPreview(fake.beneficiaryId, fake.name, fake.phone, "$label • high", System.currentTimeMillis(), 1)).distinctBy { it.partnerId }
        Toast.makeText(this, "Added ${fake.name}. Chat will be removed when you remove this beneficiary.", Toast.LENGTH_SHORT).show()
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
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TopBarHeader(
    name: String,
    roleSubtitle: String,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AvatarCircle(text = name.ifBlank { "User" }, sizeDp = 34)
            Column {
                Text(
                    text = name.ifBlank { "User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = roleSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
            }
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.ink700,
                modifier = Modifier.size(20.dp),
            )
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SoundGuard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Privacy-first sound monitoring & caregiver alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink500,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        // Outlined Google Sign In Button (preserving brand mark text)
        OutlinedButton(
            onClick = onGoogleLogin,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                " OR WITH EMAIL ",
                modifier = Modifier.padding(horizontal = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.ink500,
            )
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(12.dp))

        if (otpSent) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enter the 6-digit code sent to your email", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                OtpPillInput(value = otp, onValueChange = onOtpChange, length = 6, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onVerifyOtp,
                enabled = !busy && otp.filter(Char::isDigit).length == 6,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                ),
            ) {
                Text("Verify", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Button(
                onClick = onSendOtp,
                enabled = !busy && email.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (otpCooldownSeconds > 0) "Resend in ${otpCooldownSeconds}s" else "Send OTP Code", fontWeight = FontWeight.Bold)
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

// -------------------------------------------------------------------------------------------------
// ROLE SELECTION (FRAME 5)
// -------------------------------------------------------------------------------------------------

@Composable
private fun RoleSelection(
    initialRole: String = "Beneficiary",
    onRoleSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf(initialRole) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Who's this for?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "You can invite the other side to connect once you're set up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink500,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        // Role Card 1: Beneficiary (Monitored)
        RoleSelectionCard(
            title = "I want to be monitored",
            desc = "Emergency sounds get detected and sent to family who can check on you.",
            icon = Icons.Outlined.FavoriteBorder,
            selected = selected.equals("Beneficiary", ignoreCase = true),
            onClick = { selected = "Beneficiary" },
        )

        Spacer(Modifier.height(12.dp))

        // Role Card 2: Caregiver
        RoleSelectionCard(
            title = "I'm a caregiver",
            desc = "Get notified if someone you care for may be in danger.",
            icon = Icons.Outlined.People,
            selected = selected.equals("Caregiver", ignoreCase = true),
            onClick = { selected = "Caregiver" },
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onRoleSelected(selected) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerBg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val descColor = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.ink500
    val iconBorder = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerBg, RoundedCornerShape(18.dp))
            .border(
                1.6.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.6.dp, iconBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = descColor,
                lineHeight = 16.sp,
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETUP SCREEN
// -------------------------------------------------------------------------------------------------

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
    batteryExempted: Boolean,
    cameraReady: Boolean,
    setupMessage: String?,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onAutoApproveCameraRequestsChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onCameraTest: () -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = "${role ?: "User"} Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Complete your profile and safety permissions.",
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink500,
        )

        SectionLabel("Your details")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Emergency phone number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                val phoneValid = Validation.isPhoneValid(phone)
                Text(
                    text = when {
                        phoneValid -> "✓ Valid international phone number"
                        phone.isBlank() -> "Format: +65 81234567, +1 5551234567"
                        else -> "✗ Not a valid number — use format +65 81234567"
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        phoneValid -> MaterialTheme.colorScheme.success
                        phone.isBlank() -> MaterialTheme.colorScheme.ink500
                        else -> MaterialTheme.colorScheme.error
                    },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (role == "Beneficiary") {
            SectionLabel("Monitoring preferences")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onConsentChange(!consent) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = consent, onCheckedChange = onConsentChange)
                        Spacer(Modifier.width(8.dp))
                        Text("I consent to local audio monitoring & emergency alerts", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAutoApproveCameraRequestsChange(!autoApproveCameraRequests) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = autoApproveCameraRequests,
                            onCheckedChange = onAutoApproveCameraRequestsChange,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Auto-approve caregiver photo requests",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Off by default for your privacy. When disabled, you will be prompted to approve each photo request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.ink500,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        SectionLabel("Permissions & readiness")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Monitoring keeps listening while the phone is locked (screen off, not powered off). " +
                        "For best reliability grant mic + notification access and exempt SoundGuard from battery optimization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
                Spacer(Modifier.height(10.dp))
                ChecklistItem(isDone = microphoneGranted, title = "Microphone access")
                ChecklistItem(isDone = notificationsGranted, title = "Notification access")
                ChecklistItem(isDone = cameraReady, title = "Camera readiness")
                ChecklistItem(isDone = batteryExempted, title = "Battery optimization exemption (keeps monitoring while locked)")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRequestMic,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text("Microphone", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onRequestNotifications,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text("Notifications", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRequestBatteryExemption,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text("Battery", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onCameraTest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text("Test Camera", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onTermsChange(!termsAccepted) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = termsAccepted, onCheckedChange = onTermsChange)
            Spacer(Modifier.width(8.dp))
            Text("I agree to the Terms of Use", style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onOpenTerms) {
            Text("Read Terms of Use & camera privacy rules", color = MaterialTheme.colorScheme.ink700)
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Complete Setup", fontWeight = FontWeight.Bold)
        }

        setupMessage?.let {
            Spacer(Modifier.height(12.dp))
            CopyableErrorText(it)
        }
    }
}

@Composable
private fun ChecklistItem(isDone: Boolean, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape
                )
                .border(
                    1.4.dp,
                    if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TermsScreen(onBack: () -> Unit, onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Text("Terms of Use & Privacy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "SoundGuard is a safety support prototype, not a medical device or certified emergency response system. " +
                "It may miss sounds or produce false alerts. Keep the device powered, online, and positioned safely.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink700,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Camera privacy: Connected caregivers may request a verification snapshot during an active incident. " +
                "Snapshots are access-controlled and automatically deleted after 10 minutes. You can revoke this in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink700,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Emergency Notice: SoundGuard does not automatically contact police or ambulance services. " +
                "If an emergency occurs, caregivers and beneficiaries must contact local emergency services.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink700,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("I Understand and Agree", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingScreen(
    role: String?,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (role.equals("caregiver", ignoreCase = true)) {
                "Remind your beneficiary"
            } else {
                "Set up your device"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (role.equals("caregiver", ignoreCase = true)) {
                "Share these placement tips with your beneficiary."
            } else {
                "Follow these tips so SoundGuard can hear and see clearly."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.ink500,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))

        Image(
            painter = painterResource(id = R.drawable.onboarding),
            contentDescription = "Phone placement guide — front view and side view on a support stand",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingTip(
                icon = Icons.Outlined.PhoneAndroid,
                title = "Keep the phone upright",
                body = "Upright on a stable surface — never flat or face-down.",
            )
            OnboardingTip(
                icon = Icons.Outlined.Settings,
                title = "Use a phone stand",
                body = "Keeps the mic and camera aimed at the room.",
            )
            OnboardingTip(
                icon = Icons.Outlined.CameraAlt,
                title = "Don't block the camera",
                body = "Lens needs a clear view of the surroundings.",
            )
            OnboardingTip(
                icon = Icons.Outlined.Mic,
                title = "Keep the mic uncovered",
                body = "Nothing covering the bottom edge of the phone.",
            )
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("I've set it up", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/** Error text that can be long-pressed to select, with a one-tap Copy button. */
@Composable
private fun CopyableErrorText(text: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("SoundGuard error", text))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }) {
            Text("Copy", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun OnboardingTip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 1.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.ink500,
            )
        }
    }
}

@Composable
private fun CameraTestScreen(onBack: () -> Unit, onFinished: (String?) -> Unit) {
    var capturedPath by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Camera Readiness Test",
                style = MaterialTheme.typography.titleMedium,
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
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.success,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        Button(
            onClick = { capturedPath?.let(onFinished) },
            enabled = capturedPath != null,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(if (capturedPath == null) "Capture a photo first" else "Finish Camera Test", fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// BENEFICIARY DASHBOARD (FRAME 1)
// -------------------------------------------------------------------------------------------------

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BeneficiaryDashboard(
    fullName: String,
    email: String,
    audioState: LiveAudioState,
    riskSummary: RiskSummary?,
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
    onConnectCaregiver: (String) -> Unit,
    onSetPrimary: (String, String) -> Unit,
    onRemoveCaregiver: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenCaregiverChat: (ChatPreview) -> Unit,
    onLinkDemoCaregiver: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLab: () -> Unit = {},
    onTestHighRiskNotification: () -> Unit = {},
    onAddFakeCaregiver: () -> Unit = {},
    onRefresh: () -> Unit,
    onOpenChatList: () -> Unit = {},
) {
    var showPairingSheet by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBarHeader(
                    name = fullName.ifBlank { "James" },
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
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.dangerTint),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.danger.copy(alpha = 0.3f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CountdownRing(remaining, IncidentStateMachine.TWO_MINUTES_MS)
                                Column {
                                    Text("Sound detected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.danger)
                                    Text("Are you okay?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(incident.soundLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onIncidentResponse(BeneficiaryResponse.Safe) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                ) {
                                    Text("I'm safe", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onIncidentResponse(BeneficiaryResponse.NeedHelp) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.danger),
                                    border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.danger),
                                ) {
                                    Text("Send help", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 1. Hero monitoring card — mic = status/visual, button = action, alert = its own tappable row
                val riskTier = riskSummary?.tier ?: RiskTier.Quiet
                val isEmergency = audioState.isEmergency || (incident?.status == IncidentStatus.WaitingUser)
                val isMonitoring = audioState.isMonitoring

                // Ring color encodes monitoring state: grey = paused, pulsing green = live, red reserved for active alerts
                val heroRingColor = when {
                    isEmergency -> MaterialTheme.colorScheme.danger
                    isMonitoring -> MaterialTheme.colorScheme.success
                    else -> MaterialTheme.colorScheme.outline
                }

                val infinitePulse = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infinitePulse.animateFloat(
                    initialValue = 0.08f,
                    targetValue = 0.22f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulseAlpha",
                )

                val outerRingAlpha = if (isMonitoring && !isEmergency) pulseAlpha else 0.08f
                val innerRingAlpha = if (isMonitoring && !isEmergency) (pulseAlpha + 0.06f) else 0.14f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Status / Visual — Centered Mic Ring
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .background(heroRingColor.copy(alpha = outerRingAlpha), CircleShape)
                                .border(3.dp, heroRingColor, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .background(heroRingColor.copy(alpha = innerRingAlpha), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = null,
                                    tint = heroRingColor,
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = when {
                                isEmergency -> "Sound detected"
                                isMonitoring -> "Listening"
                                else -> "Monitoring paused"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when {
                                isEmergency -> "Caregivers alerted"
                                isMonitoring -> "Continuous sound protection active"
                                else -> "Tap below to start monitoring"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isEmergency) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.ink500,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(18.dp))

                        // Action — Start/Pause Button
                        Button(
                            onClick = onToggleMonitoring,
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(50.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                text = if (audioState.isMonitoring) "Pause monitoring" else "Start monitoring",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(Modifier.height(10.dp))

                        val isHighRiskLiveNow = audioState.isEmergency
                        val showLive = audioState.isMonitoring && audioState.soundCategory != "background"
                        val liveChipLabel = when {
                            isHighRiskLiveNow -> "HIGH RISK"
                            showLive -> "LIVE"
                            else -> null
                        }
                        val liveChipTone = when {
                            isHighRiskLiveNow -> IncidentStatusTone.Danger
                            showLive -> IncidentStatusTone.Success
                            else -> null
                        }
                        val liveAlertText = when {
                            isHighRiskLiveNow -> "${audioState.displayLabel} · ${(audioState.confidence * 100).toInt()}%"
                            showLive -> "${audioState.displayLabel} · ${(audioState.confidence * 100).toInt()}%"
                            riskTier == RiskTier.High && riskSummary?.lastAlertLabel != null -> {
                                val ago = formatLastAlertAgo(riskSummary.lastAlertAtMs)
                                if (ago != null) "${riskSummary.lastAlertLabel} detected · $ago" else "${riskSummary.lastAlertLabel} detected"
                            }
                            else -> "All quiet — no alerts in the last hour"
                        }
                        val effectiveChipLabel = liveChipLabel ?: riskTierChipLabel(riskTier)
                        val effectiveChipTone = liveChipTone ?: riskTierTone(riskTier)
                        val chipLabel = if (riskTier == RiskTier.Quiet && !isHighRiskLiveNow) riskTierChipLabel(RiskTier.Quiet) else effectiveChipLabel
                        val chipTone = if (riskTier == RiskTier.Quiet && !isHighRiskLiveNow) riskTierTone(RiskTier.Quiet) else effectiveChipTone

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDetailsSheet = true }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                StatusChip(
                                    label = chipLabel,
                                    tone = chipTone,
                                )
                                Text(
                                    text = liveAlertText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (showLive && audioState.isEmergency) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.ink500,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = "Details",
                                    tint = MaterialTheme.colorScheme.ink500,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 2. Connect a caregiver — label above the box (bidirectional pairing)
                SectionLabel("Connect a caregiver")
                var caregiverCodeInput by remember { mutableStateOf("") }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Enter your caregiver's 6-character code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.ink500,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        PairingCodeInput(
                            value = caregiverCodeInput,
                            onValueChange = { caregiverCodeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (caregiverCodeInput.length == 6) {
                                    onConnectCaregiver(caregiverCodeInput)
                                    caregiverCodeInput = ""
                                }
                            },
                            enabled = !loading && caregiverCodeInput.length == 6,
                            modifier = Modifier.fillMaxWidth(0.72f).height(44.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 3. Your caregivers — label above the box
                SectionLabel("Your caregivers")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "+ Add caregiver",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    if (pairingCode == null) onGenerateCode()
                                    showPairingSheet = true
                                },
                            )
                        }

                        if (caregivers.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No caregivers linked yet. Enter the 6-character code above or tap + Add to share yours.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.ink500,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            val supportsCarousel = caregivers.size > 1
                            if (supportsCarousel) {
                                Spacer(Modifier.height(8.dp))
                                BeneficiaryCaregiverCarousel(
                                    caregivers = caregivers,
                                    onSetPrimary = { id, cId -> onSetPrimary(id, cId) },
                                    onRemove = { id -> onRemoveCaregiver(id) },
                                    onCall = { phone -> onCall(phone) },
                                    onOpenChat = { preview -> onOpenCaregiverChat(preview) },
                                )
                            } else {
                                caregivers.forEach { caregiver ->
                                    Spacer(Modifier.height(8.dp))
                                    BeneficiaryCaregiverRow(
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
                                                    connectionId = caregiver.connectionId,
                                                    deactivated = caregiver.deactivated,
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 4. Developer & Test Tools (Collapsible Accordion)
                CollapsibleSection(
                    title = "Developer & test tools",
                    initiallyExpanded = false,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestToolButton(
                            title = "Mic lab — real YAMNet bench",
                            onClick = onOpenLab,
                        )
                        TestToolButton(
                            title = "Simulate glass break",
                            onClick = { onSimulateSound("emergency", "Glass break", 0.91f, true) },
                        )
                        TestToolButton(
                            title = "Simulate smoke alarm",
                            onClick = { onSimulateSound("emergency", "Smoke alarm", 0.94f, true) },
                        )
                        TestToolButton(
                            title = "Send test high-risk notification",
                            onClick = onTestHighRiskNotification,
                        )
                        TestToolButton(
                            title = "Add fake caregiver ( + chat)",
                            onClick = onAddFakeCaregiver,
                        )
                    }
                }

                message?.let {
                    Spacer(Modifier.height(12.dp))
                    CopyableErrorText(it)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Bottom Navigation Bar (Chat opens list when multiple connections exist)
        SoundGuardBottomNav(
            selectedTab = SoundGuardTab.Home,
            onTabSelected = { tab ->
                when (tab) {
                    SoundGuardTab.Home -> {}
                    SoundGuardTab.Chat -> onOpenChatList()
                    SoundGuardTab.People -> {
                        if (pairingCode == null) onGenerateCode()
                        showPairingSheet = true
                    }
                    SoundGuardTab.Settings -> onOpenSettings()
                }
            },
            peopleTabLabel = "Caregivers",
        )
    }

    // Pairing Code Sheet
    if (showPairingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPairingSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Connect a caregiver", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Give this 6-character code to your caregiver to link apps:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)

                Spacer(Modifier.height(16.dp))
                if (pairingCode != null) {
                    CodeDigitRow(code = pairingCode!!, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .padding(horizontal = 28.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = "••••••",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { pairingCode?.let(onCopyCode) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("Copy Code", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { pairingCode?.let(onShareCode) },
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onGenerateCode) {
                    Text("Generate new code", color = MaterialTheme.colorScheme.ink700)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Details Modal Sheet — now shows live sound type + live confidence so you can verify it's working
    if (showDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Live Audio Classification Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Classifier: Local TensorFlow Lite Audio Model", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Status: ${if (audioState.isMonitoring) "Actively processing continuous audio buffer" else "Idle"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.ink500,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (audioState.isMonitoring) "Hearing: ${audioState.displayLabel}" else "Hearing: — (monitoring paused)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (audioState.isEmergency) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Category: ${audioState.soundCategory}  •  Mic ${(audioState.amplitude * 100).toInt()}%  •  updated ${if (audioState.lastFrameAtMs == 0L) "—" else "${(System.currentTimeMillis() - audioState.lastFrameAtMs) / 1000}s ago"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
                Spacer(Modifier.height(10.dp))
                ConfidenceBar(audioState.confidence, "Live Confidence")
                if (audioState.topCandidates.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Top candidates (live)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.ink700)
                    Spacer(Modifier.height(6.dp))
                    audioState.topCandidates.take(3).forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(c.displayLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("${(c.score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        ConfidenceBar(c.score, "")
                    }
                    if (audioState.debugText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Raw: ${audioState.debugText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                    }
                }
                if (!audioState.isMonitoring) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Start monitoring to see live confidence update in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.ink500,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Tip: play a short clip from a second device near the mic — the bench logs what YAMNet actually sent back.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDetailsSheet = false },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Close Details", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MicLabScreen(
    audioState: LiveAudioState,
    onToggleMonitoring: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val clipboard = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text("Mic lab — YAMNet bench", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(if (audioState.isMonitoring) "Listening — play a short clip near the phone mic" else "Monitoring paused", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (audioState.isEmergency) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("Now hearing: ${audioState.displayLabel}  •  ${audioState.soundCategory}  •  ${(audioState.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Mic: ${(audioState.amplitude * 100).toInt()}%  •  ${if (audioState.lastFrameAtMs == 0L) "no frames yet" else "updated ${(System.currentTimeMillis() - audioState.lastFrameAtMs) / 1000}s ago"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                    Spacer(Modifier.height(8.dp))
                    ConfidenceBar(audioState.confidence, "Live Confidence")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onToggleMonitoring, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text(if (audioState.isMonitoring) "Stop" else "Start listening", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(999.dp)) {
                            Text("Clear log", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                    if (!audioState.isMonitoring) {
                        Spacer(Modifier.height(8.dp))
                        Text("Tip: start listening, then play 2–5s clips loudly near the mic. Keep raw log below to check accuracy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                    }
                }
            }
            if (audioState.topCandidates.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Why it did (or didn't) alert", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        audioState.topCandidates.take(3).forEach { c ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.displayLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(c.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                                }
                                Text("${(c.score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            ConfidenceBar(c.score, "")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Thresholds: fire 20% (1.5× boost), crying 30%, smoke/alarm 45%, siren 45%, glass break 40% — single frame triggers. Booms/impacts show as medium, never alert. TV/music >45% raises thresholds +20%.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        if (audioState.debugText.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Raw YAMNet top indices: ${audioState.debugText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Live timeline (newest first)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { val dump = audioState.probeLog.reversed().joinToString("\n") { "${it.label} [${it.category}] ${(it.confidence * 100).toInt()}% ${if (it.isEmergency) "ALERT" else ""}" }; clipboard.setPrimaryClip(ClipData.newPlainText("SoundGuard lab log", dump.ifBlank { "no frames yet" })); }) { Text("Copy") }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (audioState.probeLog.isEmpty()) {
                        Text("No frames yet — start listening and make some sound near the mic (or use a clip). The mic level above must jump above ~10%.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            audioState.probeLog.reversed().take(24).forEach { snap ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(snap.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (snap.isEmergency) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.onSurface)
                                        Text("${snap.category} · ${(snap.confidence * 100).toInt()}%${if (snap.isEmergency) " · ALERT" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.ink500)
                                    }
                                    Text("${(System.currentTimeMillis() - snap.ts) / 1000}s ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.ink300)
                                }
                            }
                        }
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Clips to test with (use a second device, play 3–8s loudly near the mic)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Search YouTube / Freesound for short, isolated clips — don't use music mixes:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                    Spacer(Modifier.height(6.dp))
                    Text("• Glass break — \"glass breaking sound effect\" (3s, single smash)\n• Smoke alarm — \"smoke alarm beep sound\" (continuous 3×beeps)\n• Siren — \"fire alarm siren\"\n• Baby crying — \"baby crying sound\"\n• Dog barking — \"dog bark isolated\"\n• Thunder — \"thunder clap\"\n• Water / tap running\n• Door knock — \"loud door knock\"\n• TV / music (negative control — should raise threshold, not alert)\n• Silence / room tone (mic should sit ~5–10%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("How to judge: play the same clip 3×. You want the same label in the top 3 with ≥30% (emergency) or ≥65% (sirens) — check the timeline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TestToolButton(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.ink700,
        )
        StatusChip(label = "TEST", tone = IncidentStatusTone.Neutral)
    }
}

@Composable
private fun BeneficiaryCaregiverCarousel(
    caregivers: List<CaregiverMember>,
    onSetPrimary: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenChat: (ChatPreview) -> Unit,
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0) { caregivers.size }
    val total = caregivers.size
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        val caregiver = caregivers[page]
        BeneficiaryCaregiverRow(
            caregiver = caregiver,
            onSetPrimary = { onSetPrimary(caregiver.connectionId, caregiver.caregiverId) },
            onRemove = { onRemove(caregiver.connectionId) },
            onCall = { onCall(caregiver.phone) },
            onOpenChat = {
                onOpenChat(
                    ChatPreview(
                        partnerId = caregiver.caregiverId,
                        partnerName = caregiver.name,
                        partnerPhone = caregiver.phone,
                        lastMessage = "",
                        lastTimestamp = System.currentTimeMillis(),
                        unreadCount = 0,
                        connectionId = caregiver.connectionId,
                        deactivated = caregiver.deactivated,
                    )
                )
            },
        )
    }
    if (total > 1) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier.padding(horizontal = 3.dp).size(if (index == pagerState.currentPage) 7.dp else 6.dp)
                        .background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun BeneficiaryCaregiverRow(
    caregiver: CaregiverMember,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
    onCall: () -> Unit,
    onOpenChat: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenChat)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarCircle(
                text = caregiver.name,
                sizeDp = 64,
                backgroundColor = if (caregiver.deactivated) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = caregiver.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            if (caregiver.deactivated) {
                StatusChip(label = "DEACTIVATED", tone = IncidentStatusTone.Neutral)
                Spacer(Modifier.height(6.dp))
                Text(
                    "This caregiver deactivated their account. Recommended: remove to clear the chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                    textAlign = TextAlign.Center,
                )
            } else {
                if (caregiver.isPrimary) {
                    StatusChip(label = "PRIMARY", tone = IncidentStatusTone.Neutral)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap to open chat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCall,
                    enabled = caregiver.phone.isNotBlank() && !caregiver.deactivated,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Call", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = onOpenChat,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Open chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.ink700,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (!caregiver.isPrimary && !caregiver.deactivated) {
                    DropdownMenuItem(
                        text = { Text("Set as primary") },
                        onClick = { menuExpanded = false; onSetPrimary() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Remove caregiver", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onRemove() },
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CAREGIVER DASHBOARD (FRAME 2)
// -------------------------------------------------------------------------------------------------

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CaregiverDashboard(
    fullName: String,
    email: String,
    beneficiaries: List<MonitoredBeneficiary>,
    riskSummaries: Map<String, RiskSummary>,
    pairingCode: String?,
    loading: Boolean,
    message: String?,
    onConnectBeneficiary: (String) -> Unit,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onShareCode: (String) -> Unit,
    onRemoveBeneficiary: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenBeneficiaryChat: (ChatPreview) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenChatList: () -> Unit = {},
    onTestHighRiskNotification: () -> Unit = {},
    onAddFakeBeneficiary: () -> Unit = {},
) {
    var codeInput by remember { mutableStateOf("") }
    var showPairingSheet by remember { mutableStateOf(false) }

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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBarHeader(
                    name = fullName.ifBlank { "Yo" },
                    roleSubtitle = "Caregiver",
                    onOpenSettings = onOpenSettings,
                )

                // 1. Connect a beneficiary — label above the box
                SectionLabel("Connect a beneficiary")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Enter your beneficiary's 6-character code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.ink500,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))

                        PairingCodeInput(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (codeInput.length == 6) {
                                    onConnectBeneficiary(codeInput)
                                    codeInput = ""
                                }
                            },
                            enabled = !loading && codeInput.length == 6,
                            modifier = Modifier.fillMaxWidth(0.72f).height(44.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 2. People you monitor — swipeable slideshow, label above the box
                SectionLabel("People you monitor")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (beneficiaries.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "You have not connected to any beneficiaries yet. Enter a 6-character code above or share yours with + Add.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.ink500,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                        } else {
                            CaregiverBeneficiaryCarousel(
                                beneficiaries = beneficiaries,
                                riskSummaries = riskSummaries,
                                onRemove = { id -> onRemoveBeneficiary(id) },
                                onCall = { phone -> onCall(phone) },
                                onOpen = { preview -> onOpenBeneficiaryChat(preview) },
                            )
                            if (beneficiaries.size > 1) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Swipe to see everyone",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.ink500,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                CollapsibleSection(title = "Developer & test tools", initiallyExpanded = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestToolButton(title = "Send test high-risk notification", onClick = onTestHighRiskNotification)
                        TestToolButton(title = "Add fake beneficiary ( + chat)", onClick = onAddFakeBeneficiary)
                    }
                }

                message?.let {
                    Spacer(Modifier.height(12.dp))
                    CopyableErrorText(it)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Bottom Navigation Bar
        SoundGuardBottomNav(
            selectedTab = SoundGuardTab.Home,
            onTabSelected = { tab ->
                when (tab) {
                    SoundGuardTab.Home -> {}
                    SoundGuardTab.Chat -> onOpenChatList()
                    SoundGuardTab.People -> {
                        if (pairingCode == null) onGenerateCode()
                        showPairingSheet = true
                    }
                    SoundGuardTab.Settings -> onOpenSettings()
                }
            },
            peopleTabLabel = "Beneficiaries",
        )
    }

    // Pairing Code Sheet — caregiver's own code, like the beneficiary's
    if (showPairingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPairingSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Connect a beneficiary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Give this 6-character code to your beneficiary to link apps:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))
                if (pairingCode != null) {
                    CodeDigitRow(code = pairingCode!!, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .padding(horizontal = 28.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = "••••••",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { pairingCode?.let(onCopyCode) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("Copy Code", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { pairingCode?.let(onShareCode) },
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onGenerateCode) {
                    Text("Generate new code", color = MaterialTheme.colorScheme.ink700)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CaregiverBeneficiaryCarousel(
    beneficiaries: List<MonitoredBeneficiary>,
    riskSummaries: Map<String, RiskSummary>,
    onRemove: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpen: (ChatPreview) -> Unit,
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0) { beneficiaries.size }
    val total = beneficiaries.size
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        val beneficiary = beneficiaries[page]
        val summary = riskSummaries[beneficiary.beneficiaryId]
        MonitoredBeneficiaryCard(
            beneficiary = beneficiary,
            summary = summary,
            onRemove = { onRemove(beneficiary.connectionId) },
            onCall = { onCall(beneficiary.phone) },
            onOpen = {
                onOpen(
                    ChatPreview(
                        partnerId = beneficiary.beneficiaryId,
                        partnerName = beneficiary.name,
                        partnerPhone = beneficiary.phone,
                        lastMessage = "",
                        lastTimestamp = System.currentTimeMillis(),
                        unreadCount = 0,
                        connectionId = beneficiary.connectionId,
                        deactivated = beneficiary.deactivated,
                    )
                )
            },
        )
    }
    if (total > 1) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier.padding(horizontal = 3.dp).size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun MonitoredBeneficiaryCard(
    beneficiary: MonitoredBeneficiary,
    summary: RiskSummary?,
    onRemove: () -> Unit,
    onCall: () -> Unit,
    onOpen: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val tier = summary?.tier ?: RiskTier.Quiet
    val lastAlertAgo = formatLastAlertAgo(summary?.lastAlertAtMs)
    val deactivated = beneficiary.deactivated

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (deactivated) MaterialTheme.colorScheme.surfaceVariant
                    else when (tier) {
                        RiskTier.High -> MaterialTheme.colorScheme.dangerTint
                        RiskTier.Medium -> MaterialTheme.colorScheme.warningTint
                        RiskTier.Quiet -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    RoundedCornerShape(18.dp),
                )
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarCircle(
                text = beneficiary.name,
                sizeDp = 64,
                backgroundColor = if (deactivated) MaterialTheme.colorScheme.outline
                else when (tier) {
                    RiskTier.High -> MaterialTheme.colorScheme.danger
                    RiskTier.Medium -> MaterialTheme.colorScheme.warning
                    RiskTier.Quiet -> MaterialTheme.colorScheme.primary
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = beneficiary.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            if (deactivated) {
                StatusChip(label = "DEACTIVATED", tone = IncidentStatusTone.Neutral)
            } else {
                StatusChip(label = riskTierChipLabel(tier), tone = riskTierTone(tier))
            }
            Spacer(Modifier.height(6.dp))
            val detailText = if (deactivated) {
                "This beneficiary deactivated their account. Recommended: remove to clear the chat."
            } else when {
                tier == RiskTier.Quiet && summary?.lastAlertLabel != null ->
                    "Last alert: ${summary.lastAlertLabel}${lastAlertAgo?.let { " · $it" } ?: ""}"
                tier != RiskTier.Quiet ->
                    buildString {
                        summary?.lastAlertLabel?.let { append(it) }
                        lastAlertAgo?.let { if (isNotEmpty()) append(" · "); append(it) }
                        if (isEmpty()) append("Recent alert")
                    }
                else -> "Tap to open chat"
            }
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodySmall,
                color = if (deactivated) MaterialTheme.colorScheme.ink500
                else if (tier == RiskTier.High) MaterialTheme.colorScheme.danger
                else if (tier == RiskTier.Medium) MaterialTheme.colorScheme.warning
                else MaterialTheme.colorScheme.ink500,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCall,
                    enabled = beneficiary.phone.isNotBlank() && !deactivated,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Call", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Open chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.ink700,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Remove beneficiary", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onRemove() },
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETTINGS SCREEN (FRAME 4 + DARK MODE TEST TOGGLE)
// -------------------------------------------------------------------------------------------------

@Composable
private fun SettingsScreen(
    fullName: String,
    email: String,
    phone: String,
    currentRole: String,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    batteryExempted: Boolean,
    autoApproveCameraRequests: Boolean,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAutoApproveCameraRequestsChange: (Boolean) -> Unit,
    onRequestSwitchRole: () -> Unit,
    onOpenTerms: () -> Unit,
    onCameraTest: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onResetAllData: () -> Unit,
) {
    var editNameDialog by remember { mutableStateOf(false) }
    var editPhoneDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(fullName) }
    var tempPhone by remember { mutableStateOf(phone) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // 1. PROFILE SECTION matching Mockup Frame 4
            Text(
                text = "PROFILE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    // Full name row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempName = fullName; editNameDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Full name", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(fullName.ifBlank { "Not set" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    // Emergency phone row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempPhone = phone; editPhoneDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Emergency phone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(phone.ifBlank { "Not set" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 2. APPEARANCE (Dark mode / Light mode testing switch)
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.ink700, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (darkModeEnabled) "Enabled (High-contrast dark)" else "Disabled (Monochrome light)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                    }
                    SoundGuardSwitch(
                        checked = darkModeEnabled,
                        onCheckedChange = onDarkModeToggle,
                    )
                }
            }

            // 3. PERMISSIONS SECTION matching Mockup Frame 4
            Text(
                text = "PERMISSIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    // Microphone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.ink700, modifier = Modifier.size(16.dp))
                            }
                            Text("Microphone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        SoundGuardSwitch(
                            checked = microphoneGranted,
                            onCheckedChange = { onRequestPermissions() },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    // Notifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.ink700, modifier = Modifier.size(16.dp))
                            }
                            Text("Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        SoundGuardSwitch(
                            checked = notificationsGranted,
                            onCheckedChange = { onRequestPermissions() },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    // Battery optimization (keeps monitoring while phone is locked)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.ink700, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Keep monitoring while locked", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (batteryExempted) "Battery optimization exempted"
                                    else "Exempt SoundGuard from battery optimization",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.ink500,
                                )
                            }
                        }
                        SoundGuardSwitch(
                            checked = batteryExempted,
                            onCheckedChange = { onRequestBatteryExemption() },
                        )
                    }
                }
            }

            // 4. SAFETY SECTION matching Mockup Frame 4
            Text(
                text = "SAFETY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            val isBeneficiaryRole = !currentRole.equals("caregiver", ignoreCase = true)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBeneficiaryRole) "Auto-approve photo requests" else "Photo verification privacy",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (isBeneficiaryRole) {
                                "Allow linked caregivers to take verification snapshots during active incidents without manual confirmation. Off by default for your privacy."
                            } else {
                                "Beneficiaries hold full control over camera snapshots. During an active incident, requests will prompt them for confirmation unless they have enabled auto-approval."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.ink500,
                        )
                    }
                    if (isBeneficiaryRole) {
                        Spacer(Modifier.width(8.dp))
                        SoundGuardSwitch(
                            checked = autoApproveCameraRequests,
                            onCheckedChange = onAutoApproveCameraRequestsChange,
                        )
                    }
                }
            }

            // 5. DIAGNOSTICS & PRIVACY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCameraTest)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Camera readiness test", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenTerms)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Terms & Privacy Notice", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 6. DANGER ZONE & SIGN OUT matching Mockup Frame 4
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            val targetRole = if (currentRole.equals("caregiver", ignoreCase = true)) "beneficiary" else "caregiver"
            Text(
                text = "Switch to $targetRole mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.ink700,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRequestSwitchRole)
                    .padding(vertical = 6.dp),
            )

            Text(
                text = "Delete all account data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.ink500,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onResetAllData)
                    .padding(vertical = 6.dp),
            )

            Text(
                text = "Sign out",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignOut)
                    .padding(vertical = 8.dp),
            )
        }
    }

    // Name Edit Dialog
    if (editNameDialog) {
        AlertDialog(
            onDismissRequest = { editNameDialog = false },
            title = { Text("Edit Full Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onFullNameChange(tempName)
                    editNameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Phone Edit Dialog
    if (editPhoneDialog) {
        AlertDialog(
            onDismissRequest = { editPhoneDialog = false },
            title = { Text("Edit Emergency Phone", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempPhone,
                    onValueChange = { tempPhone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onPhoneChange(tempPhone)
                    editPhoneDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editPhoneDialog = false }) { Text("Cancel") }
            },
        )
    }
}
