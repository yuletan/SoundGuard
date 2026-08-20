package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class IncidentClient(context: Context) {
    private val authClient = AuthClient(context)

    companion object {
        private const val MIN_DISPLAY_CONFIDENCE = 0.20
        private val HIDDEN_SEVERITIES = setOf("low")
        private val CAREGIVER_VISIBLE_SEVERITIES = setOf("medium", "high")
    }

    private fun severityForWrite(event: AlertEvent): String = when (event.severity) {
        SoundSeverity.High -> "high"
        SoundSeverity.Medium -> "medium"
        SoundSeverity.Low -> "low"
        SoundSeverity.None -> "low"
    }

    private fun isHiddenSeverity(severity: String) = severity.trim().lowercase() in HIDDEN_SEVERITIES
    private fun isCaregiverVisible(severity: String) = severity.trim().lowercase() in CAREGIVER_VISIBLE_SEVERITIES

    suspend fun fetchOwnIncidents(): Result<List<SharedIncident>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: return@runCatching emptyList()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/incidents?beneficiary_id=eq.$userId&confidence=gte.$MIN_DISPLAY_CONFIDENCE&select=id,sound_label,severity,confidence,status,started_at&order=started_at.desc&limit=50"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Incident history failed with HTTP $code: $response")
                val array = org.json.JSONArray(response)
                (0 until array.length()).mapNotNull { index ->
                    val row = array.getJSONObject(index)
                    if (row.optDouble("confidence", 0.0) < MIN_DISPLAY_CONFIDENCE) return@mapNotNull null
                    if (isHiddenSeverity(row.optString("severity", "low"))) return@mapNotNull null
                    SharedIncident(
                        id = row.getString("id"),
                        label = row.optString("sound_label", "Alert"),
                        severity = row.optString("severity", "low"),
                        confidence = row.optDouble("confidence", 0.0).toFloat(),
                        status = row.optString("status", "detected"),
                        startedAt = row.optString("started_at"),
                    )
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun createIncident(event: AlertEvent): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            if (event.confidence < MIN_DISPLAY_CONFIDENCE) return@runCatching null
            if (event.severity == SoundSeverity.Low || event.severity == SoundSeverity.None) return@runCatching null
            val token = authClient.getToken()
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
                put("severity", severityForWrite(event))
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

    suspend fun fetchIncidentsForBeneficiary(beneficiaryId: String): Result<List<SharedIncident>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/incidents?beneficiary_id=eq.$beneficiaryId&confidence=gte.$MIN_DISPLAY_CONFIDENCE" +
                "&select=id,sound_label,severity,confidence,status,started_at&order=started_at.asc&limit=200"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Incident fetch failed with HTTP $code: $response")
                val array = org.json.JSONArray(response)
                (0 until array.length()).mapNotNull { index ->
                    val row = array.getJSONObject(index)
                    if (row.optDouble("confidence", 0.0) < MIN_DISPLAY_CONFIDENCE) return@mapNotNull null
                    if (isHiddenSeverity(row.optString("severity", "low"))) return@mapNotNull null
                    SharedIncident(
                        id = row.getString("id"),
                        label = row.optString("sound_label", "Alert"),
                        severity = row.optString("severity", "low"),
                        confidence = row.optDouble("confidence", 0.0).toFloat(),
                        status = row.optString("status", "detected"),
                        startedAt = row.optString("started_at"),
                    )
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /** One request covering all monitored beneficiaries — used for risk-tier summaries. */
    suspend fun fetchRecentIncidentsForBeneficiaries(beneficiaryIds: List<String>): Result<Map<String, List<SharedIncident>>> = withContext(Dispatchers.IO) {
        runCatching {
            if (beneficiaryIds.isEmpty()) return@runCatching emptyMap()
            val token = authClient.getToken()
            val inFilter = "in.(" + beneficiaryIds.joinToString(",") + ")"
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/incidents?beneficiary_id=$inFilter&confidence=gte.$MIN_DISPLAY_CONFIDENCE" +
                "&select=id,sound_label,severity,confidence,status,started_at,beneficiary_id&order=started_at.desc&limit=300"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Incident fetch failed with HTTP $code: $response")
                val array = org.json.JSONArray(response)
                val grouped = mutableMapOf<String, MutableList<SharedIncident>>()
                for (index in 0 until array.length()) {
                    val row = array.getJSONObject(index)
                    if (row.optDouble("confidence", 0.0) < MIN_DISPLAY_CONFIDENCE) continue
                    if (isHiddenSeverity(row.optString("severity", "low"))) continue
                    val incident = SharedIncident(
                        id = row.getString("id"),
                        label = row.optString("sound_label", "Alert"),
                        severity = row.optString("severity", "low"),
                        confidence = row.optDouble("confidence", 0.0).toFloat(),
                        status = row.optString("status", "detected"),
                        startedAt = row.optString("started_at"),
                        beneficiaryId = row.optString("beneficiary_id").takeIf { it.isNotBlank() },
                    )
                    val bId = incident.beneficiaryId ?: continue
                    grouped.getOrPut(bId) { mutableListOf() }.add(incident)
                }
                grouped
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun clearOwnIncidents(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/incidents?beneficiary_id=eq.$userId"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Could not clear notifications. HTTP $code: $body")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun clearIncidentsForPartnerAsCaregiver(beneficiaryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/caregiver_clear_incidents_for_beneficiary"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            val body = JSONObject().apply { put("p_beneficiary_id", beneficiaryId) }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Clear chat failed (HTTP $code): $err")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}

data class SharedIncident(
    val id: String,
    val label: String,
    val severity: String,
    val confidence: Float,
    val status: String,
    val startedAt: String,
    val beneficiaryId: String? = null,
)
