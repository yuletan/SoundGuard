package com.yuletan.soundguard

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AuthClient(context: Context) {
    companion object {
        private const val TAG = "SoundGuardAuth"
    }

    private val preferences = context.getSharedPreferences("soundguard_auth", Context.MODE_PRIVATE)

    fun accessToken(): String? = preferences.getString("access_token", null)

    fun userId(): String? = preferences.getString("user_id", null)

    fun signOut() {
        preferences.edit().clear().apply()
    }

    suspend fun getToken(): String = withContext(Dispatchers.IO) {
        val current = accessToken()
        val issuedAt = preferences.getLong("token_issued_at", 0L)
        val ageMs = System.currentTimeMillis() - issuedAt
        if (current != null && ageMs < 50 * 60 * 1000) return@withContext current

        val refreshToken = preferences.getString("refresh_token", null)
        if (refreshToken != null) {
            val refreshed = refreshAccessToken(refreshToken).getOrNull()
            if (refreshed != null) return@withContext refreshed
        }
        error("Session expired. Please sign in again.")
    }

    private suspend fun refreshAccessToken(refreshToken: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val endpoint = "$baseUrl/auth/v1/token?grant_type=refresh_token"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            try {
                connection.outputStream.use { it.write(JSONObject().apply { put("refresh_token", refreshToken) }.toString().toByteArray()) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Token refresh failed HTTP $code: $body")
                val json = JSONObject(body)
                val newAccessToken = json.optString("access_token")
                val newRefreshToken = json.optString("refresh_token")
                if (newAccessToken.isBlank()) error("Token refresh returned empty access_token")
                preferences.edit()
                    .putString("access_token", newAccessToken)
                    .putLong("token_issued_at", System.currentTimeMillis())
                    .apply()
                if (newRefreshToken.isNotBlank()) {
                    preferences.edit().putString("refresh_token", newRefreshToken).apply()
                }
                newAccessToken
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun sendOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        request("/auth/v1/otp", JSONObject().apply {
            put("email", email.trim())
            put("create_user", true)
        }).map { }
    }

    suspend fun verifyOtp(email: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        request("/auth/v1/verify", JSONObject().apply {
            put("email", email.trim())
            put("token", token.trim())
            put("type", "email")
        }).map { response ->
            val accessToken = response.optString("access_token")
            if (accessToken.isBlank()) error("No session returned by Supabase")
            val refreshToken = response.optString("refresh_token")
            val userId = response.optJSONObject("user")?.optString("id").orEmpty()
            if (userId.isBlank()) error("No user id returned by Supabase")
            preferences.edit()
                .putString("access_token", accessToken)
                .putString("user_id", userId)
                .putLong("token_issued_at", System.currentTimeMillis())
                .apply()
            if (refreshToken.isNotBlank()) {
                preferences.edit().putString("refresh_token", refreshToken).apply()
            }
        }
    }

    fun googleAuthorizationUri(): Uri = Uri.parse(
        BuildConfig.SUPABASE_URL.trimEnd('/') +
            "/auth/v1/authorize?provider=google&redirect_to=" +
            URLEncoder.encode("soundguard://auth/callback", StandardCharsets.UTF_8.toString()),
    )

    suspend fun handleGoogleCallback(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val values = mutableMapOf<String, String>()
            uri.query?.split('&').orEmpty().forEach { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size == 2) values[parts[0]] = Uri.decode(parts[1])
            }
            uri.fragment?.split('&').orEmpty().forEach { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size == 2) values[parts[0]] = Uri.decode(parts[1])
            }
            val callbackError = values["error_description"] ?: values["error"]
            if (!callbackError.isNullOrBlank()) error("Google sign-in failed: $callbackError")
            val token = values["access_token"] ?: error("Google sign-in returned no access token")
            val refreshToken = values["refresh_token"].orEmpty()
            val user = fetchUser(token)
            val userId = user.optString("id")
            if (userId.isBlank()) error("Google sign-in returned no user id")
            preferences.edit()
                .putString("access_token", token)
                .putString("user_id", userId)
                .putLong("token_issued_at", System.currentTimeMillis())
                .apply()
            if (refreshToken.isNotBlank()) {
                preferences.edit().putString("refresh_token", refreshToken).apply()
            }
        }
    }

    private fun fetchUser(token: String): JSONObject {
        val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/user"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            val responseCode = connection.responseCode
            val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) error("User lookup failed with HTTP $responseCode: $body")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun request(path: String, body: JSONObject): Result<JSONObject> = runCatching {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        if (baseUrl.isBlank()) {
            error("Configuration error: SUPABASE_URL is empty in local.properties.")
        }
        if (BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
            error("Configuration error: SUPABASE_ANON_KEY is empty in local.properties.")
        }

        val endpoint = baseUrl + path
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            Log.d(TAG, "Supabase response: method=POST path=$path status=$responseCode body=$responseBody")

            if (responseCode !in 200..299) {
                val parsed = runCatching { JSONObject(responseBody) }.getOrNull()
                val errorCode = parsed?.optString("error_code").orEmpty()
                val errorId = parsed?.optString("error_id").orEmpty()
                val detail = parsed?.optString("msg")?.ifBlank {
                    parsed.optString("message")
                }?.ifBlank {
                    parsed.optString("error_description")
                }?.ifBlank {
                    responseBody
                } ?: responseBody
                val diagnostic = listOf(errorCode, errorId)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                error(
                    "Supabase HTTP $responseCode at $path" +
                        (if (diagnostic.isNotBlank()) " [$diagnostic]" else "") +
                        ": ${detail.ifBlank { "empty response" }}",
                )
            }
            if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }.onFailure { error ->
        Log.e(TAG, "Supabase request failed", error)
    }
}
