package com.yuletan.soundguard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CaregiverMember(
    val connectionId: String,
    val caregiverId: String,
    val name: String,
    val phone: String,
    val email: String,
    val isPrimary: Boolean,
    val escalationOrder: Int,
    val status: String,
)

data class MonitoredBeneficiary(
    val connectionId: String,
    val beneficiaryId: String,
    val name: String,
    val phone: String,
    val email: String,
    val isPrimary: Boolean,
    val status: String,
)

class CareClient(context: Context) {
    companion object {
        private const val TAG = "CareClient"
    }

    private val authClient = AuthClient(context)

    suspend fun createPairingCode(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Session expired. Please sign in again.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/create_pairing_code"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            try {
                connection.outputStream.use { it.write("{}".toByteArray()) }
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    error("Failed to generate code (HTTP $responseCode): $responseBody")
                }
                // Supabase RPC returns plain text or JSON string like "ABC123" or "\"ABC123\""
                responseBody.trim().trim('"')
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun acceptPairingCode(code: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Session expired. Please sign in again.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/accept_pairing_code"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val body = JSONObject().apply {
                put("p_code", code.trim().uppercase())
            }

            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    val message = runCatching { JSONObject(responseBody).optString("message") }.getOrNull()
                        ?: responseBody
                    error(message.ifBlank { "Invalid or expired pairing code" })
                }
                val obj = JSONObject(responseBody)
                obj.optString("beneficiary_name", "Beneficiary")
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun fetchCaregiversForBeneficiary(): Result<List<CaregiverMember>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Session expired. Please sign in again.")
            val userId = authClient.userId() ?: error("No user found.")

            // Fetch care connections
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?beneficiary_id=eq.$userId&select=id,caregiver_id,is_primary,escalation_order,status&order=escalation_order.asc"
            val rawConnections = executeGet(endpoint, token)
            val connArray = JSONArray(rawConnections)
            if (connArray.length() == 0) return@runCatching emptyList()

            // Fetch profile info for each caregiver
            val caregiverIds = mutableListOf<String>()
            for (i in 0 until connArray.length()) {
                caregiverIds.add(connArray.getJSONObject(i).getString("caregiver_id"))
            }

            val profilesMap = fetchProfilesByIds(caregiverIds, token)

            val result = mutableListOf<CaregiverMember>()
            for (i in 0 until connArray.length()) {
                val conn = connArray.getJSONObject(i)
                val cId = conn.getString("caregiver_id")
                val profile = profilesMap[cId]
                result.add(
                    CaregiverMember(
                        connectionId = conn.getString("id"),
                        caregiverId = cId,
                        name = profile?.optString("full_name")?.ifBlank { null }
                            ?: profile?.optString("email")
                            ?: "Caregiver",
                        phone = profile?.optString("phone").orEmpty(),
                        email = profile?.optString("email").orEmpty(),
                        isPrimary = conn.optBoolean("is_primary", false),
                        escalationOrder = conn.optInt("escalation_order", i + 1),
                        status = conn.optString("status", "active"),
                    )
                )
            }
            result
        }
    }

    suspend fun fetchBeneficiariesForCaregiver(): Result<List<MonitoredBeneficiary>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Session expired. Please sign in again.")
            val userId = authClient.userId() ?: error("No user found.")

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?caregiver_id=eq.$userId&status=eq.active&select=id,beneficiary_id,is_primary,status&order=created_at.desc"
            val rawConnections = executeGet(endpoint, token)
            val connArray = JSONArray(rawConnections)
            if (connArray.length() == 0) return@runCatching emptyList()

            val beneficiaryIds = mutableListOf<String>()
            for (i in 0 until connArray.length()) {
                beneficiaryIds.add(connArray.getJSONObject(i).getString("beneficiary_id"))
            }

            val profilesMap = fetchProfilesByIds(beneficiaryIds, token)

            val result = mutableListOf<MonitoredBeneficiary>()
            for (i in 0 until connArray.length()) {
                val conn = connArray.getJSONObject(i)
                val bId = conn.getString("beneficiary_id")
                val profile = profilesMap[bId]
                result.add(
                    MonitoredBeneficiary(
                        connectionId = conn.getString("id"),
                        beneficiaryId = bId,
                        name = profile?.optString("full_name")?.ifBlank { null }
                            ?: profile?.optString("email")
                            ?: "Beneficiary",
                        phone = profile?.optString("phone").orEmpty(),
                        email = profile?.optString("email").orEmpty(),
                        isPrimary = conn.optBoolean("is_primary", false),
                        status = conn.optString("status", "active"),
                    )
                )
            }
            result
        }
    }

    suspend fun removeCareConnection(connectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Session expired.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?id=eq.$connectionId"
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
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Remove failed (HTTP $code): $err")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun setPrimaryCaregiver(
        beneficiaryId: String,
        targetConnectionId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Session expired.")
            // Clear primary for all caregivers of this beneficiary
            val resetEndpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?beneficiary_id=eq.$beneficiaryId"
            executePatch(resetEndpoint, JSONObject().put("is_primary", false), token)

            // Set primary for target
            val setEndpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?id=eq.$targetConnectionId"
            executePatch(setEndpoint, JSONObject().put("is_primary", true).put("escalation_order", 1), token)
        }
    }

    suspend fun countActiveConnections(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.accessToken() ?: error("Session expired.")
            val userId = authClient.userId() ?: error("No user found.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?or=(beneficiary_id.eq.$userId,caregiver_id.eq.$userId)&status=eq.active&select=id"
            val raw = executeGet(endpoint, token)
            val array = JSONArray(raw)
            array.length()
        }
    }

    private fun fetchProfilesByIds(ids: List<String>, token: String): Map<String, JSONObject> {
        if (ids.isEmpty()) return emptyMap()
        val inFilter = "in.(" + ids.joinToString(",") + ")"
        val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
            "/rest/v1/profiles?id=$inFilter&select=id,full_name,email,phone"
        val response = executeGet(endpoint, token)
        val array = JSONArray(response)
        val map = mutableMapOf<String, JSONObject>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            map[obj.getString("id")] = obj
        }
        return map
    }

    private fun executeGet(endpoint: String, token: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) error("HTTP $responseCode: $body")
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun executePatch(endpoint: String, body: JSONObject, token: String) {
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
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("PATCH HTTP $responseCode: $err")
            }
        } finally {
            connection.disconnect()
        }
    }
}
