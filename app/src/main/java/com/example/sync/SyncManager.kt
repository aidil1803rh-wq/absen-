package com.example.sync

import android.content.Context
import android.net.wifi.WifiManager
import com.example.data.AttendanceRepository
import com.example.data.model.AttendanceRecord
import com.example.util.QrCodeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

enum class SyncRole {
    STANDALONE, HOST, CLIENT
}

data class SyncUiState(
    val role: SyncRole = SyncRole.STANDALONE,
    val isHostRunning: Boolean = false,
    val hostIp: String = "",
    val hostPort: Int = 8888,
    val clientTargetHost: String = "",
    val isConnectedToHost: Boolean = false,
    val lastSyncMessage: String = "Belum ada sinkronisasi",
    val lastSyncTimestamp: Long = 0,
    val connectedClientsCount: Int = 0,
    val isSyncing: Boolean = false,
    val githubGistId: String = "",
    val githubToken: String = "",
    val githubRawUrl: String = "",
    val isGithubSyncing: Boolean = false,
    val lastGithubMessage: String = "Belum ada sinkronisasi GitHub"
)

class SyncManager(
    private val context: Context,
    private val repository: AttendanceRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        loadGithubConfig()
    }

    fun getDeviceIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress ?: ""
                        if (!hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun startHost(port: Int = 8888) {
        if (_uiState.value.isHostRunning) return

        val localIp = getDeviceIpAddress()
        serverJob = scope.launch {
            try {
                val server = ServerSocket(port)
                serverSocket = server
                _uiState.value = _uiState.value.copy(
                    role = SyncRole.HOST,
                    isHostRunning = true,
                    hostIp = localIp,
                    hostPort = port,
                    lastSyncMessage = "Host Server aktif di $localIp:$port"
                )

                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        scope.launch {
                            handleClientConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isHostRunning = false,
                    lastSyncMessage = "Gagal memulai server: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stopHost() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverJob?.cancel()
        serverSocket = null
        _uiState.value = _uiState.value.copy(
            isHostRunning = false,
            role = SyncRole.STANDALONE,
            lastSyncMessage = "Server Host dihentikan"
        )
    }

    private suspend fun handleClientConnection(socket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val requestLine = reader.readLine() ?: return@withContext
                // Sample requestLine: "GET /api/sync HTTP/1.1" or "POST /api/record HTTP/1.1"
                var contentLength = 0
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.lowercase().startsWith("content-length:")) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                val body = if (contentLength > 0) {
                    val charArray = CharArray(contentLength)
                    reader.read(charArray, 0, contentLength)
                    String(charArray)
                } else ""

                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: "GET"
                val path = parts.getOrNull(1) ?: "/"

                when {
                    path == "/api/ping" -> {
                        val responseJson = JSONObject().apply {
                            put("status", "ok")
                            put("app", "pks_magang")
                            put("serverTime", System.currentTimeMillis())
                        }.toString()
                        sendHttpResponse(writer, 200, "application/json", responseJson)
                    }

                    path == "/api/sync" && method == "GET" -> {
                        val fullData = repository.exportFullJson()
                        sendHttpResponse(writer, 200, "application/json", fullData)
                        _uiState.value = _uiState.value.copy(
                            connectedClientsCount = _uiState.value.connectedClientsCount + 1,
                            lastSyncMessage = "Klien terhubung & data disinkronkan (${QrCodeUtil.getCurrentTimeString()})"
                        )
                    }

                    path == "/api/record" && method == "POST" -> {
                        val json = JSONObject(body)
                        val participantId = json.getString("participantId")
                        val status = json.getString("status")
                        val date = json.optString("date", QrCodeUtil.getTodayDateString())
                        val time = json.optString("time", QrCodeUtil.getCurrentTimeString())
                        val timeOut = if (json.has("timeOut")) json.getString("timeOut") else null
                        val isQuranRead = if (json.has("isQuranRead")) json.getBoolean("isQuranRead") else null
                        val quranSurahJuz = if (json.has("quranSurahJuz")) json.getString("quranSurahJuz") else null
                        val dailyActivities = if (json.has("dailyActivities")) {
                            json.getString("dailyActivities").split(";").filter { it.isNotBlank() }
                        } else null
                        val activityNotes = if (json.has("activityNotes")) json.getString("activityNotes") else null

                        val result = repository.recordAttendance(
                            participantId = participantId,
                            status = status,
                            dateStr = date,
                            timeStr = time,
                            timeOutStr = timeOut,
                            isQuranRead = isQuranRead,
                            quranSurahJuz = quranSurahJuz,
                            dailyActivities = dailyActivities,
                            activityNotes = activityNotes
                        )
                        val resp = JSONObject().apply {
                            put("success", result.isSuccess)
                            put("message", if (result.isSuccess) "Absensi tercatat" else "Gagal")
                        }.toString()
                        sendHttpResponse(writer, 200, "application/json", resp)
                        _uiState.value = _uiState.value.copy(
                            lastSyncMessage = "Menerima absensi $participantId ($status) dari Klien (${QrCodeUtil.getCurrentTimeString()})"
                        )
                    }

                    path == "/api/participant" && method == "POST" -> {
                        val json = JSONObject(body)
                        val name = json.getString("name")
                        val div = json.getString("division")
                        val uni = json.getString("university")
                        val created = repository.addParticipant(name, div, uni)
                        val resp = JSONObject().apply {
                            put("success", true)
                            put("id", created.id)
                        }.toString()
                        sendHttpResponse(writer, 200, "application/json", resp)
                    }

                    else -> {
                        sendHttpResponse(writer, 404, "text/plain", "Not Found")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun sendHttpResponse(writer: PrintWriter, code: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $code OK\r\n")
        writer.print("Content-Type: $contentType; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.print(body)
        writer.flush()
    }

    // Client functions
    fun setClientTarget(host: String) {
        var cleanHost = host.trim()
        if (cleanHost.startsWith("PKS-SYNC:")) {
            cleanHost = cleanHost.removePrefix("PKS-SYNC:").trim()
        }
        if (cleanHost.startsWith("http://")) {
            cleanHost = cleanHost.removePrefix("http://")
        }
        if (cleanHost.contains("/")) {
            cleanHost = cleanHost.substringBefore("/")
        }
        _uiState.value = _uiState.value.copy(
            clientTargetHost = cleanHost,
            role = SyncRole.CLIENT
        )
    }

    fun syncWithHost(customHost: String? = null, onComplete: ((Boolean, String) -> Unit)? = null) {
        val target = customHost ?: _uiState.value.clientTargetHost
        if (target.isBlank()) {
            onComplete?.invoke(false, "Alamat Host kosong.")
            return
        }

        _uiState.value = _uiState.value.copy(isSyncing = true)
        scope.launch {
            try {
                val hostUrl = if (target.startsWith("http://") || target.startsWith("https://")) target else "http://$target"
                val request = Request.Builder()
                    .url("$hostUrl/api/sync")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val success = repository.importFullJson(bodyString)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isConnectedToHost = true,
                            role = SyncRole.CLIENT,
                            clientTargetHost = target,
                            lastSyncMessage = "Berhasil sinkron dengan Host $target",
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        onComplete?.invoke(true, "Sinkronisasi berhasil! Data terupdate.")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isConnectedToHost = false,
                            lastSyncMessage = "Gagal: Server merespons ${response.code}"
                        )
                        onComplete?.invoke(false, "Gagal terhubung ke host (${response.code})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        isConnectedToHost = false,
                        lastSyncMessage = "Error: ${e.localizedMessage}"
                    )
                    onComplete?.invoke(false, "Tidak dapat menghubungi $target. Pastikan satu Wi-Fi/Hotspot.")
                }
            }
        }
    }

    fun pushAttendanceToHost(record: AttendanceRecord) {
        val target = _uiState.value.clientTargetHost
        if (target.isBlank() || _uiState.value.role != SyncRole.CLIENT) return

        scope.launch {
            try {
                val hostUrl = if (target.startsWith("http://") || target.startsWith("https://")) target else "http://$target"
                val json = JSONObject().apply {
                    put("participantId", record.participantId)
                    put("status", record.status)
                    put("date", record.date)
                    put("time", record.time)
                    put("timeOut", record.timeOut)
                    put("isQuranRead", record.isQuranRead)
                    put("quranSurahJuz", record.quranSurahJuz)
                    put("dailyActivities", record.dailyActivities)
                    put("activityNotes", record.activityNotes)
                }

                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("$hostUrl/api/record")
                    .post(body)
                    .build()

                okHttpClient.newCall(request).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSyncQrCodeString(): String {
        val ip = _uiState.value.hostIp.ifBlank { getDeviceIpAddress() }
        val port = _uiState.value.hostPort
        return "PKS-SYNC:http://$ip:$port"
    }

    companion object {
        private const val PREFS_NAME = "pks_github_sync_prefs"
        private const val KEY_GIST_ID = "github_gist_id"
        private const val KEY_TOKEN = "github_token"
        private const val KEY_RAW_URL = "github_raw_url"
    }

    private fun loadGithubConfig() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _uiState.value = _uiState.value.copy(
            githubGistId = prefs.getString(KEY_GIST_ID, "") ?: "",
            githubToken = prefs.getString(KEY_TOKEN, "") ?: "",
            githubRawUrl = prefs.getString(KEY_RAW_URL, "") ?: ""
        )
    }

    fun saveGithubConfig(gistId: String, token: String, rawUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_GIST_ID, gistId.trim())
            .putString(KEY_TOKEN, token.trim())
            .putString(KEY_RAW_URL, rawUrl.trim())
            .apply()
        _uiState.value = _uiState.value.copy(
            githubGistId = gistId.trim(),
            githubToken = token.trim(),
            githubRawUrl = rawUrl.trim()
        )
    }

    fun pullFromGitHub(customUrlOrGistId: String? = null, onResult: ((Boolean, String) -> Unit)? = null) {
        val target = customUrlOrGistId?.trim()?.ifBlank { null }
            ?: _uiState.value.githubRawUrl.ifBlank { null }
            ?: _uiState.value.githubGistId.ifBlank { null }

        if (target.isNullOrBlank()) {
            onResult?.invoke(false, "Masukkan Link GitHub Raw atau Gist ID terlebih dahulu.")
            return
        }

        _uiState.value = _uiState.value.copy(isGithubSyncing = true, lastGithubMessage = "Menghubungi GitHub...")

        scope.launch {
            try {
                val isDirectUrl = target.startsWith("http://") || target.startsWith("https://")
                val requestUrl = if (isDirectUrl) {
                    target
                } else {
                    "https://api.github.com/gists/$target"
                }

                val reqBuilder = Request.Builder()
                    .url(requestUrl)
                    .header("Accept", "application/vnd.github.v3+json")

                val token = _uiState.value.githubToken.trim()
                if (token.isNotBlank()) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }

                val response = okHttpClient.newCall(reqBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val msg = "Gagal mengambil data dari GitHub (HTTP ${response.code})"
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isGithubSyncing = false, lastGithubMessage = msg)
                        onResult?.invoke(false, msg)
                    }
                    return@launch
                }

                val jsonToImport: String = if (responseBody.trim().startsWith("{") && responseBody.contains("\"files\"")) {
                    val gistObj = JSONObject(responseBody)
                    val filesObj = gistObj.getJSONObject("files")
                    val keys = filesObj.keys()
                    var contentFound = ""
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val fileObj = filesObj.getJSONObject(key)
                        contentFound = fileObj.getString("content")
                        break
                    }
                    contentFound
                } else {
                    responseBody
                }

                val success = repository.importFullJson(jsonToImport)
                val statusMsg = if (success) {
                    "Sinkronisasi GitHub Berhasil! Seluruh peserta & presensi diperbarui."
                } else {
                    "Format file JSON dari GitHub tidak sesuai."
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isGithubSyncing = false,
                        lastGithubMessage = statusMsg
                    )
                    onResult?.invoke(success, statusMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val err = "Gagal terhubung ke GitHub: ${e.localizedMessage}"
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isGithubSyncing = false, lastGithubMessage = err)
                    onResult?.invoke(false, err)
                }
            }
        }
    }

    fun pushToGitHub(
        customGistId: String? = null,
        customToken: String? = null,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val token = customToken?.trim()?.ifBlank { null }
            ?: _uiState.value.githubToken.trim().ifBlank { null }

        if (token.isNullOrBlank()) {
            onResult?.invoke(false, "Memerlukan GitHub Personal Access Token (PAT) untuk mengunggah ke GitHub.")
            return
        }

        val gistId = customGistId?.trim()?.ifBlank { null }
            ?: _uiState.value.githubGistId.trim().ifBlank { null }

        _uiState.value = _uiState.value.copy(isGithubSyncing = true, lastGithubMessage = "Mengunggah data ke GitHub...")

        scope.launch {
            try {
                val fullJson = repository.exportFullJson()
                val isUpdate = !gistId.isNullOrBlank()

                val requestUrl = if (isUpdate) {
                    "https://api.github.com/gists/$gistId"
                } else {
                    "https://api.github.com/gists"
                }

                val fileObj = JSONObject().apply {
                    put("content", fullJson)
                }
                val filesObj = JSONObject().apply {
                    put("pks_magang_absensi.json", fileObj)
                }
                val payloadObj = JSONObject().apply {
                    put("description", "PKS Magang Absensi Database Backup")
                    put("public", false)
                    put("files", filesObj)
                }

                val reqBody = payloadObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val reqBuilder = Request.Builder()
                    .url(requestUrl)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github.v3+json")

                if (isUpdate) {
                    reqBuilder.patch(reqBody)
                } else {
                    reqBuilder.post(reqBody)
                }

                val response = okHttpClient.newCall(reqBuilder.build()).execute()
                val responseStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val msg = "Gagal upload ke GitHub (HTTP ${response.code})"
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isGithubSyncing = false, lastGithubMessage = msg)
                        onResult?.invoke(false, msg)
                    }
                    return@launch
                }

                val respObj = JSONObject(responseStr)
                val newGistId = respObj.optString("id", gistId ?: "")
                val rawUrl = respObj.optJSONObject("files")?.optJSONObject("pks_magang_absensi.json")?.optString("raw_url", "") ?: ""

                saveGithubConfig(newGistId, token, rawUrl)

                val msg = "Berhasil mengunggah ke GitHub Gist! ID: $newGistId. HP lain kini dapat sinkronisasi."
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isGithubSyncing = false,
                        githubGistId = newGistId,
                        githubRawUrl = rawUrl,
                        lastGithubMessage = msg
                    )
                    onResult?.invoke(true, msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val err = "Gagal upload: ${e.localizedMessage}"
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isGithubSyncing = false, lastGithubMessage = err)
                    onResult?.invoke(false, err)
                }
            }
        }
    }
}
