package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class IncidentClient(context: Context) {
    private val authClient = AuthClient(context)

    suspend fun createIncident(event: AlertEvent): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: return@runCatching null
            val beneficiaryId = authClient.userId() ?: return@runCatching null
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/incidents"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
            }
            val body = JSONObject().apply {
                put("beneficiary_id", beneficiaryId)
                put("sound_label", event.label)
                put("severity", if (event.severity == SoundSeverity.High) "high" else "low")
                put("confidence", event.confidence)
                put("status", if (event.severity == SoundSeverity.High) "waiting_user" else "detected")
                put("started_at", java.time.Instant.ofEpochMilli(event.timestamp).toString())
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                val response = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    error("Incident save failed with HTTP $responseCode: $response")
                }
                org.json.JSONArray(response).optJSONObject(0)?.optString("id")
            } finally {
                connection.disconnect()
            }
        }
    }
}
