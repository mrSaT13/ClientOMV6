package com.omv.client.ui.smart

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omv.client.data.model.SmartAttribute
import com.omv.client.data.model.SmartDevice
import com.omv.client.data.model.SmartInfo
import com.omv.client.ui.components.*
import com.omv.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartScreen(
    viewModel: SmartViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val smartInfo by viewModel.smartInfo.collectAsState()
    val attributes by viewModel.attributes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedDevice != null) selectedDevice?.model ?: "S.M.A.R.T."
                        else "S.M.A.R.T. Мониторинг",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (selectedDevice != null) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
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
            isLoading && devices.isEmpty() -> LoadingOverlay()
            error != null && devices.isEmpty() -> ErrorMessage(
                message = error ?: "",
                onRetry = { viewModel.refresh() }
            )
            else -> {
                if (selectedDevice == null) {
                    DeviceList(
                        devices = devices,
                        padding = padding,
                        onDeviceClick = { viewModel.selectDevice(it) }
                    )
                } else {
                    DeviceDetails(
                        device = selectedDevice!!,
                        info = smartInfo,
                        attributes = attributes,
                        padding = padding
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<SmartDevice>,
    padding: PaddingValues,
    onDeviceClick: (SmartDevice) -> Unit
) {
    if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Grey600
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Нет устройств с S.M.A.R.T.", color = Grey600, fontSize = 16.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(devices, key = { it.deviceFile }) { device ->
            SmartDeviceCard(device = device, onClick = { onDeviceClick(device) })
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SmartDeviceCard(device: SmartDevice, onClick: () -> Unit) {
    val isGood = device.overallStatus.lowercase() in listOf("passed", "ok", "healthy", "")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isGood) Green500.copy(alpha = 0.12f) else Red500.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = if (isGood) Green500 else Red500,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.model.ifEmpty { device.deviceName },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${device.deviceName} • ${device.deviceFile}",
                    fontSize = 12.sp,
                    color = Grey600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (device.temperature.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Thermostat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Grey600
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(device.temperature, fontSize = 13.sp, color = Grey600)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(
                    label = device.overallStatus.ifEmpty { "OK" },
                    isActive = isGood
                )
            }
        }
    }
}

@Composable
private fun DeviceDetails(
    device: SmartDevice,
    info: SmartInfo?,
    attributes: List<SmartAttribute>,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Blue900, Blue500))
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            device.model.ifEmpty { device.deviceName },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            device.deviceFile,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem("Температура", info?.temperature?.ifEmpty { device.temperature } ?: device.temperature)
                            DetailItem("Серийный", device.serialNumber)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem("Включений", info?.powerCycles ?: "")
                            DetailItem("Часов работы", info?.powerOnHours ?: "")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem("Прошивка", info?.firmwareVersion ?: "")
                            DetailItem("Размер", formatSize(device.size))
                        }
                    }
                }
            }
        }

        // Overall status
        item {
            val status = device.overallStatus
            val isGood = status.lowercase() in listOf("passed", "ok", "healthy", "")
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
                            .size(40.dp)
                            .background(
                                if (isGood) Green500.copy(alpha = 0.12f) else Red500.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isGood) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isGood) Green500 else Red500,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Общий статус",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            status.ifEmpty { "Нет данных" },
                            fontSize = 13.sp,
                            color = if (isGood) Green500 else Red500
                        )
                    }
                }
            }
        }

        // Attributes header
        if (attributes.isNotEmpty()) {
            item {
                Text(
                    "Атрибуты S.M.A.R.T.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Attributes list
        items(attributes, key = { it.id }) { attr ->
            AttributeRow(attr)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Text(
            value.ifEmpty { "—" },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun AttributeRow(attr: SmartAttribute) {
    val rawVal = attr.rawValue.toLongOrNull() ?: 0
    val threshold = attr.threshold.toIntOrNull() ?: 0
    val value = attr.value.toIntOrNull() ?: 0
    val isWarning = if (threshold > 0) value <= threshold else false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attr.name.ifEmpty { "Attr #${attr.id}" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isWarning) Red500 else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "RAW: $rawVal",
                    fontSize = 11.sp,
                    color = Grey600
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    attr.value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) Red500 else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "worst:${attr.worst} thr:${attr.threshold}",
                    fontSize = 10.sp,
                    color = Grey600
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)
    return String.format(
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}
