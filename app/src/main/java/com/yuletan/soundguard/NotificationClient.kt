package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CaregiverNotification(
    val id: String,
    val incidentId: String,
    val beneficiaryId: String,
    val soundLabel: String,
    val severity: String,
    val confidence: Float,
    val status: String,
    val createdAt: String,
)

class NotificationClient(context: Context) {
    private val authClient = AuthClient(context)

    suspend fun fetchMine(): Result<List<CaregiverNotification>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: return@runCatching emptyList()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/notifications?select=id,incident_id,status,created_at,incidents(beneficiary_id,sound_label,severity,confidence)&order=created_at.desc&limit=50"
            val connection = open(endpoint, token, "GET")
            try {
                val response = readResponse(connection)
                JSONArray(response).let { array ->
                    (0 until array.length()).map { index ->
                        val row = array.getJSONObject(index)
                        val incident = row.optJSONObject("incidents")
                        CaregiverNotification(
                            id = row.getString("id"),
                            incidentId = row.getString("incident_id"),
                            beneficiaryId = incident?.optString("beneficiary_id").orEmpty(),
                            soundLabel = incident?.optString("sound_label").orEmpty(),
                            severity = incident?.optString("severity").orEmpty(),
                            confidence = incident?.optDouble("confidence", 0.0)?.toFloat() ?: 0f,
                            status = row.optString("status", "queued"),
                            createdAt = row.optString("created_at"),
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun acknowledge(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Your session has expired.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/notifications?id=eq.$id"
            val connection = open(endpoint, token, "PATCH")
            connection.setRequestProperty("Prefer", "return=minimal")
            connection.doOutput = true
            try {
                connection.outputStream.use {
                    it.write(JSONObject().apply {
                        put("status", "acknowledged")
                        put("acknowledged_at", java.time.Instant.now().toString())
                    }.toString().toByteArray())
                }
                readResponse(connection)
                Unit
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun open(endpoint: String, token: String, method: String): HttpURLConnection =
        (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }

    private fun readResponse(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Notification request failed with HTTP $code: $response")
        return response
    }
}
