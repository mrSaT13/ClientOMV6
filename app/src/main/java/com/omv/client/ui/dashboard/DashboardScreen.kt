package com.omv.client.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.data.model.*
import com.omv.client.data.repository.OmvRepository
import com.omv.client.data.security.SecurePrefs
import com.omv.client.ui.components.*
import com.omv.client.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: OmvRepository,
    private val securePrefs: SecurePrefs
) : ViewModel() {

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo

    private val _disks = MutableStateFlow<List<DiskInfo>>(emptyList())
    val disks: StateFlow<List<DiskInfo>> = _disks

    private val _services = MutableStateFlow<List<ServiceStatus>>(emptyList())
    val services: StateFlow<List<ServiceStatus>> = _services

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired

    private var consecutiveFailures = 0

    init {
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val info = repository.getSystemInfo()
            info.onSuccess { _systemInfo.value = it }

            val disksResult = repository.getDisks()
            disksResult.onSuccess { _disks.value = it }

            val servicesResult = repository.getServices()
            servicesResult.onSuccess { _services.value = it }

            if (info.isFailure && disksResult.isFailure && servicesResult.isFailure) {
                _error.value = info.exceptionOrNull()?.message
                consecutiveFailures++
                if (consecutiveFailures >= 2) {
                    val alive = repository.keepSessionAlive()
                    if (!alive) {
                        _sessionExpired.value = true
                    }
                }
            } else {
                consecutiveFailures = 0
                _error.value = null
            }

            saveWidgetCache()

            _isLoading.value = false
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                val infoResult = repository.getSystemInfo()
                val disksResult = repository.getDisks()
                val servicesResult = repository.getServices()

                infoResult.onSuccess { _systemInfo.value = it }
                disksResult.onSuccess { _disks.value = it }
                servicesResult.onSuccess { _services.value = it }

                val allFailed = infoResult.isFailure && disksResult.isFailure && servicesResult.isFailure
                if (allFailed) {
                    consecutiveFailures++
                    if (consecutiveFailures >= 3) {
                        val alive = repository.keepSessionAlive()
                        if (!alive) {
                            _sessionExpired.value = true
                            break
                        }
                        consecutiveFailures = 0
                    }
                } else {
                    consecutiveFailures = 0
                }

                saveWidgetCache()
            }
        }
    }

    private fun saveWidgetCache() {
        try {
            val info = _systemInfo.value ?: return
            val cpu = info.cpuUsage.firstOrNull()?.let { v ->
                when (v) {
                    is Number -> "${v.toFloat()}"
                    is String -> v
                    else -> "0"
                }
            } ?: "0"
            val cpuPct = cpu.toFloatOrNull()?.let { "${String.format("%.0f", it)}%" } ?: "—"
            val ramPct = "${String.format("%.0f", info.memUsage)}%"
            val diskCount = _disks.value.size
            val hostname = info.hostname.ifEmpty { "—" }
            securePrefs.saveWidgetCache(cpuPct, ramPct, "$diskCount дисков", hostname)
        } catch (_: Exception) {}
    }

    fun rebootServer() {
        viewModelScope.launch {
            repository.rebootServer()
        }
    }

    fun shutdownServer() {
        viewModelScope.launch {
            repository.shutdownServer()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToServices: () -> Unit = {},
    onNavigateToMonitoring: () -> Unit = {},
    onNavigateToSmart: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToTerminal: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val disks by viewModel.disks.collectAsState()
    val services by viewModel.services.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sessionExpired by viewModel.sessionExpired.collectAsState()

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) onNavigateToLogin()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OMV Client",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigateToNotifications() }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Уведомления",
                            tint = MaterialTheme.colorScheme.primary
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
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    systemInfo?.let { info ->
                        SystemOverviewCard(info)
                    }

                    QuickStatsRow(
                        disks = disks,
                        services = services,
                        onClickDisks = onNavigateToMonitoring,
                        onClickServices = onNavigateToServices,
                        onClickActive = onNavigateToServices
                    )

                    systemInfo?.let { info ->
                        LoadGraphsCard(info, disks)
                    }

                    QuickActionsCard(
                        onReboot = { viewModel.rebootServer() },
                        onShutdown = { viewModel.shutdownServer() }
                    )

                    if (services.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                            shape = RoundedCornerShape(14.dp),
                            onClick = onNavigateToServices
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Все сервисы",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = onNavigateToMonitoring
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Детальный мониторинг",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = onNavigateToSmart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Teal700,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "S.M.A.R.T. Диски",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = onNavigateToTerminal
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = Green500, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Терминал", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = onNavigateToBackup
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Blue500, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Бэкап настроек", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SystemOverviewCard(info: SystemInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Blue900, Blue500)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = info.hostname.ifEmpty { "OMV Server" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v${info.version.ifEmpty { "6.x" }}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = Teal700,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoColumn("Uptime", info.uptime.ifEmpty { "—" }, Icons.Default.Schedule)

                    val cpuUsage = info.cpuUsage.firstOrNull()?.let { v ->
                        when (v) {
                            is Number -> v.toFloat()
                            is String -> v.toFloatOrNull() ?: 0f
                            else -> 0f
                        }
                    } ?: 0f
                    ProgressInfoColumn(
                        label = "CPU",
                        percentage = cpuUsage / 100f,
                        displayText = "${info.cpuUsage.size} cores",
                        icon = Icons.Default.Memory
                    )
                    val ramUsage = (info.memUsage / 100.0).toFloat().coerceIn(0f, 1f)
                    ProgressInfoColumn(
                        label = "RAM",
                        percentage = ramUsage,
                        displayText = "${String.format("%.0f", info.memUsage)}%",
                        icon = Icons.Default.SdStorage
                    )
                    val temp = info.cpuTemp?.toString()?.trimEnd() ?: ""
                    val tempValue = temp.replace(Regex("[^0-9.]"), "").toFloatOrNull()
                    val tempColor = when {
                        tempValue == null -> Color.White
                        tempValue <= 50f -> Green500
                        tempValue <= 70f -> Orange500
                        else -> Red500
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DeviceThermostat,
                            contentDescription = null,
                            tint = tempColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = temp.ifEmpty { "—" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Temp",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun ProgressInfoColumn(
    label: String,
    percentage: Float,
    displayText: String,
    icon: ImageVector
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 1f),
        label = "progress"
    )
    val trackColor = Color.White.copy(alpha = 0.15f)
    val progressColor = when {
        percentage <= 0.5f -> Green500
        percentage <= 0.8f -> Orange500
        else -> Red500
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = animatedPercentage * 360f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = displayText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun LoadGraphsCard(info: SystemInfo, disks: List<DiskInfo>) {
    val cpuUsage = info.cpuUsage.firstOrNull()?.let { v ->
        when (v) {
            is Number -> v.toFloat()
            is String -> v.toFloatOrNull() ?: 0f
            else -> 0f
        }
    } ?: 0f

    val ramPct = info.memUsage.toFloat().coerceIn(0f, 100f)

    val diskUsages = disks.filter { it.size > 0 }.take(3).map { d ->
        val usedPct = if (d.size > 0) ((d.size - 0) / d.size.toFloat() * 100) else 0f
        d.devName to usedPct
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Нагрузка",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))

            LoadBar(label = "CPU", percentage = cpuUsage, color = Blue500)
            Spacer(modifier = Modifier.height(10.dp))
            LoadBar(label = "RAM", percentage = ramPct, color = Green500)

            if (diskUsages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                diskUsages.forEach { (name, pct) ->
                    LoadBar(label = name, percentage = pct, color = Teal700)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun LoadBar(label: String, percentage: Float, color: Color) {
    val animPct by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f) / 100f,
        label = "bar"
    )
    val barColor = when {
        percentage <= 50f -> color
        percentage <= 80f -> Orange500
        else -> Red500
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(48.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animPct)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${String.format("%.0f", percentage)}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun QuickActionsCard(
    onReboot: () -> Unit,
    onShutdown: () -> Unit
) {
    var showRebootDialog by remember { mutableStateOf(false) }
    var showShutdownDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Быстрые действия",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showRebootDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue500)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Перезагрузка", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { showShutdownDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Выключение", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("Перезагрузка") },
            text = { Text("Перезагрузить сервер?") },
            confirmButton = {
                TextButton(onClick = {
                    showRebootDialog = false
                    onReboot()
                }) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showShutdownDialog) {
        AlertDialog(
            onDismissRequest = { showShutdownDialog = false },
            title = { Text("Выключение") },
            text = { Text("Выключить сервер?") },
            confirmButton = {
                TextButton(onClick = {
                    showShutdownDialog = false
                    onShutdown()
                }) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShutdownDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun QuickStatsRow(
    disks: List<DiskInfo>,
    services: List<ServiceStatus>,
    onClickDisks: () -> Unit = {},
    onClickServices: () -> Unit = {},
    onClickActive: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Storage,
            label = "Диски",
            value = "${disks.size}",
            color = Teal700,
            onClick = onClickDisks
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Memory,
            label = "Сервисы",
            value = "${services.count { it.enabled }}",
            color = Blue500,
            onClick = onClickServices
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            label = "Активны",
            value = "${services.count { it.running }}",
            color = Green500,
            onClick = onClickActive
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
