package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val setupCompletedAt: String?,
)

class ProfileClient(context: Context) {
    private val authClient = AuthClient(context)

    suspend fun fetchMyProfile(): Result<UserProfile?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: return@runCatching null
            val userId = authClient.userId() ?: return@runCatching null
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/profiles?id=eq.$userId&select=id,email,full_name,phone,role,setup_completed_at"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }

            try {
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    error("Profile fetch failed with HTTP $responseCode: $responseBody")
                }
                val jsonArray = org.json.JSONArray(responseBody)
                if (jsonArray.length() == 0) return@runCatching null
                val obj = jsonArray.getJSONObject(0)
                UserProfile(
                    id = obj.optString("id", userId),
                    email = obj.optString("email", ""),
                    fullName = obj.optString("full_name", ""),
                    phone = obj.optString("phone", ""),
                    role = obj.optString("role", "")
                        .takeUnless { it.equals("null", ignoreCase = true) }
                        .orEmpty(),
                    setupCompletedAt = obj.optString("setup_completed_at").takeIf { it.isNotBlank() },
                )
            } finally {
                connection.disconnect()
            }
        }
    }

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
            val nowIso = java.time.Instant.now().toString()
            val body = JSONObject().apply {
                put("id", userId)
                put("email", email.trim())
                put("full_name", fullName.trim())
                put("phone", phone.trim())
                put("role", role.lowercase())
                put("setup_completed_at", nowIso)
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

    suspend fun saveBeneficiarySettings(
        monitoringConsent: Boolean,
        shareWithCaregiver: Boolean = false,
        cameraRequestsConsent: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Your session has expired. Please sign in again.")
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/beneficiary_settings"
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
                put("user_id", userId)
                put("consent_monitoring", monitoringConsent)
                put("consent_share_with_caregiver", shareWithCaregiver)
                put("consent_camera_requests", cameraRequestsConsent)
                put("updated_at", java.time.Instant.now().toString())
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Beneficiary settings save failed with HTTP $responseCode: ${response.ifBlank { "empty response" }}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun resetRole(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Your session has expired.")
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/profiles?id=eq.$userId"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
                doOutput = true
            }
            val body = JSONObject().apply {
                put("role", JSONObject.NULL)
                put("setup_completed_at", JSONObject.NULL)
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Role reset failed with HTTP $responseCode: $response")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun resetAllAccountData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Your session has expired.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/reset_my_account_data"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            try {
                connection.outputStream.use { it.write("{}".toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Account reset failed with HTTP $responseCode: $response")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
