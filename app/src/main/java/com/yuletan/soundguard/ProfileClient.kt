package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ProfileClient(context: Context) {
    private val authClient = AuthClient(context)

    suspend fun saveProfile(
        email: String,
        fullName: String,
        phone: String,
        role: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Your session has expired. Please sign in again.")
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/profiles"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                doOutput = true
            }
            val body = JSONObject().apply {
                put("id", userId)
                put("email", email.trim())
                put("full_name", fullName.trim())
                put("phone", phone.trim())
                put("role", role.lowercase())
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Profile save failed with HTTP $responseCode: ${response.ifBlank { "empty response" }}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
