package com.omv.client.ui.docker

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omv.client.data.model.DockerContainer
import com.omv.client.ui.components.*
import com.omv.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockerScreen(
    modifier: Modifier = Modifier,
    viewModel: DockerViewModel = hiltViewModel()
) {
    val containers by viewModel.containers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val apiInfo by viewModel.apiInfo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredContainers = remember(containers, searchQuery) {
        if (searchQuery.isBlank()) containers
        else containers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.image.contains(searchQuery, ignoreCase = true) ||
            it.project.contains(searchQuery, ignoreCase = true)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = isSearchActive,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                                initialOffsetX = { it / 2 },
                                animationSpec = tween(200)
                            ) togetherWith fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                targetOffsetX = { it / 2 },
                                animationSpec = tween(200)
                            )
                        },
                        label = "search_title"
                    ) { searching ->
                        if (searching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Поиск контейнеров...", fontSize = 16.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, "Очистить")
                                        }
                                    }
                                }
                            )
                        } else {
                            Text("Docker", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Поиск",
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            isLoading && containers.isEmpty() -> LoadingOverlay()
            error != null && containers.isEmpty() -> ErrorMessage(
                message = error ?: "",
                onRetry = { viewModel.refresh() }
            )
            else -> {
                val running = filteredContainers.filter { it.state.lowercase() == "running" }
                val stopped = filteredContainers.filter { it.state.lowercase() != "running" }

                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredContainers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (searchQuery.isNotEmpty()) Icons.Default.SearchOff
                                        else Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Grey600
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        if (searchQuery.isNotEmpty()) "Ничего не найдено"
                                        else "Нет контейнеров",
                                        color = Grey600, fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    if (running.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Green500)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Работают (${running.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(running, key = { it.name }) { container ->
                            DockerContainerCard(
                                container = container,
                                onStart = {},
                                onStop = { viewModel.stopContainer(container.name) },
                                onRestart = { viewModel.restartContainer(container.name) }
                            )
                        }
                    }

                    if (stopped.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Grey600)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Остановлены (${stopped.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(stopped, key = { it.name }) { container ->
                            DockerContainerCard(
                                container = container,
                                onStart = { viewModel.startContainer(container.name) },
                                onStop = {},
                                onRestart = { viewModel.restartContainer(container.name) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (apiInfo.isNotEmpty()) {
                            Text(
                                "API: $apiInfo",
                                fontSize = 10.sp,
                                color = Grey600,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DockerContainerCard(
    container: DockerContainer,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    val isRunning = container.state.lowercase() == "running"
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isRunning) Green500.copy(alpha = 0.12f) else Grey200,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isRunning) Icons.Default.PlayArrow else Icons.Default.Stop,
                        contentDescription = null,
                        tint = if (isRunning) Green500 else Grey600,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        container.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        container.image,
                        fontSize = 12.sp,
                        color = Grey600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = Grey600
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!isRunning) {
                            DropdownMenuItem(
                                text = { Text("Запустить") },
                                onClick = { showMenu = false; onStart() },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                            )
                        }
                        if (isRunning) {
                            DropdownMenuItem(
                                text = { Text("Остановить") },
                                onClick = { showMenu = false; onStop() },
                                leadingIcon = { Icon(Icons.Default.Stop, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Перезапустить") },
                            onClick = { showMenu = false; onRestart() },
                            leadingIcon = { Icon(Icons.Default.RestartAlt, null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(
                    label = if (isRunning) "Работает" else container.status.ifEmpty { "Остановлен" },
                    isActive = isRunning
                )
                if (container.id.isNotEmpty()) {
                    SmallChip(label = container.id.take(12))
                }
            }

            if (container.ports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lan,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Grey600
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(container.ports, fontSize = 11.sp, color = Grey600, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            if (container.service.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Grey600
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(container.service, fontSize = 12.sp, color = Grey600)
                }
            }

            if (container.composeFile.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Grey600
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(container.composeFile, fontSize = 11.sp, color = Grey600, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SmallChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Blue500.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = Blue500,
            fontWeight = FontWeight.Medium
        )
    }
}
