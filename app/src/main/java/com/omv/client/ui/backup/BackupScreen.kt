package com.omv.client.ui.backup

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.data.repository.OmvRepository
import com.omv.client.data.security.SecurePrefs
import com.omv.client.ui.components.LoadingOverlay
import com.omv.client.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: OmvRepository,
    private val securePrefs: SecurePrefs,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isExporting = mutableStateOf(false)
    val isExporting: Boolean get() = _isExporting.value

    private val _isImporting = mutableStateOf(false)
    val isImporting: Boolean get() = _isImporting.value

    private val _message = mutableStateOf<String?>(null)
    val message: String? get() = _message.value

    private val _isSuccess = mutableStateOf<Boolean?>(null)
    val isSuccess: Boolean? get() = _isSuccess.value

    val lastBackupDate: String get() = securePrefs.lastBackupDate

    fun clearMessage() {
        _message.value = null
        _isSuccess.value = null
    }

    fun exportBackup() {
        viewModelScope.launch {
            _isExporting.value = true
            _message.value = null
            _isSuccess.value = null

            repository.getBackupConfig()
                .onSuccess { config ->
                    withContext(Dispatchers.IO) {
                        try {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                            val dateStr = dateFormat.format(Date())
                            val fileName = "omv_backup_$dateStr.json"

                            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                            dir?.mkdirs()
                            val file = File(dir, fileName)
                            file.writeText(config)

                            securePrefs.lastBackupDate = dateStr.replace("_", " ").replace("-", ".")
                            _message.value = "Бэкап сохранён: $fileName"
                            _isSuccess.value = true
                        } catch (e: Exception) {
                            _message.value = "Ошибка сохранения: ${e.message}"
                            _isSuccess.value = false
                        }
                    }
                }
                .onFailure { e ->
                    _message.value = "Ошибка API: ${e.message}"
                    _isSuccess.value = false
                }

            _isExporting.value = false
        }
    }

    fun importBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _message.value = null
            _isSuccess.value = null

            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("Не удалось открыть файл")

                    val fileName = getFileName(context, uri) ?: "backup.json"

                    val content = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    repository.restoreConfig(content)
                        .onSuccess {
                            _message.value = "Настройки восстановлены из: $fileName"
                            _isSuccess.value = true
                        }
                        .onFailure { e ->
                            _message.value = "Ошибка восстановления: ${e.message}"
                            _isSuccess.value = false
                        }
                } catch (e: Exception) {
                    _message.value = "Ошибка чтения файла: ${e.message}"
                    _isSuccess.value = false
                }
            }

            _isImporting.value = false
        }
    }

    private fun getFileName(context: Context, uri: android.net.Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Резервное копирование", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .then(
                                    Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Blue500,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Экспорт настроек",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "OMV на устройство",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Grey600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (viewModel.lastBackupDate.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Green500.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Green500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Последний бэкап: ${viewModel.lastBackupDate}",
                                    fontSize = 13.sp,
                                    color = Green500
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = { viewModel.exportBackup() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue500),
                        enabled = !viewModel.isExporting
                    ) {
                        if (viewModel.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (viewModel.isExporting) "Создание..." else "Создать бэкап",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Orange500,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Импорт настроек",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "OMV с устройства",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Grey600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isImporting
                    ) {
                        if (viewModel.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (viewModel.isImporting) "Восстановление..." else "Выбрать файл",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (viewModel.isExporting || viewModel.isImporting) {
            LoadingOverlay()
        }
    }
}
