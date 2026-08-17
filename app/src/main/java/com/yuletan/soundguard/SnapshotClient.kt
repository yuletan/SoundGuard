package com.yuletan.soundguard

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class SnapshotRequest(
    val id: String,
    val storagePath: String,
    val expiresAt: String?,
)

class SnapshotClient(context: Context) {
    companion object {
        const val MAX_FILE_BYTES = 5L * 1024L * 1024L
        private const val BUCKET = "camera-snapshots"
    }

    private val authClient = AuthClient(context)

    suspend fun requestSnapshot(
        incidentId: String,
        beneficiaryId: String,
        cameraFacing: String = "rear",
    ): Result<SnapshotRequest> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val requesterId = authClient.userId() ?: error("No authenticated user was found.")
            require(cameraFacing == "front" || cameraFacing == "rear") { "Unsupported camera direction." }
            val path = "$beneficiaryId/$incidentId/${UUID.randomUUID()}.jpg"
            val body = JSONObject().apply {
                put("incident_id", incidentId)
                put("beneficiary_id", beneficiaryId)
                put("requested_by", requesterId)
                put("camera_facing", cameraFacing)
                put("storage_path", path)
                put("status", "requested")
            }
            val response = requestJson(
                method = "POST",
                endpoint = restUrl("camera_snapshots"),
                token = token,
                body = body,
                prefer = "return=representation",
            )
            val row = org.json.JSONArray(response).optJSONObject(0)
                ?: error("Snapshot request returned no row.")
            SnapshotRequest(
                id = row.getString("id"),
                storagePath = row.getString("storage_path"),
                expiresAt = row.optString("expires_at").takeIf { it.isNotBlank() },
            )
        }
    }

    suspend fun uploadSnapshot(request: SnapshotRequest, file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(file.isFile) { "Snapshot file does not exist." }
            require(file.length() in 1..MAX_FILE_BYTES) { "Snapshot must be between 1 byte and 5 MB." }
            val token = requireToken()
            val upload = (URL(storageUrl(request.storagePath)).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "image/jpeg")
                setRequestProperty("x-upsert", "false")
                doOutput = true
            }
            try {
                file.inputStream().use { input -> upload.outputStream.use { output -> input.copyTo(output) } }
                if (upload.responseCode !in 200..299) {
                    error("Snapshot upload failed with HTTP ${upload.responseCode}.")
                }
            } finally {
                upload.disconnect()
            }
            requestJson(
                method = "PATCH",
                endpoint = "${restUrl("camera_snapshots")}?id=eq.${encode(request.id)}",
                token = token,
                body = JSONObject().apply { put("status", "uploaded") },
                prefer = "return=minimal",
            )
            Unit
        }
    }

    suspend fun createSignedUrl(request: SnapshotRequest, expiresInSeconds: Int = 60): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val response = requestJson(
                method = "POST",
                endpoint = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/sign/$BUCKET/${encodePath(request.storagePath)}",
                token = token,
                body = JSONObject().apply { put("expiresIn", expiresInSeconds.coerceIn(1, 600)) },
                prefer = null,
            )
            val signedPath = JSONObject(response).getString("signedURL")
            if (signedPath.startsWith("http")) signedPath else BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1" + signedPath
        }
    }

    private fun requireToken(): String = authClient.accessToken() ?: error("Your session has expired.")

    private fun restUrl(table: String): String = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/$table"

    private fun storageUrl(path: String): String = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/$BUCKET/${encodePath(path)}"

    private fun encodePath(path: String): String = path.split('/').joinToString("/") { encode(it) }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun requestJson(
        method: String,
        endpoint: String,
        token: String,
        body: JSONObject,
        prefer: String?,
    ): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
            doOutput = true
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val responseCode = connection.responseCode
            val response = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) error("Supabase request failed with HTTP $responseCode: $response")
            response
        } finally {
            connection.disconnect()
        }
    }
}
