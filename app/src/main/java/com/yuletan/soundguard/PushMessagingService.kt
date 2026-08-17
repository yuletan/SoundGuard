package com.yuletan.soundguard

import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PushMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushTokenRegistrar.saveAndRegister(this, token)
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
