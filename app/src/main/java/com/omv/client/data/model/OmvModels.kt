package com.omv.client.data.model

import com.google.gson.annotations.SerializedName

data class RpcRequest(
    @SerializedName("service") val service: String,
    @SerializedName("method") val method: String,
    @SerializedName("params") val params: Any = emptyMap<String, Any>(),
    @SerializedName("options") val options: Any? = null
)

data class RpcResponse(
    @SerializedName("response") val response: com.google.gson.JsonElement?,
    @SerializedName("error") val error: RpcError?
)

data class RpcError(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String = ""
)

data class LoginParams(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResult(
    val status: String = "",
    val sessionId: String = "",
    val username: String = "",
    val permissions: Map<String, Any>? = null
)

data class SystemInfo(
    val hostname: String = "",
    val version: String = "",
    val date: String = "",
    val uptime: String = "",
    val cpuUsage: List<Any> = emptyList(),
    val memUsage: Double = 0.0,
    val cpuTemp: Any? = null
)

data class DiskInfo(
    val devName: String = "",
    val model: String = "",
    val size: Long = 0,
    val temperature: Int? = null,
    val health: String = "",
    val serialNumber: String = ""
)

data class FileSystem(
    val uuid: String = "",
    val devName: String = "",
    val label: String = "",
    val mountPoint: String = "",
    val type: String = "",
    val size: Long = 0,
    val available: Long = 0,
    val used: Long = 0
)

data class ServiceStatus(
    val name: String = "",
    val title: String = "",
    val enabled: Boolean = false,
    val running: Boolean = false,
    val status: String = ""
)

data class NotificationItem(
    val date: String = "",
    val message: String = "",
    val priority: String = ""
)

data class NetworkInterface(
    val dev: String = "",
    val address: String = "",
    val netmask: String = "",
    val gateway: String = "",
    val linkSpeed: String = ""
)

data class UserItem(
    val name: String = "",
    val email: String = "",
    val shell: String = "",
    val group: String = ""
)

data class DockerContainer(
    val name: String = "",
    val image: String = "",
    val state: String = "unknown",
    val id: String = "",
    val created: String = "",
    val status: String = "",
    val running: String = "",
    val command: String = "",
    val network: String = "",
    val ports: String = "",
    val mounts: String = "",
    val project: String = "",
    val service: String = "",
    val composeFile: String = ""
)

data class SmartDevice(
    val deviceName: String = "",
    val deviceFile: String = "",
    val model: String = "",
    val size: Long = 0,
    val temperature: String = "",
    val serialNumber: String = "",
    val overallStatus: String = "",
    val vendor: String = ""
)

data class SmartInfo(
    val deviceModel: String = "",
    val serialNumber: String = "",
    val firmwareVersion: String = "",
    val temperature: String = "",
    val powerCycles: String = "",
    val powerOnHours: String = "",
    val overallHealth: String = ""
)

data class SmartAttribute(
    val id: String = "",
    val name: String = "",
    val value: String = "",
    val worst: String = "",
    val threshold: String = "",
    val rawValue: String = "",
    val flags: String = ""
)

data class OmvPlugin(
    val name: String = "",
    val description: String = "",
    val installed: Boolean = false,
    val version: String = ""
)
