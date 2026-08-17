package com.yuletan.soundguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PushMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title
            ?: data["title"]
            ?: "SoundGuard alert"
        val body = message.notification?.body
            ?: data["body"]
            ?: "A caregiver alert requires your attention."
        showAlertNotification(title, body, data["incident_id"])
    }

    override fun onNewToken(token: String) {
        PushTokenRegistrar.saveAndRegister(this, token)
    }

    private fun showAlertNotification(title: String, body: String, incidentId: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val channelId = "soundguard_alerts"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "SoundGuard alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Urgent caregiver notifications from SoundGuard"
                    enableVibration(true)
                },
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("incident_id", incidentId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            incidentId?.hashCode() ?: System.currentTimeMillis().toInt(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0,
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(
            incidentId?.hashCode() ?: System.currentTimeMillis().toInt(),
            notification,
        )
    }
}

object PushTokenRegistrar {
    private const val PREFERENCES = "soundguard_push"
    private const val TOKEN_KEY = "pending_token"

    fun register(context: Context) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> saveAndRegister(context, token) }
    }

    fun saveAndRegister(context: Context, token: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN_KEY, token)
            .apply()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            DeviceTokenClient(context.applicationContext).registerToken(token)
        }
    }
}
