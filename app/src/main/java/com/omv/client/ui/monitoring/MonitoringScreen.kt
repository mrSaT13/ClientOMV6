package com.omv.client.ui.monitoring

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.data.model.*
import com.omv.client.data.repository.OmvRepository
import com.omv.client.ui.components.*
import com.omv.client.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val repository: OmvRepository
) : ViewModel() {

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo

    private val _disks = MutableStateFlow<List<DiskInfo>>(emptyList())
    val disks: StateFlow<List<DiskInfo>> = _disks

    private val _fileSystems = MutableStateFlow<List<FileSystem>>(emptyList())
    val fileSystems: StateFlow<List<FileSystem>> = _fileSystems

    private val _network = MutableStateFlow<List<NetworkInterface>>(emptyList())
    val network: StateFlow<List<NetworkInterface>> = _network

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var refreshJob: kotlinx.coroutines.Job? = null

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated: StateFlow<String> = _lastUpdated

    init {
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getSystemInfo().onSuccess { _systemInfo.value = it }
            repository.getDisks().onSuccess { _disks.value = it }
            repository.getFileSystems().onSuccess { _fileSystems.value = it }
            repository.getNetworkInterfaces().onSuccess { _network.value = it }

            val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            _lastUpdated.value = now
            _isLoading.value = false
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                repository.getSystemInfo().onSuccess { _systemInfo.value = it }
                repository.getFileSystems().onSuccess { _fileSystems.value = it }
                repository.getDisks().onSuccess { _disks.value = it }
                repository.getNetworkInterfaces().onSuccess { _network.value = it }
                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                _lastUpdated.value = now
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    modifier: Modifier = Modifier,
    viewModel: MonitoringViewModel = hiltViewModel()
) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val disks by viewModel.disks.collectAsState()
    val fileSystems by viewModel.fileSystems.collectAsState()
    val network by viewModel.network.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мониторинг", fontWeight = FontWeight.Bold) },
                actions = {
                    if (lastUpdated.isNotEmpty()) {
                        Text(
                            "Обновлено: $lastUpdated",
                            fontSize = 11.sp,
                            color = Grey600,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
            )
        }
    ) { padding ->
        when {
            isLoading && systemInfo == null -> LoadingOverlay()
            error != null && systemInfo == null -> ErrorMessage(message = error ?: "", onRetry = { viewModel.refresh() })
            else -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    systemInfo?.let { info ->
                        item { SystemResourcesCard(info) }
                    }

                    if (fileSystems.isNotEmpty()) {
                        item {
                            Text(
                                "Файловые системы",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(fileSystems.size) { index ->
                            FileSystemCard(fileSystems[index])
                        }
                    }

                    if (disks.isNotEmpty()) {
                        item {
                            Text(
                                "Диски",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(disks.size) { index ->
                            DiskCard(disks[index])
                        }
                    }

                    if (network.isNotEmpty()) {
                        item {
                            Text(
                                "Сеть",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(network.size) { index ->
                            NetworkCard(network[index])
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SystemResourcesCard(info: SystemInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Purple900, Purple500)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Системные ресурсы",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                val cpuVal = try {
                    (info.cpuUsage.firstOrNull() as? Number)?.toFloat() ?: 0f
                } catch (_: Exception) { 0f }
                ResourceRow(
                    label = "ЦПУ",
                    progress = (cpuVal.coerceAtMost(100f)) / 100f,
                    value = "${String.format("%.1f", cpuVal)}%",
                    color = Teal200
                )
                Spacer(modifier = Modifier.height(12.dp))

                ResourceRow(
                    label = "ОЗУ",
                    progress = (info.memUsage / 100.0).toFloat(),
                    value = "${String.format("%.1f", info.memUsage)}%",
                    color = Orange500
                )

                if (info.cpuTemp != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Температура CPU", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                        Text(
                            "${info.cpuTemp}°C",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(label: String, progress: Float, value: String, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color))
                    )
            )
        }
    }
}

@Composable
private fun FileSystemCard(fs: FileSystem) {
    val usedPercent = if (fs.size > 0) (fs.used.toFloat() / fs.size) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        fs.label.ifEmpty { fs.devName },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${fs.mountPoint} • ${fs.type.uppercase()}",
                        fontSize = 12.sp,
                        color = Grey600
                    )
                }
                Text(
                    "${String.format("%.1f", usedPercent * 100)}%",
                    fontWeight = FontWeight.Bold,
                    color = if (usedPercent > 0.9f) Red500 else Blue500
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ProgressBar(
                progress = usedPercent,
                color = if (usedPercent > 0.9f) Red500 else if (usedPercent > 0.7f) Orange500 else Blue500
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Использовано: ${formatBytes(fs.used)}", fontSize = 11.sp, color = Grey600)
                Text("Свободно: ${formatBytes(fs.size - fs.used)}", fontSize = 11.sp, color = Grey600)
            }
        }
    }
}

@Composable
private fun DiskCard(disk: DiskInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Teal700.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = Teal700,
                        modifier = Modifier.size(22.dp)
                    )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(disk.model.ifEmpty { disk.devName }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(label = disk.health.ifEmpty { "OK" }, isActive = disk.health.lowercase() == "ok")
                    disk.temperature?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${it}°C", fontSize = 12.sp, color = Grey600)
                    }
                }
            }
            Text(formatBytes(disk.size), fontSize = 13.sp, color = Grey600)
        }
    }
}

@Composable
private fun NetworkCard(iface: NetworkInterface) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Blue500.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Blue500,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(iface.dev, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (iface.linkSpeed.isNotEmpty()) {
                        Text(iface.linkSpeed, fontSize = 12.sp, color = Grey600)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (iface.address.isNotEmpty()) {
                InfoRow(label = "IP", value = iface.address, icon = Icons.Default.Lan)
            }
            if (iface.netmask.isNotEmpty()) {
                InfoRow(label = "Маска", value = iface.netmask)
            }
            if (iface.gateway.isNotEmpty()) {
                InfoRow(label = "Шлюз", value = iface.gateway)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)
    return String.format(
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}
