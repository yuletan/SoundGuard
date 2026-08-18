package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DeviceTokenClient(context: Context) {
    private val authClient = AuthClient(context)

    suspend fun registerToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val accessToken = authClient.getToken()
            val userId = authClient.userId() ?: return@runCatching
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) return@runCatching

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/device_push_tokens"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                doOutput = true
            }
            val body = JSONObject().apply {
                put("user_id", userId)
                put("token", token)
                put("platform", "android")
                put("updated_at", java.time.Instant.now().toString())
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Push token registration failed with HTTP $responseCode: $response")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
