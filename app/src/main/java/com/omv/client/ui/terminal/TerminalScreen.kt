package com.omv.client.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.data.repository.OmvRepository
import com.omv.client.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalEntry(
    val command: String,
    val output: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val repository: OmvRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<TerminalEntry>>(emptyList())
    val entries: StateFlow<List<TerminalEntry>> = _entries

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            _isExecuting.value = true
            _entries.value = _entries.value + TerminalEntry(command = command, isLoading = true)
            repository.executeCommand(command)
                .onSuccess { output ->
                    _entries.value = _entries.value.map {
                        if (it.command == command && it.isLoading) {
                            it.copy(output = output, isLoading = false)
                        } else it
                    }
                }
                .onFailure { e ->
                    _entries.value = _entries.value.map {
                        if (it.command == command && it.isLoading) {
                            it.copy(output = "Ошибка: ${e.message}", isLoading = false)
                        } else it
                    }
                }
            _isExecuting.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    var commandText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Терминал", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Введите команду для выполнения",
                            color = Grey600,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(entries) { entry ->
                            CommandCard(entry)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Введите команду...") },
                        singleLine = true,
                        enabled = !isExecuting,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            viewModel.executeCommand(commandText)
                            commandText = ""
                        },
                        enabled = commandText.isNotBlank() && !isExecuting,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Blue500
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Отправить")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandCard(entry: TerminalEntry) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 4.dp, 4.dp),
            color = Blue500.copy(alpha = 0.12f)
        ) {
            Text(
                text = "$ ${entry.command}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Blue700
            )
        }

        if (entry.isLoading) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp, 4.dp, 12.dp, 12.dp),
                color = Grey100
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Blue500
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Выполняется...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Grey600
                    )
                }
            }
        } else if (entry.output.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp, 4.dp, 12.dp, 12.dp),
                color = Grey100
            ) {
                Text(
                    text = entry.output,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Grey800,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
