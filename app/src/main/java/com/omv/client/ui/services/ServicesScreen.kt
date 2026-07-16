package com.omv.client.ui.services

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.data.model.ServiceStatus
import com.omv.client.data.repository.OmvRepository
import com.omv.client.ui.components.*
import com.omv.client.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val repository: OmvRepository
) : ViewModel() {

    private val _services = MutableStateFlow<List<ServiceStatus>>(emptyList())
    val services: StateFlow<List<ServiceStatus>> = _services

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _togglingService = MutableStateFlow<String?>(null)
    val togglingService: StateFlow<String?> = _togglingService

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getServices().fold(
                onSuccess = { _services.value = it; _error.value = null },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun toggleService(service: ServiceStatus) {
        viewModelScope.launch {
            _togglingService.value = service.name
            repository.toggleService(service.name, !service.enabled).fold(
                onSuccess = { refresh() },
                onFailure = { _error.value = it.message }
            )
            _togglingService.value = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    modifier: Modifier = Modifier,
    viewModel: ServicesViewModel = hiltViewModel()
) {
    val services by viewModel.services.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val togglingService by viewModel.togglingService.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сервисы", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
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
            isLoading -> LoadingOverlay()
            error != null -> ErrorMessage(message = error ?: "", onRetry = { viewModel.refresh() })
            else -> {
                val grouped = services.groupBy { it.enabled }

                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (services.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Нет данных", color = Grey600)
                            }
                        }
                    }

                    if (grouped[true]?.isNotEmpty() == true) {
                        item {
                            Text(
                                "Активные",
                                style = MaterialTheme.typography.titleSmall,
                                color = Green500,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(grouped[true] ?: emptyList()) { service ->
                            ServiceCard(
                                service = service,
                                isToggling = togglingService == service.name,
                                onToggle = { viewModel.toggleService(service) }
                            )
                        }
                    }

                    if (grouped[false]?.isNotEmpty() == true) {
                        item {
                            Text(
                                "Отключённые",
                                style = MaterialTheme.typography.titleSmall,
                                color = Grey600,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(grouped[false] ?: emptyList()) { service ->
                            ServiceCard(
                                service = service,
                                isToggling = togglingService == service.name,
                                onToggle = { viewModel.toggleService(service) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: ServiceStatus,
    isToggling: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                .background(
                    if (service.running) Green500.copy(alpha = 0.12f)
                    else Grey300.copy(alpha = 0.2f),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (service.name.lowercase()) {
                        "smb" -> Icons.Default.FolderShared
                        "nfs" -> Icons.Default.Storage
                        "docker" -> Icons.Default.ViewInAr
                        "webgui" -> Icons.Default.Language
                        "ssh" -> Icons.Default.Terminal
                        "ftp" -> Icons.Default.CloudUpload
                        "rsyncd" -> Icons.Default.SyncAlt
                        else -> Icons.Default.Extension
                    },
                    contentDescription = null,
                    tint = if (service.running) Green500 else Grey600,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.title.ifEmpty { service.name },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(
                        label = if (service.running) "Запущен" else "Остановлен",
                        isActive = service.running
                    )
                    if (service.enabled && !service.running) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusChip(
                            label = "Включён",
                            isActive = false
                        )
                    }
                }
            }

            if (isToggling) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Switch(
                    checked = service.enabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}
