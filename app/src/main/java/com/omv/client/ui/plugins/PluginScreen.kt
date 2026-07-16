package com.omv.client.ui.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.omv.client.data.model.OmvPlugin
import com.omv.client.data.repository.OmvRepository
import com.omv.client.ui.components.LoadingOverlay
import com.omv.client.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginViewModel @Inject constructor(
    private val repository: OmvRepository
) : ViewModel() {

    private val _plugins = MutableStateFlow<List<OmvPlugin>>(emptyList())
    val plugins: StateFlow<List<OmvPlugin>> = _plugins

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    init {
        loadPlugins()
    }

    fun loadPlugins() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getPlugins()
                .onSuccess { _plugins.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun installPlugin(name: String) {
        viewModelScope.launch {
            repository.installPlugin(name)
                .onSuccess {
                    _snackbarMessage.value = "Плагин $name установлен"
                    loadPlugins()
                }
                .onFailure { _snackbarMessage.value = "Ошибка: ${it.message}" }
        }
    }

    fun removePlugin(name: String) {
        viewModelScope.launch {
            repository.removePlugin(name)
                .onSuccess {
                    _snackbarMessage.value = "Плагин $name удалён"
                    loadPlugins()
                }
                .onFailure { _snackbarMessage.value = "Ошибка: ${it.message}" }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginScreen(
    modifier: Modifier = Modifier,
    viewModel: PluginViewModel = hiltViewModel()
) {
    val plugins by viewModel.plugins.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Плагины", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPlugins() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            isLoading && plugins.isEmpty() -> LoadingOverlay()
            error != null && plugins.isEmpty() -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Red500,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error ?: "", color = Grey600)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPlugins() }) {
                            Text("Повторить")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(plugins) { plugin ->
                        PluginCard(
                            plugin = plugin,
                            onInstall = { viewModel.installPlugin(plugin.name) },
                            onRemove = { viewModel.removePlugin(plugin.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginCard(
    plugin: OmvPlugin,
    onInstall: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (plugin.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plugin.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Grey600
                        )
                    }
                }
                if (plugin.version.isNotEmpty()) {
                    Text(
                        text = "v${plugin.version}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Grey600
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plugin.installed) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Green500.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Установлен",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Green500,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.buttonColors(containerColor = Red500)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = Blue500)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Установить")
                    }
                }
            }
        }
    }
}
