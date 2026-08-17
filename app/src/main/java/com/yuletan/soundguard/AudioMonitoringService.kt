package com.yuletan.soundguard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class LiveAudioState(
    val isMonitoring: Boolean = false,
    val soundCategory: String = "background",
    val displayLabel: String = "Normal Background",
    val confidence: Float = 0.95f,
    val amplitude: Float = 0.05f,
    val isEmergency: Boolean = false,
    val isSimulated: Boolean = false,
    val lastEmergencyTimestamp: Long? = null,
)

class AudioMonitoringService : Service() {
    companion object {
        private const val TAG = "AudioMonitoringService"
        private const val CHANNEL_ID = "soundguard_audio_monitoring"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.yuletan.soundguard.action.START_MONITORING"
        const val ACTION_STOP = "com.yuletan.soundguard.action.STOP_MONITORING"

        private var simulatedUntil: Long = 0L

        private val _audioState = MutableStateFlow(LiveAudioState())
        val audioState: StateFlow<LiveAudioState> = _audioState.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, AudioMonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun simulateSound(category: String, label: String, confidence: Float, isEmergency: Boolean) {
            val isAmbient = (category == "background")
            simulatedUntil = if (isAmbient) 0L else System.currentTimeMillis() + 8000L
            _audioState.value = _audioState.value.copy(
                soundCategory = category,
                displayLabel = label,
                confidence = confidence,
                isEmergency = isEmergency,
                isSimulated = !isAmbient,
                lastEmergencyTimestamp = if (isEmergency) System.currentTimeMillis() else _audioState.value.lastEmergencyTimestamp,
                amplitude = if (isEmergency) 0.88f else if (isAmbient) 0.08f else 0.55f,
            )
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var audioRecord: AudioRecord? = null
    private var classifier: SoundClassifier? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        classifier = SoundClassifier(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("Active monitoring enabled"))
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (_audioState.value.isMonitoring) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Microphone permission not granted")
            stopSelf()
            return
        }

        _audioState.value = _audioState.value.copy(
            isMonitoring = true,
            displayLabel = "Listening...",
            soundCategory = "background",
            confidence = 0.90f,
            amplitude = 0.08f,
        )

        serviceScope.launch {
            recordAndClassifyLoop()
        }
    }

    private fun recordAndClassifyLoop() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
            SoundClassifier.INPUT_SAMPLES * 2,
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                _audioState.value = _audioState.value.copy(displayLabel = "Mic Init Failed")
                return
            }

            audioRecord?.startRecording()
            Log.i(TAG, "Audio recording started at 16kHz Mono")

            val pcmShorts = ShortArray(SoundClassifier.INPUT_SAMPLES)
            val floatBuffer = FloatArray(SoundClassifier.INPUT_SAMPLES)

            while (serviceScope.isActive && _audioState.value.isMonitoring) {
                var readCount = 0
                while (readCount < SoundClassifier.INPUT_SAMPLES && serviceScope.isActive) {
                    val read = audioRecord?.read(pcmShorts, readCount, SoundClassifier.INPUT_SAMPLES - readCount) ?: -1
                    if (read <= 0) break
                    readCount += read
                }

                if (readCount > 0) {
                    // Check if simulation lock is active
                    if (System.currentTimeMillis() < simulatedUntil) {
                        continue
                    }

                    // Convert PCM 16-bit to Float normalized [-1.0, 1.0] and calculate RMS amplitude
                    var sumSquares = 0.0
                    for (i in 0 until readCount) {
                        val norm = pcmShorts[i] / 32768.0f
                        floatBuffer[i] = norm
                        sumSquares += (norm * norm)
                    }
                    val rms = sqrt(sumSquares / readCount).toFloat()
                    val boostedAmplitude = (rms * 12.0f).coerceIn(0.05f, 1f)

                    val classification = classifier?.classify(floatBuffer)
                    if (classification != null) {
                        _audioState.value = _audioState.value.copy(
                            soundCategory = classification.category,
                            displayLabel = classification.displayLabel,
                            confidence = classification.confidence,
                            amplitude = boostedAmplitude,
                            isEmergency = classification.isEmergency,
                            isSimulated = false,
                            lastEmergencyTimestamp = if (classification.isEmergency) System.currentTimeMillis() else _audioState.value.lastEmergencyTimestamp,
                        )

                        if (classification.isEmergency) {
                            updateNotification("🚨 Emergency Sound: ${classification.displayLabel}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in audio classification loop: ${e.message}", e)
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord: ${e.message}")
            }
        }
    }

    private fun stopMonitoring() {
        simulatedUntil = 0L
        _audioState.value = _audioState.value.copy(
            isMonitoring = false,
            displayLabel = "Monitoring Paused",
            soundCategory = "background",
            amplitude = 0f,
            isEmergency = false,
            isSimulated = false,
        )
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SoundGuard Audio Monitoring",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows live audio monitoring status for safety support"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SoundGuard Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
