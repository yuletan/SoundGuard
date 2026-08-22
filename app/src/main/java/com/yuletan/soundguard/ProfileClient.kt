package com.yuletan.soundguard

import android.content.Context
import android.util.Base64
import android.util.Log
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
    val deactivatedAt: String? = null,
) {
    /** True when role/name/phone were cleared (deactivated or never set up). */
    val needsSetup: Boolean
        get() = deactivatedAt != null ||
            role.isBlank() ||
            fullName.isBlank() ||
            phone.isBlank()
}

data class BeneficiarySettings(
    val autoApproveCameraRequests: Boolean,
)

class ProfileClient(context: Context) {
    companion object {
        private const val TAG = "SoundGuardProfile"
    }

    private val authClient = AuthClient(context)

    suspend fun fetchMyProfile(): Result<UserProfile?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: return@runCatching null
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/profiles?id=eq.$userId&select=id,email,full_name,phone,role,setup_completed_at,deactivated_at"
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
                    fullName = cleanNull(obj, "full_name"),
                    phone = cleanNull(obj, "phone"),
                    role = obj.optString("role", "")
                        .takeUnless { it.equals("null", ignoreCase = true) }
                        .orEmpty(),
                    // optString() yields the literal "null" for SQL NULL columns
                    // (same gotcha cleanNull guards against) — treat as absent.
                    setupCompletedAt = obj.optString("setup_completed_at")
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                    deactivatedAt = obj.optString("deactivated_at")
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
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
            val token = authClient.getToken()
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
                // Completing setup re-activates the account.
                put("deactivated_at", JSONObject.NULL)
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
        autoApproveCameraRequests: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
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
                put("auto_approve_camera_requests", autoApproveCameraRequests)
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

    suspend fun fetchBeneficiarySettings(): Result<BeneficiarySettings> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/beneficiary_settings?user_id=eq.$userId&select=auto_approve_camera_requests"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Beneficiary settings fetch failed with HTTP $code: $response")
                val row = org.json.JSONArray(response).optJSONObject(0)
                BeneficiarySettings(row?.optBoolean("auto_approve_camera_requests", false) ?: false)
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun resetRole(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
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
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/reset_my_account_data"
            var rpcError: String? = null
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
                    rpcError = "HTTP $responseCode: ${response.take(300)}"
                }
            } finally {
                connection.disconnect()
            }

            if (rpcError != null) {
                Log.e(TAG, "reset RPC failed ($rpcError) jwtRole=${jwtRole(token)}")
                // Fallback: the RPC has historically 403'd/404'd when grants or the
                // schema cache are stale on the live DB. Deactivate via direct REST
                // calls so the account is still reset (keeps chat for the other side).
                val directResult = resetAllAccountDataDirect(token)
                directResult.fold(
                    onSuccess = {
                        Log.e(TAG, "direct fallback reset SUCCEEDED after RPC failure")
                        return@runCatching
                    },
                    onFailure = { directError ->
                        Log.e(TAG, "direct fallback reset FAILED: ${directError.message}")
                        error("Account reset failed — RPC ($rpcError); direct fallback (${directError.message})")
                    },
                )
            }
        }
    }

    /** Decodes the JWT payload's role claim for diagnostics. */
    private fun jwtRole(token: String): String = runCatching {
        val payload = token.split(".")[1]
        val json = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)))
        json.optString("role", "?")
    }.getOrDefault("?")

    /**
     * Best-effort deactivation via direct REST (used when the RPC is wedged).
     * Deletes the caller's device tokens + settings, then nulls the profile so
     * the other party sees a "Deactivated" account. Care connections + chat
     * history are intentionally kept (removing the connection purges them later).
     */
    private suspend fun resetAllAccountDataDirect(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            val base = BuildConfig.SUPABASE_URL.trimEnd('/')

            // Best-effort deletes — a failure here is non-critical.
            runCatching { executeDelete("$base/rest/v1/device_push_tokens?user_id=eq.$userId", token) }
            runCatching { executeDelete("$base/rest/v1/beneficiary_settings?user_id=eq.$userId", token) }
            runCatching { executeDelete("$base/rest/v1/caregiver_settings?user_id=eq.$userId", token) }
            runCatching { executeDelete("$base/rest/v1/care_connections?beneficiary_id=eq.$userId", token) }
            runCatching { executeDelete("$base/rest/v1/care_connections?caregiver_id=eq.$userId", token) }

            // The deactivation marker — this is the step that must succeed.
            val baseBody = JSONObject().apply {
                put("role", JSONObject.NULL)
                put("setup_completed_at", JSONObject.NULL)
                put("full_name", JSONObject.NULL)
                put("phone", JSONObject.NULL)
            }
            // First attempt includes the deactivation marker (migration 035+).
            val withMarker = JSONObject(baseBody.toString()).apply {
                put("deactivated_at", java.time.Instant.now().toString())
            }
            val marked = patchProfile(withMarker, userId)
            if (marked.isFailure) Log.e(TAG, "direct PATCH with deactivated_at failed: ${marked.exceptionOrNull()?.message}")
            marked.onSuccess { return@runCatching }
            // Retry without deactivated_at in case migration 035's column is missing.
            val plain = patchProfile(baseBody, userId)
            if (plain.isFailure) Log.e(TAG, "direct PATCH without deactivated_at failed: ${plain.exceptionOrNull()?.message}")
            plain.getOrThrow()
        }
    }

    private suspend fun patchProfile(body: org.json.JSONObject, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val token = runCatching { authClient.getToken() }.getOrNull().orEmpty()
        return@withContext runCatching {
            val connection = (URL("$base/rest/v1/profiles?id=eq.$userId").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
                doOutput = true
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("profile PATCH HTTP $code: ${response.take(300)}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Reads a profile field, treating JSON null / "null" / blank as empty. */
    private fun cleanNull(obj: org.json.JSONObject?, key: String): String {
        val v = obj?.optString(key) ?: return ""
        return if (v.isBlank() || v.equals("null", ignoreCase = true)) "" else v
    }

    private fun executeDelete(endpoint: String, token: String): Boolean {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }
}
