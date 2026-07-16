package com.omv.client.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.omv.client.data.api.OmvApi
import com.omv.client.data.model.*
import com.omv.client.data.security.SecurePrefs
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class OmvRepository @Inject constructor(private val securePrefs: SecurePrefs) {

    private var api: OmvApi? = null
    private val gson = Gson()

    private fun getApi(): OmvApi {
        if (api == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            val cookieJar = object : CookieJar {
                private val cookieStore = HashMap<String, MutableList<Cookie>>()

                init {
                    // Restore saved session cookie
                    val savedCookie = securePrefs.sessionCookie
                    val savedExpiry = securePrefs.cookieExpiry
                    if (savedCookie.isNotEmpty() && savedExpiry > System.currentTimeMillis()) {
                        val host = securePrefs.hostPort.substringBefore(":")
                        val proto = if (securePrefs.useHttps) "https" else "http"
                        try {
                            val url = "$proto://$host/".toHttpUrl()
                            val cookie = Cookie.Builder()
                                .domain(host)
                                .path("/")
                                .name("X-OPENMEDIAVAULT-SESSIONID")
                                .value(savedCookie)
                                .expiresAt(savedExpiry)
                                .build()
                            val list = cookieStore.getOrPut(host) { mutableListOf() }
                            list.add(cookie)
                        } catch (_: Exception) {}
                    }
                }

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val list = cookieStore.getOrPut(url.host) { mutableListOf() }
                    for (cookie in cookies) {
                        list.removeAll { it.name == cookie.name }
                        list.add(cookie)
                        // Persist session cookie
                        if (cookie.name == "X-OPENMEDIAVAULT-SESSIONID") {
                            securePrefs.sessionCookie = cookie.value
                            securePrefs.cookieExpiry = cookie.expiresAt
                        }
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val stored = cookieStore[url.host] ?: return emptyList()
                    val result = mutableListOf<Cookie>()
                    val now = System.currentTimeMillis()
                    for (cookie in stored) {
                        if (cookie.expiresAt < now) continue
                        if (!cookie.path.endsWith("/") && !url.encodedPath.startsWith(cookie.path)) continue
                        result.add(cookie)
                    }
                    return result
                }
            }

            val client = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .cookieJar(cookieJar)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(securePrefs.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            api = retrofit.create(OmvApi::class.java)
        }
        return api!!
    }

    fun reconnect() {
        api = null
    }

    private suspend fun call(service: String, method: String, params: Any = emptyMap<String, Any>(), options: Any? = null): JsonElement? {
        val resp: Response<RpcResponse> = getApi().rpcCall(
            RpcRequest(service = service, method = method, params = params, options = options)
        )

        if (!resp.isSuccessful) {
            throw Exception("HTTP ${resp.code()}: ${resp.message()}")
        }

        val body = resp.body() ?: throw Exception("Пустой ответ")
        if (body.error != null && body.error.message.isNotEmpty()) {
            val msg = body.error.message
            if (msg.contains("Session not authenticated") || msg.contains("Session expired") || body.error.code in listOf(5001, 5002)) {
                throw SessionExpiredException(msg)
            }
            throw Exception(msg)
        }

        return body.response
    }

    private suspend fun <T> callWithRetry(
        maxRetries: Int = 1,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: SessionExpiredException) {
                lastException = e
                if (attempt < maxRetries) {
                    autoReconnect()
                }
            }
        }
        throw lastException ?: Exception("Unknown error")
    }

    private fun unwrapArray(element: JsonElement?): JsonArray {
        if (element == null || element.isJsonNull) return JsonArray()
        if (element.isJsonArray) return element.asJsonArray
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("data")) return obj.get("data").asJsonArray
            val total = obj.get("total")?.asInt ?: 0
            if (obj.has("data")) return obj.get("data").asJsonArray
        }
        return JsonArray()
    }

    suspend fun login(username: String, password: String): Result<LoginResult> {
        return try {
            val resp = call("session", "login", mapOf("username" to username, "password" to password))
            if (resp == null) {
                Result.failure(Exception("Пустой ответ от сервера"))
            } else {
                val obj = resp.asJsonObject
                val authenticated = obj.get("authenticated")?.asBoolean
                    ?: (obj.get("status")?.asString == "authenticated")
                if (authenticated) {
                    val userName = obj.get("username")?.asString ?: username
                    securePrefs.username = username
                    securePrefs.password = password
                    Result.success(LoginResult("authenticated", "", userName))
                } else {
                    Result.failure(Exception("Авторизация отклонена сервером"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка входа: ${e.message}"))
        }
    }

    suspend fun logout(): Result<Boolean> {
        return try {
            call("session", "logout")
            securePrefs.logout()
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun getSystemInfo(): Result<SystemInfo> {
        return try {
            val resp = callWithRetry {
                call("System", "getInformation")
            } ?: return Result.success(SystemInfo())
            val obj = resp.asJsonObject
            val cpuUsage = obj.get("cpuUsage")?.asFloat ?: 0f
            val memTotal = obj.get("memTotal")?.asLong ?: 1
            val memUsed = obj.get("memUsed")?.asLong ?: 0
            val memUsage = if (memTotal > 0) (memUsed.toDouble() / memTotal * 100) else 0.0

            val rawTemp = obj.get("cputemperature")
                ?: obj.get("cpuTemperature")
                ?: obj.get("temperature")
            val cpuTempStr = when {
                rawTemp == null || rawTemp.isJsonNull -> null
                rawTemp.isJsonPrimitive -> {
                    val prim = rawTemp.asJsonPrimitive
                    if (prim.isNumber) {
                        val v = prim.asDouble
                        if (v > 1000) "${String.format("%.0f", v / 1000)}°C"
                        else "${String.format("%.0f", v)}°C"
                    } else prim.asString.ifEmpty { null }
                }
                else -> null
            }

            Result.success(
                SystemInfo(
                    hostname = obj.get("hostname")?.asString ?: "",
                    version = obj.get("version")?.asString ?: "",
                    uptime = obj.get("uptime")?.asString ?: "",
                    memUsage = memUsage,
                    cpuUsage = listOf(cpuUsage),
                    cpuTemp = cpuTempStr
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDisks(): Result<List<DiskInfo>> {
        return try {
            val resp = callWithRetry { call("DiskMgmt", "enumerateDevices") }
            val list = unwrapArray(resp)
            Result.success(list.map { el ->
                val d = el.asJsonObject
                DiskInfo(
                    devName = d.get("devicename")?.asString ?: d.get("devname")?.asString ?: "",
                    model = d.get("model")?.asString ?: "",
                    size = d.get("size")?.asLong ?: 0,
                    temperature = d.get("temperature")?.let { if (it.isJsonPrimitive) it.asInt else null },
                    health = d.get("health")?.asString ?: "OK",
                    serialNumber = d.get("serialnumber")?.asString ?: ""
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileSystems(): Result<List<FileSystem>> {
        return try {
            val resp = callWithRetry { call("FileSystemMgmt", "enumerateFilesystems", mapOf("start" to 0, "limit" to -1)) }
            val list = unwrapArray(resp)
            Result.success(list.mapNotNull { el ->
                try {
                    val fs = el.asJsonObject
                    FileSystem(
                        uuid = fs.get("uuid")?.asString ?: "",
                        devName = fs.get("devicename")?.asString ?: "",
                        label = fs.get("label")?.asString ?: "",
                        mountPoint = fs.get("mountpoint")?.asString ?: "",
                        type = fs.get("type")?.asString ?: "",
                        size = fs.get("size")?.asLong ?: 0,
                        available = fs.get("available")?.asLong ?: 0,
                        used = (fs.get("size")?.asLong ?: 0) - (fs.get("available")?.asLong ?: 0)
                    )
                } catch (e: Exception) { null }
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServices(): Result<List<ServiceStatus>> {
        return try {
            val resp = callWithRetry { call("Services", "getStatus") }
            val list = unwrapArray(resp)
            Result.success(list.map { el ->
                val s = el.asJsonObject
                ServiceStatus(
                    name = s.get("name")?.asString ?: "",
                    title = s.get("title")?.asString ?: "",
                    enabled = s.get("enabled")?.asBoolean ?: false,
                    running = s.get("running")?.asBoolean ?: false
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleService(name: String, enabled: Boolean): Result<Boolean> {
        return try {
            val method = if (enabled) "enable" else "disable"
            callWithRetry { call("Services", method, mapOf("name" to name)) }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(): Result<List<NotificationItem>> {
        return try {
            val resp = call("Notification", "enumerate", mapOf("start" to 0, "limit" to -1))
                ?: return Result.success(emptyList())
            val list = unwrapArray(resp)
            Result.success(list.map { el ->
                val n = el.asJsonObject
                NotificationItem(
                    date = n.get("date")?.asString ?: "",
                    message = n.get("message")?.asString ?: "",
                    priority = n.get("priority")?.asString ?: ""
                )
            })
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun getNetworkInterfaces(): Result<List<NetworkInterface>> {
        return try {
            val resp = call("Network", "enumerateDevices", mapOf("start" to 0, "limit" to -1))
            val list = unwrapArray(resp)
            Result.success(list.mapNotNull { el ->
                val iface = el.asJsonObject
                if (iface.get("type")?.asString == "loopback") return@mapNotNull null
                NetworkInterface(
                    dev = iface.get("devicename")?.asString ?: "",
                    address = iface.get("address")?.asString ?: "",
                    netmask = iface.get("netmask")?.asString ?: "",
                    gateway = iface.get("gateway")?.asString ?: "",
                    linkSpeed = ""
                )
            })
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    // --- Universal Docker API auto-detection ---

    private var dockerService: String? = null
    private var dockerListMethod: String? = null

    private val dockerServiceCandidates = listOf("Compose", "compose", "docker", "container")
    private val dockerListMethodCandidates = listOf("getContainerList", "enumerateContainers", "enumerate", "getContainers")

    private suspend fun detectDockerApi() {
        if (dockerService != null && dockerListMethod != null) return

        for (service in dockerServiceCandidates) {
            for (method in dockerListMethodCandidates) {
                try {
                    val resp = call(service, method, mapOf("start" to 0, "limit" to 1))
                    if (resp != null) {
                        dockerService = service
                        dockerListMethod = method
                        return
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun getDockerContainers(): Result<List<DockerContainer>> {
        return try {
            detectDockerApi()

            val service = dockerService ?: return Result.success(emptyList())
            val method = dockerListMethod ?: return Result.success(emptyList())

            val resp = callWithRetry { call(service, method, mapOf("start" to 0, "limit" to 999)) }
            val list = unwrapArray(resp)
            Result.success(list.map { el ->
                val c = el.asJsonObject
                DockerContainer(
                    name = c.get("name")?.asString ?: "",
                    image = c.get("image")?.asString ?: "",
                    state = c.get("state")?.asString ?: "unknown",
                    id = c.get("id")?.asString ?: "",
                    created = c.get("created")?.asString ?: "",
                    status = c.get("status")?.asString ?: "",
                    running = c.get("running")?.asString ?: "",
                    command = c.get("command")?.asString ?: "",
                    network = c.get("network")?.asString ?: "",
                    ports = c.get("ports")?.asString ?: "",
                    mounts = c.get("mounts")?.asString ?: ""
                )
            })
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun dockerAction(containerName: String, action: String): Result<Boolean> {
        return try {
            detectDockerApi()
            val service = dockerService ?: return Result.failure(Exception("Docker API не найден"))

            val method = "doContainerCommand"
            val command = when (action.lowercase()) {
                "start" -> "start"
                "stop" -> "stop"
                "restart" -> "restart"
                else -> return Result.failure(Exception("Неизвестное действие: $action"))
            }

            val containerId = containerName

            callWithRetry {
                call(service, method, mapOf(
                    "command" to command,
                    "command2" to "",
                    "id" to containerId
                ))
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDockerApiInfo(): String {
        return "service=$dockerService, listMethod=$dockerListMethod"
    }

    // --- S.M.A.R.T. ---

    suspend fun getSmartDevices(): Result<List<SmartDevice>> {
        return try {
            val resp = callWithRetry { call("Smart", "enumerateDevices") }
            val list = unwrapArray(resp)
            Result.success(list.map { el ->
                val d = el.asJsonObject
                SmartDevice(
                    deviceName = d.get("devicename")?.asString ?: "",
                    deviceFile = d.get("devicefile")?.asString ?: "",
                    model = d.get("model")?.asString ?: "",
                    size = d.get("size")?.asLong ?: 0,
                    temperature = d.get("temperature")?.asString ?: "",
                    serialNumber = d.get("serialnumber")?.asString ?: "",
                    overallStatus = d.get("overallstatus")?.asString ?: "",
                    vendor = d.get("vendor")?.asString ?: ""
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmartInfo(deviceFile: String): Result<SmartInfo> {
        return try {
            val resp = callWithRetry { call("Smart", "getInformation", mapOf("devicefile" to deviceFile)) }
                ?: return Result.success(SmartInfo())
            val obj = resp.asJsonObject
            Result.success(
                SmartInfo(
                    deviceModel = obj.get("devicemodel")?.asString ?: "",
                    serialNumber = obj.get("serialnumber")?.asString ?: "",
                    firmwareVersion = obj.get("firmwareversion")?.asString ?: "",
                    temperature = obj.get("temperature")?.asString ?: "",
                    powerCycles = obj.get("powercycles")?.asString ?: "",
                    powerOnHours = obj.get("poweronhours")?.asString ?: "",
                    overallHealth = obj.get("overallhealth")?.asString ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmartAttributes(deviceFile: String): Result<List<SmartAttribute>> {
        return try {
            val resp = callWithRetry { call("Smart", "getAttributes", mapOf("devicefile" to deviceFile)) }
            val list = unwrapArray(resp)
            Result.success(list.map { el ->
                val a = el.asJsonObject
                SmartAttribute(
                    id = a.get("id")?.asString ?: "",
                    name = a.get("name")?.asString ?: "",
                    value = a.get("value")?.asString ?: "",
                    worst = a.get("worst")?.asString ?: "",
                    threshold = a.get("threshold")?.asString ?: "",
                    rawValue = a.get("rawvalue")?.asString ?: "",
                    flags = a.get("flags")?.asString ?: ""
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rebootServer(): Result<Boolean> {
        return try {
            callWithRetry { call("System", "shutdown", mapOf("reboot" to true)) }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun shutdownServer(): Result<Boolean> {
        return try {
            callWithRetry { call("System", "shutdown", mapOf("reboot" to false)) }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun autoReconnect() {
        if (securePrefs.username.isNotEmpty() && securePrefs.password.isNotEmpty()) {
            try {
                login(securePrefs.username, securePrefs.password)
            } catch (_: Exception) { }
        }
    }

    suspend fun getPlugins(): Result<List<OmvPlugin>> {
        return try {
            val resp = callWithRetry { call("Extending", "enumerate") }
            val list = unwrapArray(resp)
            Result.success(list.map { el ->
                val p = el.asJsonObject
                OmvPlugin(
                    name = p.get("name")?.asString ?: "",
                    description = p.get("description")?.asString ?: "",
                    installed = p.get("installed")?.asBoolean ?: false,
                    version = p.get("version")?.asString ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun installPlugin(name: String): Result<Boolean> {
        return try {
            callWithRetry { call("Extending", "install", mapOf("name" to name)) }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removePlugin(name: String): Result<Boolean> {
        return try {
            callWithRetry { call("Extending", "remove", mapOf("name" to name)) }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun executeCommand(command: String): Result<String> {
        return try {
            val resp = callWithRetry { call("System", "executeCommand", mapOf("command" to command)) }
            Result.success(resp?.asString ?: resp?.asJsonObject?.get("output")?.asString ?: "")
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun keepSessionAlive(): Boolean {
        return try {
            callWithRetry { call("System", "getInformation") }
            true
        } catch (_: Exception) { false }
    }

    suspend fun getBackupConfig(): Result<String> {
        return try {
            val resp = callWithRetry { call("Config", "get") }
            Result.success(resp?.toString() ?: "")
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun restoreConfig(configJson: String): Result<Boolean> {
        return try {
            val params = mapOf("config" to configJson)
            callWithRetry { call("Config", "import", params) }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }
}

class SessionExpiredException(message: String) : Exception(message)
