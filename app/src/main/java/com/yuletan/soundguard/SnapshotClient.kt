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
    val approvalStatus: String,
)

data class PendingSnapshotRequest(
    val id: String,
    val incidentId: String,
    val requestedBy: String,
    val requestedAt: String,
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

            // Preferred path: SECURITY DEFINER RPC — bypasses table RLS entirely,
            // so "new row violates RLS for table camera_snapshots" can never happen.
            // Falls back to a direct table insert for databases without migration 032.
            val row = runCatching {
                requestRpcSnapshot(incidentId, beneficiaryId, cameraFacing, path, token)
            }.recoverCatching {
                requestTableSnapshot(incidentId, beneficiaryId, cameraFacing, path, requesterId, token)
            }.getOrThrow()

            SnapshotRequest(
                id = row.getString("id"),
                storagePath = row.getString("storage_path"),
                expiresAt = row.optString("expires_at").takeIf { it.isNotBlank() },
                approvalStatus = row.optString("approval_status", "pending"),
            )
        }
    }

    private fun requestRpcSnapshot(
        incidentId: String,
        beneficiaryId: String,
        cameraFacing: String,
        path: String,
        token: String,
    ): org.json.JSONObject {
        val body = JSONObject().apply {
            put("p_incident_id", incidentId)
            put("p_beneficiary_id", beneficiaryId)
            put("p_camera_facing", cameraFacing)
            put("p_storage_path", path)
        }
        val response = requestJson(
            method = "POST",
            endpoint = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/request_camera_snapshot",
            token = token,
            body = body,
            prefer = null,
        )
        return JSONObject(response)
    }

    private fun requestTableSnapshot(
        incidentId: String,
        beneficiaryId: String,
        cameraFacing: String,
        path: String,
        requesterId: String,
        token: String,
    ): org.json.JSONObject {
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
        return org.json.JSONArray(response).optJSONObject(0)
            ?: error("Snapshot request returned no row.")
    }

    suspend fun uploadSnapshot(request: SnapshotRequest, file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(file.isFile) { "Snapshot file does not exist." }
            require(file.length() in 1..MAX_FILE_BYTES) { "Snapshot must be between 1 byte and 5 MB." }
            val token = requireToken()
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase is not configured (check local.properties).")
            }
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
                    val body = upload.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val hint = when {
                        upload.responseCode == 401 || upload.responseCode == 403 -> " Check RLS: your profile may not have an active care connection, or the snapshot approval is still pending."
                        upload.responseCode == 413 -> " Image too large."
                        body.contains("bucket", ignoreCase = true) -> " Storage bucket \"camera-snapshots\" does not exist or is not configured."
                        body.contains("timeout", ignoreCase = true) || body.contains("connect", ignoreCase = true) -> " Network timeout — the device may be offline."
                        else -> ""
                    }
                    error("Snapshot upload failed with HTTP ${upload.responseCode}: $body$hint")
                }
            } finally {
                upload.disconnect()
            }
            try {
                requestJson(
                    method = "PATCH",
                    endpoint = "${restUrl("camera_snapshots")}?id=eq.${encode(request.id)}",
                    token = token,
                    body = JSONObject().apply { put("status", "uploaded") },
                    prefer = "return=minimal",
                )
            } catch (e: Exception) {
                error("Upload succeeded but marking snapshot as 'uploaded' failed: ${e.message}. The photo will not appear in chat until this succeeds.")
            }
            Unit
        }
    }

    suspend fun fetchApprovedSnapshotForBeneficiary(): Result<PendingSnapshotRequest?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val beneficiaryId = authClient.userId() ?: error("No authenticated user was found.")
            // Ignore stale requests: only auto-open the camera for requests made in the last 24h.
            val cutoff = java.time.Instant.now().minus(java.time.Duration.ofHours(24)).toString()
            val endpoint = "${restUrl("camera_snapshots")}?beneficiary_id=eq.$beneficiaryId&approval_status=eq.approved&status=eq.requested&requested_at=gte.$cutoff&select=id,incident_id,requested_by,requested_at&order=requested_at.desc&limit=1"
            val response = requestJson("GET", endpoint, token, JSONObject(), null)
            val row = org.json.JSONArray(response).optJSONObject(0) ?: return@runCatching null
            PendingSnapshotRequest(
                id = row.getString("id"),
                incidentId = row.getString("incident_id"),
                requestedBy = row.getString("requested_by"),
                requestedAt = row.optString("requested_at"),
            )
        }
    }

    suspend fun fetchPendingForBeneficiary(): Result<PendingSnapshotRequest?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val beneficiaryId = authClient.userId() ?: error("No authenticated user was found.")
            // Approval popup expires after 1 hour — older pending requests are ignored.
            val cutoff = java.time.Instant.now().minus(java.time.Duration.ofHours(1)).toString()
            val endpoint = "${restUrl("camera_snapshots")}?beneficiary_id=eq.$beneficiaryId&approval_status=eq.pending&status=eq.requested&requested_at=gte.$cutoff&select=id,incident_id,requested_by,requested_at&order=requested_at.desc&limit=1"
            val response = requestJson("GET", endpoint, token, JSONObject(), null)
            val row = org.json.JSONArray(response).optJSONObject(0) ?: return@runCatching null
            PendingSnapshotRequest(
                id = row.getString("id"),
                incidentId = row.getString("incident_id"),
                requestedBy = row.getString("requested_by"),
                requestedAt = row.optString("requested_at"),
            )
        }
    }

    suspend fun decidePendingRequest(id: String, approved: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            requestJson(
                "PATCH",
                "${restUrl("camera_snapshots")}?id=eq.${encode(id)}",
                token,
                JSONObject().put("approval_status", if (approved) "approved" else "declined"),
                "return=minimal",
            )
            Unit
        }
    }

    suspend fun fetchApprovalStatus(id: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val response = requestJson(
                "GET",
                "${restUrl("camera_snapshots")}?id=eq.${encode(id)}&select=approval_status&limit=1",
                token,
                JSONObject(),
                null,
            )
            org.json.JSONArray(response).optJSONObject(0)?.optString("approval_status")
        }
    }

    data class SnapshotStatus(
        val approvalStatus: String?,
        val uploadStatus: String?,
        val storagePath: String?,
    )

    suspend fun fetchSnapshotStatus(id: String): Result<SnapshotStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val response = requestJson(
                "GET",
                "${restUrl("camera_snapshots")}?id=eq.${encode(id)}&select=approval_status,status,storage_path&limit=1",
                token,
                JSONObject(),
                null,
            )
            val row = org.json.JSONArray(response).optJSONObject(0)
            SnapshotStatus(
                approvalStatus = row?.optString("approval_status"),
                uploadStatus = row?.optString("status"),
                storagePath = row?.optString("storage_path"),
            )
        }
    }

    data class SnapshotWithUrl(
        val id: String,
        val approvalStatus: String?,
        val uploadStatus: String?,
        val storagePath: String?,
        val signedUrl: String?,
        val requestedAt: String?,
        val expiresAt: String?,
    )

    suspend fun fetchSnapshotsForIncident(incidentId: String): Result<List<SnapshotWithUrl>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val response = requestJson(
                "GET",
                "${restUrl("camera_snapshots")}?incident_id=eq.${encode(incidentId)}&select=id,approval_status,status,storage_path,requested_at,expires_at&order=created_at.asc",
                token,
                JSONObject(),
                null,
            )
            val array = org.json.JSONArray(response)
            val results = mutableListOf<SnapshotWithUrl>()
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                val storagePath = row.optString("storage_path").takeIf { it.isNotBlank() }
                val uploadStatus = row.optString("status")
                val requestedAt = row.optString("requested_at").takeIf { it.isNotBlank() }
                val expiresAt = row.optString("expires_at").takeIf { it.isNotBlank() }
                val expiresAtMillis = expiresAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
                val isAvailable = expiresAtMillis == null || expiresAtMillis > System.currentTimeMillis()
                val signedUrl = if (uploadStatus == "uploaded" && storagePath != null && isAvailable) {
                    try {
                        val signResponse = requestJson(
                            "POST",
                            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/sign/$BUCKET/${encodePath(storagePath)}",
                            token,
                            JSONObject().apply { put("expiresIn", 600) },
                            null,
                        )
                        val signedPath = JSONObject(signResponse).getString("signedURL")
                        if (signedPath.startsWith("http")) signedPath else BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1" + signedPath
                    } catch (_: Exception) { null }
                } else null
                results.add(
                    SnapshotWithUrl(
                        id = row.getString("id"),
                        approvalStatus = row.optString("approval_status").takeIf { it.isNotBlank() },
                        uploadStatus = uploadStatus.takeIf { it.isNotBlank() },
                        storagePath = storagePath,
                        signedUrl = signedUrl,
                        requestedAt = requestedAt,
                        expiresAt = expiresAt,
                    )
                )
            }
            results
        }
    }

    suspend fun fetchRequest(id: String): Result<SnapshotRequest> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            val response = requestJson(
                "GET",
                "${restUrl("camera_snapshots")}?id=eq.${encode(id)}&select=id,storage_path,expires_at,approval_status&limit=1",
                token,
                JSONObject(),
                null,
            )
            val row = org.json.JSONArray(response).optJSONObject(0)
                ?: error("Snapshot request not found.")
            SnapshotRequest(
                id = row.getString("id"),
                storagePath = row.getString("storage_path"),
                expiresAt = row.optString("expires_at").takeIf { it.isNotBlank() },
                approvalStatus = row.optString("approval_status", "pending"),
            )
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

    private suspend fun requireToken(): String = authClient.getToken()

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
            doOutput = method != "GET"
        }
        return try {
            if (method != "GET") {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
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
